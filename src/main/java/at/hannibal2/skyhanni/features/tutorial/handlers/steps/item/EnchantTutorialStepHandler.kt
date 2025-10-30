package at.hannibal2.skyhanni.features.tutorial.handlers.steps.item

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.itemstep.EnchantTutorialStep

class EnchantTutorialStepHandler : TutorialStepHandler<EnchantTutorialStep> {
    override fun getStepName(step: EnchantTutorialStep, tutorial: Tutorial): String = "TODO: Implement"
    override fun getStepDescription(step: EnchantTutorialStep, tutorial: Tutorial): String? = null
    override fun getRequirements(step: EnchantTutorialStep): List<TutorialNode> = emptyList()
    
    companion object {
        private val activeSteps = mutableSetOf<EnchantTutorialStep>()
    }
}
