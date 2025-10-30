package at.hannibal2.skyhanni.features.tutorial.handlers.steps.item

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.itemstep.TagItemTutorialStep

class TagItemTutorialStepHandler : TutorialStepHandler<TagItemTutorialStep> {
    override fun getStepName(step: TagItemTutorialStep, tutorial: Tutorial): String = "TODO: Implement"
    override fun getStepDescription(step: TagItemTutorialStep, tutorial: Tutorial): String? = null
    override fun getRequirements(step: TagItemTutorialStep): List<TutorialNode> = emptyList()
    
    companion object {
        private val activeSteps = mutableSetOf<TagItemTutorialStep>()
    }
}
