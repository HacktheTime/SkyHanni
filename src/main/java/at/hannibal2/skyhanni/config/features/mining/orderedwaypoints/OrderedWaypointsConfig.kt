package at.hannibal2.skyhanni.config.features.mining.orderedwaypoints

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class OrderedWaypointsConfig {

    @ConfigOption(name = "Credits", desc = "This feature is from Coleweight and SoopyV2, huge thanks to them!")
    @ConfigEditorInfoText
    val notice: String = ""

    @Expose
    @ConfigOption(name = "Enable Ordered Waypoints", desc = "Enables ordered waypoints.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Current Color", desc = "Color of the current ordered waypoint.")
    @ConfigEditorColour
    @FeatureDependencyRequirement("#enabled")
    var currentWaypointColor: ChromaColour = ChromaColour.fromRGB(85, 255, 85, 0, 153)

    @Expose
    @ConfigOption(name = "Previous Color", desc = "Color of the previous ordered waypoint.")
    @ConfigEditorColour
    @FeatureDependencyRequirement("#enabled")
    var previousWaypointColor: ChromaColour = ChromaColour.fromRGB(85, 85, 255, 0, 153)

    @Expose
    @ConfigOption(name = "Next Color", desc = "Color of the next ordered waypoint(s).")
    @ConfigEditorColour
    @FeatureDependencyRequirement("#enabled")
    var nextWaypointColor: ChromaColour = ChromaColour.fromRGB(255, 255, 85, 0, 153)

    @Expose
    @ConfigOption(name = "Next Waypoints", desc = "How many waypoints in front of the current waypoint should be rendered.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 5f, minStep = 1f)
    @FeatureDependencyRequirement("#enabled")
    var nextCount: Float = 2f

    @Expose
    @ConfigOption(name = "Block Outline Thickness", desc = "Thickness of the block outline.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    @FeatureDependencyRequirement("#enabled")
    var blockOutlineThickness: Float = 1f

    @Expose
    @ConfigOption(name = "Fill Block", desc = "Whether the waypoints should be filled instead of just being the outline.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var fillBlock: Boolean = false

    @Expose
    @ConfigOption(name = "Waypoint Range", desc = "How close you have to be for it to go to the next waypoint.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 0.1f)
    @FeatureDependencyRequirement("#enabled")
    var waypointRange: Float = 3f

    @Expose
    @ConfigOption(name = "Enable trace line", desc = "Enables the trace line.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var traceLine: Boolean = true

    @Expose
    @ConfigOption(name = "Trace Line Color", desc = "Color of the trace line.")
    @ConfigEditorColour
    @FeatureDependencyRequirement("#enabled", "#traceLine")
    var traceLineColor: ChromaColour = ChromaColour.fromRGB(85, 255, 85, 0, 255)

    @Expose
    @ConfigOption(name = "Trace Line Thickness", desc = "Thickness of the trace line.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    @FeatureDependencyRequirement("#enabled", "#traceLine")
    var traceLineThickness: Float = 1.0f

    @Expose
    @ConfigOption(name = "Show Distance", desc = "Whether the distance for ordered waypoints should be shown.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var showDistance: Boolean = true

    @Expose
    @ConfigOption(name = "Show Name", desc = "Whether the name for ordered waypoints should be shown.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var showName: Boolean = true

    @Expose
    @ConfigOption(name = "Setup Mode", desc = "Setup mode for route clearing.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var setupMode: Boolean = false

    @Expose
    @ConfigOption(name = "Setup Mode Line Color", desc = "Line color for the setup mode lines.")
    @ConfigEditorColour
    @FeatureDependencyRequirement("#enabled", "#setupMode")
    var setupModeLineColor: ChromaColour = ChromaColour.fromStaticRGB(255, 0, 0, 102)

    @Expose
    @ConfigOption(name = "Setup Mode Waypoint Color", desc = "Color used for additional waypoints displayed by setup mode.")
    @ConfigEditorColour
    @FeatureDependencyRequirement("#enabled", "#setupMode")
    var setupModeColor: ChromaColour = ChromaColour.fromStaticRGB(255, 0, 0, 102)

    @Expose
    @ConfigOption(name = "Setup Mode Range", desc = "How close you need to be for nearby waypoints to show in setup mode.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 100f, minStep = 1f)
    @FeatureDependencyRequirement("#enabled", "#setupMode")
    var setupModeRange: Float = 16f

    @Expose
    @ConfigOption(name = "Setup Mode Line Thickness", desc = "Thickness of the setup mode lines.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    @FeatureDependencyRequirement("#enabled", "#setupMode")
    var setupModeLineThickness: Float = 1.0f

    @Expose
    @ConfigOption(
        name = "Sneaking During Route",
        desc = "" +
            "Whether you'll be sneaking when moving between waypoints (e.g., using AOTV)." +
            "This is used for drawing the line of sight line for setup mode.",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled", "#setupMode")
    var sneakingDuringRoute: Boolean = true

    @Expose
    @ConfigOption(name = "Show All Waypoints", desc = "Whether all waypoints should be displayed.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var showAll: Boolean = false

    @Expose
    @ConfigOption(name = "All Waypoint Color", desc = "Color used for waypoints when using show all mode.")
    @ConfigEditorColour
    @FeatureDependencyRequirement("#enabled", "#showAll")
    var showAllWaypointColor: ChromaColour = ChromaColour.fromStaticRGB(0, 255, 0, 102)

    @Expose
    @ConfigOption(
        name = "Auto Unload",
        desc = "Automatically unloads route when changing Islands (Including Mineshafts)."
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var autoUnload: Boolean = false

    @Expose
    @ConfigOption(
        name = "Auto-Load Shaft Routes",
        desc = "Automatically loads a matching route if found when entering a Mineshaft. " +
            "(Format is from the scoreboard, e.g. JASP_1/PERI_C)"
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var autoLoadMatchingShaftRoute: Boolean = false

    @Expose
    @ConfigOption(
        name = "Auto unload Mineshaft",
        desc = "Automatically unloads SHO route when leaving mineshafts."
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var autoUnloadWhenLeavingMineshaft: Boolean = false

    @Expose
    @ConfigOption(
        name = "Auto-Skip Forward",
        desc = "Automatically skip forward when reaching a waypoint further down in the route.",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var autoSkipForward: Boolean = false
}
