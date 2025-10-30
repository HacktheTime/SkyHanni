package at.hannibal2.skyhanni.features.tutorial.handlers

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.paths.TutorialFork

/**
 * Base interface for fork-specific handlers.
 * Each fork type should have a handler implementing this interface.
 */
interface TutorialForkHandler<T : TutorialFork> {
    
    /**
     * Get the nodes of this fork based on current tutorial state
     */
    fun getNodes(fork: T, tutorial: Tutorial): List<TutorialNode>
    
    /**
     * Check if this fork is asynchronous
     */
    fun isAsync(fork: T): Boolean
    
    /**
     * Get all internal nodes (regardless of which path is active)
     */
    fun getAllInternalNodes(fork: T): List<TutorialNode>
    
    /**
     * Get the header text for this fork
     */
    fun getHeader(fork: T, tutorial: Tutorial): String
    
    /**
     * Check if the fork is complete
     */
    fun isComplete(fork: T, tutorial: Tutorial): Boolean
}
