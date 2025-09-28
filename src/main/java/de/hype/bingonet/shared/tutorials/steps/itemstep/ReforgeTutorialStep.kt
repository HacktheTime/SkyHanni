package de.hype.bingonet.shared.tutorials.steps.itemstep

import at.hannibal2.skyhanni.api.ReforgeApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.features.inventory.ReforgeHelper
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeModifier
import de.hype.bingonet.shared.tutorials.TaggedItemCheck
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import java.awt.Color

class ReforgeTutorialStep(
    val item: TaggedItemCheck,
    val reforgeName: String,
) : TutorialStep() {

    private val desiredReforge = reforgeName.lowercase().replace('-', '_')


    override fun getStepName(tutorial: Tutorial): String = "Reforge ${item.displayText} to ${reforgeName}"

    override fun getStepDescription(tutorial: Tutorial): String? = item.descriptionText

    override fun getRequirements(): List<TutorialNode> = emptyList()

    override fun isComplete(tutorial: Tutorial): Boolean = completed


    override fun onActivate(tutorial: Tutorial) {
        // Pre-select the required reforge in ReforgeHelper; it will block the button until matched
        ReforgeHelper.setTutorialTargetReforge(ReforgeApi.reforges.firstOrNull { it.name.equals(reforgeName, true) })
    }

    override fun onDeactivate(tutorial: Tutorial) {
        ReforgeHelper.setTutorialTargetReforge(null)
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (ignoreEvent()) return

        // Highlight the desired item by UUID in the player's inventory area of the open container
        val wantedUuid = TaggedItemCheck(
            displayText = item.displayText,
            descriptionText = item.descriptionText,
            tag = item.tag,
        )
        val targetUuid = at.hannibal2.skyhanni.features.inventory.storage.ItemTagManager.getUuidByTag(wantedUuid.tag)
        if (targetUuid != null) {
            InventoryUtils.getSlotsInOwnInventory().forEach { slot ->
                val stack = slot.stack ?: return@forEach
                if (stack.getItemUuid() == targetUuid) {
                    slot.highlight(Color.YELLOW)
                }
            }
        }

        // Check completion: if the tagged item exists in inventory and already has the desired reforge
        val hasReforged = InventoryUtils.getSlotsInOwnInventory().any { slot ->
            val stack = slot.stack ?: return@any false
            val uuid = stack.getItemUuid() ?: return@any false
            val matches = targetUuid != null && uuid == targetUuid
            if (!matches) return@any false
            val current = stack.getReforgeModifier().orEmpty().lowercase()
            current == desiredReforge
        }
        if (hasReforged) complete()
    }
}
