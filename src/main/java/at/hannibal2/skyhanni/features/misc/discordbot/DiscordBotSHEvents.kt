package at.hannibal2.skyhanni.features.misc.discordbot

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.misc.DiscordBotConfig
import at.hannibal2.skyhanni.data.hypixel.chat.event.PlayerAllChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PrivateMessageChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.StringUtils.chunkAtLastNewline
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import de.hype.bingonet.shared.constants.Formatting
import kotlinx.coroutines.delay
import net.minecraft.client.Minecraft
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DiscordBotSHEvents {
    val jda get() = DiscordBotManager.getJdaOrNull()
    val dms get() = DiscordBotManager.owner.openPrivateChannel().complete()
    val config get() = SkyHanniMod.feature.discordBot
    val chatConfig get() = SkyHanniMod.feature.discordBot.chatConfig
    val username get() = MinecraftCompat.localPlayer.name

    val baseCheck: Boolean
        get() {
            if (jda == null) return false
            if (Minecraft.getMinecraft().inGameHasFocus) return false
            return true
        }

    @HandleEvent
    fun coopChat(event: PrivateMessageChatEvent) {
        if (baseCheck) return
        handleChat(event.message, chatConfig.coop)
    }

    @HandleEvent
    fun partyChat(event: PrivateMessageChatEvent) {
        if (baseCheck) return
        handleChat(event.message, chatConfig.party)
    }

    @HandleEvent
    fun guildChat(event: PrivateMessageChatEvent) {
        if (baseCheck) return
        handleChat(event.message, chatConfig.guild)
    }

    @HandleEvent
    fun msgChat(event: PrivateMessageChatEvent) {
        if (baseCheck) return
        handleChat(event.message, chatConfig.msg)
    }

    @HandleEvent
    fun allChat(event: PlayerAllChatEvent) {
        if (baseCheck) return
        handleChat(event.message, chatConfig.allChat)
    }

    @HandleEvent
    fun serverChat(event: SystemMessageEvent) {
        if (baseCheck) return
        handleChat(event.message, chatConfig.server)
    }

    @Volatile
    var immediate = false

    @Volatile
    var messageContentBuilder = StringBuilder()

    fun handleChat(message: String, behaviour: DiscordBotConfig.ChatConfig.PingBehaviour) {
        val mention = message.contains(username, ignoreCase = true) ||
            chatConfig.nickNames.any { message.contains(it, ignoreCase = true) }
        if (behaviour == DiscordBotConfig.ChatConfig.PingBehaviour.NOTHING && !mention) return
        messageContentBuilder.append(message).appendLine("§r")
        if (
            behaviour == DiscordBotConfig.ChatConfig.PingBehaviour.IMMEDIATE || mention
        ) {
            immediate = true
        }
        triggerSend()
    }

    @Volatile
    var groupTask: kotlinx.coroutines.Job? = null

    fun triggerSend() {
        val grouptime = chatConfig.groupTimeSeconds
        if (groupTask != null) {
            groupTask = SkyHanniMod.launchCoroutine("Discord Bot Message Grouping", (grouptime * 2).seconds) {
                delay(grouptime.seconds)
                sendNow()
                groupTask = null
            }
        }
        if (messageContentBuilder.length >= 1900) {
            groupTask?.cancel()
            SkyHanniMod.launchCoroutine("Discord Bot Message Immediate Send") {
                sendNow()
            }
        }
    }

    @Synchronized
    fun sendNow() {
        //Synchronized means it chunks quickly after the first send even if people send more messages shortly after.
        val original = messageContentBuilder
        messageContentBuilder = StringBuilder()
        val messagesToSend = original.toString().trim().let {
            if (chatConfig.useDiscordAnsi) {
                Formatting.covertToDiscordAnsi(it)
            } else it
        }.chunkAtLastNewline(1950)
        val lastIndex = messagesToSend.size - 1
        messagesToSend.forEachIndexed { index, content ->
            val isLast = index == lastIndex
            dms.sendMessage(
                """
                ```ansi
                $content
                ```
                """.trimIndent(),
            ).setSuppressedNotifications(isLast).complete()
        }
    }
}
