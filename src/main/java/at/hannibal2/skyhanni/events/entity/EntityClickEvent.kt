package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.events.WorldClickEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NeuNPC
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.unformattedTextCompat
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack

@PrimaryFunction("onEntityClick")
class EntityClickEvent(clickType: InteractClickType, val action: ActionType, val clickedEntity: Entity, itemInHand: SafeItemStack?) :
    WorldClickEvent(itemInHand, clickType) {

    enum class ActionType {
        INTERACT,
        ATTACK,
        INTERACT_AT,
    }

    fun getAsNPC(): NeuNPC? {
        val armorStand = this.clickedEntity.getLorenzVec().getEntitiesNearby<ArmorStand>(2.0)
        val results = NeuItems.npcs.filter {
            val npc = it.value.displayName.replace("§.".toRegex(), "").trim()
            return@filter armorStand.any { it.displayName?.unformattedTextCompat() == npc }
        }.values
        if (results.size > 1) {
            ChatUtils.chat("§cMultiple NPCs found with the same name, please report this to the developers.")
        }
        return results.firstOrNull()
    }
}
