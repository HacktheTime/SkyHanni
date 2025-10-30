package at.hannibal2.skyhanni.features.tutorial.handlers.steps.misc

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.misc.ObtainBingoGoalTutorialStep

class ObtainBingoGoalTutorialStepHandler : TutorialStepHandler<ObtainBingoGoalTutorialStep> {
    override fun getStepName(step: ObtainBingoGoalTutorialStep, tutorial: Tutorial): String = "ObtainBingoGoal step"
    override fun getStepDescription(step: ObtainBingoGoalTutorialStep, tutorial: Tutorial): String? = null
    override fun getRequirements(step: ObtainBingoGoalTutorialStep): List<TutorialNode> = emptyList()
    
    companion object {
        private val activeSteps = mutableSetOf<ObtainBingoGoalTutorialStep>()
    }
}
