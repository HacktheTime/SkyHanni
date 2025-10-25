package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic

/**
 * Logic handler for TutorialFork operations.
 * Handles all business logic for tutorial forks, keeping the fork classes as pure data.
 */
object TutorialForkLogic {

    /**
     * Check if a fork is complete (default: check if all internal nodes are complete)
     */
    fun isComplete(fork: TutorialFork, tutorial: Tutorial): Boolean {
        return fork.getAllInternalNodes().all { node ->
            when (node) {
                is de.hype.bingonet.shared.tutorials.steps.TutorialStep -> TutorialStepLogic.isComplete(node, tutorial)
                is TutorialFork -> isComplete(node, tutorial)
                else -> false
            }
        }
    }

    /**
     * Populate node IDs for all internal nodes in the fork
     */
    fun populateNodeIds(fork: TutorialFork, tutorial: Tutorial) {
        fork.getAllInternalNodes().forEach { node ->
            when (node) {
                is de.hype.bingonet.shared.tutorials.steps.TutorialStep -> TutorialStepLogic.populateNodeIds(node, tutorial)
                is TutorialFork -> populateNodeIds(node, tutorial)
            }
        }
    }

    /**
     * Refresh all internal nodes in the fork
     */
    fun refresh(fork: TutorialFork, tutorial: Tutorial) {
        fork.getAllInternalNodes().forEach { node ->
            when (node) {
                is de.hype.bingonet.shared.tutorials.steps.TutorialStep -> TutorialStepLogic.refresh(node, tutorial)
                is TutorialFork -> refresh(node, tutorial)
            }
        }
    }

    /**
     * Reset all internal nodes in the fork
     */
    fun reset(fork: TutorialFork, tutorial: Tutorial) {
        fork.getAllInternalNodes().forEach { node ->
            when (node) {
                is de.hype.bingonet.shared.tutorials.steps.TutorialStep -> TutorialStepLogic.reset(node, tutorial)
                is TutorialFork -> reset(node, tutorial)
            }
        }
    }

    /**
     * Validate all internal nodes in the fork
     */
    fun validate(fork: TutorialFork, tutorial: Tutorial): List<String> {
        return fork.getAllInternalNodes().flatMap { node ->
            when (node) {
                is de.hype.bingonet.shared.tutorials.steps.TutorialStep -> TutorialStepLogic.validate(node, tutorial)
                is TutorialFork -> validate(node, tutorial)
                else -> emptyList()
            }
        }
    }
}
