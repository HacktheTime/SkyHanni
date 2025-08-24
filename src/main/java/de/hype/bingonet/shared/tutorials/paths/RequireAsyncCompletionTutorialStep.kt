package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class RequireAsyncCompletionTutorialStep(node: TutorialNode) : TutorialStep() {
    val toToCompleteGoalId: String = node.nodeId

    fun getNodeReference(tutorial: Tutorial): TutorialNode {
        return tutorial.getNodeReference(toToCompleteGoalId)?:error("$toToCompleteGoalId not found")
    }

    override fun getStepName(tutorial: Tutorial): String {
        return "Awaiting completion of ${getNodeReference(tutorial)}"
    }

    override fun getStepDescription(tutorial: Tutorial): String? {
        return null
    }

    override fun showOnActive(): Boolean = false

    override fun isComplete(tutorial: Tutorial): Boolean {
        return getNodeReference(tutorial).isComplete(tutorial)
    }
}
