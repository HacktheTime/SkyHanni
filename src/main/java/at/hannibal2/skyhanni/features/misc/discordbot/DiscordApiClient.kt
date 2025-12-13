package at.hannibal2.skyhanni.features.misc.discordbot

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.apache.http.client.methods.HttpPatch
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import java.nio.charset.StandardCharsets

object DiscordApiClient {

    private val client: CloseableHttpClient = HttpClients.createDefault()
    private val gson = Gson()

    data class ApiResult(val success: Boolean, val statusCode: Int, val body: String)

    private fun buildAuthHeader(): String {
        val token = DiscordBotManager.getDeveloperOauthToken()
        return "Bearer $token"
    }
}
