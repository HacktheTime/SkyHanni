package de.hype.bingonet.shared.tutorials.steps.location

import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.objects.Position
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class GoToPositionTutorialStep(
    /**
     * If null use SkyHannis internal routing System
     */
    val node: List<Position>?,
    val island: Islands,
    val allowSkip: Boolean = true,
) : TutorialStep() {

    override fun getStepName(tutorial: Tutorial): String = "Go to position"

    override fun getStepDescription(tutorial: Tutorial): String? = null

    override fun getRequirements(): List<TutorialNode> = emptyList()
}
