package de.hype.bingonet.shared.tutorials.steps
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode

class MinionTutorialStep(val slots: Int) : TutorialStep() {
    //TODO following things
    // Add automatic tips for minion slots if not enough yet
    // Add detection for which minions need to be replaced etc.
    // like essentially apply a exact minion configuration with mismatch detection
    override fun getStepName(tutorial: Tutorial): String {
        return "Place $slots minions"
    }

    override fun getStepDescription(tutorial: Tutorial): String? {
        return null
    }

    override fun getRequirements(): List<TutorialNode> = emptyList()

    override fun isComplete(tutorial: Tutorial): Boolean = completed
}
