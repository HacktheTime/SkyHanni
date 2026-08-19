package at.hannibal2.skyhanni.config.features.gui.customscoreboard

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

@FeatureDependencyRequirement("CustomScoreboardConfig#hasEnabled")
class MaxwellConfig {
    @Expose
    @ConfigOption(name = "Show Magical Power", desc = "Show your amount of Magical Power in the scoreboard.")
    @ConfigEditorBoolean
    var showMagicalPower: Boolean = true

    @Expose
    @ConfigOption(name = "Compact Tuning", desc = "Show tuning stats compact")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#showMagicalPower")
    var compactTuning: Boolean = false

    @Expose
    @ConfigOption(
        name = "Tuning Amount",
        desc = "Only show the first # tunings.\n" +
            "§cDoes not work with Compact Tuning.",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 8f, minStep = 1f)
    @FeatureDependencyRequirement("#showMagicalPower")
    var tuningAmount: Int = 2
}
