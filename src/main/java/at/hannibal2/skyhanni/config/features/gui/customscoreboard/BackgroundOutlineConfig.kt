package at.hannibal2.skyhanni.config.features.gui.customscoreboard

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

@FeatureDependencyRequirement("BackgroundConfig#enabled", "CustomScoreboardConfig#hasEnabled")
class BackgroundOutlineConfig {
    @Expose
    @ConfigOption(name = "Outline", desc = "Show an outline around the scoreboard.")
    @ConfigEditorBoolean
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Thickness", desc = "Thickness of the outline.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 15f, minStep = 1f)
    @FeatureDependencyRequirement("#enabled")
    var thickness: Int = 5

    @Expose
    @ConfigOption(name = "Blur", desc = "Amount that the outline is blurred.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 1f, minStep = 0.1f)
    @FeatureDependencyRequirement("#enabled")
    var blur: Float = 0.7f

    @Expose
    @ConfigOption(name = "Color Top", desc = "Color of the top of the outline.")
    @ConfigEditorColour
    @FeatureDependencyRequirement("#enabled")
    var colorTop: ChromaColour = ChromaColour.fromStaticRGB(175, 89, 255, 255)

    @Expose
    @ConfigOption(name = "Color Bottom", desc = "Color of the bottom of the outline.")
    @ConfigEditorColour
    @FeatureDependencyRequirement("#enabled")
    var colorBottom: ChromaColour = ChromaColour.fromStaticRGB(127, 237, 255, 255)
}
