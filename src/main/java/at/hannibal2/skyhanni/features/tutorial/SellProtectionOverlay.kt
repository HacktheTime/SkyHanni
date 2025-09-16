package at.hannibal2.skyhanni.features.tutorial

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.features.inventory.NPCManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.entity.player.InventoryPlayer

/**
 * Highlights inventory items that are protected by the active tutorial, and blocks selling to NPC when enabled.
 */
@SkyHanniModule
object SellProtectionOverlay {

    private val cfg get() = SkyHanniMod.feature.tutorials

    private fun enabled(): Boolean = cfg.tutorialProtectRequiredItems && TutorialManager.activeTutorial != null

    @HandleEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!enabled()) return
        val protected = SellProtection.getProtectedResources()
        if (protected.isEmpty()) return
        event.container.inventorySlots
            .filter { it.inventory is InventoryPlayer && it.stack != null }
            .forEach { slot ->
                val internal = slot.stack.getInternalNameOrNull() ?: return@forEach
                if (protected[internal]?.let { it > 0.0 } == true) {
                    slot.highlight(LorenzColor.DARK_RED)
                }
            }
    }

    @HandleEvent
    fun onTooltip(event: ToolTipEvent) {
        if (!enabled()) return
        val protected = SellProtection.getProtectedResources()
        if (protected.isEmpty()) return
        val internal = event.itemStack.getInternalNameOrNull() ?: return
        if (protected[internal]?.let { it > 0.0 } == true) {
            event.toolTip.add("§cSell-protected by Tutorial")
        }
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!enabled()) return
        // Only block selling while in an NPC shop (or recently clicked NPC)
        val inNpcGui = NPCManager.lastClickedNPC != null
        if (!inNpcGui) return
        val slot = event.slot ?: return
        // Only guard clicks on player's inventory slots
        if (slot.inventory !is InventoryPlayer) return
        val internal = slot.stack?.getInternalNameOrNull() ?: return
        // Block sale if protected
        if (SellProtection.shouldBlockSale(internal, slot.stack.stackSize)) {
            event.cancel()
        }
    }
}
