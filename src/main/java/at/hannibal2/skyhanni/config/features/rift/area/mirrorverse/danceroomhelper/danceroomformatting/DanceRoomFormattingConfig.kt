package at.hannibal2.skyhanni.config.features.rift.area.mirrorverse.danceroomhelper.danceroomformatting

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

@FeatureDependencyRequirement(
    "at.hannibal2.skyhanni.config.features.rift.area.mirrorverse.danceroomhelper.DanceRoomHelperConfig#enabled",
)
class DanceRoomFormattingConfig {
    @Expose
    @ConfigOption(name = "Now", desc = "Formatting for \"Now:\"")
    @ConfigEditorText
    var now: String = "&7Now:"

    @Expose
    @ConfigOption(name = "Next", desc = "Formatting for \"Next:\"")
    @ConfigEditorText
    var next: String = "&7Next:"

    @Expose
    @ConfigOption(name = "Later", desc = "Formatting for \"Later:\"")
    @ConfigEditorText
    var later: String = "&7Later:"

    @Expose
    @ConfigOption(name = "Color Option", desc = "")
    @Accordion
    val color: ColorConfig = ColorConfig()
}
