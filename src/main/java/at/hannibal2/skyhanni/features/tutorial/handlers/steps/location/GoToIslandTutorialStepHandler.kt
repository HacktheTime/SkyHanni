package at.hannibal2.skyhanni.features.tutorial.handlers.steps.location

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.toBNIsland
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import at.hannibal2.skyhanni.utils.HypixelCommands
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialStepLogic
import de.hype.bingonet.shared.tutorials.steps.location.GoToIslandTutorialStep

class GoToIslandTutorialStepHandler : TutorialStepHandler<GoToIslandTutorialStep> {
    
    override fun getStepName(step: GoToIslandTutorialStep, tutorial: Tutorial): String {
        return "Travel to ${step.island.getDisplayName()}"
    }
    
    override fun getStepDescription(step: GoToIslandTutorialStep, tutorial: Tutorial): String? {
        return null
    }
    
    override fun getRequirements(step: GoToIslandTutorialStep): List<TutorialNode> {
        return emptyList()
    }
    
    override fun onActivate(step: GoToIslandTutorialStep, tutorial: Tutorial) {
        if (HypixelData.skyBlockIsland.toBNIsland() == step.island) {
            TutorialStepLogic.complete(step)
        }
        TutorialStepLogic.chatPromptSuggestion("Next Task: Travel to ${step.island.getDisplayName()}. Warp now?") {
            step.island.warpArgument?.let {
                HypixelCommands.warp(it)
            }
        }
        activeSteps.add(step)
    }
    
    override fun onDeactivate(step: GoToIslandTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    override fun onReset(step: GoToIslandTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        activeSteps.forEach { step ->
            if (!ignoreEvent(step) && event.newIsland.toBNIsland() == step.island) {
                TutorialStepLogic.complete(step)
            }
        }
    }
    
    companion object {
        private val activeSteps = mutableSetOf<GoToIslandTutorialStep>()
    }
}
