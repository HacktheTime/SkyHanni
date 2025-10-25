package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Pure data class for async completion requirement steps.
 * Step that awaits completion of another node (referenced by nodeId).
 */
class RequireAsyncCompletionTutorialStep(
    val toToCompleteGoalId: String,
) : TutorialStep() {
    constructor(node: TutorialNode) : this(node.nodeId)
}
