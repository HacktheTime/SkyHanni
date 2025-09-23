package de.hype.bingonet.shared.tutorials.steps.misc
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.bingo.BingoGoalReachedEvent
import at.hannibal2.skyhanni.features.bingo.BingoApi
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class ObtainBingoGoalTutorialStep(
    val displayName: String,
    /**
     * Set to false if your using this with an optional fork so it is not listed as alternative.
     */
    val showOnActive : Boolean = true
) : TutorialStep() {

    override fun getStepName(tutorial: Tutorial): String {
        return "Obtain the Bingo Goal: $displayName"
    }

    override fun getStepDescription(tutorial: Tutorial): String? = null

    override fun getRequirements(): List<TutorialNode> = emptyList()

    

    

    

    override fun showOnActive() = showOnActive

override fun isComplete(tutorial: Tutorial): Boolean {
        val goal = BingoApi.personalGoals.firstOrNull { it.displayName == displayName }
        return goal?.done == true || completed
    }

@HandleEvent
    fun onBingoGoalCompleted(event: BingoGoalReachedEvent) {
        if (event.goal.displayName == displayName) complete()
    }

override fun onActivate(tutorial: Tutorial) {
        val goal = BingoApi.personalGoals.firstOrNull { it.displayName == displayName }
        if (goal?.done == true) {
            complete()
        }
    }
}
