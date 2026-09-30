package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.hypixeltools.HypixelToolsRepository
import at.hannibal2.skyhanni.api.hypixeltools.ItemId
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import de.hype.hypixeltools.data.SkyblockItemReference

data class PrimitiveItemStack(val item: SkyblockItemReference, val amount: Int) {

    @Deprecated("Use item")
    val internalName: NeuInternalName
        get() = item.getId().toInternalName()

    @Deprecated("Use the SkyblockItemReference constructor")
    constructor(internalName: NeuInternalName, amount: Int) : this(
        HypixelToolsRepository.findById(ItemId(internalName.asString()))
            ?: error("Item ${internalName.asString()} is not present in HypixelTools"),
        amount,
    )

    fun createItem(): SafeItemStack = internalName?.getItemStack()?.apply { count = amount }
        ?: error("Could not resolve ${item.getId()} to a Minecraft item stack")

    operator fun times(multiplier: Int): PrimitiveItemStack = PrimitiveItemStack(item, amount * multiplier)

    operator fun plus(amount: Int): PrimitiveItemStack = PrimitiveItemStack(item, this.amount + amount)

    val itemName by lazy { HypixelToolsRepository.displayName(item) ?: item.getId() }

    fun toPair() = Pair(item, amount)

    fun toPrimitiveIngredient() = PrimitiveIngredient(item, amount.toDouble())

    companion object {

        @Deprecated("Use SkyblockItemReference.makePrimitiveStack")
        fun NeuInternalName.makePrimitiveStack(amount: Int = 1) = PrimitiveItemStack(this, amount)
        fun SkyblockItemReference.makePrimitiveStack(amount: Int = 1) = PrimitiveItemStack(this, amount)
        fun SafeItemStack.toPrimitiveStackOrNull() = getInternalNameOrNull()
            ?.let { HypixelToolsRepository.findById(ItemId(it.asString())) }
            ?.let { PrimitiveItemStack(it, count) }
    }
}
