package at.hannibal2.skyhanni.config.features.tutorials

import at.hannibal2.skyhanni.config.core.config.KeyBind
import com.google.gson.annotations.Expose
import io.github.moulberry.notenoughupdates.core.config.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.Category


class TutorialConfig {

    @Expose
    @Category(
        name = "Assume Grand Carrier",
        desc = "Whether you have a friend that carries you the Grands. Means you do not need to obtain them yourself.",
    )
    @ConfigEditorBoolean
    var assumeGrandCarrier = false

    @Expose
    @Category(
        name = "Assume Max EChest",
        desc = "Max EChest means you have a lot of accessible Space in your EChest. " +
            "This can be used with Tutorials to route you better but require more Storage usage.",
    )
    @ConfigEditorBoolean
    var assumeMaxEChest = false

    @Expose
    @Category(
        name = "Mark to Keep Resources Across Paths",
        desc = "Mark Resources to keep across paths EVEN if you selected the other path in an optional tutorial step."
    )
    @ConfigEditorBoolean
    var protectResourcesAcrossPaths = true

    @Expose
    @Category(name = "Tutorials Chat Prompt Key", desc = "Chat Prompt key used for Tutorials.")
    val chatPromptKey: KeyBind = KeyBind()

}
