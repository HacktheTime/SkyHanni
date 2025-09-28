package de.hype.bingonet.shared.tutorials.steps.requirement

import at.hannibal2.skyhanni.api.CollectionApi
import de.hype.bingonet.environment.NeuEnvironmentRepo
import de.hype.bingonet.shared.constants.Collections
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Requirement that the player has reached at least a specific collection level.
 * Uses CollectionApi.getCollectionCounter to check current total.
 */
class CollectionLevelRequirement(
    val collection: Collections,
    val minLevel: Int,
    /** If provided, overrides the threshold from collection tiers. */
    val requiredTotal: Int? = null,
) : TutorialStep() {

    override fun getStepName(tutorial: Tutorial): String =
        "Reach ${collection.displayName} Collection level $minLevel"

    override fun getStepDescription(tutorial: Tutorial): String? = null

    override fun getRequirements(): List<TutorialNode> = emptyList()


    override fun check(tutorial: Tutorial): Boolean {
        val internal = NeuEnvironmentRepo.getFromSBName(collection.id)
        val current = CollectionApi.getCollectionCounter(internal) ?: return false
        val need = requiredTotal ?: collection.getCollectionForTier(minLevel)
        return current >= need
    }
}
