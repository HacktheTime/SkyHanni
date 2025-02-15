package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.item.ItemStack

class BlockClickEvent(val clickType: ClickType, val position: LorenzVec, val itemInHand: ItemStack?) : LorenzEvent() {
    val getBlockState by lazy { position.getBlockStateAt() }
}