package at.hannibal2.skyhanni.features.tutorial.handlers.steps

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.TutorialNodeLogic
import de.hype.bingonet.shared.tutorials.paths.RequireAsyncCompletionTutorialStep

class RequireAsyncCompletionTutorialStepHandler : TutorialStepHandler<RequireAsyncCompletionTutorialStep> {
    
    fun getNodeReference(step: RequireAsyncCompletionTutorialStep, tutorial: Tutorial): TutorialNode {
        return tutorial.getNodeReference(step.toToCompleteGoalId) 
            ?: error("${step.toToCompleteGoalId} not found")
    }
    
    override fun getStepName(step: RequireAsyncCompletionTutorialStep, tutorial: Tutorial): String {
        return "Awaiting completion of ${getNodeReference(step, tutorial)}"
    }
    
    override fun getStepDescription(step: RequireAsyncCompletionTutorialStep, tutorial: Tutorial): String? {
        return null
    }
    
    override fun showOnActive(step: RequireAsyncCompletionTutorialStep): Boolean = false
    
    override fun getRequirements(step: RequireAsyncCompletionTutorialStep): List<TutorialNode> {
        return emptyList()
    }
    
    override fun check(step: RequireAsyncCompletionTutorialStep, tutorial: Tutorial): Boolean {
        return TutorialNodeLogic.isComplete(getNodeReference(step, tutorial), tutorial)
    }
}
