package de.hype.bingonet.shared.tutorials.steps

import de.hype.bingonet.shared.tutorials.Tutorial

class TextTutorialStep(
    val name: String,
    val description: String,
) : de.hype.bingonet.shared.tutorials.steps.TutorialStep() {
    override fun getStepName(): String {
        return name
    }

    override fun getStepDescription(tutorial: Tutorial): String {
        return "$description\n This Skip does not auto complete! run /shtutorial skip to skip this step."
    }
}
