package de.hype.bingonet.shared.tutorials.steps.itemstep

import de.hype.bingonet.shared.tutorials.TaggedItemCheck
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Pure data class for reforge tutorial steps.
 * Step completes when the specified item is reforged to the target reforge.
 */
class ReforgeTutorialStep(
    val item: TaggedItemCheck,
    val reforgeName: String,
) : TutorialStep()
