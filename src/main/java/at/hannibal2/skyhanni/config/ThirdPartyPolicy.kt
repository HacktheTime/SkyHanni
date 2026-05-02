package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod

object ThirdPartyPolicy {
    private val consent get() = SkyHanniMod.feature.thirdPartyConsent

    fun hasMainToggle(tp: ThirdParty): Boolean = tp.mainToggleField != null

    /** Whether user consent currently allows using this third party. */
    fun isConsented(tp: ThirdParty): Boolean {
        val mode = consent.mode
        return when (mode) {
            ThirdPartyConsentMode.ALL -> true
            ThirdPartyConsentMode.SELECT -> consent.isAllowed(tp)
            ThirdPartyConsentMode.NONE -> false
        }
    }

    /**
     * Overall gate: for main-toggle third parties, the main toggle is the consent and must be ON.
     * For non-main-toggle third parties, respect consent mode/allow list.
     */
    fun canUse(tp: ThirdParty): Boolean {
        return if (hasMainToggle(tp)) {
            tp.isEnabled()
        } else {
            isConsented(tp)
        }
    }

    /** Helper for mass actions/migrations to decide if an option should be auto-enabled. */
    fun shouldSkipAutoEnable(tp: ThirdParty): Boolean = !canUse(tp)
}
