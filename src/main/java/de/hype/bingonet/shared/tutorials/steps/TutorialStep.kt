package de.hype.bingonet.shared.tutorials.steps

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ChatUtils

abstract class TutorialStep(
    val guiderMakerExtraNotes: String? = null,
) : TutorialNode {
    open fun onReset() {}
    open fun onActivate() {}
    open fun onDeactivate() {}
    var isActive: Boolean = false
    var completed: Boolean = false

    abstract fun getStepName(): String
    abstract fun getStepDescription(): String?

    open fun complete() {
        completed = true
    }

    protected fun chatPromptSuggestion(message: String, code: () -> Unit) {
        ChatUtils.chatPrompt("§e[SkyHanni Tutorial] $message (Press %KEYBIND% to activate)", SkyHanniMod.feature.tutorials.chatPromptKey, code, prefix = false)
    }

    open fun ignoreEvent(): Boolean{
        return !isActive
    }
}
