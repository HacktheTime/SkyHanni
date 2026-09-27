package at.hannibal2.skyhanni.config.features.dungeon

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class BossDialogueDetectorConfig {

    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Detects boss dialogue in Dungeons to trigger auto warp and party warnings.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Auto Warp",
        desc = "Automatically warp out when the final boss is defeated.\n" +
            "§7If more than 5 final kill dialogue lines exist, warns the party before warping.",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    @FeatureToggle
    var autoWarp: Boolean = false

    @Expose
    @ConfigOption(
        name = "Warn Phases to Party",
        desc = "Send a party chat message when a boss phase transition is detected.\n" +
            "§7On Entrance, this behaves like Warn Watcher Start.",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    @FeatureToggle
    var warnPhases: Boolean = false

    @Expose
    @ConfigOption(
        name = "Warn Watcher Start",
        desc = "Send a party chat message when the Watcher fight begins (Entrance boss or F1-F7 mini-boss).",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    @FeatureToggle
    var warnWatcherStart: Boolean = false
}
