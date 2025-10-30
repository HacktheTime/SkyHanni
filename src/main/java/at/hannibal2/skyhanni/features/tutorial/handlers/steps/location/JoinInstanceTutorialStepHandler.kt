package at.hannibal2.skyhanni.features.tutorial.handlers.steps.location

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic
import de.hype.bingonet.shared.tutorials.steps.location.JoinInstanceTutorialStep

class JoinInstanceTutorialStepHandler : TutorialStepHandler<JoinInstanceTutorialStep> {
    
    override fun getStepName(step: JoinInstanceTutorialStep, tutorial: Tutorial): String {
        return "Join ${step.instance.displayName}"
    }
    
    override fun getStepDescription(step: JoinInstanceTutorialStep, tutorial: Tutorial): String? {
        return null
    }
    
    override fun getRequirements(step: JoinInstanceTutorialStep): List<TutorialNode> {
        return emptyList()
    }
    
    override fun onActivate(step: JoinInstanceTutorialStep, tutorial: Tutorial) {
        if (HypixelData.serverId?.startsWith(step.instance.internalName) == true) {
            TutorialStepLogic.complete(step)
        }
        activeSteps.add(step)
    }
    
    override fun onDeactivate(step: JoinInstanceTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    override fun onReset(step: JoinInstanceTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        activeSteps.forEach { step ->
            if (!ignoreEvent(step) && HypixelData.serverId?.startsWith(step.instance.internalName) == true) {
                TutorialStepLogic.complete(step)
            }
        }
    }
    
    companion object {
        private val activeSteps = mutableSetOf<JoinInstanceTutorialStep>()
    }
}
