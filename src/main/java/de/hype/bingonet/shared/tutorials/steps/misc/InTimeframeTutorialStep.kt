package de.hype.bingonet.shared.tutorials.steps.misc

import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import kotlinx.datetime.Instant

/**
 * Pure data class for timeframe tutorial steps.
 * Step completes when the current time is within the specified timeframe.
 */
class InTimeframeTutorialStep(
    val start: Instant = Instant.MIN,
    val end: Instant = Instant.MAX,
) : TutorialStep()
