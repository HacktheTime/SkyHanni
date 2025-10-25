package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.TutorialNode

/**
 * Pure data class for optional tutorial forks.
 * Fork that presents multiple optional paths where completing any one satisfies the fork.
 * 
 * @param paths List of path options, each paired with a boolean indicating if hidden
 */
class OptionalTutorialFork(
    val paths: List<Pair<List<TutorialNode>, Boolean>>,
) : TutorialFork()
