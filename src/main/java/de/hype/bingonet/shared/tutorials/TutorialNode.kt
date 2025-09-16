package de.hype.bingonet.shared.tutorials

import de.hype.bingonet.shared.tutorials.steps.TutorialStep

abstract class TutorialNode {

    lateinit var nodeId: String
        protected set

    abstract fun populateNodeIds(tutorial: Tutorial)
    abstract fun isComplete(tutorial: Tutorial): Boolean
    abstract fun reset(tutorial: Tutorial)
    abstract fun refresh(tutorial: Tutorial)

    /**
     * Validate this node's configuration. Return a list of human-readable error strings, empty if valid.
     * Implementations should also validate sub-nodes if applicable.
     */
    open fun validate(tutorial: Tutorial): List<String> = emptyList()
}
