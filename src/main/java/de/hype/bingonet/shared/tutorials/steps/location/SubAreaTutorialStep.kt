package de.hype.bingonet.shared.tutorials.steps.location

import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Pure data class for sub-area tutorial steps.
 * Step completes when the player enters the specified sub-area.
 */
class SubAreaTutorialStep(
    val area: String,
) : TutorialStep()
