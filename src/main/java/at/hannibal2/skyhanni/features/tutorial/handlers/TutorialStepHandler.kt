package at.hannibal2.skyhanni.features.tutorial.handlers

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Base interface for step-specific handlers.
 * Each step type should have a handler implementing this interface.
 */
interface TutorialStepHandler<T : TutorialStep> {
    
    /**
     * Get the step name for display
     */
    fun getStepName(step: T, tutorial: Tutorial): String
    
    /**
     * Get the step description for display
     */
    fun getStepDescription(step: T, tutorial: Tutorial): String?
    
    /**
     * Get the list of required nodes
     */
    fun getRequirements(step: T): List<TutorialNode>
    
    /**
     * Check if the step is complete (active check)
     */
    fun check(step: T, tutorial: Tutorial): Boolean = false
    
    /**
     * Check if step should be shown when active
     */
    fun showOnActive(step: T): Boolean = true
    
    /**
     * Check if events should be ignored
     */
    fun ignoreEvent(step: T): Boolean = !step.isActive
    
    /**
     * Called when the step is reset
     */
    fun onReset(step: T, tutorial: Tutorial) {}
    
    /**
     * Called when the step is activated
     */
    fun onActivate(step: T, tutorial: Tutorial) {}
    
    /**
     * Called when the step is deactivated
     */
    fun onDeactivate(step: T, tutorial: Tutorial) {}
    
    /**
     * Register event handlers for this step type (passive checks)
     */
    fun registerEventHandlers() {}
}
