package at.hannibal2.skyhanni.features.tutorial.handlers.steps.misc

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.MinionTutorialStep

class MinionTutorialStepHandler : TutorialStepHandler<MinionTutorialStep> {
    
    override fun getStepName(step: MinionTutorialStep, tutorial: Tutorial): String {
        return "Place ${step.slots} minion slots"
    }
    
    override fun getStepDescription(step: MinionTutorialStep, tutorial: Tutorial): String? {
        return null
    }
    
    override fun getRequirements(step: MinionTutorialStep): List<TutorialNode> {
        return emptyList()
    }
    
    override fun check(step: MinionTutorialStep, tutorial: Tutorial): Boolean {
        // Check minion slot count from profile data
        return false // TODO: Implement actual check
    }
}
