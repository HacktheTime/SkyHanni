package de.hype.bingonet.shared.tutorials.steps.guisteps

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import de.hype.bingonet.shared.tutorials.steps.GUIBasedTutorialStep
import java.awt.Color
import java.util.regex.Pattern

class GuiClickSlotTutorialStep(
    guiName: Pattern,
    val slotIndex: Int,
) : GUIBasedTutorialStep(guiName) {
    override fun getStepName(): String {
        TODO("Not yet implemented")
    }

    override fun getStepDescription(): String? {
        TODO("Not yet implemented")
    }

    @HandleEvent
    fun onClick(event: GuiContainerEvent.SlotClickEvent) {
        if (ignoreEvent()) return
        complete()
    }

    @HandleEvent
    fun highlightSlot(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (ignoreEvent()) return
        InventoryUtils.getItemsInOpenChestWithNull().getOrNull(slotIndex)?.highlight(
            Color.YELLOW,
        )
    }
}
