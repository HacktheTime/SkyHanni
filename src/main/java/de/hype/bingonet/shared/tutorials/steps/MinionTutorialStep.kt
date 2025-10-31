package de.hype.bingonet.shared.tutorials.steps

/**
 * Pure data class for minion-based tutorial steps.
 * Step completes when the specified number of minion slots are placed.
 */
class MinionTutorialStep(
    val slots: Int,
) : TutorialStep()
