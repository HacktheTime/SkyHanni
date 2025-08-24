package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode

class OptionalTutorialFork(
    val paths: List<List<TutorialNode>>,
) : TutorialFork() {

    override fun getNodes(tutorial: Tutorial): List<TutorialNode> {
        return paths.maxBy { it.count { it.isComplete(tutorial) } }
    }

    override fun isAsync(): Boolean = false

    override fun isComplete(tutorial: Tutorial): Boolean {
        return paths.any { it.all { it.isComplete(tutorial) } }
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
        return paths.flatten()
    }
}
