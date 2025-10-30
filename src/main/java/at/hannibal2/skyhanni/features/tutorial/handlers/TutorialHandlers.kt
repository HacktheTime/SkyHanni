package at.hannibal2.skyhanni.features.tutorial.handlers

import at.hannibal2.skyhanni.features.tutorial.handlers.steps.MessageTutorialStepHandler
import at.hannibal2.skyhanni.features.tutorial.handlers.steps.TextTutorialStepHandler
import de.hype.bingonet.shared.tutorials.steps.MessageTutorialStep
import de.hype.bingonet.shared.tutorials.steps.TextTutorialStep

/**
 * Central initialization point for all tutorial handlers.
 * Call init() during mod initialization to register all handlers.
 */
object TutorialHandlers {
    
    private var initialized = false
    
    fun init() {
        if (initialized) return
        initialized = true
        
        // Register step handlers
        registerStepHandlers()
        
        // Register fork handlers
        registerForkHandlers()
    }
    
    private fun registerStepHandlers() {
        // Basic steps
        TutorialHandlerRegistry.registerStepHandler(MessageTutorialStep::class, MessageTutorialStepHandler())
        TutorialHandlerRegistry.registerStepHandler(TextTutorialStep::class, TextTutorialStepHandler())
        
        // TODO: Register remaining step handlers as they are implemented
        // - CollectionTutorialStep
        // - MinionTutorialStep
        // - AwaitMiningEvent
        // - GUI-based steps
        // - Item steps
        // - Location steps
        // - Misc steps
        // - Requirement steps
        // - Storage steps
    }
    
    private fun registerForkHandlers() {
        // TODO: Register fork handlers as they are implemented
        // - AsyncTutorialFork
        // - OptionalTutorialFork
        // - SelectPathTutorialFork
        // - HiddenOptionalImprovementFork
        // - RequireAsyncCompletionTutorialStep
        // - WhileTutorialNode
    }
}
