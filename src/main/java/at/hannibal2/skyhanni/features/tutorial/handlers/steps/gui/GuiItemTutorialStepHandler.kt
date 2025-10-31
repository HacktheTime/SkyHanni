package at.hannibal2.skyhanni.features.tutorial.handlers.steps.gui

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialStepLogic
import de.hype.bingonet.shared.tutorials.steps.guisteps.GuiItemTutorialStep

class GuiItemTutorialStepHandler : TutorialStepHandler<GuiItemTutorialStep> {
    
    override fun getStepName(step: GuiItemTutorialStep, tutorial: Tutorial): String {
        return "Check items in ${step.guiName}"
    }
    
    override fun getStepDescription(step: GuiItemTutorialStep, tutorial: Tutorial): String? {
        return step.description
    }
    
    override fun getRequirements(step: GuiItemTutorialStep): List<TutorialNode> {
        return emptyList()
    }
    
    override fun onActivate(step: GuiItemTutorialStep, tutorial: Tutorial) {
        activeSteps.add(step)
    }
    
    override fun onDeactivate(step: GuiItemTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    override fun onReset(step: GuiItemTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    @HandleEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        activeSteps.forEach { step ->
            if (!ignoreEvent(step)) {
                val inventory = event.inventoryItems
                val hasRequired = step.has?.all { reqItem ->
                    inventory.values.any { it.getInternalName() == reqItem }
                } ?: true
                val lacksProhibited = step.doesntHave?.none { prohibItem ->
                    inventory.values.any { it.getInternalName() == prohibItem }
                } ?: true
                
                if (hasRequired && lacksProhibited) {
                    TutorialStepLogic.complete(step)
                }
            }
        }
    }
    
    companion object {
        private val activeSteps = mutableSetOf<GuiItemTutorialStep>()
    }
}
