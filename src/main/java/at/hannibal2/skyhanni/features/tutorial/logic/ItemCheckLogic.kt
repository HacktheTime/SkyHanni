package at.hannibal2.skyhanni.features.tutorial.logic

import at.hannibal2.skyhanni.features.inventory.storage.ItemTagManager
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import de.hype.bingonet.shared.tutorials.ItemCheck
import de.hype.bingonet.shared.tutorials.ResourceItemCheck
import de.hype.bingonet.shared.tutorials.SingleItemCheck
import de.hype.bingonet.shared.tutorials.TaggedItemCheck
import net.minecraft.item.ItemStack

/**
 * Logic handler for ItemCheck operations.
 * Evaluates item check data classes against item stacks.
 */
object ItemCheckLogic {

    fun check(itemCheck: ItemCheck, itemStack: ItemStack): Boolean {
        return when (itemCheck) {
            is ResourceItemCheck -> {
                itemCheck.item == itemStack.getInternalNameOrNull()
            }
            is TaggedItemCheck -> {
                val itemID = itemStack.getItemUuid() ?: return false
                itemID == ItemTagManager.getUuidByTag(itemCheck.tag)
            }
            is SingleItemCheck -> {
                itemCheck.itemCondition.check(itemStack)
            }
            else -> false
        }
    }
}
