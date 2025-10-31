package de.hype.bingonet.shared.tutorials

import at.hannibal2.skyhanni.utils.NeuInternalName
import net.minecraft.item.ItemStack

/**
 * Pure data interface for item checks.
 * Logic for checking items is in ItemCheckLogic in at.hannibal2 package.
 */
interface ItemCheck {
    val displayText: String
    val descriptionText: String?
}

/**
 * Pure data class: Check for a specific NEU item
 */
class ResourceItemCheck(
    val item: NeuInternalName,
    val amount: Int,
    override val displayText: String,
    override val descriptionText: String? = null,
) : ItemCheck

/**
 * Pure data class: Check for an item with a specific tag
 */
class TaggedItemCheck(
    override val displayText: String,
    override val descriptionText: String? = null,
    val tag: String,
) : ItemCheck

/**
 * Pure data class: Stores an item condition predicate
 */
class ItemCondition(val predicate: (ItemStack) -> Boolean = { false }) {
    fun check(itemStack: ItemStack): Boolean {
        return predicate(itemStack)
    }
}

/**
 * Pure data class: Check for an item using a custom condition
 */
class SingleItemCheck(
    val itemCondition: ItemCondition,
    override val displayText: String,
    override val descriptionText: String? = null,
) : ItemCheck
