package at.hannibal2.skyhanni.features.tutorial.handlers.steps.misc

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.misc.EquipPetTutorialStep

class EquipPetTutorialStepHandler : TutorialStepHandler<EquipPetTutorialStep> {
    override fun getStepName(step: EquipPetTutorialStep, tutorial: Tutorial): String = "EquipPet step"
    override fun getStepDescription(step: EquipPetTutorialStep, tutorial: Tutorial): String? = null
    override fun getRequirements(step: EquipPetTutorialStep): List<TutorialNode> = emptyList()
    
    companion object {
        private val activeSteps = mutableSetOf<EquipPetTutorialStep>()
    }
}
