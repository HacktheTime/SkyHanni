package at.hannibal2.skyhanni.features.tutorial.logic

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.paths.TutorialFork
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Utility object for handling TutorialNode operations polymorphically.
 * Routes calls to the appropriate Logic handler based on node type.
 */
object TutorialNodeLogic {

    fun isComplete(node: TutorialNode, tutorial: Tutorial): Boolean {
        return when (node) {
            is TutorialStep -> TutorialStepLogic.isComplete(node, tutorial)
            is TutorialFork -> TutorialForkLogic.isComplete(node, tutorial)
            else -> false
        }
    }

    fun reset(node: TutorialNode, tutorial: Tutorial) {
        when (node) {
            is TutorialStep -> TutorialStepLogic.reset(node, tutorial)
            is TutorialFork -> TutorialForkLogic.reset(node, tutorial)
        }
    }

    fun refresh(node: TutorialNode, tutorial: Tutorial) {
        when (node) {
            is TutorialStep -> TutorialStepLogic.refresh(node, tutorial)
            is TutorialFork -> TutorialForkLogic.refresh(node, tutorial)
        }
    }

    fun populateNodeIds(node: TutorialNode, tutorial: Tutorial) {
        when (node) {
            is TutorialStep -> TutorialStepLogic.populateNodeIds(node, tutorial)
            is TutorialFork -> TutorialForkLogic.populateNodeIds(node, tutorial)
        }
    }

    fun validate(node: TutorialNode, tutorial: Tutorial): List<String> {
        return when (node) {
            is TutorialStep -> TutorialStepLogic.validate(node, tutorial)
            is TutorialFork -> TutorialForkLogic.validate(node, tutorial)
            else -> emptyList()
        }
    }
}
