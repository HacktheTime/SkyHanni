package at.hannibal2.skyhanni.features.tutorial.handlers.steps.misc

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.misc.CakeTutorialStep

class CakeTutorialStepHandler : TutorialStepHandler<CakeTutorialStep> {
    override fun getStepName(step: CakeTutorialStep, tutorial: Tutorial): String = "Cake step"
    override fun getStepDescription(step: CakeTutorialStep, tutorial: Tutorial): String? = null
    override fun getRequirements(step: CakeTutorialStep): List<TutorialNode> = emptyList()
    
    companion object {
        private val activeSteps = mutableSetOf<CakeTutorialStep>()
    }
}
