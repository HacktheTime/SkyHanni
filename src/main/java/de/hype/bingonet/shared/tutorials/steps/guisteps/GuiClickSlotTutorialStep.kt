package de.hype.bingonet.shared.tutorials.steps.guisteps

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.GUIBasedTutorialStep
import java.awt.Color
import java.util.regex.Pattern

class GuiClickSlotTutorialStep(
    guiName: Pattern,
    val slotIndex: Int,
    val description: String? = null,
) : GUIBasedTutorialStep(guiName) {

    override fun getStepName(tutorial: Tutorial): String {
        return "Click the highlighted slot"
    }

    override fun getStepDescription(tutorial: Tutorial): String? {
        return description
    }

    override fun getRequirements(): List<TutorialNode> = emptyList()

    override fun isComplete(tutorial: Tutorial): Boolean = completed


    @HandleEvent
    fun onClick(event: GuiContainerEvent.SlotClickEvent) {
        if (ignoreEvent()) return
        val target = InventoryUtils.getItemsInOpenChestWithNull().getOrNull(slotIndex) ?: return
        if (event.slot == target) {
            complete()
        }
    }

    @HandleEvent
    fun highlightSlot(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (ignoreEvent()) return
        InventoryUtils.getItemsInOpenChestWithNull().getOrNull(slotIndex)?.highlight(
            Color.YELLOW,
        )
    }
}
