package de.hype.bingonet.shared.tutorials.steps.requirement

import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Pure data class for coin obtaining tutorial steps.
 * Step completes when the player has at least the specified amount of coins.
 */
class ObtainCoinsTutorialStep(
    val amount: Long,
) : TutorialStep()
