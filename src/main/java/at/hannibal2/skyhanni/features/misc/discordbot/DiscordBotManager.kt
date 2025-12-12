package at.hannibal2.skyhanni.features.misc.discordbot

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.feature
import at.hannibal2.skyhanni.utils.ChatUtils
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair
import org.apache.http.NameValuePair
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import kotlin.concurrent.withLock
import java.util.concurrent.locks.ReentrantLock

object DiscordBotManager {
    val config get() = feature.discordBot

    val token get() = config.botToken.takeIf { it.isNotEmpty() && config.enable }

    // Lazy JDA - will be initialized on first access when token is set and feature enabled
    private val jda: JDA by lazy {
        val tok = token.let {
            if (it != null) return@let it
            if (config.enable) ChatUtils.clickableChat(
                "The Discord Bot Feature you have enabled requires setup. click here to open the setup " +
                    "screen.",
                onClick = {
                    SkyHanniMod.screenToOpen = DiscordBotSetupScreen()
                },
            )
            throw IllegalStateException("Discord bot token is not set or bot is disabled")
        }

        try {
            JDABuilder.create(
                tok,
                listOf(
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_PRESENCES,
                ),
            ).build()
        } catch (_: Throwable) {
            JDABuilder.createLight(tok).build()
        }.also { it.awaitReady()
            it.addEventListener(DiscordBotListener)
        }
    }

    val applicationInfo get() = jda.retrieveApplicationInfo().complete()
    val owner get() = applicationInfo.owner

    // In-memory cached developer token and expiry
    private val developerTokenLock = ReentrantLock()
    private var developerToken: String? = null
    private var developerTokenExpiry: Instant? = null

    private val gson = Gson()

    /**
     * Gets a valid developer oauth token (client credentials) using Apache HttpClient.
     * Caches the token in-memory until shortly before expiry.
     */
    fun getDeveloperOauthToken(): String {
        // quick check without lock
        developerToken?.let { token ->
            if (developerTokenExpiry?.isAfter(Instant.now().plusSeconds(10)) == true) {
                return token
            }
        }

        developerTokenLock.withLock {
            // check again inside lock
            developerToken?.let { token ->
                if (developerTokenExpiry?.isAfter(Instant.now().plusSeconds(10)) == true) {
                    return token
                }
            }

            val clientId = applicationInfo.id
            val clientSecret = config.clientSecret
            val params = listOf<NameValuePair>(
                BasicNameValuePair("grant_type", "client_credentials"),
                BasicNameValuePair("scope", "identify connections applications.commands.permissions.update"),
            )

            val httpClient: CloseableHttpClient = HttpClients.createDefault()
            val post = HttpPost("https://discord.com/api/v10/oauth2/token")
            post.setHeader(
                "Authorization",
                "Basic " + Base64.getEncoder()
                    .encodeToString(("$clientId:$clientSecret").toByteArray(StandardCharsets.UTF_8)),
            )
            post.setHeader("Content-Type", "application/x-www-form-urlencoded")
            val body = URLEncodedUtils.format(params, StandardCharsets.UTF_8)
            post.entity = StringEntity(body, ContentType.create("application/x-www-form-urlencoded", StandardCharsets.UTF_8))

            httpClient.use { client ->
                client.execute(post).use { response ->
                    val status = response.statusLine.statusCode
                    val respBody = response.entity?.content?.reader(StandardCharsets.UTF_8)?.readText().orEmpty()
                    if (status < 200 || status >= 300) {
                        throw RuntimeException("Failed to get token, status code: $status body: $respBody")
                    }

                    val json = gson.fromJson(respBody, JsonObject::class.java)
                    val accessToken = json.get("access_token").asString
                    val expiresIn = json.get("expires_in").asInt
                    developerToken = accessToken
                    developerTokenExpiry = Instant.now().plusSeconds(expiresIn.toLong())
                    return accessToken
                }
            }
        }
    }

    fun getJdaOrNull(): JDA? {
        if (!config.enable) return null
        return try {
            jda
        } catch (_: Throwable) {
            null
        }
    }

}
