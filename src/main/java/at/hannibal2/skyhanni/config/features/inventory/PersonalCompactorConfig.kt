package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.DependencyDelegate
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

class PersonalCompactorConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable showing what items are inside your personal compactor/deletor.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Visibility Mode", desc = "Choose when to show the overlay.")
    @ConfigEditorDropdown
    @FeatureDependencyRequirement("#enabled")
    var visibilityMode: VisibilityMode = VisibilityMode.EXCEPT_KEYBIND

    @DependencyDelegate(field = "visibilityMode")
    private var hasKeybindMode: Boolean
        get() = visibilityMode != VisibilityMode.ALWAYS
        set(value) {
            visibilityMode = if (value) VisibilityMode.KEYBIND else VisibilityMode.ALWAYS
        }

    enum class VisibilityMode(private val displayName: String) {
        ALWAYS("Always"),
        KEYBIND("Keybind Held"),
        EXCEPT_KEYBIND("Except Keybind Held"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Keybind", desc = "The keybind to hold to show the overlay.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_LEFT_SHIFT)
    @FeatureDependencyRequirement("#enabled", "#hasKeybindMode")
    var keybind: Int = GLFW.GLFW_KEY_LEFT_SHIFT

    @Expose
    @ConfigOption(
        name = "Show On/Off",
        desc = "Show whether the Personal Compactor/Deletor is currently turned on or off.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    @FeatureDependencyRequirement("#enabled")
    var showToggle: Boolean = true
}
