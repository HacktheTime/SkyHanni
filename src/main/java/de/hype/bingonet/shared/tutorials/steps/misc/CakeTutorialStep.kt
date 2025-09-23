package de.hype.bingonet.shared.tutorials.steps.misc
import at.hannibal2.skyhanni.features.misc.CenturyCakeAPI
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class CakeTutorialStep(
    val minimumDuration: Duration = 22.hours,
) : TutorialStep() {

    override fun getStepName(tutorial: Tutorial): String {
        return "Eat all Century Cakes"
    }

    override fun getStepDescription(tutorial: Tutorial): String? = null

    override fun getRequirements(): List<TutorialNode> = emptyList()

    

    

override fun isComplete(tutorial: Tutorial): Boolean {
        return CenturyCakeAPI.ateAllCakes(minimumDuration)
    }

override fun onActivate(tutorial: Tutorial) {
        if (CenturyCakeAPI.ateAllCakes(minimumDuration)) {
            complete()
        }
    }
}
