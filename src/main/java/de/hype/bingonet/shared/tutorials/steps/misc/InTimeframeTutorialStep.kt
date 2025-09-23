package de.hype.bingonet.shared.tutorials.steps.misc
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import java.time.Instant

class InTimeframeTutorialStep(
    val start: Instant = Instant.MIN,
    val end: Instant = Instant.MAX,
) : TutorialStep() {
    override fun getStepName(tutorial: Tutorial): String {
        if (Instant.now() < end) return "Wait until ${end} | → ${java.time.Duration.between(Instant.now(), start)}"
        return ""
    }

    override fun getStepDescription(tutorial: Tutorial): String? {
        return null
    }

    override fun getRequirements(): List<TutorialNode> {
        return emptyList()
    }

    override fun isComplete(tutorial: Tutorial): Boolean {
        val now = Instant.now()
        if (now < start) return false
        if (now > end) return false
        return true
    }
}
