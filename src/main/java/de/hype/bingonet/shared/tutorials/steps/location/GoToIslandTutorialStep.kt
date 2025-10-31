package de.hype.bingonet.shared.tutorials.steps.location

import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Pure data class for island navigation tutorial steps.
 * Step completes when the player travels to the specified island.
 */
class GoToIslandTutorialStep(
    val island: Islands,
) : TutorialStep()
