package de.hype.bingonet.shared.tutorials.steps

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode

class TextTutorialStep(
    val name: String,
    val description: String,
) : TutorialStep() {
    override fun getStepName(tutorial: Tutorial): String {
        return name
    }

    override fun getStepDescription(tutorial: Tutorial): String {
        return "$description\n This Skip does not auto complete! run /shtutorial skip to skip this step."
    }

    override fun getRequirements(): List<TutorialNode> = emptyList()
}
