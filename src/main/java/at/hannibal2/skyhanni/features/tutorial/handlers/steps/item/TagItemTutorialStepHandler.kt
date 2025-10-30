package at.hannibal2.skyhanni.features.tutorial.handlers.steps.item

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ItemTagEvent
import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic
import de.hype.bingonet.shared.tutorials.steps.itemstep.TagItemTutorialStep

class TagItemTutorialStepHandler : TutorialStepHandler<TagItemTutorialStep> {
    
    override fun getStepName(step: TagItemTutorialStep, tutorial: Tutorial): String {
        return "Tag an item with '${step.tagName}'"
    }
    
    override fun getStepDescription(step: TagItemTutorialStep, tutorial: Tutorial): String? {
        return step.explenation
    }
    
    override fun getRequirements(step: TagItemTutorialStep): List<TutorialNode> {
        return emptyList()
    }
    
    override fun onActivate(step: TagItemTutorialStep, tutorial: Tutorial) {
        TutorialStepLogic.chatPromptSuggestion("Tag an item with '${step.tagName}'") {
            // User can tag via GUI
        }
        activeSteps.add(step)
    }
    
    override fun onDeactivate(step: TagItemTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    override fun onReset(step: TagItemTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    @HandleEvent
    fun onItemTag(event: ItemTagEvent) {
        activeSteps.forEach { step ->
            if (!ignoreEvent(step) && event.tag == step.tagName) {
                TutorialStepLogic.complete(step)
            }
        }
    }
    
    companion object {
        private val activeSteps = mutableSetOf<TagItemTutorialStep>()
    }
}
