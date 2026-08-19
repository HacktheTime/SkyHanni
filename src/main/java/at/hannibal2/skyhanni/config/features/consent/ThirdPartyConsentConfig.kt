package at.hannibal2.skyhanni.config.features.consent

import at.hannibal2.skyhanni.config.ThirdParty
import at.hannibal2.skyhanni.config.ThirdPartyConsentMode
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

/**
 * Global third-party consent settings.
 */
class ThirdPartyConsentConfig {
    @Expose
    @ConfigOption(
        name = "Third-Party Consent Mode",
        desc = "Choose how third-party features are allowed by default. NONE = block all, SELECT = pick per third party, ALL = allow all.",
    )
    @ConfigEditorDropdown
    var mode: ThirdPartyConsentMode = ThirdPartyConsentMode.NONE

    // Convenience toggles for known third parties when mode == SELECT
    @Expose
    @ConfigOption(
        name = "Allow Bingo Net",
        desc = "Permit features that depend on Bingo Net when in SELECT mode.",
    )
    @ConfigEditorBoolean
    var allowBingoNet: Boolean = false

    @Expose
    @ConfigOption(
        name = "Allow Bingo Brewers",
        desc = "Permit features that depend on Bingo Brewers when in SELECT mode.",
    )
    @ConfigEditorBoolean
    var allowBingoBrewers: Boolean = false

    @Expose
    @ConfigOption(
        name = "Allow Bingo Splash Community",
        desc = "Permit features that depend on Bingo Splash Community when in SELECT mode.",
    )
    @ConfigEditorBoolean
    var allowBSC: Boolean = false

    @Expose
    @ConfigOption(
        name = "Allow Soopy",
        desc = "Permit features that depend on Soopy services when in SELECT mode.",
    )
    @ConfigEditorBoolean
    var allowSoopy: Boolean = false

    @Expose
    @ConfigOption(
        name = "Allow Farming Elite",
        desc = "Permit features that depend on Farming Elite (eliteskyblock.com) when in SELECT mode.",
    )
    @ConfigEditorBoolean
    var allowFarmingElite: Boolean = false

    fun isAllowed(thirdParty: ThirdParty): Boolean {
        // Prefer explicit toggles for known entries
        return when (thirdParty) {
            ThirdParty.BINGO_NET -> allowBingoNet
            ThirdParty.BINGO_BREWERS -> allowBingoBrewers
            ThirdParty.BINGO_SPLASH_COMMUNITY -> allowBSC
            ThirdParty.SOOPY -> allowSoopy
            ThirdParty.FARMING_ELITE -> allowFarmingElite
        }
    }

    fun setAllowed(thirdParty: ThirdParty, allowed: Boolean) {
        when (thirdParty) {
            ThirdParty.BINGO_NET -> allowBingoNet = allowed
            ThirdParty.BINGO_BREWERS -> allowBingoBrewers = allowed
            ThirdParty.BINGO_SPLASH_COMMUNITY -> allowBSC = allowed
            ThirdParty.SOOPY -> allowSoopy = allowed
            ThirdParty.FARMING_ELITE -> allowFarmingElite = allowed
        }
    }
}
