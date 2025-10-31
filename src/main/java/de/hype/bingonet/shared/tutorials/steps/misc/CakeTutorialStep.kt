package de.hype.bingonet.shared.tutorials.steps.misc

import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Pure data class for cake year tutorial steps.
 * Step completes when a cake year of at least the minimum duration is active.
 */
class CakeTutorialStep(
    val minimumDuration: Duration = 22.hours,
) : TutorialStep()
