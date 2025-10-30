package at.hannibal2.skyhanni.features.tutorial.handlers

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import kotlin.reflect.KClass

/**
 * Registry for tutorial step handlers.
 * Maps step types to their handlers for efficient processing.
 */
object TutorialHandlerRegistry {
    
    private val stepHandlers = mutableMapOf<KClass<out TutorialStep>, TutorialStepHandler<*>>()
    
    /**
     * Register a handler for a specific step type
     */
    fun <T : TutorialStep> registerStepHandler(stepClass: KClass<T>, handler: TutorialStepHandler<T>) {
        stepHandlers[stepClass] = handler
        handler.registerEventHandlers()
    }
    
    /**
     * Get handler for a specific step
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : TutorialStep> getHandler(step: T): TutorialStepHandler<T>? {
        return stepHandlers[step::class] as? TutorialStepHandler<T>
    }
    
    /**
     * Get step name using registered handler
     */
    fun getStepName(step: TutorialStep, tutorial: Tutorial): String {
        val handler = getHandler(step)
        return handler?.getStepName(step, tutorial) ?: "Unknown Step"
    }
    
    /**
     * Get step description using registered handler
     */
    fun getStepDescription(step: TutorialStep, tutorial: Tutorial): String? {
        val handler = getHandler(step)
        return handler?.getStepDescription(step, tutorial)
    }
    
    /**
     * Get requirements using registered handler
     */
    fun getRequirements(step: TutorialStep): List<TutorialNode> {
        val handler = getHandler(step)
        return handler?.getRequirements(step) ?: emptyList()
    }
    
    /**
     * Check if step is complete using registered handler
     */
    fun check(step: TutorialStep, tutorial: Tutorial): Boolean {
        val handler = getHandler(step)
        return handler?.check(step, tutorial) ?: false
    }
    
    /**
     * Check if step should show when active
     */
    fun showOnActive(step: TutorialStep): Boolean {
        val handler = getHandler(step)
        return handler?.showOnActive(step) ?: true
    }
    
    /**
     * Check if events should be ignored
     */
    fun ignoreEvent(step: TutorialStep): Boolean {
        val handler = getHandler(step)
        return handler?.ignoreEvent(step) ?: !step.isActive
    }
    
    /**
     * Call onReset using registered handler
     */
    fun onReset(step: TutorialStep, tutorial: Tutorial) {
        val handler = getHandler(step)
        handler?.onReset(step, tutorial)
    }
    
    /**
     * Call onActivate using registered handler
     */
    fun onActivate(step: TutorialStep, tutorial: Tutorial) {
        val handler = getHandler(step)
        handler?.onActivate(step, tutorial)
    }
    
    /**
     * Call onDeactivate using registered handler
     */
    fun onDeactivate(step: TutorialStep, tutorial: Tutorial) {
        val handler = getHandler(step)
        handler?.onDeactivate(step, tutorial)
    }
    
    /**
     * Initialize all handlers
     */
    fun init() {
        // Handlers will be registered here
        // This method should be called on mod init
    }
}
