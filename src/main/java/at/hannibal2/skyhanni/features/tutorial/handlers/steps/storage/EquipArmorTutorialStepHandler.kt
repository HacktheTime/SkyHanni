package at.hannibal2.skyhanni.features.tutorial.handlers.steps.storage

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.storagestep.EquipArmorTutorialStep

class EquipArmorTutorialStepHandler : TutorialStepHandler<EquipArmorTutorialStep> {
    override fun getStepName(step: EquipArmorTutorialStep, tutorial: Tutorial): String = "EquipArmorTutorialStep step"
    override fun getStepDescription(step: EquipArmorTutorialStep, tutorial: Tutorial): String? = null
    override fun getRequirements(step: EquipArmorTutorialStep): List<TutorialNode> = emptyList()
    
    companion object {
        private val activeSteps = mutableSetOf<EquipArmorTutorialStep>()
    }
}
