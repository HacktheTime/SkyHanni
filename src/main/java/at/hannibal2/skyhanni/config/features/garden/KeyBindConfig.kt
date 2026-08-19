package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.features.garden.farming.GardenCustomKeybinds
import at.hannibal2.skyhanni.utils.KeyboardManager
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import org.lwjgl.glfw.GLFW

class KeyBindConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Use custom keybinds while holding a farming tool in your hand.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Include Squeaky Mousemat",
        desc = "Also use custom keybinds while holding a Squeaky Mousemat in your hand.\n" +
            "§eRequires main toggle to be enabled!",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var mousemat: Boolean = false

    @Expose
    @ConfigOption(
        name = "Include Vacuum",
        desc = "Also use custom keybinds while holding a Vacuum in your hand.\n" +
            "§eRequires main toggle to be enabled!",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var vacuum: Boolean = false

    @Expose
    @ConfigOption(
        name = "Include Fishing Rod",
        desc = "Also use custom keybinds while holding a fishing rod in your hand.\n" +
            "§eRequires main toggle to be enabled!",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var fishingRod: Boolean = false

    @Expose
    @ConfigOption(
        name = "Include Empty Hand with Sun's Grasp",
        desc = "Also use custom keybinds while holding nothing in your hand if you have a Sun's Grasp equipped.\n" +
            "§eRequires main toggle to be enabled!",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var sunsGrasp: Boolean = true

    // TODO Rename excludeBarn to excludeUnfarmablePlots
    @Expose
    @ConfigOption(name = "Exclude Unfarmable Plots", desc = "Disable this feature while on the barn plot or in a greenhouse.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var excludeBarn: Boolean = false

    @ConfigOption(name = "Disable All", desc = "Disable all keys.")
    @ConfigEditorButton(buttonText = "Disable")
    val presetDisable: Runnable = Runnable(GardenCustomKeybinds::disableAll)

    @ConfigOption(name = "Set Default", desc = "Reset all keys to default.")
    @ConfigEditorButton(buttonText = "Default")
    val presetDefault: Runnable = Runnable(GardenCustomKeybinds::defaultAll)

    @Expose
    @ConfigOption(name = "Attack", desc = "")
    @ConfigEditorKeybind(defaultKey = KeyboardManager.LEFT_MOUSE)
    @FeatureDependencyRequirement("#enabled")
    val attack: Property<Int> = Property.of(KeyboardManager.LEFT_MOUSE)

    @Expose
    @ConfigOption(name = "Use Item", desc = "")
    @ConfigEditorKeybind(defaultKey = KeyboardManager.RIGHT_MOUSE)
    @FeatureDependencyRequirement("#enabled")
    val useItem: Property<Int> = Property.of(KeyboardManager.RIGHT_MOUSE)

    @Expose
    @ConfigOption(name = "Move Left", desc = "")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_A)
    @FeatureDependencyRequirement("#enabled")
    val left: Property<Int> = Property.of(GLFW.GLFW_KEY_A)

    @Expose
    @ConfigOption(name = "Move Right", desc = "")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_D)
    @FeatureDependencyRequirement("#enabled")
    val right: Property<Int> = Property.of(GLFW.GLFW_KEY_D)

    @Expose
    @ConfigOption(name = "Move Forward", desc = "")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_W)
    @FeatureDependencyRequirement("#enabled")
    val forward: Property<Int> = Property.of(GLFW.GLFW_KEY_W)

    @Expose
    @ConfigOption(name = "Move Back", desc = "")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_S)
    @FeatureDependencyRequirement("#enabled")
    val back: Property<Int> = Property.of(GLFW.GLFW_KEY_S)

    @Expose
    @ConfigOption(name = "Jump", desc = "")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_SPACE)
    @FeatureDependencyRequirement("#enabled")
    val jump: Property<Int> = Property.of(GLFW.GLFW_KEY_SPACE)

    @Expose
    @ConfigOption(name = "Sneak", desc = "")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_LEFT_SHIFT)
    @FeatureDependencyRequirement("#enabled")
    val sneak: Property<Int> = Property.of(GLFW.GLFW_KEY_LEFT_SHIFT)
}
