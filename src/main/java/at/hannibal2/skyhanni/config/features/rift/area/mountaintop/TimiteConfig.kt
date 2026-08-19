package at.hannibal2.skyhanni.config.features.rift.area.mountaintop

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.IndividualItemTrackerConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class TimiteConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Helps you with mining Timite and Obsolite.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Timite Evolution Timer", desc = "Count down the time until Timite evolves with the time gun.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var evolutionTimer: Boolean = true

    @Expose
    @ConfigOption(name = "Expiry Timer", desc = "Count down the time until Timite/Obsolite expires.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var expiryTimer: Boolean = true

    @Expose
    @ConfigOption(name = "Timite Tracker", desc = "Tracks collected Timite ores and shows mote profit.")
    @ConfigEditorBoolean
    @FeatureToggle
    var tracker: Boolean = false

    @Expose
    @ConfigOption(name = "Only Show While Holding", desc = "Only shows the tracker while holding the Timite pickaxes or the Time Gun.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#tracker")
    var onlyShowWhileHolding: Boolean = false

    @Expose
    @ConfigOption(
        name = "Tracker Settings",
        desc = ""
    )
    @Accordion
    val perTrackerConfig: IndividualItemTrackerConfig = IndividualItemTrackerConfig()

    @Expose
    @ConfigLink(owner = TimiteConfig::class, field = "evolutionTimer")
    @FeatureDependencyRequirement("#evolutionTimer")
    val timerPosition: Position = Position(421, -220)

    @Expose
    @ConfigLink(owner = TimiteConfig::class, field = "tracker")
    @FeatureDependencyRequirement("#tracker")
    val trackerPosition: Position = Position(-201, -220)
}
