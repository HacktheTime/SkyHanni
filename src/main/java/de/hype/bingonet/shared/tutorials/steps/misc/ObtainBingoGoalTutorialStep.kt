package de.hype.bingonet.shared.tutorials.steps.misc

import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Pure data class for bingo goal tutorial steps.
 * Step completes when the specified bingo goal is obtained.
 * 
 * @param showOnActive Set to false if using with an optional fork so it's not listed as alternative.
 */
class ObtainBingoGoalTutorialStep(
    val displayName: String,
    val showOnActive: Boolean = true,
) : TutorialStep()
