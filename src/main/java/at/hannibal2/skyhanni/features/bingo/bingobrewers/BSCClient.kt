package at.hannibal2.skyhanni.features.bingo.bingobrewers

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.features.bingo.bingobrewers.BingoBrewersClient.isEnabled
import at.hannibal2.skyhanni.features.bingo.bingonet.SplashManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.constants.StatusConstants
import de.hype.bingonet.shared.objects.SplashData
import de.hype.bingonet.shared.objects.SplashLocations
import kotlinx.coroutines.Job
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object BSCClient {
    val config get() = SkyHanniMod.feature.event.bingo.bingoNetworks
    val enabled get() = config.useBSC
    private val splashIdCounter = AtomicInteger(1)

    @Volatile
    private var connectJob: Job? = null

    @Volatile
    var client: Socket? = null

    @Volatile
    var thread: Thread? = null

    @HandleEvent
    fun event(event: ConfigLoadEvent) {
        init()
    }

    init {
        init()
    }

    fun init() {
        if (enabled) {
            asyncConnect()
        } else {
            stop()
        }
    }

    fun asyncConnect() {
        if (client?.isConnected == true) return
        val existing = connectJob
        if (existing != null && existing.isActive) return
        connectJob = SkyHanniMod.launchCoroutine("BSC Connect", 5.minutes) {
            connect()
        }.also { job ->
            job.invokeOnCompletion { connectJob = null }
        }
    }

    @Throws(IOException::class)
    private fun connect() {
        stopInternal()
        if (!isEnabled()) {
            ChatUtils.chatAndOpenConfig(
                "Bingo Splash Community is not enabled right now. Please enable it first,",
                SkyHanniMod.feature
                    .event.bingo
                    .bingoNetworks::useBSC,
            )
            return
        }
        val client = try {
            Socket().apply {
                soTimeout = 10_000
                connect(InetSocketAddress("bsn.morazzer.dev", 1807), 10_000)
            }
        } catch (e: SocketTimeoutException) {
            // Server unreachable — don't log as error, just silently bail. connectJob completion
            // will clear itself; the user can retry with /bsc reconnect.
            return
        } catch (e: IOException) {
            // Any other connection failure — also not worth spamming the user.
            return
        }
        this.client = client
        if (client.isConnected) ChatUtils.chat("§aSuccessfully connected to BSC Server")
        thread = Thread {
            while (!Thread.currentThread().isInterrupted && client.isConnected) {
                val inputStream = client.getInputStream()
                val buffer = ByteArray(1024)
                val read = try {
                    inputStream.read(buffer)
                } catch (_: SocketTimeoutException) {
                    continue // re-check loop conditions without parking a thread forever
                }
                if (read == -1) {
                    ChatUtils.clickableChat(
                        "Error trying to connect to BSC Server",
                        onClick = {
                            asyncConnect()
                        },
                    )
                    BSCClient.client?.close()
                    return@Thread
                }
                val id = buffer[0]
                if (id == 0.toByte()) {
                    //Ignore
                } else if (id == 1.toByte()) {
                    val message = String(buffer, 1, read - 1)
                        .replace("<@&[0-9]+>".toRegex(), "")
                    val serverId = Pattern.compile("((mini|mega)[0-9]+[a-z])", Pattern.CASE_INSENSITIVE).matcher(message).let {
                        it.find()
                        it.group(1)
                    }
                    val hubNumber = Pattern.compile("(hub ([0-9]{1,2}))", Pattern.CASE_INSENSITIVE).matcher(message).let {
                        it.find()
                        it.group(2)?.toIntOrNull()
                    }
                    val island = if (message.contains("dhub", true) || message.contains("dungeon")) Islands.DUNGEON_HUB else Islands.HUB
                    val hubSelectorData = if (hubNumber != null) SplashData.HubSelectorData(hubNumber, island) else null
                    if (hubSelectorData == null) continue
                    val sploosh = SplashData(
                        announcer = "One of the BSC Splashers",
                        locationInHub = SplashLocations.KAT,
                        extraMessage = message,
                        lessWaste = false,
                        serverID = serverId,
                        hubSelectorData = hubSelectorData,
                        status = StatusConstants.WAITING,
                        funder = null,
                    ).also { it.splashId = -splashIdCounter.incrementAndGet() }
                    SplashManager.addSplashAndDisplay(sploosh, SplashManager.SplashSource.BSC)
                } else if (id == 2.toByte()) {
                    ChatUtils.chat("§cConnection limit on BSC server reached. Try again later with /bsc reconnect")
                    BSCClient.client?.close()
                    return@Thread
                }
            }
        }
        thread?.start()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("bsc") {
            description = "Copies information about the item in hand to the clipboard"
            category = CommandCategory.USERS_ACTIVE
            literalCallback("reconnect") {
                ChatUtils.chat("§eReconnecting to BSC Server...")
                asyncConnect()
            }
            literalCallback("stop") {
                stop()
            }
            literalCallback("state") {
                if (client?.isConnected == true) {
                    ChatUtils.chat("§aConnected to BSC Server")
                } else {
                    ChatUtils.chat("§cNot connected to BSC Server")
                }
            }
        }
    }

    private fun stopInternal() {
        if (client?.isConnected == true) ChatUtils.chat("§eDisconnecting from BSC Server...")
        connectJob?.cancel()
        connectJob = null
        client?.close()
        client = null
        thread?.interrupt()
        thread = null
    }

    fun stop() {
        SkyHanniMod.launchCoroutine("BSC Disconnect") {
            stopInternal()
        }
    }
}
