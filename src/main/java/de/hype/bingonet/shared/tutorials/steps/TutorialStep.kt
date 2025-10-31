package de.hype.bingonet.shared.tutorials.steps

import de.hype.bingonet.shared.objects.WaypointData
import de.hype.bingonet.shared.tutorials.TutorialNode

/**
 * Pure data class for tutorial steps.
 * Contains only data properties - NO methods or logic.
 * All behavior is handled externally by TutorialStepLogic.
 */
abstract class TutorialStep(
    val guiderMakerExtraNotes: String? = null,
) : TutorialNode() {
    // Data properties only
    var isActive: Boolean = false
    var completed: Boolean = false
    val waypoints: MutableList<WaypointData> = mutableListOf()
}
