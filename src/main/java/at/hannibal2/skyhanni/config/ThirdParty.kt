package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.event.bingo.BingoNetConfig
import at.hannibal2.skyhanni.config.features.event.bingo.BingoNetworksConfig
import kotlin.reflect.KMutableProperty1

/**
 * Registry of supported third-party integrations.
 */
enum class ThirdParty(
    val id: String,
    val displayName: String,
    val description: String,
    val mainToggleField: KMutableProperty1<out Any, Boolean>?,
) {
    BINGO_NET(
        id = "bingo_net",
        displayName = "Bingo Net",
        description = "Advanced Network centered around Skyblock Bingo by Hype_the_Time.",
        mainToggleField = BingoNetConfig::useBN,
    ) {
        override fun isEnabled(): Boolean =
            try {
                SkyHanniMod.feature.event.bingo.bingoNetworks.bingoNet.useBN
            } catch (_: Throwable) {
                false
            }

        override fun setEnabled(enabled: Boolean) {
            try {
                SkyHanniMod.feature.event.bingo.bingoNetworks.bingoNet.useBN = enabled
            } catch (_: Throwable) { /* ignore in case of early init */
            }
        }
    },
    BINGO_BREWERS(
        id = "bingo_brewers",
        displayName = "Bingo Brewers",
        description = "Simple Bingo network by indigo_polecat.",
        mainToggleField = BingoNetworksConfig::useBB,
    ) {
        override fun isEnabled(): Boolean =
            try {
                SkyHanniMod.feature.event.bingo.bingoNetworks.useBB
            } catch (_: Throwable) {
                false
            }

        override fun setEnabled(enabled: Boolean) {
            try {
                SkyHanniMod.feature.event.bingo.bingoNetworks.useBB = enabled
            } catch (_: Throwable) { /* ignore in case of early init */
            }
        }
    },
    BINGO_SPLASH_COMMUNITY(
        id = "bingo_splash_community",
        displayName = "Bingo Splash Community",
        description = "Very basic splash announcement server by the Bingo Splash Community Discord Server. Hosted by Morazzer",
        mainToggleField = BingoNetworksConfig::useBSC
    ){
        override fun isEnabled(): Boolean =
            try {
                SkyHanniMod.feature.event.bingo.bingoNetworks.useBSC
            } catch (_: Throwable) {
                false
            }

        override fun setEnabled(enabled: Boolean) {
            try {
                SkyHanniMod.feature.event.bingo.bingoNetworks.useBSC = enabled
            } catch (_: Throwable) { /* ignore in case of early init */
            }
        }
    };

    abstract fun isEnabled(): Boolean
    abstract fun setEnabled(enabled: Boolean)
}

/** Tri-state helper for annotation parameters. */
enum class TriState {
    AUTO,
    YES,
    NO
}
