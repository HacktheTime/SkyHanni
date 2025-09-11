package de.hype.bingonet.shared.tutorials

import at.hannibal2.skyhanni.features.inventory.storage.ItemTagManager
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import de.hype.bingonet.environment.skyblockItemId
import de.hype.bingonet.sharedcompilation.sbenums.BNNEUItem
import net.minecraft.item.ItemStack

interface ItemCheck{
    val displayText: String
    val descriptionText: String?

    fun check(itemStack: ItemStack): Boolean
}

class ItemCondition{
    fun check(itemStack: ItemStack): Boolean {

    }

}

class SingleItemCheck(
    val itemCondition : ItemCondition,
    override val displayText: String,
    override val descriptionText: String? = null,
): ItemCheck {
    override fun check(itemStack: ItemStack): Boolean {
        return itemCondition.check(itemStack)
    }
}

class ResourceItemCheck(
    val item : BNNEUItem,
    val amount : Int,
    override val displayText: String,
    override val descriptionText: String? = null,
): ItemCheck {
    override fun check(itemStack: ItemStack): Boolean {
        return item.skyblockItemId == itemStack.getInternalNameOrNull()?.skyblockItemId
    }
}

class TaggedItemCheck(
    override val displayText: String,
    override val descriptionText: String? = null,
    val tag : String,
): ItemCheck {
    override fun check(itemStack: ItemStack): Boolean {
        val itemID = itemStack.getItemUuid()?: return false
        return itemID == ItemTagManager.getUuidByTag(tag)
    }

}
