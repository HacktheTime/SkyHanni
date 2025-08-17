package de.hype.bingonet.shared.tutorials.steps

class TextTutorialStep(
    val name: String,
    val description: String,
) : de.hype.bingonet.shared.tutorials.steps.TutorialStep() {
    override fun getStepName(): String {
        return name
    }

    override fun getStepDescription(): String {
        return "$description\n This Skip does not auto complete! run /shtutorial skip to skip this step."
    }
}
