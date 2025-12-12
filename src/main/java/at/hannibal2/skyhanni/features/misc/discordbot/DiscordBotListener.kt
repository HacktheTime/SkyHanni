package at.hannibal2.skyhanni.features.misc.discordbot

import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

object DiscordBotListener : ListenerAdapter() {

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        val message = event.retrieveMessage().complete()
        if (message.author == event.jda.selfUser && event.emoji.asReactionCode == ":recycle:") {
            message.delete().queue()
        }
    }
}
