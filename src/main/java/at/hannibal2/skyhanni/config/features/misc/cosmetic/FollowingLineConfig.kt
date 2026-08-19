package at.hannibal2.skyhanni.config.features.misc.cosmetic

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class FollowingLineConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Draw a colored line behind the player.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Line Color", desc = "Color of the line.")
    @ConfigEditorColour
    @FeatureDependencyRequirement("#enabled")
    var lineColor: ChromaColour = ChromaColour.fromStaticRGB(255, 255, 255, 255)

    @Expose
    @ConfigOption(name = "Time Alive", desc = "Time in seconds until the line fades out.")
    @ConfigEditorSlider(minStep = 1f, minValue = 1f, maxValue = 30f)
    @FeatureDependencyRequirement("#enabled")
    var secondsAlive: Int = 3

    @Expose
    @ConfigOption(name = "Max Line Width", desc = "Max width of the line.")
    @ConfigEditorSlider(minStep = 1f, minValue = 1f, maxValue = 10f)
    @FeatureDependencyRequirement("#enabled")
    var lineWidth: Int = 4

    @Expose
    @ConfigOption(name = "Behind Blocks", desc = "Show behind blocks.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var behindBlocks: Boolean = false
}
