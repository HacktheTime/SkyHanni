package de.hype.bingonet.shared.tutorials.steps

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.convertToUnformatted
import de.hype.bingonet.shared.tutorials.Tutorial
import java.util.regex.Pattern

class MessageTutorialStep(
    val criteria: Pattern,
    val name: String,
    val description: String
) : TutorialStep() {
    override fun getStepName(): String {
        return name
    }

    override fun getStepDescription(tutorial: Tutorial): String {
       return description
    }

    @HandleEvent
    fun onMessageEvent(event: SkyHanniChatEvent) {
        if (isActive && Pattern.compile(criteria.pattern(), Pattern.CASE_INSENSITIVE).matches(event.message.convertToUnformatted())) {
            complete()
        }
    }
}
