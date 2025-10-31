package at.hannibal2.skyhanni.features.tutorial.handlers.steps.requirement

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.requirement.CollectionLevelRequirement

class CollectionLevelRequirementHandler : TutorialStepHandler<CollectionLevelRequirement> {
    override fun getStepName(step: CollectionLevelRequirement, tutorial: Tutorial): String = "CollectionLevelRequirement step"
    override fun getStepDescription(step: CollectionLevelRequirement, tutorial: Tutorial): String? = null
    override fun getRequirements(step: CollectionLevelRequirement): List<TutorialNode> = emptyList()
    
    companion object {
        private val activeSteps = mutableSetOf<CollectionLevelRequirement>()
    }
}
