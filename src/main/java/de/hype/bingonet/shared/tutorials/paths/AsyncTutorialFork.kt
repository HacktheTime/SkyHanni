package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.TutorialNode

class AsyncTutorialFork(
    val pathNodes : List<TutorialNode>
)  : TutorialFork() {
    override fun getNodes(): List<TutorialNode> {
        return pathNodes
    }

    override fun isAsync(): Boolean = true

    override fun isComplete(): Boolean {
        return pathNodes.all { it.isComplete() }
    }
}
