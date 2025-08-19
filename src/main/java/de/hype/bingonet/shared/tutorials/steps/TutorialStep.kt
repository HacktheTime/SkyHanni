package de.hype.bingonet.shared.tutorials.steps

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ChatUtils
import de.hype.bingonet.shared.objects.WaypointData
import de.hype.bingonet.shared.tutorials.TutorialNode

abstract class TutorialStep(
    val guiderMakerExtraNotes: String? = null,
) : TutorialNode() {
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

    //TODO add render waypoint or go to postion maybe? and then add /shtutorial routeme to show path to take maybe?

    //TODO it would be good to pre plan any item
    // the user obtains on whether it might be needed later still and to keep that amount then by highlighting needed stacks
    // to avoid selling needed items. sounds hyper complicated though. maybe not feasable

    override fun reset() {
        onReset()
        completed = false
        isActive = false
    }

    open fun showOnActive() = true

    open fun refresh() {

    }

    val waypoints : MutableList<WaypointData> = mutableListOf()
}
