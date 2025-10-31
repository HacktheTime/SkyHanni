package at.hannibal2.skyhanni.features.tutorial.handlers.steps

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TextTutorialStep

/**
 * Handler for TextTutorialStep.
 * Simple text step that requires manual skip.
 */
class TextTutorialStepHandler : TutorialStepHandler<TextTutorialStep> {
    
    override fun getStepName(step: TextTutorialStep, tutorial: Tutorial): String {
        return step.name
    }
    
    override fun getStepDescription(step: TextTutorialStep, tutorial: Tutorial): String {
        return "${step.description}\n This step does not auto complete! Run /shtutorial skip to skip this step."
    }
    
    override fun getRequirements(step: TextTutorialStep): List<TutorialNode> {
        return emptyList()
    }
}
