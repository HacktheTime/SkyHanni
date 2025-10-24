package de.hype.bingonet.shared.tutorials.steps

import de.hype.bingonet.shared.objects.WaypointData
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.paths.TutorialFork

abstract class TutorialStep(
    val guiderMakerExtraNotes: String? = null,
) : TutorialNode() {
    open fun onReset(tutorial: Tutorial) {}
    open fun onActivate(tutorial: Tutorial) {
    }

    open fun onDeactivate(tutorial: Tutorial) {}
    var isActive: Boolean = false
    var completed: Boolean = false

    abstract fun getStepName(tutorial: Tutorial): String
    abstract fun getStepDescription(tutorial: Tutorial): String?

    /**
     * Default check implementation so subclasses only need to override this
     * instead of wiring both onActivate and isComplete manually.
     */
    open fun check(tutorial: Tutorial): Boolean = false

    override fun isComplete(tutorial: Tutorial): Boolean {
        return completed || check(tutorial)
    }


    open fun ignoreEvent(): Boolean = !isActive

    //TODO add render waypoint or go to postion maybe? and then add /shtutorial routeme to show path to take maybe?

    //TODO it would be good to pre plan any item
    // the user obtains on whether it might be needed later still and to keep that amount then by highlighting needed stacks
    // to avoid selling needed items. sounds hyper complicated though. maybe not feasable

    override fun reset(tutorial: Tutorial) {
        TutorialStepLogic.reset(this, tutorial)
    }

    open fun showOnActive() = true

    override fun refresh(tutorial: Tutorial) {}

    val waypoints: MutableList<WaypointData> = mutableListOf()

    override fun populateNodeIds(tutorial: Tutorial) {
        nodeId = tutorial.generateNodeId()
    }

    abstract fun getRequirements(): List<TutorialNode>

    fun getRecursiveRequirements(tutorial: Tutorial): List<TutorialNode> {
        return TutorialStepLogic.getRecursiveRequirements(this, tutorial)
    }

    open fun complete() {
        TutorialStepLogic.complete(this)
    }

    protected fun chatPromptSuggestion(message: String, code: () -> Unit) {
        TutorialStepLogic.chatPromptSuggestion(message, code)
    }
}
