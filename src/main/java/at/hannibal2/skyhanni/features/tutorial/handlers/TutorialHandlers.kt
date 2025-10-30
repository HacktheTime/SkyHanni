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
        
        // Priority 1 - Core handlers
        TutorialHandlerRegistry.registerStepHandler(
            de.hype.bingonet.shared.tutorials.steps.CollectionTutorialStep::class,
            at.hannibal2.skyhanni.features.tutorial.handlers.steps.CollectionTutorialStepHandler()
        )
        TutorialHandlerRegistry.registerStepHandler(
            de.hype.bingonet.shared.tutorials.steps.storagestep.ObtainTutorialStep::class,
            at.hannibal2.skyhanni.features.tutorial.handlers.steps.storage.ObtainTutorialStepHandler()
        )
        TutorialHandlerRegistry.registerStepHandler(
            de.hype.bingonet.shared.tutorials.steps.itemstep.TagItemTutorialStep::class,
            at.hannibal2.skyhanni.features.tutorial.handlers.steps.item.TagItemTutorialStepHandler()
        )
        TutorialHandlerRegistry.registerStepHandler(
            de.hype.bingonet.shared.tutorials.steps.itemstep.EnchantTutorialStep::class,
            at.hannibal2.skyhanni.features.tutorial.handlers.steps.item.EnchantTutorialStepHandler()
        )
        TutorialHandlerRegistry.registerStepHandler(
            de.hype.bingonet.shared.tutorials.steps.itemstep.ReforgeTutorialStep::class,
            at.hannibal2.skyhanni.features.tutorial.handlers.steps.item.ReforgeTutorialStepHandler()
        )
        TutorialHandlerRegistry.registerStepHandler(
            de.hype.bingonet.shared.tutorials.steps.requirement.SkillTutorialStep::class,
            at.hannibal2.skyhanni.features.tutorial.handlers.steps.requirement.SkillTutorialStepHandler()
        )
        
        // TODO: Register remaining step handlers as they are implemented
        // - MinionTutorialStep
        // - AwaitMiningEvent  
        // - GUI-based steps (3)
        // - Misc steps (6)
        // - Requirement steps (3 more)
        // - Storage steps (2 more)
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
