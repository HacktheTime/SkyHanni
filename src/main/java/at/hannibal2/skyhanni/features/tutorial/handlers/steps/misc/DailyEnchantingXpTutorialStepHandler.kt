package at.hannibal2.skyhanni.features.tutorial.handlers.steps.misc

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.misc.DailyEnchantingXpTutorialStep

class DailyEnchantingXpTutorialStepHandler : TutorialStepHandler<DailyEnchantingXpTutorialStep> {
    override fun getStepName(step: DailyEnchantingXpTutorialStep, tutorial: Tutorial): String = "DailyEnchantingXp step"
    override fun getStepDescription(step: DailyEnchantingXpTutorialStep, tutorial: Tutorial): String? = null
    override fun getRequirements(step: DailyEnchantingXpTutorialStep): List<TutorialNode> = emptyList()
    
    companion object {
        private val activeSteps = mutableSetOf<DailyEnchantingXpTutorialStep>()
    }
}
