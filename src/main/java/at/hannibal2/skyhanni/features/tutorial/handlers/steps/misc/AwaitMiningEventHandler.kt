package at.hannibal2.skyhanni.features.tutorial.handlers.steps.misc

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.MiningEventType
import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.AwaitMiningEvent
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialStepLogic

class AwaitMiningEventHandler : TutorialStepHandler<AwaitMiningEvent> {
    
    override fun getStepName(step: AwaitMiningEvent, tutorial: Tutorial): String {
        return "Participate in ${step.event.displayName} on ${step.islands.joinToString(" or ") { it.getDisplayName() }}"
    }
    
    override fun getStepDescription(step: AwaitMiningEvent, tutorial: Tutorial): String? {
        return null
    }
    
    override fun getRequirements(step: AwaitMiningEvent): List<TutorialNode> {
        return emptyList()
    }
    
    override fun onActivate(step: AwaitMiningEvent, tutorial: Tutorial) {
        activeSteps.add(step)
    }
    
    override fun onDeactivate(step: AwaitMiningEvent, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    override fun onReset(step: AwaitMiningEvent, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    companion object {
        private val activeSteps = mutableSetOf<AwaitMiningEvent>()
    }
}
