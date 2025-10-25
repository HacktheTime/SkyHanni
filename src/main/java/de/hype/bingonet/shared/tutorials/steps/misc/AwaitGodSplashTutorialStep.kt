package de.hype.bingonet.shared.tutorials.steps.misc

import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import kotlin.time.Duration

/**
 * Pure data class for god splash tutorial steps.
 * Step completes after receiving a god splash of at least the minimum duration.
 */
class AwaitGodSplashTutorialStep(
    val minimumDuration: Duration,
) : TutorialStep()
