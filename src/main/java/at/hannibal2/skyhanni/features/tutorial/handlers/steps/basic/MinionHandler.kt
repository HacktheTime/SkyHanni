package at.hannibal2.skyhanni.features.tutorial.handlers.steps.basic

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.*

class MinionHandler : TutorialStepHandler<MinionMinion> {
    override fun getStepName(step: MinionMinion, tutorial: Tutorial): String = "TODO"
    override fun getStepDescription(step: MinionMinion, tutorial: Tutorial): String? = null
    override fun getRequirements(step: MinionMinion): List<TutorialNode> = emptyList()
    
    companion object {
        private val activeSteps = mutableSetOf<MinionMinion>()
    }
}
