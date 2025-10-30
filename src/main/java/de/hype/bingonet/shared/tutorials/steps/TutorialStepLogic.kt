package de.hype.bingonet.shared.tutorials.steps

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.tutorials.TutorialStepCompleteEvent
import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialHandlerRegistry
import at.hannibal2.skyhanni.utils.ChatUtils
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.paths.TutorialFork
import de.hype.bingonet.shared.tutorials.paths.TutorialForkLogic

/**
 * Logic handler for TutorialStep operations.
 * Handles all business logic for tutorial steps, keeping the step classes as pure data.
 * Delegates to type-specific handlers for step-specific logic.
 */
object TutorialStepLogic {

    /**
     * Check if a step is complete
     */
    fun isComplete(step: TutorialStep, tutorial: Tutorial): Boolean {
        return step.completed || TutorialHandlerRegistry.check(step, tutorial)
    }

    /**
     * Handle the reset logic for a tutorial step
     */
    fun reset(step: TutorialStep, tutorial: Tutorial) {
        TutorialHandlerRegistry.onReset(step, tutorial)
        step.completed = false
        step.isActive = false
    }

    /**
     * Refresh a tutorial step (currently no-op but can be extended)
     */
    fun refresh(step: TutorialStep, tutorial: Tutorial) {
        // Empty implementation - can be extended by handlers
    }

    /**
     * Populate node IDs for a step
     */
    fun populateNodeIds(step: TutorialStep, tutorial: Tutorial) {
        step.nodeId = tutorial.generateNodeId()
    }

    /**
     * Validate a tutorial step (default empty, can be overridden)
     */
    fun validate(step: TutorialStep, tutorial: Tutorial): List<String> {
        return emptyList()
    }

    /**
     * Handle the complete logic for a tutorial step
     */
    fun complete(step: TutorialStep) {
        step.completed = true
        TutorialStepCompleteEvent(step).post()
    }

    /**
     * Get all recursive requirements for a step
     */
    fun getRecursiveRequirements(step: TutorialStep, tutorial: Tutorial): List<TutorialNode> {
        val reqs = mutableListOf<TutorialNode>()
        fun collect(node: TutorialNode) {
            when (node) {
                is TutorialStep -> {
                    TutorialHandlerRegistry.getRequirements(node).forEach {
                        if (!reqs.contains(it)) {
                            reqs.add(it)
                            collect(it)
                        }
                    }
                }
                is TutorialFork -> {
                    TutorialForkLogic.getNodes(node, tutorial).forEach {
                        if (!reqs.contains(it)) {
                            reqs.add(it)
                            collect(it)
                        }
                    }
                }
            }
        }
        collect(step)
        return reqs
    }

    /**
     * Show a chat prompt with suggestion for a step
     */
    fun chatPromptSuggestion(message: String, code: () -> Unit) {
        ChatUtils.chatPrompt(
            "§e[SkyHanni Tutorial] $message (Press %KEYBIND% to activate)",
            SkyHanniMod.feature.tutorials.chatPromptKey,
            code,
            prefix = false,
        )
    }
}
