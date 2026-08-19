package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.DependencyDelegate
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class HideArmorConfig {
    @Expose
    @ConfigOption(name = "Mode", desc = "Hide the armor of players.")
    @ConfigEditorDropdown
    var mode: ModeEntry = ModeEntry.OFF

    enum class ModeEntry(private val displayName: String) {
        ALL("All"),
        OWN("Own Armor"),
        OTHERS("Other's Armor"),
        OFF("Off"),
        ;

        override fun toString() = displayName
    }

    @DependencyDelegate(field = "mode")
    private val hasMode: Boolean
        get() = mode != ModeEntry.OFF

    @Expose
    @ConfigOption(name = "Only Helmet", desc = "Only hide the helmet.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#hasMode")
    var onlyHelmet: Boolean = false
}
