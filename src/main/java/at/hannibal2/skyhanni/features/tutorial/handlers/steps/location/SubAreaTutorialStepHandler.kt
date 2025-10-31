package at.hannibal2.skyhanni.features.tutorial.handlers.steps.location

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialStepLogic
import de.hype.bingonet.shared.tutorials.steps.location.SubAreaTutorialStep

class SubAreaTutorialStepHandler : TutorialStepHandler<SubAreaTutorialStep> {
    
    override fun getStepName(step: SubAreaTutorialStep, tutorial: Tutorial): String {
        return "Go to ${step.area}"
    }
    
    override fun getStepDescription(step: SubAreaTutorialStep, tutorial: Tutorial): String? {
        return null
    }
    
    override fun getRequirements(step: SubAreaTutorialStep): List<TutorialNode> {
        return emptyList()
    }
    
    override fun onActivate(step: SubAreaTutorialStep, tutorial: Tutorial) {
        if (HypixelData.skyBlockArea == step.area) {
            TutorialStepLogic.complete(step)
        }
        activeSteps.add(step)
    }
    
    override fun onDeactivate(step: SubAreaTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    override fun onReset(step: SubAreaTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        activeSteps.forEach { step ->
            if (!ignoreEvent(step) && HypixelData.skyBlockArea == step.area) {
                TutorialStepLogic.complete(step)
            }
        }
    }
    
    companion object {
        private val activeSteps = mutableSetOf<SubAreaTutorialStep>()
    }
}
