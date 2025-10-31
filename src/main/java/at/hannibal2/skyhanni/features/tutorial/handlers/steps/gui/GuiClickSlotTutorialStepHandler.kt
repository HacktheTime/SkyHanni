package at.hannibal2.skyhanni.features.tutorial.handlers.steps.gui

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialStepLogic
import de.hype.bingonet.shared.tutorials.steps.guisteps.GuiClickSlotTutorialStep

class GuiClickSlotTutorialStepHandler : TutorialStepHandler<GuiClickSlotTutorialStep> {
    
    override fun getStepName(step: GuiClickSlotTutorialStep, tutorial: Tutorial): String {
        return "Click slot ${step.slotIndex} in ${step.guiName}"
    }
    
    override fun getStepDescription(step: GuiClickSlotTutorialStep, tutorial: Tutorial): String? {
        return step.description
    }
    
    override fun getRequirements(step: GuiClickSlotTutorialStep): List<TutorialNode> {
        return emptyList()
    }
    
    override fun onActivate(step: GuiClickSlotTutorialStep, tutorial: Tutorial) {
        activeSteps.add(step)
    }
    
    override fun onDeactivate(step: GuiClickSlotTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    override fun onReset(step: GuiClickSlotTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        activeSteps.forEach { step ->
            if (!ignoreEvent(step) && event.slotId == step.slotIndex) {
                TutorialStepLogic.complete(step)
            }
        }
    }
    
    companion object {
        private val activeSteps = mutableSetOf<GuiClickSlotTutorialStep>()
    }
}
