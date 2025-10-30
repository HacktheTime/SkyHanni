package at.hannibal2.skyhanni.features.tutorial.handlers.steps

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.convertToUnformatted
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.MessageTutorialStep
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic
import java.util.regex.Pattern

/**
 * Handler for MessageTutorialStep.
 * Completes when a chat message matching the criteria pattern is received.
 */
class MessageTutorialStepHandler : TutorialStepHandler<MessageTutorialStep> {
    
    override fun getStepName(step: MessageTutorialStep, tutorial: Tutorial): String {
        return step.name
    }
    
    override fun getStepDescription(step: MessageTutorialStep, tutorial: Tutorial): String {
        return step.description
    }
    
    override fun getRequirements(step: MessageTutorialStep): List<TutorialNode> {
        return emptyList()
    }
    
    @HandleEvent
    fun onMessageEvent(event: SkyHanniChatEvent) {
        // Process all active MessageTutorialSteps
        // This is the efficient passive check - only processes when chat event occurs
        activeSteps.forEach { step ->
            if (!ignoreEvent(step)) {
                val pattern = Pattern.compile(step.criteria.pattern(), Pattern.CASE_INSENSITIVE)
                if (pattern.matches(event.message.convertToUnformatted())) {
                    TutorialStepLogic.complete(step)
                }
            }
        }
    }
    
    companion object {
        // Track active steps for efficient event processing
        private val activeSteps = mutableSetOf<MessageTutorialStep>()
        
        fun addActiveStep(step: MessageTutorialStep) {
            activeSteps.add(step)
        }
        
        fun removeActiveStep(step: MessageTutorialStep) {
            activeSteps.remove(step)
        }
    }
    
    override fun onActivate(step: MessageTutorialStep, tutorial: Tutorial) {
        addActiveStep(step)
    }
    
    override fun onDeactivate(step: MessageTutorialStep, tutorial: Tutorial) {
        removeActiveStep(step)
    }
    
    override fun onReset(step: MessageTutorialStep, tutorial: Tutorial) {
        removeActiveStep(step)
    }
}
