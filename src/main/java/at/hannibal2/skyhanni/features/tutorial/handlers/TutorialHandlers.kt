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
        
        // Register RequireAsyncCompletionTutorialStep (technically a step but in paths package)
        TutorialHandlerRegistry.registerStepHandler(
            de.hype.bingonet.shared.tutorials.paths.RequireAsyncCompletionTutorialStep::class,
            at.hannibal2.skyhanni.features.tutorial.handlers.steps.RequireAsyncCompletionTutorialStepHandler()
        )
        
        // Location steps
        TutorialHandlerRegistry.registerStepHandler(
            de.hype.bingonet.shared.tutorials.steps.location.GoToIslandTutorialStep::class,
            at.hannibal2.skyhanni.features.tutorial.handlers.steps.location.GoToIslandTutorialStepHandler()
        )
        TutorialHandlerRegistry.registerStepHandler(
            de.hype.bingonet.shared.tutorials.steps.location.GoToPositionTutorialStep::class,
            at.hannibal2.skyhanni.features.tutorial.handlers.steps.location.GoToPositionTutorialStepHandler()
        )
        TutorialHandlerRegistry.registerStepHandler(
            de.hype.bingonet.shared.tutorials.steps.location.JoinInstanceTutorialStep::class,
            at.hannibal2.skyhanni.features.tutorial.handlers.steps.location.JoinInstanceTutorialStepHandler()
        )
        TutorialHandlerRegistry.registerStepHandler(
            de.hype.bingonet.shared.tutorials.steps.location.SubAreaTutorialStep::class,
            at.hannibal2.skyhanni.features.tutorial.handlers.steps.location.SubAreaTutorialStepHandler()
        )
        
        // TODO: Register remaining step handlers as they are implemented
        // - CollectionTutorialStep
        // - MinionTutorialStep
        // - AwaitMiningEvent
        // - GUI-based steps (2)
        // - Item steps (3)
        // - Misc steps (6)
        // - Requirement steps (4)
        // - Storage steps (3)
    }
    
    private fun registerForkHandlers() {
        TutorialForkHandlerRegistry.registerForkHandler(
            de.hype.bingonet.shared.tutorials.paths.AsyncTutorialFork::class,
            at.hannibal2.skyhanni.features.tutorial.handlers.forks.AsyncTutorialForkHandler()
        )
        TutorialForkHandlerRegistry.registerForkHandler(
            de.hype.bingonet.shared.tutorials.paths.HiddenOptionalImprovementFork::class,
            at.hannibal2.skyhanni.features.tutorial.handlers.forks.HiddenOptionalImprovementForkHandler()
        )
        TutorialForkHandlerRegistry.registerForkHandler(
            de.hype.bingonet.shared.tutorials.paths.OptionalTutorialFork::class,
            at.hannibal2.skyhanni.features.tutorial.handlers.forks.OptionalTutorialForkHandler()
        )
        TutorialForkHandlerRegistry.registerForkHandler(
            de.hype.bingonet.shared.tutorials.paths.SelectPathTutorialFork::class,
            at.hannibal2.skyhanni.features.tutorial.handlers.forks.SelectPathTutorialForkHandler()
        )
        TutorialForkHandlerRegistry.registerForkHandler(
            de.hype.bingonet.shared.tutorials.paths.WhileTutorialNode::class,
            at.hannibal2.skyhanni.features.tutorial.handlers.forks.WhileTutorialNodeHandler()
        )
    }
}
