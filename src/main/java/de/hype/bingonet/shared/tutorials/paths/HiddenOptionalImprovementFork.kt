package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialCondition
import de.hype.bingonet.shared.tutorials.TutorialNode

/**
 * A fork that
 */
class HiddenOptionalImprovementFork(
    val default: List<TutorialNode>,
    val improved: List<TutorialNode>,
    val condition: TutorialCondition,
) : TutorialFork() {
    override fun getNodes(tutorial: Tutorial): List<TutorialNode> {
        if (condition.matches(tutorial)){
            return improved
        }
        else {
            return default
        }
    }

    override fun isAsync(): Boolean {
        return false
    }

    override fun getAllInternalNodes(): List<TutorialNode> {
        return default + improved
    }

    override fun isComplete(tutorial: Tutorial): Boolean {
        return getNodes(tutorial).all { it.isComplete(tutorial) }
    }

    override fun getHeader(tutorial: Tutorial): String = "Optional Improvement"

    override fun refresh(tutorial: Tutorial) {
        getAllInternalNodes().forEach { it.refresh(tutorial) }
    }
}
