package at.hannibal2.skyhanni.features.misc.massconfiguration

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.ThirdParty
import net.minecraft.client.Minecraft

/**
 * Coordinates when and how the third-party recap should be shown after the default options flow.
 */
object ThirdPartySummaryFlow {

    fun showIfNeeded(orderedOptions: Map<Category, List<FeatureToggleableOption>>) {
        val entries = collectEntries(orderedOptions)
        if (entries.isEmpty()) return
        val consent = SkyHanniMod.feature.thirdPartyConsent
        if (!consent.showThirdPartySummary) return
        if (!consent.hasSeenThirdPartySummaryInfo) {
            Minecraft.getInstance().setScreen(ThirdPartySummaryIntroScreen(entries))
        } else {
            openSummary(entries)
        }
    }

    internal fun openSummary(entries: List<ThirdPartySummaryEntry>) {
        SkyHanniMod.screenToOpen = ThirdPartySummaryScreen(entries)
    }

    internal fun markIntroSeen() {
        val consent = SkyHanniMod.feature.thirdPartyConsent
        if (consent.hasSeenThirdPartySummaryInfo) return
        consent.hasSeenThirdPartySummaryInfo = true
        try {
            SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "third-party-summary-intro")
        } catch (_: Throwable) {
            // Failing to persist should not block the UX flow; it will simply show again next time.
        }
    }

    fun collectEntries(orderedOptions: Map<Category, List<FeatureToggleableOption>>): List<ThirdPartySummaryEntry> {
        return orderedOptions.entries.flatMap { (category, options) ->
            options.filter { it.thirdParty != null }.map { option ->
                ThirdPartySummaryEntry(category, option.thirdParty!!, option)
            }
        }
    }
}

/**
 * Lightweight description of a third-party dependent option used by the summary screen.
 */
data class ThirdPartySummaryEntry(
    val category: Category,
    val thirdParty: ThirdParty,
    val option: FeatureToggleableOption,
)
