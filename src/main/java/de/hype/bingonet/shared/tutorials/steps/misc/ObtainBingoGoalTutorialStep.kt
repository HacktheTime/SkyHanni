package de.hype.bingonet.shared.tutorials.steps.misc

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.bingo.BingoGoalReachedEvent
import at.hannibal2.skyhanni.features.bingo.BingoApi
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class ObtainBingoGoalTutorialStep(
    val displayName: String,
    /**
     * Set to false if your using this with an optional fork so it is not listed as alternative.
     */
    val showOnActive : Boolean = true
) : TutorialStep() {
    //TODO suggest displayname via API?

    //TODO use message event to detect goal completion + start check with manager.

    //TODO make it so each bingo goal is marked as optional fork so when done early or sth its auto completes anyway
    @HandleEvent
    fun onBingoGoalCompleted(event: BingoGoalReachedEvent) {
        if (event.goal.displayName == displayName) complete()
    }

    override fun onActivate() {
        val goal = BingoApi.personalGoals.firstOrNull { it.displayName == displayName }
        if (goal?.done ?: false) {
            complete()
        }
    }

    override fun showOnActive() = showOnActive
}
