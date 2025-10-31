package at.hannibal2.skyhanni.features.tutorial.handlers.steps.requirement

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.requirement.ObtainCoinsTutorialStep

class ObtainCoinsTutorialStepHandler : TutorialStepHandler<ObtainCoinsTutorialStep> {
    override fun getStepName(step: ObtainCoinsTutorialStep, tutorial: Tutorial): String = "ObtainCoinsTutorialStep step"
    override fun getStepDescription(step: ObtainCoinsTutorialStep, tutorial: Tutorial): String? = null
    override fun getRequirements(step: ObtainCoinsTutorialStep): List<TutorialNode> = emptyList()
    
    companion object {
        private val activeSteps = mutableSetOf<ObtainCoinsTutorialStep>()
    }
}
