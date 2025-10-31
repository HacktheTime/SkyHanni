package de.hype.bingonet.shared.tutorials.steps.location

import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.objects.Position
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Pure data class for position navigation tutorial steps.
 * Step completes when the player reaches the specified position.
 * 
 * @param node Waypoint path to follow. If null, use SkyHanni's internal routing system.
 */
class GoToPositionTutorialStep(
    val node: List<Position>?,
    val island: Islands,
    val allowSkip: Boolean = true,
) : TutorialStep()
