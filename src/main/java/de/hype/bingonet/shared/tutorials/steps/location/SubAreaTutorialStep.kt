package de.hype.bingonet.shared.tutorials.steps.location

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.skyblock.ScoreboardAreaChangeEvent
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class SubAreaTutorialStep(
    val area: String,
) : TutorialStep() {
    override fun getStepName(tutorial: Tutorial): String {
        return "Go to $area"
    }


    override fun getStepDescription(tutorial: Tutorial): String? {
        return null
    }

    override fun getRequirements(): List<TutorialNode> = emptyList()

    @HandleEvent
    fun onAreaChange(event: ScoreboardAreaChangeEvent) {
        if (event.area == area) complete()
    }
}
