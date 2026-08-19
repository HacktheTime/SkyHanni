package at.hannibal2.skyhanni.config.features.inventory.customloadout

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

class LoadoutKeybindConfig {

    @Expose
    @ConfigOption(
        name = "Slot Keybinds Toggle",
        desc = "Enable/Disable the loadout slot keybinds.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var slotKeybindsToggle: Boolean = false

    @Expose
    @ConfigOption(name = "Slot 1", desc = "Keybind for loadout slot 1.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_1)
    @FeatureDependencyRequirement("#slotKeybindsToggle")
    var slot1: Int = GLFW.GLFW_KEY_1

    @Expose
    @ConfigOption(name = "Slot 2", desc = "Keybind for loadout slot 2.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_2)
    @FeatureDependencyRequirement("#slotKeybindsToggle")
    var slot2: Int = GLFW.GLFW_KEY_2

    @Expose
    @ConfigOption(name = "Slot 3", desc = "Keybind for loadout slot 3.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_3)
    @FeatureDependencyRequirement("#slotKeybindsToggle")
    var slot3: Int = GLFW.GLFW_KEY_3

    @Expose
    @ConfigOption(name = "Slot 4", desc = "Keybind for loadout slot 4.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_4)
    @FeatureDependencyRequirement("#slotKeybindsToggle")
    var slot4: Int = GLFW.GLFW_KEY_4

    @Expose
    @ConfigOption(name = "Slot 5", desc = "Keybind for loadout slot 5.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_5)
    @FeatureDependencyRequirement("#slotKeybindsToggle")
    var slot5: Int = GLFW.GLFW_KEY_5

    @Expose
    @ConfigOption(name = "Slot 6", desc = "Keybind for loadout slot 6.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_6)
    @FeatureDependencyRequirement("#slotKeybindsToggle")
    var slot6: Int = GLFW.GLFW_KEY_6

    @Expose
    @ConfigOption(name = "Slot 7", desc = "Keybind for loadout slot 7.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_7)
    @FeatureDependencyRequirement("#slotKeybindsToggle")
    var slot7: Int = GLFW.GLFW_KEY_7

    @Expose
    @ConfigOption(name = "Slot 8", desc = "Keybind for loadout slot 8.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_8)
    @FeatureDependencyRequirement("#slotKeybindsToggle")
    var slot8: Int = GLFW.GLFW_KEY_8

    @Expose
    @ConfigOption(name = "Slot 9", desc = "Keybind for loadout slot 9.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_9)
    @FeatureDependencyRequirement("#slotKeybindsToggle")
    var slot9: Int = GLFW.GLFW_KEY_9

    @Expose
    @ConfigOption(name = "Slot 10", desc = "Keybind for loadout slot 10.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    @FeatureDependencyRequirement("#slotKeybindsToggle")
    var slot10: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Slot 11", desc = "Keybind for loadout slot 11.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    @FeatureDependencyRequirement("#slotKeybindsToggle")
    var slot11: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Slot 12", desc = "Keybind for loadout slot 12.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    @FeatureDependencyRequirement("#slotKeybindsToggle")
    var slot12: Int = GLFW.GLFW_KEY_UNKNOWN
}
