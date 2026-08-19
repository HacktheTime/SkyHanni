package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.event.bingo.BingoNetConfig
import at.hannibal2.skyhanni.config.features.event.bingo.BingoNetworksConfig
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteFarmersLeaderboardsConfig
import at.hannibal2.skyhanni.config.features.mining.MiningEventConfig
import kotlin.reflect.KMutableProperty1

/**
 * Registry of supported third-party integrations.
 */
enum class ThirdParty(
    val id: String,
    val displayName: String,
    val description: String,
    val mainToggleField: KMutableProperty1<out Any, Boolean>?,
    val sourceAccess: Boolean,
    val serverAccess: Boolean,
    val websiteUrl: String? = null,
    val privacyPolicyUrl: String? = null,
    val termsOfServiceUrl: String? = null,
    val discordUrl: String? = null,
) {
    BINGO_NET(
        id = "bingo_net",
        displayName = "Bingo Net",
        description = "Advanced Network centered around Skyblock Bingo by Hype_the_Time.",
        mainToggleField = BingoNetConfig::useBN,
        sourceAccess = false,
        serverAccess = false,
        websiteUrl = "https://hackthetime.de",
        privacyPolicyUrl = "https://hackthetime.de/privacy",
        termsOfServiceUrl = "https://hackthetime.de/tos",
        discordUrl = "https://hackthetime.de/discord",
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
        termsOfServiceUrl = null,
        privacyPolicyUrl = null,
        websiteUrl = null,
        serverAccess = false,
        sourceAccess = false,
        discordUrl = "https://discord.gg/bingobrewers",
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
        termsOfServiceUrl = null,
        privacyPolicyUrl = null,
        websiteUrl = null,
        serverAccess = false,
        sourceAccess = false,
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
    },
    SOOPY(
        id = "soopy",
        displayName = "Soopy",
        description = "External services provided by Soopy, e.g. the Mining Event data API (api.soopy.dev).",
        termsOfServiceUrl = null,
        privacyPolicyUrl = null,
        websiteUrl = "https://soopy.dev",
        serverAccess = false,
        sourceAccess = false,
        mainToggleField = MiningEventConfig::enabled
    ) {
        override fun isEnabled(): Boolean =
            try {
                SkyHanniMod.feature.mining.miningEvent.enabled
            } catch (_: Throwable) {
                false
            }

        override fun setEnabled(enabled: Boolean) {
            try {
                SkyHanniMod.feature.mining.miningEvent.enabled = enabled
            } catch (_: Throwable) { /* ignore in case of early init */
            }
        }
    },
    FARMING_ELITE(
        id = "farming_elite",
        displayName = "Farming Elite",
        description = "Farming leaderboards and contest data provided by eliteskyblock.com.",
        termsOfServiceUrl = "https://eliteskyblock.com/privacy",
        privacyPolicyUrl = "https://eliteskyblock.com/privacy",
        websiteUrl = "https://eliteskyblock.com",
        serverAccess = false,
        sourceAccess = false,
        mainToggleField = EliteFarmersLeaderboardsConfig::enabled
    ) {
        override fun isEnabled(): Boolean =
            try {
                SkyHanniMod.feature.garden.eliteFarmersLeaderboards.enabled
            } catch (_: Throwable) {
                false
            }

        override fun setEnabled(enabled: Boolean) {
            try {
                SkyHanniMod.feature.garden.eliteFarmersLeaderboards.enabled = enabled
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
