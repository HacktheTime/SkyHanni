package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode

/**
 * Logic handler for TutorialFork operations.
 * Separates business logic from data structure.
 */
object TutorialForkLogic {

    /**
     * Populate node IDs for all internal nodes in the fork
     */
    fun populateNodeIds(fork: TutorialFork, tutorial: Tutorial) {
        fork.getAllInternalNodes().forEach { it.populateNodeIds(tutorial) }
    }

    /**
     * Refresh all internal nodes in the fork
     */
    fun refresh(fork: TutorialFork, tutorial: Tutorial) {
        fork.getAllInternalNodes().forEach { it.refresh(tutorial) }
    }

    /**
     * Reset all internal nodes in the fork
     */
    fun reset(fork: TutorialFork, tutorial: Tutorial) {
        fork.getAllInternalNodes().forEach { it.reset(tutorial) }
    }

    /**
     * Validate all internal nodes in the fork
     */
    fun validate(fork: TutorialFork, tutorial: Tutorial): List<String> {
        return fork.getAllInternalNodes().flatMap { it.validate(tutorial) }
    }
}
