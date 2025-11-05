package de.hype.bingonet.shared.tutorials

import at.hannibal2.skyhanni.features.inventory.storage.ItemTagManager
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import net.minecraft.item.ItemStack

interface ItemCheck {
    val displayText: String
    val descriptionText: String?


    fun check(itemStack: ItemStack): Boolean
}

class ResourceItemCheck(
    val item: NeuInternalName,
    val amount: Int,
    override val displayText: String,
    override val descriptionText: String? = null,
) : ItemCheck {

    override fun check(itemStack: ItemStack): Boolean {
        return item == itemStack.getInternalNameOrNull()
    }
}

class TaggedItemCheck(
    override val displayText: String,
    override val descriptionText: String? = null,
    val tag: String,
) : ItemCheck {


    override fun check(itemStack: ItemStack): Boolean {
        val itemID = itemStack.getItemUuid() ?: return false
        return itemID == ItemTagManager.getUuidByTag(tag)
    }
}

class ItemCondition(private val predicate: (ItemStack) -> Boolean = { false }) {
    fun check(itemStack: ItemStack): Boolean {
        return predicate(itemStack)
    }
}

class SingleItemCheck(
    val itemCondition: ItemCondition,
    override val displayText: String,
    override val descriptionText: String? = null,
) : ItemCheck {
    override fun check(itemStack: ItemStack): Boolean {
        return itemCondition.check(itemStack)
    }
}
