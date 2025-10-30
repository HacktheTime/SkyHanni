package at.hannibal2.skyhanni.features.tutorial.handlers.steps.location

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import at.hannibal2.skyhanni.utils.LocationUtils
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic
import de.hype.bingonet.shared.tutorials.steps.location.GoToPositionTutorialStep

class GoToPositionTutorialStepHandler : TutorialStepHandler<GoToPositionTutorialStep> {
    
    override fun getStepName(step: GoToPositionTutorialStep, tutorial: Tutorial): String {
        return "Go to position on ${step.island.getDisplayName()}"
    }
    
    override fun getStepDescription(step: GoToPositionTutorialStep, tutorial: Tutorial): String? {
        return null
    }
    
    override fun getRequirements(step: GoToPositionTutorialStep): List<TutorialNode> {
        return emptyList()
    }
    
    override fun onActivate(step: GoToPositionTutorialStep, tutorial: Tutorial) {
        activeSteps.add(step)
    }
    
    override fun onDeactivate(step: GoToPositionTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    override fun onReset(step: GoToPositionTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        activeSteps.forEach { step ->
            if (!ignoreEvent(step)) {
                // Check if player is near target position
                step.node?.lastOrNull()?.let { targetPos ->
                    if (LocationUtils.playerLocation().distance(targetPos.toLorenzVec()) < 5.0) {
                        TutorialStepLogic.complete(step)
                    }
                }
            }
        }
    }
    
    companion object {
        private val activeSteps = mutableSetOf<GoToPositionTutorialStep>()
    }
}
