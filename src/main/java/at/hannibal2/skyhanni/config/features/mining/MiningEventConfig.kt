package at.hannibal2.skyhanni.config.features.mining

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.ThirdParty
import at.hannibal2.skyhanni.config.ThirdPartyDependency
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.mining.eventtracker.MiningEventType.Companion.CompressFormat
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

@ThirdPartyDependency(ThirdParty.SOOPY)
class MiningEventConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Show information about upcoming Dwarven Mines and Crystal Hollows mining events.\n" +
            "§eAlso enables sending data from your client. May take up to a minute to sync new events.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Show Outside Mining Islands",
        desc = "Show the event tracker even if you're outside of the Dwarven Mines or Crystal Hollows.",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var outsideMining: Boolean = false

    @Expose
    @ConfigOption(name = "What to Show", desc = "Choose which island's events are shown in the GUI.")
    @ConfigEditorDropdown
    @FeatureDependencyRequirement("#enabled")
    var showType: ShowType = ShowType.ALL

    @Expose
    @ConfigOption(name = "Compressed Format", desc = "Compress the event names so that they are shorter.")
    @ConfigEditorDropdown
    @FeatureDependencyRequirement("#enabled")
    var compressedFormat: CompressFormat = CompressFormat.DEFAULT

    @Expose
    @ConfigOption(name = "Compressed Island", desc = "Show the islands only as an icon.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var islandAsIcon: Boolean = false

    @Expose
    @ConfigOption(
        name = "Show Passed Events",
        desc = "Show the most recently passed event at the start, greyed out.\n" +
            "§eTakes a little while to save the last event.",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var passedEvents: Boolean = false

    enum class ShowType(private val displayName: String) {
        ALL("All Mining Islands"),
        CRYSTAL("Crystal Hollows Only"),
        DWARVEN("Dwarven Mines Only"),
        CURRENT("Current Island Only");

        override fun toString() = displayName
    }

    @Expose
    @ConfigLink(owner = MiningEventConfig::class, field = "enabled")
    @FeatureDependencyRequirement("#enabled")
    val position: Position = Position(200, 60)

    @Expose
    @ConfigOption(
        name = "Sharing Event Data",
        desc = "Sending Mining Event data to a server. This allows everyone to see more precise mining event timings." +
            " Thanks for your help!",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var allowDataSharing: Boolean = true

    @Expose
    @ConfigOption(name = "Goblin Raid Features", desc = "")
    @Accordion
    val goblinRaidConfig: GoblinRaidConfig = GoblinRaidConfig()
}
