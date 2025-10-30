package at.hannibal2.skyhanni.features.tutorial.handlers.steps.storage

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import at.hannibal2.skyhanni.utils.InventoryUtils
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic
import de.hype.bingonet.shared.tutorials.steps.storagestep.ObtainTutorialStep

class ObtainTutorialStepHandler : TutorialStepHandler<ObtainTutorialStep> {
    
    override fun getStepName(step: ObtainTutorialStep, tutorial: Tutorial): String {
        return "Obtain ${step.amount}x ${step.itemStack.getDisplayNameOrDefault()}"
    }
    
    override fun getStepDescription(step: ObtainTutorialStep, tutorial: Tutorial): String? {
        return null
    }
    
    override fun getRequirements(step: ObtainTutorialStep): List<TutorialNode> {
        return step.requirements
    }
    
    override fun check(step: ObtainTutorialStep, tutorial: Tutorial): Boolean {
        return InventoryUtils.countItemsInLowerInventory { it.getInternalName() == step.itemStack } >= step.amount
    }
    
    override fun onActivate(step: ObtainTutorialStep, tutorial: Tutorial) {
        if (check(step, tutorial)) {
            TutorialStepLogic.complete(step)
        }
        activeSteps.add(step)
    }
    
    override fun onDeactivate(step: ObtainTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    override fun onReset(step: ObtainTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    @HandleEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        activeSteps.forEach { step ->
            if (!ignoreEvent(step) && check(step, Tutorial.DUMMY)) {
                TutorialStepLogic.complete(step)
            }
        }
    }
    
    companion object {
        private val activeSteps = mutableSetOf<ObtainTutorialStep>()
    }
}
