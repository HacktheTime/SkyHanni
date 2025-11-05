package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import kotlin.collections.flatten
import kotlin.collections.maxBy

class OptionalTutorialFork(
    val paths: List<Pair<List<TutorialNode>, Boolean>>,
) : TutorialFork() {

    override fun getNodes(tutorial: Tutorial): List<TutorialNode> {
        return paths.maxBy { it.first.count { it.isComplete(tutorial) } }.first
    }

    override fun isAsync(): Boolean = false

    override fun isComplete(tutorial: Tutorial): Boolean {
        return paths.any { it.first.all { it.isComplete(tutorial) } }
    }

    data class SelectedPathTutorialFork(
        val option: SelectPathTutorialOption,
        val pathNodes: List<TutorialNode>,
    ) {
        constructor(id: String, name: String, guideMakerOption: String? = null, pathNodes: List<TutorialNode>) : this(
            SelectPathTutorialOption(id, name, guideMakerOption),
            pathNodes,
        )
    }

    data class SelectPathTutorialOption(
        val id: String,
        val name: String,
        val guideMakerDescription: String? = null,
    )

    override fun getAllInternalNodes(): List<TutorialNode> {
        return paths.map { it.first }.flatten()
    }

    override fun getHeader(tutorial: Tutorial): String = "Do Either"
}
