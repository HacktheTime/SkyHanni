package at.hannibal2.skyhanni.features.tutorial.handlers.steps.requirement

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.requirement.NeuRecipeRequirement

class NeuRecipeRequirementHandler : TutorialStepHandler<NeuRecipeRequirement> {
    override fun getStepName(step: NeuRecipeRequirement, tutorial: Tutorial): String = "NeuRecipeRequirement step"
    override fun getStepDescription(step: NeuRecipeRequirement, tutorial: Tutorial): String? = null
    override fun getRequirements(step: NeuRecipeRequirement): List<TutorialNode> = emptyList()
    
    companion object {
        private val activeSteps = mutableSetOf<NeuRecipeRequirement>()
    }
}
