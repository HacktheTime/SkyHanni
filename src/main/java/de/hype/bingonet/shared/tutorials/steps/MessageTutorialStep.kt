package de.hype.bingonet.shared.tutorials.steps

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.convertToUnformatted
import java.util.regex.Pattern

class MessageTutorialStep(val criteria: Pattern) : TutorialStep() {
    override fun getStepName(): String {
        TODO("Not yet implemented")
    }

    override fun getStepDescription(): String? {
        TODO("Not yet implemented")
    }

    @HandleEvent
    fun onMessageEvent(event: SkyHanniChatEvent) {
        if (isActive && Pattern.compile(criteria.pattern(), Pattern.CASE_INSENSITIVE).matches(event.message.convertToUnformatted())) {
            complete()
        }
    }
}
