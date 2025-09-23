package de.hype.bingonet.shared.tutorials.steps
import at.hannibal2.skyhanni.api.CollectionApi
import de.hype.bingonet.environment.NeuEnvironmentRepo
import de.hype.bingonet.shared.constants.Collections
import de.hype.bingonet.shared.tutorials.Tutorial

class CollectionTutorialStep(
    val collection: Collections,
    val amount: Int,
    val forCollectionGoal: Boolean
): TutorialStep() {
    override fun getStepName(tutorial: Tutorial): String {
        return "Obtain $amount ${collection.displayName}"
    }

    

    

    

    override fun getStepDescription(tutorial: Tutorial): String? {
        return null
    }

    override fun getRequirements(): List<de.hype.bingonet.shared.tutorials.TutorialNode> = emptyList()

    

override fun onActivate(tutorial: Tutorial) {
        CollectionApi.startTracking(collection, amount.toLong())
    }

override fun onDeactivate(tutorial: Tutorial) {
        CollectionApi.stopTracking()
    }

override fun onReset(tutorial: Tutorial) {
        CollectionApi.stopTracking()
    }

override fun isComplete(tutorial: Tutorial): Boolean {
        val internal = NeuEnvironmentRepo.getFromSBName(collection.id)
        val current = CollectionApi.getCollectionCounter(internal) ?: return false
        return current >= amount
    }
}
