package at.hannibal2.skyhanni.features.tutorial.handlers.steps.item

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.itemstep.ReforgeTutorialStep

class ReforgeTutorialStepHandler : TutorialStepHandler<ReforgeTutorialStep> {
    override fun getStepName(step: ReforgeTutorialStep, tutorial: Tutorial): String = "TODO: Implement"
    override fun getStepDescription(step: ReforgeTutorialStep, tutorial: Tutorial): String? = null
    override fun getRequirements(step: ReforgeTutorialStep): List<TutorialNode> = emptyList()
    
    companion object {
        private val activeSteps = mutableSetOf<ReforgeTutorialStep>()
    }
}
