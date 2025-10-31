package at.hannibal2.skyhanni.features.tutorial.handlers.steps.storage

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.storagestep.ObtainFromNEURecipeTutorialStep

class ObtainFromNEURecipeTutorialStepHandler : TutorialStepHandler<ObtainFromNEURecipeTutorialStep> {
    override fun getStepName(step: ObtainFromNEURecipeTutorialStep, tutorial: Tutorial): String = "ObtainFromNEURecipeTutorialStep step"
    override fun getStepDescription(step: ObtainFromNEURecipeTutorialStep, tutorial: Tutorial): String? = null
    override fun getRequirements(step: ObtainFromNEURecipeTutorialStep): List<TutorialNode> = emptyList()
    
    companion object {
        private val activeSteps = mutableSetOf<ObtainFromNEURecipeTutorialStep>()
    }
}
