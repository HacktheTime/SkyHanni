package at.hannibal2.skyhanni.config.features.event.carnival

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ZombieShootoutConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "QOL Features for Zombie Shootout.")
    @FeatureToggle
    @ConfigEditorBoolean
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Colored Hitboxes", desc = "Display colored hitboxes for zombies and lamps.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var coloredHitboxes: Boolean = true

    @Expose
    @ConfigOption(name = "Zombie Timer", desc = "Displays a timer above the heads of the zombies.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var zombieTimer: Boolean = true

    @Expose
    @ConfigOption(name = "Highest Only", desc = "Only draw colored hitboxes/timers for the highest scoring zombies.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var highestOnly: Boolean = false

    @Expose
    @ConfigOption(name = "Colored Line", desc = "Display a colored line to lamps.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var coloredLines: Boolean = true

    @Expose
    @ConfigOption(name = "Lamp Timer", desc = "Show time until current lamp disappears.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var lampTimer: Boolean = true

    @Expose
    @ConfigLink(owner = ZombieShootoutConfig::class, field = "lampTimer")
    @FeatureDependencyRequirement("#lampTimer")
    val lampPosition: Position = Position(20, 20)
}
