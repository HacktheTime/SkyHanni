package at.hannibal2.skyhanni.features.tutorial.handlers.steps.basic

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.*

class AwaitMiningEventHandler : TutorialStepHandler<MiningEventAwaitMiningEventAwaitMiningEvent> {
    override fun getStepName(step: MiningEventAwaitMiningEventAwaitMiningEvent, tutorial: Tutorial): String = "TODO"
    override fun getStepDescription(step: MiningEventAwaitMiningEventAwaitMiningEvent, tutorial: Tutorial): String? = null
    override fun getRequirements(step: MiningEventAwaitMiningEventAwaitMiningEvent): List<TutorialNode> = emptyList()
    
    companion object {
        private val activeSteps = mutableSetOf<MiningEventAwaitMiningEventAwaitMiningEvent>()
    }
}
