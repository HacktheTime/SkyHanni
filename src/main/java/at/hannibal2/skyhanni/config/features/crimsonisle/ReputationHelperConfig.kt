package at.hannibal2.skyhanni.config.features.crimsonisle

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.DependencyDelegate
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import org.lwjgl.glfw.GLFW

class ReputationHelperConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable features around Reputation features in the Crimson Isle.")
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @DependencyDelegate(field = "enabled")
    private var hasEnabled: Boolean
        get() = enabled.get()
        set(value) { enabled.set(value) }

    @Expose
    @ConfigOption(name = "Hide Completed", desc = "Hide tasks after they've been completed.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#hasEnabled")
    val hideComplete: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Use Hotkey", desc = "Only show the Reputation Helper while pressing the hotkey.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#hasEnabled")
    var useHotkey: Boolean = false

    @Expose
    @ConfigOption(name = "Hotkey", desc = "Press this hotkey to show the Reputation Helper.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    @FeatureDependencyRequirement("#hasEnabled", "#useHotkey")
    var hotkey: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigLink(owner = ReputationHelperConfig::class, field = "enabled")
    @FeatureDependencyRequirement("#hasEnabled")
    val position: Position = Position(10, 10)

    @Expose
    @ConfigOption(name = "Show Locations", desc = "Crimson Isles waypoints for locations to get reputation.")
    @ConfigEditorDropdown
    @FeatureDependencyRequirement("#hasEnabled")
    var showLocation: ShowLocationEntry = ShowLocationEntry.ONLY_HOTKEY

    @Expose
    @ConfigOption(name = "Rescue Mission", desc = "")
    @Accordion
    val rescueMission: RescueMissionConfig = RescueMissionConfig()

    enum class ShowLocationEntry(private val displayName: String) {
        ALWAYS("Always"),
        ONLY_HOTKEY("Only With Hotkey"),
        NEVER("Never");

        override fun toString() = displayName
    }
}
