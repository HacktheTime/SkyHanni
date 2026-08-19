package at.hannibal2.skyhanni.config.features.dungeon.spiritleap

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

@FeatureDependencyRequirement("SpiritLeapConfig#enabled")
class SpiritLeapKeybindConfig {
    @Expose
    @ConfigOption(
        name = "Leap Keybinds",
        desc = "Enable or disable the Spirit Leap keybinds.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enableKeybind = false

    @Expose
    @ConfigOption(
        name = "Display Keybind Hints",
        desc = "Show keybind hints to indicate which key to press for leap menu.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    @FeatureDependencyRequirement("#enableKeybind")
    var showKeybindHint = false

    @Expose
    @ConfigOption(
        name = "Keybind: First Target",
        desc = "Keybind for teleporting to the first available Spirit Leap target.",
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_1)
    @FeatureDependencyRequirement("#enableKeybind")
    var keybindOption1 = GLFW.GLFW_KEY_1

    @Expose
    @ConfigOption(
        name = "Keybind: Second Target",
        desc = "Keybind for teleporting to the second available Spirit Leap target.",
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_2)
    @FeatureDependencyRequirement("#enableKeybind")
    var keybindOption2 = GLFW.GLFW_KEY_2

    @Expose
    @ConfigOption(
        name = "Keybind: Third Target",
        desc = "Keybind for teleporting to the third available Spirit Leap target.",
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_3)
    @FeatureDependencyRequirement("#enableKeybind")
    var keybindOption3 = GLFW.GLFW_KEY_3

    @Expose
    @ConfigOption(
        name = "Keybind: Fourth Target",
        desc = "Keybind for teleporting to the fourth available Spirit Leap target.",
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_4)
    @FeatureDependencyRequirement("#enableKeybind")
    var keybindOption4 = GLFW.GLFW_KEY_4
}
