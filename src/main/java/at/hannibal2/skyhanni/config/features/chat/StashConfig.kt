package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class StashConfig {
    @Expose
    @ConfigOption(name = "Stash Warnings", desc = "Compact warnings relating to items/materials in your stash.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    @ConfigOption(
        name = "§cNotice",
        desc = "Hypixel sends undetectable empty messages wrapping the stash message. " +
            "Enable §e§l/sh empty messages §r§7to hide them."
    )
    @ConfigEditorInfoText
    val notice: String = ""

    @Expose
    @ConfigOption(name = "Hide Dupe Warnings", desc = "")
    @Accordion
    val hideDuplicateWarning: HideDuplicateWarningConfig = HideDuplicateWarningConfig()

    @FeatureDependencyRequirement("StashConfig#enabled")
    class HideDuplicateWarningConfig {
        @Expose
        @ConfigOption(
            name = "Enabled",
            desc = "Hide duplicate warnings for previously reported stash counts."
        )
        @ConfigEditorBoolean
        var enabled: Boolean = true

        @Expose
        @ConfigOption(
            name = "Once Per World",
            desc = "Show warnings even if the counts are previously reported, once per world change."
        )
        @ConfigEditorBoolean
        @FeatureDependencyRequirement("#enabled")
        var worldChangeReset: Boolean = true
    }

    @Expose
    @ConfigOption(name = "Hide Added Messages", desc = "Hide the messages when something is added to your stash.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var hideAddedMessages: Boolean = true

    @Expose
    @ConfigOption(name = "Hide Low Warnings", desc = "Hide warnings with a total count below this number.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 1_000_000f, minStep = 100f)
    @FeatureDependencyRequirement("#enabled")
    var hideLowWarningsThreshold: Int = 0

    @Expose
    @ConfigOption(name = "Use /ViewStash", desc = "Use /viewstash [type] instead of /pickupstash.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var useViewStash: Boolean = false

    @Expose
    @ConfigOption(name = "Disable Empty Warnings", desc = "Disable first-time warnings for empty messages left behind.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var disableEmptyWarnings: Boolean = false

    @Expose
    @ConfigOption(name = "Tab Complete Stash Command", desc = "Adds tab completion to the /viewstash commands.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#enabled")
    var tabCompleteStashCommand: Boolean = true
}
