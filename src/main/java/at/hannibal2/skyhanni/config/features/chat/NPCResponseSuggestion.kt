package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.compat.execute
import net.minecraft.network.chat.ClickEvent

@SkyHanniModule
object NPCResponseSuggestion {
    val commandPrefixes = listOf("/selectnpcoption", "/chatprompt")

    @HandleEvent
    fun onMessage(event: SkyHanniChatEvent.Modify) {
        SkyHanniMod.launchCoroutine("NPC Response Suggestion Message Analyser") {
            val validComponents = event.chatComponent.siblings.filter { sib ->
                val event = sib.style.clickEvent
                if (event is ClickEvent.RunCommand) {
                    commandPrefixes.any { prefix ->
                        event.command.startsWith(prefix)
                    }
                } else if (event is ClickEvent.SuggestCommand) {
                    commandPrefixes.any { prefix ->
                        event.command.startsWith(prefix)
                    }
                } else if (event is ClickEvent.Custom) {
                    event.id.toString().contains("dialogue_response")
                } else {
                    false
                }
            }.let {
                val filtered = it.filter { it.string.contains("§a") }
                return@let filtered.ifEmpty { it }
            }
            if (validComponents.isEmpty()) return@launchCoroutine
            val components = validComponents.first()
            val event = components.style.clickEvent ?: return@launchCoroutine
            ChatUtils.chatPrompt(
                "Press §a%KEY%§e to respond with \"${components.string.trim()}§r§e\"", SkyHanniMod.feature.chat.npcResponseSuggestion,
                {
                    event.execute()
                },
            )
        }
    }
}
