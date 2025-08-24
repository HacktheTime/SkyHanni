package de.hype.bingonet.shared.tutorials.steps

import at.hannibal2.skyhanni.features.misc.CollectionTracker
import de.hype.bingonet.shared.constants.Collections
import de.hype.bingonet.shared.tutorials.Tutorial

class CollectionTutorialStep(
    val collection: Collections,
    val amount: Int,
    val forCollectionGoal: Boolean
): TutorialStep() {
    override fun getStepName(): String {
        return "Obtain $amount ${collection.displayName}"
    }

    override fun onActivate() {
        CollectionTracker.trackcollection
    }

    override fun onDeactivate() {
        CollectionTracker.resetTracking()
    }

    override fun onReset() {
        CollectionTracker.resetTracking()
    }

    override fun getStepDescription(tutorial: Tutorial): String? {
        return null
    }
}
