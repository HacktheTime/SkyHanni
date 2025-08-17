package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class RequireAsyncCompletionTutorialStep(node: TutorialNode) : TutorialStep() {
    val nodeId: String = node.nodeId

    fun getNodeReference(tutorial: Tutorial){
        tutorial.getNodeReference(nodeId)
    }

    override fun getStepName(): String {
        return "Require Async Completion"
    }

    override fun getStepDescription(): String? {
        TODO("Not yet implemented")
    }
}
