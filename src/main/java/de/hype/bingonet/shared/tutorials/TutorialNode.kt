package de.hype.bingonet.shared.tutorials

abstract class TutorialNode {

    abstract val nodeId: String
    abstract fun isComplete(tutorial: Tutorial): Boolean
    abstract fun reset()
}
