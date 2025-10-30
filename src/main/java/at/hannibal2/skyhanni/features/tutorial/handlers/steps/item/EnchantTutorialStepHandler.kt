package at.hannibal2.skyhanni.features.tutorial.handlers.steps.item

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ItemEnchantedEvent
import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic
import de.hype.bingonet.shared.tutorials.steps.itemstep.EnchantTutorialStep

class EnchantTutorialStepHandler : TutorialStepHandler<EnchantTutorialStep> {
    
    override fun getStepName(step: EnchantTutorialStep, tutorial: Tutorial): String {
        return "Enchant ${step.item.getDisplayNameOrDefault()} with specified enchantments"
    }
    
    override fun getStepDescription(step: EnchantTutorialStep, tutorial: Tutorial): String? {
        return "Required enchantments: ${step.enchantIds.joinToString(", ")}"
    }
    
    override fun getRequirements(step: EnchantTutorialStep): List<TutorialNode> {
        return emptyList()
    }
    
    override fun onActivate(step: EnchantTutorialStep, tutorial: Tutorial) {
        activeSteps.add(step)
    }
    
    override fun onDeactivate(step: EnchantTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    override fun onReset(step: EnchantTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    @HandleEvent
    fun onItemEnchanted(event: ItemEnchantedEvent) {
        activeSteps.forEach { step ->
            if (!ignoreEvent(step) && 
                event.item.getInternalName() == step.item &&
                step.enchantIds.all { enchId -> event.enchantments.contains(enchId) }) {
                TutorialStepLogic.complete(step)
            }
        }
    }
    
    companion object {
        private val activeSteps = mutableSetOf<EnchantTutorialStep>()
    }
}
