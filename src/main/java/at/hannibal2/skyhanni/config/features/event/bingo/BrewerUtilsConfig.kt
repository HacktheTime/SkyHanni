package at.hannibal2.skyhanni.config.features.event.bingo

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

class BrewerUtilsConfig {
    @Expose
    @ConfigOption(
        name = "Require Witch Pet",
        desc = "Prevents you from opening brewing stands if you don't have a Lvl 100 Rare Witch pet or higher equipped",
    )
    @FeatureToggle
    @ConfigEditorBoolean
    val requireWitchPet: Boolean = true

    @Expose
    @ConfigOption(
        name = "Highlight correct Item",
        desc = "Auto highlight the items to be inserted into the brewing stand in green. (Item Frames + Carpet Color Codes)",
    )
    @FeatureToggle
    @ConfigEditorBoolean
    val highlightCorrectItem = true

    @Expose
    @ConfigOption(name = "Insert Right Item into Brewing Stand Or Get from Sacks", desc = "Like Highlight correct Item but with Keybind. " +
        "If you do no have the needed materials in your inventory it will try to get them from your sacks. ")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    val insertKeyBind: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(
        name = "Get Brewing Item from Sacks Material Count",
        desc = "How many materials to get when pressing the Get Brewing Item from Sacks Keybind.",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 64f, minStep = 1f)
    val gfsMaterialCount = 64
}
