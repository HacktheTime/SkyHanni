package de.hype.bingonet.shared.tutorials.steps

import de.hype.bingonet.shared.constants.Collections

/**
 * Pure data class for collection-based tutorial steps.
 * Step completes when the specified collection amount is reached.
 */
class CollectionTutorialStep(
    val collection: Collections,
    val amount: Int,
    val forCollectionGoal: Boolean,
) : TutorialStep()
