package de.hype.bingonet.shared.tutorials.steps

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.tutorials.TutorialStepCompleteEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.paths.TutorialFork

/**
 * Logic handler for TutorialStep operations.
 * Separates business logic from data structure.
 */
object TutorialStepLogic {

    /**
     * Handle the reset logic for a tutorial step
     */
    fun reset(step: TutorialStep, tutorial: Tutorial) {
        step.onReset(tutorial)
        step.completed = false
        step.isActive = false
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
            if (node is TutorialStep) {
                node.getRequirements().forEach {
                    if (!reqs.contains(it)) {
                        reqs.add(it)
                        collect(it)
                    }
                }
            } else if (node is TutorialFork) {
                node.getNodes(tutorial).forEach {
                    if (!reqs.contains(it)) {
                        reqs.add(it)
                        collect(it)
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
