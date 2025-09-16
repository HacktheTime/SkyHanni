package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialCondition
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class WhileTutorialNode(
    val condition: TutorialCondition,
    val conditionExplenation : String,
    val nodes: List<TutorialStep>,
) : TutorialFork() {
    override fun getNodes(tutorial: Tutorial): List<TutorialNode> {
        return nodes
    }

    override fun isAsync(): Boolean {
        return false
    }

    override fun getAllInternalNodes(): List<TutorialNode> {
        return nodes
    }

    override fun isComplete(tutorial: Tutorial): Boolean {
        val done = condition.matches(tutorial)
        if (done) {
            nodes.forEach { it.complete() }
        } else {
            val lastDone = nodes.last().completed
            if (lastDone) nodes.forEach { it.reset(tutorial) }
        }
        return done
    }

    override fun getHeader(tutorial: Tutorial): String = "Do While ${conditionExplenation}"
}
