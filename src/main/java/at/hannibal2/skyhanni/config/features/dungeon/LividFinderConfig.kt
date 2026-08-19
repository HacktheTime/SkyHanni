package at.hannibal2.skyhanni.config.features.dungeon

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.DependencyDelegate
import at.hannibal2.skyhanni.features.dungeon.DungeonLividFinder.LividColorHighlight
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class LividFinderConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Help find the correct livid in F5 and in M5.")
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @DependencyDelegate(field = "enabled")
    private var hasEnabled: Boolean
        get() = enabled.get()
        set(value) { enabled.set(value) }

    @Expose
    @ConfigOption(name = "Hide Wrong Livids", desc = "Hide wrong livids entirely.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#hasEnabled")
    var hideWrong: Boolean = false

    @Expose
    @ConfigOption(name = "Color Override", desc = "Forces the livid highlight to be a specific color.")
    @ConfigEditorDropdown
    @FeatureDependencyRequirement("#hasEnabled")
    var colorOverride: LividColorHighlight = LividColorHighlight.DEFAULT
}
