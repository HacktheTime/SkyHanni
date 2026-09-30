package at.hannibal2.skyhanni.api.hypixeltools

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.MojangUtils
import com.google.gson.Gson
import de.hype.hypixeltools.canonical.CanonicalEntityId
import de.hype.hypixeltools.canonical.CanonicalItemRecord
import de.hype.hypixeltools.canonical.CanonicalRepositoryClient
import de.hype.hypixeltools.canonical.ObservedNpc
import de.hype.hypixeltools.data.SkyblockItemReference
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant

/**
 * SkyHanni's boundary to the typed HypixelTools repository.
 *
 * Legacy NEU data remains available to features that have not migrated yet,
 * but new observations are written only through this boundary.
 */
object HypixelToolsRepository : SkyblockItemRepository {
    private val gson = Gson()
    private var client: CanonicalRepositoryClient? = null
    private var configuredDirectory: String? = null
    private var authenticationToken: String? = null
    private var authenticationEndpoint: String? = null

    private fun client(): CanonicalRepositoryClient? {
        val config = SkyHanniMod.feature.dev.hypixelTools
        if (!config.enabled) return null
        if (configuredDirectory != config.repositoryDirectory) {
            configuredDirectory = config.repositoryDirectory
            client = CanonicalRepositoryClient(File(config.repositoryDirectory))
        }
        return client
    }

    override fun find(id: SkyblockItemReference) = client()?.findItem(id)

    override fun findById(id: ItemId): SkyblockItemReference? =
        client()?.itemReference(CanonicalEntityId(normalizeId(id.value)))

    override fun displayName(item: SkyblockItemReference): String? = find(item)?.displayName

    @Deprecated("Use typed ItemId/SkyblockItemRepository APIs.")
    fun isKnownItem(itemId: String): Boolean = isKnownItem(ItemId(itemId))

    fun isKnownItem(itemId: ItemId): Boolean = findById(itemId) != null

    fun observeItem(itemId: String, displayName: String): Boolean {
        val config = SkyHanniMod.feature.dev.hypixelTools
        if (!config.allowAutomaticUpdates) return false
        val result = client()?.observeItem(CanonicalEntityId(normalizeId(itemId)), displayName)
            ?: return false
        if (result.added && config.reportObservations) {
            report("ITEM", normalizeId(itemId), gson.toJson(mapOf("displayName" to displayName)))
        }
        return result.added
    }

    fun observeNpc(
        id: String,
        displayName: String,
        dialogue: List<String> = emptyList(),
        position: String? = null,
        island: String? = null,
        guiName: String? = null,
        inventory: List<String> = emptyList(),
    ): Boolean {
        val config = SkyHanniMod.feature.dev.hypixelTools
        if (!config.allowAutomaticUpdates) return false
        val changed = client()?.observeNpc(
            ObservedNpc(
                id = CanonicalEntityId(normalizeId(id)),
                displayName = displayName,
                dialogue = dialogue,
                position = position,
                island = island,
                guiName = guiName,
                inventory = inventory,
            ),
        ) ?: false
        if (changed && config.reportObservations) {
            report("NPC", normalizeId(id), gson.toJson(mapOf("displayName" to displayName, "dialogue" to dialogue, "guiName" to guiName)))
        }
        return changed
    }

    private fun normalizeId(value: String): String = value.trim().uppercase().replace(':', '-')

    private fun report(domain: String, entityKey: String, proposal: String) {
        val endpoint = SkyHanniMod.feature.dev.hypixelTools.contributionServerUrl.trimEnd('/')
        if (!endpoint.startsWith("https://", ignoreCase = true)) return
        val token = authenticate(endpoint) ?: return
        val connection = URI.create("$endpoint/api/v1/reports").toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            val body = gson.toJson(
                mapOf(
                    "domain" to domain,
                    "entityKey" to entityKey,
                    "proposal" to proposal,
                ),
            )
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) return
        } finally {
            connection.disconnect()
        }
    }

    private fun authenticate(endpoint: String): String? {
        if (authenticationEndpoint != endpoint) {
            authenticationEndpoint = endpoint
            authenticationToken = null
        }
        loadCachedGrant(endpoint)?.let {
            authenticationToken = it.token
            return it.token
        }
        return runCatching {
            val challengeConnection = URI.create("$endpoint/api/v1/auth/minecraft/challenge")
                .toURL().openConnection() as HttpURLConnection
            val challenge = try {
                challengeConnection.requestMethod = "POST"
                challengeConnection.connectTimeout = 5_000
                challengeConnection.readTimeout = 5_000
                challengeConnection.inputStream.bufferedReader().use { gson.fromJson(it, MinecraftChallenge::class.java) }
            } finally {
                challengeConnection.disconnect()
            }
            val clientNonceBytes = ByteArray(32).also(SecureRandom()::nextBytes)
            val clientNonce = clientNonceBytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
            val serverId = serverId(challenge.challengeId, challenge.serverNonce, clientNonce)
            MojangUtils.joinServer(serverId)
            val verifyConnection = URI.create("$endpoint/api/v1/auth/minecraft/verify")
                .toURL().openConnection() as HttpURLConnection
            try {
                verifyConnection.requestMethod = "POST"
                verifyConnection.connectTimeout = 5_000
                verifyConnection.readTimeout = 5_000
                verifyConnection.doOutput = true
                verifyConnection.setRequestProperty("Content-Type", "application/json")
                val request = gson.toJson(
                    mapOf(
                        "challengeId" to challenge.challengeId,
                        "clientNonce" to clientNonce,
                        "serverId" to serverId,
                        "username" to PlayerUtils.getName(),
                    ),
                )
                verifyConnection.outputStream.use { it.write(request.toByteArray(Charsets.UTF_8)) }
                if (verifyConnection.responseCode !in 200..299) return null
                val response = verifyConnection.inputStream.bufferedReader().use {
                    gson.fromJson(it, MinecraftVerification::class.java)
                }
                authenticationToken = response.token
                saveCachedGrant(endpoint, response)
                response.token
            } finally {
                verifyConnection.disconnect()
            }
        }.getOrNull()
    }

    private data class MinecraftChallenge(
        val challengeId: String,
        val serverNonce: String,
        val expiresAt: String,
        val protocolVersion: Int,
    )
    private data class MinecraftVerification(val token: String, val expiresAt: String)
    private data class CachedGrant(val token: String, val expiresAt: String)

    private fun grantFile(endpoint: String): File {
        val key = MessageDigest.getInstance("SHA-256").digest(endpoint.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return File("config/skyhanni", "hypixeltools-auth-$key.json")
    }

    private fun loadCachedGrant(endpoint: String): CachedGrant? {
        val file = grantFile(endpoint)
        if (!file.isFile) return null
        return runCatching {
            val grant = gson.fromJson(file.readText(), CachedGrant::class.java)
            if (Instant.parse(grant.expiresAt).isAfter(Instant.now())) grant else null
        }.getOrNull()
    }

    private fun saveCachedGrant(endpoint: String, response: MinecraftVerification) {
        val file = grantFile(endpoint)
        file.parentFile.mkdirs()
        file.writeText(gson.toJson(CachedGrant(response.token, response.expiresAt)))
    }

    private fun serverId(challengeId: String, serverNonce: String, clientNonce: String): String {
        val input = "1\u0000$challengeId\u0000$serverNonce\u0000$clientNonce".toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256").digest(input)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
