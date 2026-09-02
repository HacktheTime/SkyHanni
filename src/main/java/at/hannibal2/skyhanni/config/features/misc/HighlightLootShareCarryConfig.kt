package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HighlightLootShareCarryConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Highlight mobs that have taken damage compared to what they spawned with.\n" +
            "Use §e/shhighlightlootsharecarry §7to toggle or set the percentage.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Damage Threshold",
        desc = "The minimum percentage of max HP a mob must have lost (relative to what it spawned with) to be highlighted.\n" +
            "§eExample: §75 means mobs that lost 5% or more of their max HP are highlighted.",
    )
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 100f, minStep = 0.5f)
    @FeatureDependencyRequirement("#enabled")
    var damageThreshold: Float = 5.0f

    @Expose
    @ConfigOption(name = "Highlight Color", desc = "The color to highlight damaged mobs in.")
    @ConfigEditorColour
    @FeatureDependencyRequirement("#enabled")
    var highlightColor: ChromaColour = ChromaColour.fromStaticRGB(85, 255, 85, 245)
}
