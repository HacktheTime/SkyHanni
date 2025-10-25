package de.hype.bingonet.shared.tutorials.steps

import de.hype.bingonet.shared.objects.WaypointData
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode

/**
 * Pure data class for tutorial steps.
 * Contains only data properties and abstract methods for data access.
 * All logic is handled by TutorialStepLogic.
 */
abstract class TutorialStep(
    val guiderMakerExtraNotes: String? = null,
) : TutorialNode() {
    // Data properties
    var isActive: Boolean = false
    var completed: Boolean = false
    val waypoints: MutableList<WaypointData> = mutableListOf()

    // Abstract methods for data access (to be implemented by subclasses)
    abstract fun getStepName(tutorial: Tutorial): String
    abstract fun getStepDescription(tutorial: Tutorial): String?
    abstract fun getRequirements(): List<TutorialNode>
    
    /**
     * Default check implementation so subclasses only need to override this
     * instead of wiring both onActivate and isComplete manually.
     */
    open fun check(tutorial: Tutorial): Boolean = false
    
    open fun showOnActive() = true
    open fun ignoreEvent(): Boolean = !isActive

    // Lifecycle hooks for subclasses to override
    open fun onReset(tutorial: Tutorial) {}
    open fun onActivate(tutorial: Tutorial) {}
    open fun onDeactivate(tutorial: Tutorial) {}
}
