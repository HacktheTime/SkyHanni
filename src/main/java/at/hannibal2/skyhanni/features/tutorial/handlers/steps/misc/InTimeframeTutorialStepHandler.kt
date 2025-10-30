package at.hannibal2.skyhanni.features.tutorial.handlers.steps.misc

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.misc.InTimeframeTutorialStep

class InTimeframeTutorialStepHandler : TutorialStepHandler<InTimeframeTutorialStep> {
    override fun getStepName(step: InTimeframeTutorialStep, tutorial: Tutorial): String = "InTimeframe step"
    override fun getStepDescription(step: InTimeframeTutorialStep, tutorial: Tutorial): String? = null
    override fun getRequirements(step: InTimeframeTutorialStep): List<TutorialNode> = emptyList()
    
    companion object {
        private val activeSteps = mutableSetOf<InTimeframeTutorialStep>()
    }
}
