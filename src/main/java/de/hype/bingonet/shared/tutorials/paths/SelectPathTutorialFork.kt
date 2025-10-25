package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.TutorialNode

/**
 * Pure data class for select path tutorial forks.
 * Fork that requires user to explicitly select one path from multiple options.
 */
class SelectPathTutorialFork(
    val paths: List<SelectedPathTutorialFork>,
) : TutorialFork() {
    
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
}
