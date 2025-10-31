package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.TutorialCondition
import de.hype.bingonet.shared.tutorials.TutorialNode

/**
 * Pure data class for hidden optional improvement forks.
 * Fork that shows improved path if condition matches, otherwise shows default path.
 */
class HiddenOptionalImprovementFork(
    val default: List<TutorialNode>,
    val improved: List<TutorialNode>,
    val condition: TutorialCondition,
) : TutorialFork()
