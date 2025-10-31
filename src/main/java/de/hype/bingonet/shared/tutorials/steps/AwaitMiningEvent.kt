package de.hype.bingonet.shared.tutorials.steps

import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.constants.MiningEvents

/**
 * Pure data class for mining event tutorial steps.
 * Step completes when the specified mining event occurs on the specified island.
 */
class AwaitMiningEvent(
    val event: MiningEvents,
    val islands: Islands?,
) : TutorialStep()
