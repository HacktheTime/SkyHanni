package at.hannibal2.skyhanni.config.features.dev

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HypixelToolsConfig {
    @Expose
    @ConfigOption(
        name = "Use HypixelTools Repository",
        desc = "Use the typed HypixelTools repository for canonical item observations and lookups.",
    )
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @Expose
    @ConfigOption(
        name = "Allow Automatic Repository Updates",
        desc = "Allow observed missing items, NPCs, and inventories to be written as provisional local data.",
    )
    @ConfigEditorBoolean
    var allowAutomaticUpdates: Boolean = false

    @Expose
    @ConfigOption(
        name = "Report Observations",
        desc = "With automatic updates enabled, submit provisional observations to the contribution server.",
    )
    @ConfigEditorBoolean
    var reportObservations: Boolean = false

    @Expose
    @ConfigOption(name = "Canonical Repository Location", desc = "Directory containing the HypixelTools canonical repository.")
    @ConfigEditorText
    var repositoryDirectory: String = "config/skyhanni/hypixeltools"

    @Expose
    @ConfigOption(name = "Contribution Server URL", desc = "HTTPS endpoint receiving authenticated community observations.")
    @ConfigEditorText
    var contributionServerUrl: String = "https://contributions.hackthetime.de"
}
