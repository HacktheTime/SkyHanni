package at.hannibal2.skyhanni.features.misc.discordrpc

import at.hannibal2.skyhanni.utils.ChatUtils
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.AsynchronousCloseException
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * A lightweight Discord IPC client implementing the Rich Presence protocol.
 *
 * Manages handshake, presence updates, and clean disconnection over a [DiscordIPCPipe]
 * obtained from [DiscordIPCPipeManager].
 *
 * @param clientId The Discord application client ID (from the Discord Developer Portal).
 * @param onDebugInfo Called with diagnostic key-value pairs if pipe discovery fails.
 */
class DiscordIPC(
    private val clientId: Long,
    private val onDebugInfo: (Map<String, String>) -> Unit = {},
) : Closeable {

    @Volatile
    private var _connected = false
    private var pipe: DiscordIPCPipe? = null

    // Because IPC is separate from the mod, we need our own registered hook
    private var shutdownHook: Thread? = null

    // We need to use our own GSON here, rather than the ConfigManager one,
    // as we need NON-null serialization, as discord does not play nice with nulls.
    private val gson = com.google.gson.GsonBuilder().create()
    private val pendingFrameRequests = ConcurrentHashMap<String, CompletableFuture<String>>()

    /** Whether this client is currently connected and ready for Discord IPC. */
    val isConnected: Boolean get() = _connected
    private val clientPayload = """{"v":1,"client_id":"$clientId"}"""

    /**
     * Opens a pipe to Discord and performs the version-1 handshake.
     *
     * @throws DiscordIPCException If no Discord client is running, the pipe cannot be opened,
     *   or the handshake does not complete successfully.
     */
    fun connect(): DiscordUserIdentity? {
        pipe = DiscordIPCPipeManager.open(onDebugInfo)
        sendFrame(Opcode.HANDSHAKE, clientPayload)
        val (opcode, body) = readFrame()
        if (opcode != Opcode.FRAME) throw DiscordIPCException("Expected FRAME after handshake, got $opcode. Body: $body")
        _connected = true
        shutdownHook = Thread(::close, "discord-rpc-shutdown").also(Runtime.getRuntime()::addShutdownHook)
        return parseReadyUser(body)
    }

    fun requestCurrentUserIdentity(timeoutMillis: Long = 3_000): DiscordUserIdentity? {
        if (!_connected) throw DiscordIPCException("requestCurrentUserIdentity called while not connected")

        val commands = listOf(
            CommandPayload(cmd = "GET_CURRENT_USER", nonce = UUID.randomUUID().toString()),
            CommandPayload(cmd = "GET_USER", nonce = UUID.randomUUID().toString(), args = mapOf("id" to "@me")),
            CommandPayload(cmd = "GET_USER", nonce = UUID.randomUUID().toString(), args = mapOf("id" to "me")),
            CommandPayload(cmd = "GET_USER", nonce = UUID.randomUUID().toString()),
        )

        for (command in commands) {
            requestFrameAndAwait(command, timeoutMillis)?.let { parseUserFromCommandResponse(it) }?.let { return it }
        }
        return null
    }

    var lastActivityJson: String? = null
        private set

    /**
     * Updates the rich presence activity displayed on the user's Discord profile.
     *
     * @param presence The [DiscordRichPresence] to send. Null fields are omitted from the payload.
     * @throws DiscordIPCException If the client is not connected or writing to the pipe fails.
     */
    fun setActivity(presence: DiscordRichPresence) {
        if (!_connected) throw DiscordIPCException("setActivity called while not connected")
        val json = gson.toJson(buildActivityPayload(presence))
        lastActivityJson = json
        sendFrame(Opcode.FRAME, json)
    }

    /**
     * Sends a CLOSE frame to Discord and releases all pipe resources.
     * Safe to call when not connected; errors during the close frame are silently swallowed.
     */
    override fun close() {
        shutdownHook?.let { runCatching { Runtime.getRuntime().removeShutdownHook(it) } }
        shutdownHook = null
        if (_connected) runCatching { sendFrame(Opcode.CLOSE, clientPayload) }
        clearSessionState()
        runCatching { pipe?.close() }
        pipe = null
    }

    private enum class Opcode(val id: Int) {
        HANDSHAKE(0),
        FRAME(1),
        CLOSE(2),
        PING(3),
        PONG(4);

        companion object {
            fun fromId(id: Int) = entries.firstOrNull { it.id == id }
        }
    }

    /**
     * Writes a single framed IPC message to the pipe.
     *
     * Discord's IPC wire format: `[opcode: Int32LE][length: Int32LE][payload: UTF-8 bytes]`.
     *
     * Synchronized to guard against concurrent writes from the presence loop and [close].
     *
     * @throws DiscordIPCException If there is no active pipe connection or the write fails.
     */
    @Synchronized
    private fun sendFrame(opcode: Opcode, json: String) {
        val out = pipe?.output ?: throw DiscordIPCException("sendFrame called with no active connection")
        val bytes = json.toByteArray(Charsets.UTF_8)
        val frame = ByteBuffer.allocate(8 + bytes.size).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(opcode.id)
            .putInt(bytes.size)
            .put(bytes)
        try {
            out.write(frame.array())
            out.flush()
        } catch (e: IOException) {
            clearSessionState()
            throw DiscordIPCException("IPC write failed: ${e.message}", e)
        }
    }

    /**
     * Blocks reading frames from Discord until the connection is closed.
     *
     * Responds to [Opcode.PING] with [Opcode.PONG] to keep the connection alive.
     * Returns when [isConnected] becomes false, Discord sends a CLOSE frame, or a read error occurs.
     *
     * Call from a background coroutine alongside the presence-update loop.
     */
    internal fun readerLoop() {
        try {
            while (_connected) {
                val (opcode, body) = readFrame()
                when (opcode) {
                    Opcode.PING -> sendFrame(Opcode.PONG, body)
                    Opcode.CLOSE -> {
                        clearSessionState()
                        ChatUtils.debug("Discord RPC CLOSE frame: $body")
                    }
                    else -> {
                        if (opcode == Opcode.FRAME) tryCompletePendingRequest(body)
                        ChatUtils.debug("Discord RPC frame ($opcode): $body")
                    }
                }
            }
        } catch (_: DiscordIPCException) {
            clearSessionState()
        } catch (_: AsynchronousCloseException) {
            // Expected when a temporary probe client closes while the reader is blocked on input.
            clearSessionState()
        } catch (_: IOException) {
            clearSessionState()
        }
    }

    /**
     * Reads one framed IPC message from the pipe. Blocks until a full frame is available.
     *
     * Sets [isConnected] to false and throws if Discord closes the pipe mid-read.
     *
     * @return A pair of the received [Opcode] and its decoded JSON payload.
     * @throws DiscordIPCException If the connection is closed or an unrecognized opcode is received.
     */
    @Suppress("ThrowsCount")
    private fun readFrame(): Pair<Opcode, String> {
        val inp = pipe?.input ?: throw DiscordIPCException("readFrame called with no active connection")
        val header = inp.readNBytes(8)
        if (header.size < 8) {
            clearSessionState()
            throw DiscordIPCException("Discord closed the IPC pipe unexpectedly (EOF in frame header)")
        }
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val opcodeId = buffer.int
        val opcode = Opcode.fromId(opcodeId) ?: throw DiscordIPCException("Received unknown opcode: $opcodeId")
        val length = buffer.int
        return opcode to String(inp.readNBytes(length), Charsets.UTF_8)
    }

    /**
     * Builds the full `SET_ACTIVITY` JSON payload for the given [presence].
     *
     * Payload structure: `{ cmd, args: { pid, activity: { ... } }, nonce }`.
     */
    private fun buildActivityPayload(presence: DiscordRichPresence) = buildActivityPayload(
        presence,
        ProcessHandle.current().pid().toInt(),
        UUID.randomUUID().toString(),
    )

    private fun clearSessionState() {
        _connected = false
        val ex = DiscordIPCException("Discord IPC session closed")
        pendingFrameRequests.values.forEach { it.completeExceptionally(ex) }
        pendingFrameRequests.clear()
    }

    private fun requestFrameAndAwait(command: CommandPayload, timeoutMillis: Long): String? {
        val responseFuture = CompletableFuture<String>()
        pendingFrameRequests[command.nonce] = responseFuture
        try {
            sendFrame(Opcode.FRAME, gson.toJson(command))
            return responseFuture.get(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            return null
        } finally {
            pendingFrameRequests.remove(command.nonce)
        }
    }

    private fun tryCompletePendingRequest(body: String) {
        val frame = runCatching { gson.fromJson(body, DispatchFrame::class.java) }.getOrNull() ?: return
        val nonce = frame.nonce ?: return
        pendingFrameRequests[nonce]?.complete(body)
    }

    private fun parseReadyUser(body: String): DiscordUserIdentity? {
        val frame = runCatching { gson.fromJson(body, DispatchFrame::class.java) }.getOrNull() ?: return null
        if (frame.cmd != "DISPATCH" || frame.evt != "READY") return null
        val user = frame.data?.user ?: return null
        val userId = user.id ?: return null
        val username = user.username ?: user.globalName ?: return null
        return DiscordUserIdentity(userId = userId, username = username)
    }

    private fun parseUserFromCommandResponse(body: String): DiscordUserIdentity? {
        val json = runCatching { gson.fromJson(body, JsonObject::class.java) }.getOrNull() ?: return null
        val cmd = json.getStringOrNull("cmd")
        if (cmd == "ERROR") return null

        val data = json.getObjectOrNull("data") ?: return null
        val directUserId = data.getStringOrNull("id")
        val directUsername = data.getStringOrNull("username") ?: data.getStringOrNull("global_name")
        if (directUserId != null && directUsername != null) {
            return DiscordUserIdentity(userId = directUserId, username = directUsername)
        }

        val nestedUser = data.getObjectOrNull("user") ?: return null
        val nestedUserId = nestedUser.getStringOrNull("id") ?: return null
        val nestedUsername = nestedUser.getStringOrNull("username") ?: nestedUser.getStringOrNull("global_name") ?: return null
        return DiscordUserIdentity(userId = nestedUserId, username = nestedUsername)
    }

    private fun JsonObject.getObjectOrNull(key: String): JsonObject? {
        val element = get(key) ?: return null
        if (element.isJsonNull || !element.isJsonObject) return null
        return element.asJsonObject
    }

    private fun JsonObject.getStringOrNull(key: String): String? {
        val element = get(key) ?: return null
        if (element.isJsonNull) return null
        return runCatching { element.asString }.getOrNull()
    }

    private data class CommandPayload(
        val cmd: String,
        val nonce: String,
        val args: Map<String, String>? = null,
    )

    private data class DispatchFrame(
        val cmd: String? = null,
        val evt: String? = null,
        val nonce: String? = null,
        val data: DispatchData? = null,
    )

    private data class DispatchData(
        val user: DispatchUser? = null,
    )

    private data class DispatchUser(
        val id: String? = null,
        val username: String? = null,
        @SerializedName("global_name") val globalName: String? = null,
    )
}
