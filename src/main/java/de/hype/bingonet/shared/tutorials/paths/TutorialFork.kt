package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode

abstract class TutorialFork() : TutorialNode() {
    /**
     * Nodes of this fork.
     */
    abstract fun getNodes(tutorial: Tutorial): List<TutorialNode>

    /**
     * If this fork is asynchronous this means that its node are splitting off from the main branch and multiple nodes can be active at the same time.
     * This is useful when waiting for something like a Mining Event before you can continue effectively
     */
    abstract fun isAsync(): Boolean

    abstract fun getAllInternalNodes(): List<TutorialNode>

    /**
     * Header text to display for this fork in UIs.
     */
    abstract fun getHeader(tutorial: Tutorial): String

    override fun populateNodeIds(tutorial: Tutorial) {
        TutorialForkLogic.populateNodeIds(this, tutorial)
    }

    override fun refresh(tutorial: Tutorial) {
        TutorialForkLogic.refresh(this, tutorial)
    }

    override fun reset(tutorial: Tutorial) {
        TutorialForkLogic.reset(this, tutorial)
    }

    override fun validate(tutorial: Tutorial): List<String> {
        return TutorialForkLogic.validate(this, tutorial)
    }
}
