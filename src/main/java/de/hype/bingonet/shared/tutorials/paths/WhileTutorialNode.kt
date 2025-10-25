package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.TutorialCondition
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Pure data class for while-loop tutorial forks.
 * Fork that repeats nodes while a condition is true.
 */
class WhileTutorialNode(
    val condition: TutorialCondition,
    val conditionExplenation: String,
    val nodes: List<TutorialStep>,
) : TutorialFork()
