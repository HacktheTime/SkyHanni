package de.hype.bingonet.shared.tutorials.paths

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialForkHandlerRegistry
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic

/**
 * Logic handler for TutorialFork operations.
 * Handles all business logic for tutorial forks, keeping the fork classes as pure data.
 * Delegates to type-specific handlers for fork-specific logic.
 */
object TutorialForkLogic {

    /**
     * Get nodes for this fork
     */
    fun getNodes(fork: TutorialFork, tutorial: Tutorial): List<TutorialNode> {
        return TutorialForkHandlerRegistry.getNodes(fork, tutorial)
    }

    /**
     * Check if a fork is complete (default: check if all internal nodes are complete)
     */
    fun isComplete(fork: TutorialFork, tutorial: Tutorial): Boolean {
        return TutorialForkHandlerRegistry.getAllInternalNodes(fork).all { node ->
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
        TutorialForkHandlerRegistry.getAllInternalNodes(fork).forEach { node ->
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
        TutorialForkHandlerRegistry.getAllInternalNodes(fork).forEach { node ->
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
        TutorialForkHandlerRegistry.getAllInternalNodes(fork).forEach { node ->
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
        return TutorialForkHandlerRegistry.getAllInternalNodes(fork).flatMap { node ->
            when (node) {
                is de.hype.bingonet.shared.tutorials.steps.TutorialStep -> TutorialStepLogic.validate(node, tutorial)
                is TutorialFork -> validate(node, tutorial)
                else -> emptyList()
            }
        }
    }
}
