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
    @Category(name = "Tutorials Chat Prompt Key", desc = "Chat Prompt key used for Tutorials.")
    val chatPromptKey: KeyBind = KeyBind()

}
