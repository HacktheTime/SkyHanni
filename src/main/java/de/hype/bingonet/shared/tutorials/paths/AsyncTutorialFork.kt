package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.TutorialNode

/**
 * Pure data class for async tutorial forks.
 * Contains only data properties - no methods or logic.
 */
class AsyncTutorialFork(
    val pathNodes: List<TutorialNode>,
) : TutorialFork()
