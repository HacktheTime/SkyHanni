package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode

class SelectPathTutorialFork(
    val paths: List<SelectedPathTutorialFork>,
) : TutorialFork() {

    override fun getNodes(tutorial: Tutorial): List<TutorialNode> {
        //TODO fix the issue of paths not being available yet.
        val selectedOption: SelectedPathTutorialFork = tutorial.getSelectedOption(paths)?: return emptyList()
        return selectedOption.pathNodes
    }

    override fun isAsync(): Boolean = false

    override fun isComplete(tutorial: Tutorial): Boolean {
        return paths.any { it.pathNodes.all { it.isComplete(tutorial) } }
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

    override fun reset() {
        paths.forEach { it.pathNodes.forEach { it.reset() } }
    }
}
