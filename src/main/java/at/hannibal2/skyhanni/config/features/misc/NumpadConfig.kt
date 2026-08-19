package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.combat.InstanceChestProfitConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import at.hannibal2.skyhanni.data.IslandType
import org.lwjgl.glfw.GLFW

// New small serializable types for storing numpad codes in the FEATURES config
data class SavedNumpadAction(
    @Expose
    var command: String = "",
    @Expose
    var delaySeconds: Double = 1.0,
)

data class SavedNumpadCode(
    @Expose
    var code: String = "",
    @Expose
    var actions: MutableList<SavedNumpadAction> = mutableListOf(),
    @Expose
    var commandDelaySeconds: Double = 0.0,
    @Expose
    var allowedIslands: MutableSet<IslandType> = mutableSetOf(IslandType.ANY),
    // New: allow executing this code when not on SkyBlock (outside SkyBlock), defaults to false for backward compatibility
    @Expose
    var allowOutsideSkyBlock: Boolean = false,
)

class NumpadConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable the numpad codes system (global toggle).")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Input timeout (seconds)", desc = "Time in seconds until typed input clears automatically.")
    @ConfigEditorSlider(minValue = 1.0f, maxValue = 30.0f, minStep = 0.1f)
    @FeatureDependencyRequirement("#enabled")
    var inputTimeoutSeconds: Double = 10.0

    @Expose
    @ConfigLink(owner = NumpadConfig::class, field = "enabled")
    @FeatureDependencyRequirement("#enabled")
    val pos: Position = Position(10, 10)

    @Expose
    @ConfigOption(name = "Show suggestions", desc = "Show matching suggestions in overlay. If disabled, only the current exact code (if any) is shown.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var showSuggestions: Boolean = false

    @Expose
    @ConfigOption(name = "Show overlay label", desc = "Render a custom label above the numpad overlay")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var showLabel: Boolean = false

    @Expose
    @ConfigOption(name = "Overlay label", desc = "Custom label text to render above the overlay")
    @ConfigEditorText
    @FeatureDependencyRequirement("#enabled")
    var overlayLabel: String = ""

    @Expose
    @ConfigOption(name = "Show 'Numpad:' prefix", desc = "Show the leading 'Numpad:' label before the current input")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var showPrefix: Boolean = true

    // Persisted saved codes (serialized into the FEATURES config). Editors will ignore these fields in the moulconfig UI.
    @Expose
    var savedCodes: MutableList<SavedNumpadCode> = mutableListOf()

    @Expose
    @ConfigOption(name = "Numpad 0", desc = "Key for digit 0")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_0)
    @FeatureDependencyRequirement("#enabled")
    var key_numpad0: Int = GLFW.GLFW_KEY_KP_0

    @Expose
    @ConfigOption(name = "Numpad 1", desc = "Key for digit 1")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_1)
    @FeatureDependencyRequirement("#enabled")
    var key_numpad1: Int = GLFW.GLFW_KEY_KP_1

    @Expose
    @ConfigOption(name = "Numpad 2", desc = "Key for digit 2")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_2)
    @FeatureDependencyRequirement("#enabled")
    var key_numpad2: Int = GLFW.GLFW_KEY_KP_2

    @Expose
    @ConfigOption(name = "Numpad 3", desc = "Key for digit 3")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_3)
    @FeatureDependencyRequirement("#enabled")
    var key_numpad3: Int = GLFW.GLFW_KEY_KP_3

    @Expose
    @ConfigOption(name = "Numpad 4", desc = "Key for digit 4")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_4)
    @FeatureDependencyRequirement("#enabled")
    var key_numpad4: Int = GLFW.GLFW_KEY_KP_4

    @Expose
    @ConfigOption(name = "Numpad 5", desc = "Key for digit 5")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_5)
    @FeatureDependencyRequirement("#enabled")
    var key_numpad5: Int = GLFW.GLFW_KEY_KP_5

    @Expose
    @ConfigOption(name = "Numpad 6", desc = "Key for digit 6")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_6)
    @FeatureDependencyRequirement("#enabled")
    var key_numpad6: Int = GLFW.GLFW_KEY_KP_6

    @Expose
    @ConfigOption(name = "Numpad 7", desc = "Key for digit 7")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_7)
    @FeatureDependencyRequirement("#enabled")
    var key_numpad7: Int = GLFW.GLFW_KEY_KP_7

    @Expose
    @ConfigOption(name = "Numpad 8", desc = "Key for digit 8")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_8)
    @FeatureDependencyRequirement("#enabled")
    var key_numpad8: Int = GLFW.GLFW_KEY_KP_8

    @Expose
    @ConfigOption(name = "Numpad 9", desc = "Key for digit 9")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_9)
    @FeatureDependencyRequirement("#enabled")
    var key_numpad9: Int = GLFW.GLFW_KEY_KP_9

    @Expose
    @ConfigOption(name = "Enter (activate)", desc = "Key used to activate the entered code")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_ENTER)
    @FeatureDependencyRequirement("#enabled")
    var key_enter: Int = GLFW.GLFW_KEY_KP_ENTER

    @Expose
    @ConfigOption(name = "Delete last (+)", desc = "Key used to delete the last entered digit")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_ADD)
    @FeatureDependencyRequirement("#enabled")
    var key_deleteLast: Int = GLFW.GLFW_KEY_KP_ADD

    @Expose
    @ConfigOption(name = "Clear (-)", desc = "Key used to clear the current input")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_KP_SUBTRACT)
    @FeatureDependencyRequirement("#enabled")
    var key_clear: Int = GLFW.GLFW_KEY_KP_SUBTRACT
}
