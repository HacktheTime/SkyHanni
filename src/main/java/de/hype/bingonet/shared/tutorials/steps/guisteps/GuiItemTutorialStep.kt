package de.hype.bingonet.shared.tutorials.steps.guisteps

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.GUIBasedTutorialStep
import java.util.regex.Pattern

class GuiItemTutorialStep(
    guiName: Pattern,
    val itemIndex: Int,
    val has: Regex?,
    val doesntHave: Regex?,
    val description: String? = null,
) : GUIBasedTutorialStep(guiName) {

    @HandleEvent
    fun checkMatch(event: SecondPassedEvent) {
        if (ignoreEvent()) return
        InventoryUtils.getItemsInOpenChestWithNull().getOrNull(itemIndex).let {
            val stack = it?.stack ?: return@let
            val has = has
            if (has!=null&&!(stack.displayName.matches(has)||stack.getLore().joinToString("\n").matches(has))) return@let
            if (doesntHave!=null&&(stack.displayName.contains(doesntHave)||stack.getLore().joinToString("\n").matches(doesntHave))) return@let
            complete()
        }
    }

    override fun getStepName(tutorial: Tutorial): String {
        return "Inspect item in GUI (slot $itemIndex)"
    }

    override fun getStepDescription(tutorial: Tutorial): String? {
        return description
    }

    override fun getRequirements(): List<TutorialNode> = emptyList()

    override fun isComplete(tutorial: Tutorial): Boolean = completed
}
