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

    data class MessageDto(val channelId: String, val id: String, var content: String) {
        fun edit(newContent: String): ApiResult {
            val res = editMessage(channelId, id, newContent)
            if (res.success) this.content = newContent
            return res
        }
    }

    private fun buildAuthHeader(): String {
        val token = DiscordBotManager.getDeveloperOauthToken()
        return "Bearer $token"
    }

    fun sendMessage(channelId: String, content: String): MessageDto? {
        val url = "https://discord.com/api/v10/channels/$channelId/messages"
        val post = HttpPost(url)
        post.setHeader("Authorization", buildAuthHeader())
        post.setHeader("Content-Type", "application/json")
        val body = JsonObject().apply {
            addProperty("content", content)
        }
        post.entity = StringEntity(body.toString(), ContentType.APPLICATION_JSON)

        client.execute(post).use { resp ->
            val status = resp.statusLine.statusCode
            val respBody = resp.entity?.content?.reader(StandardCharsets.UTF_8)?.readText().orEmpty()
            if (status in 200..299) {
                val json = gson.fromJson(respBody, JsonObject::class.java)
                val messageId = json.get("id").asString
                val returnedContent = json.get("content")?.asString ?: content
                return MessageDto(channelId, messageId, returnedContent)
            } else {
                return null
            }
        }
    }

    fun editMessage(channelId: String, messageId: String, newContent: String): ApiResult {
        val url = "https://discord.com/api/v10/channels/$channelId/messages/$messageId"
        val patch = HttpPatch(url)
        patch.setHeader("Authorization", buildAuthHeader())
        patch.setHeader("Content-Type", "application/json")
        val body = JsonObject().apply {
            addProperty("content", newContent)
        }
        patch.entity = StringEntity(body.toString(), ContentType.APPLICATION_JSON)

        client.execute(patch).use { resp ->
            val status = resp.statusLine.statusCode
            val respBody = resp.entity?.content?.reader(StandardCharsets.UTF_8)?.readText().orEmpty()
            return ApiResult(status in 200..299, status, respBody)
        }
    }
}
