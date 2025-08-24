package de.hype.bingonet.shared.tutorials

import de.hype.bingonet.shared.tutorials.steps.TutorialStep

abstract class TutorialNode {

    lateinit var nodeId: String
        protected set

    abstract fun populateNodeIds(tutorial: Tutorial)
    abstract fun isComplete(tutorial: Tutorial): Boolean
    abstract fun reset()
    abstract fun refresh()
}
