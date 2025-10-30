package at.hannibal2.skyhanni.features.tutorial.handlers.steps.item

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ItemReforgedEvent
import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic
import de.hype.bingonet.shared.tutorials.steps.itemstep.ReforgeTutorialStep

class ReforgeTutorialStepHandler : TutorialStepHandler<ReforgeTutorialStep> {
    
    override fun getStepName(step: ReforgeTutorialStep, tutorial: Tutorial): String {
        return "Reforge ${step.item.getDisplayNameOrDefault()} to ${step.reforgeName}"
    }
    
    override fun getStepDescription(step: ReforgeTutorialStep, tutorial: Tutorial): String? {
        return null
    }
    
    override fun getRequirements(step: ReforgeTutorialStep): List<TutorialNode> {
        return emptyList()
    }
    
    override fun onActivate(step: ReforgeTutorialStep, tutorial: Tutorial) {
        activeSteps.add(step)
    }
    
    override fun onDeactivate(step: ReforgeTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    override fun onReset(step: ReforgeTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    @HandleEvent
    fun onItemReforged(event: ItemReforgedEvent) {
        activeSteps.forEach { step ->
            if (!ignoreEvent(step) && 
                event.item.getInternalName() == step.item &&
                event.reforgeName == step.reforgeName) {
                TutorialStepLogic.complete(step)
            }
        }
    }
    
    companion object {
        private val activeSteps = mutableSetOf<ReforgeTutorialStep>()
    }
}
