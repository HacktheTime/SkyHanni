package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.features.misc.massconfiguration.FeatureToggleProcessor

object ThirdPartyFeatureRegistry {
    fun getFeatures(tp: ThirdParty) = FeatureToggleProcessor.thirdPartyRegistry[tp].orEmpty()
}
