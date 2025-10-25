package de.hype.bingonet.shared.tutorials.steps.requirement

import de.hype.bingonet.shared.constants.Collections
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Pure data class for collection level requirement steps.
 * Step completes when the collection reaches the specified level.
 * 
 * @param requiredTotal If provided, overrides the threshold from collection tiers.
 */
class CollectionLevelRequirement(
    val collection: Collections,
    val minLevel: Int,
    val requiredTotal: Int? = null,
) : TutorialStep()
