package at.hannibal2.skyhanni.features.tutorial.handlers.steps.gui

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.GUIBasedTutorialStep

open class GUIBasedTutorialStepHandler<T : GUIBasedTutorialStep> : TutorialStepHandler<T> {
    
    override fun getStepName(step: T, tutorial: Tutorial): String {
        return "Open GUI: ${step.guiName}"
    }
    
    override fun getStepDescription(step: T, tutorial: Tutorial): String? {
        return null
    }
    
    override fun getRequirements(step: T): List<TutorialNode> {
        return emptyList()
    }
}
