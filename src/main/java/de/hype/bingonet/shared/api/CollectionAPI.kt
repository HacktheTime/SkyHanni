package de.hype.bingonet.shared.api

import at.hannibal2.skyhanni.features.misc.CollectionTracker
import at.hannibal2.skyhanni.utils.NeuInternalName
import de.hype.bingonet.environment.NeuEnvironmentRepo
import de.hype.bingonet.shared.constants.Collections

object CollectionAPI {
    fun startTracking(collection: Collections, goalAmount: Long = -1L) {
        val internal: NeuInternalName = NeuEnvironmentRepo.getFromSBName(collection.id)
        CollectionTracker.startTrackingByInternalName(internal, goalAmount)
    }

    fun stopTracking() {
        CollectionTracker.resetTracking()
    }
}
