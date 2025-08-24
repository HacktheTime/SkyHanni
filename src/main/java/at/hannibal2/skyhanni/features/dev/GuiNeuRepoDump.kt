package at.hannibal2.skyhanni.features.dev

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.features.inventory.NPCManager
import at.hannibal2.skyhanni.features.inventory.NPCManager.annualStockPattern
import at.hannibal2.skyhanni.features.inventory.NPCManager.buyableItemPattern
import at.hannibal2.skyhanni.features.inventory.NPCManager.chocolatePricePattern
import at.hannibal2.skyhanni.features.inventory.NPCManager.coinPricePattern
import at.hannibal2.skyhanni.features.inventory.NPCManager.copperPricePattern
import at.hannibal2.skyhanni.features.inventory.NPCManager.costPattern
import at.hannibal2.skyhanni.features.inventory.NPCManager.itemPricePattern
import at.hannibal2.skyhanni.features.inventory.NPCManager.lastClickedEntity
import at.hannibal2.skyhanni.features.inventory.NPCManager.lastClickedNPC
import at.hannibal2.skyhanni.features.inventory.NPCManager.npcPattern
import at.hannibal2.skyhanni.features.inventory.NPCManager.remainingPattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NeuNPC
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.json.BaseGsonBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.item.ItemStack
//#if MC < 1.21
//#else
//$$ import net.minecraft.component.DataComponentTypes
//#endif
import java.io.File
import kotlin.time.Duration.Companion.milliseconds


@SkyHanniModule
object GuiNeuRepoDump {

    val gson = BaseGsonBuilder.gson().create()


    val folder = File(EnoughUpdatesManager.repoDirectory, "items").also {
        it.mkdir()
    }


    fun getLastNPC(guiName: String): NeuNPC? {
        NPCManager.getLastNPC(guiName)?.let {
            return it
        }
        if (InventoryUtils.getItemsInOpenChestWithNull().none { it.stack.getLore().any { buyableItemPattern.matches(it) } }) return null
        val lastClicked = lastClickedEntity
        val internalId = lastClickedNPC?.internalName ?: (guiName.uppercase().replace(" ", "_").replace(Regex("[^A-Z_]+"), ""))
        ChatUtils.chat("§cNo NPC found with internal name: $internalId")
        if (lastClicked != null) {
            return NeuNPC(
                displayName = guiName,
                nbtTag = null,
                x = lastClicked.position.x,
                y = lastClicked.position.y,
                z = lastClicked.position.z,
                island = HypixelData.locrawData?.get("hub")?.asString,
                _offers = mutableListOf(),
            )
        } else
            return NeuNPC(
                displayName = guiName,
                nbtTag = null,
                x = null,
                y = null,
                z = null,
                island = null,
                _offers = mutableListOf(),
            )
    }


    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiOpen(event: InventoryFullyOpenedEvent) {
        SkyHanniMod.launchCoroutine {
            delay(10.milliseconds)
            val npc = getLastNPC(event.inventoryName) ?: return@launchCoroutine
            npc.removeCategoryOffers(event.inventoryName)
            val items = event.inventoryItems.forEach {
                addOffer(npc, it.value, event.inventoryName)
            }
            val json = gson.toJson(npc)
            File(folder, "${npc.internalName}_NPC.json").bufferedWriter().also {
                it.write(json)
                it.flush()
                it.close()
            }
        }
    }

    fun addOffer(
        npcData: NeuNPC,
        stack: ItemStack,
        inventoryName: String,
    ) {
        val lore = stack.getLore()
        val cost: MutableList<PrimitiveIngredient> = mutableListOf()
        var inCostBlock = false
        for (line in lore) {
            if (line.isEmpty()) inCostBlock = false
            if (inCostBlock) {
                coinPricePattern.matchGroup(line, "price")?.let { price ->
                    cost.add(PrimitiveIngredient.coinIngredient(price.replace(",", "").toDouble()))
                }
                copperPricePattern.matchGroup(line, "price")?.let { price ->
                    cost.add(PrimitiveIngredient.copperIngredient(price.replace(",", "").toDouble()))
                }
                chocolatePricePattern.matchGroup(line, "price")?.let { price ->
                    cost.add(PrimitiveIngredient.chocolateIngredient(price.replace(",", "").toDouble()))
                }
                itemPricePattern.matchMatcher(
                    line,
                    {
                        val itemName = NeuItems.findItemByNameWithoutColor(group("item").replace(Regex("§."), "")) ?: return@matchMatcher
                        val count = group("count")?.toIntOrNull() ?: 1
                        cost.add(PrimitiveIngredient(itemName, count))
                    },
                )

            }
            if (costPattern.matches(line)) inCostBlock = true
        }
        if (cost.isEmpty() && lore.none { buyableItemPattern.matches(it) }) return
        val result: PrimitiveIngredient = PrimitiveIngredient(stack.getInternalName(), stack.stackSize)
        val limit: Int? = stack.getLore().firstNotNullOfOrNull { remainingPattern.matchGroup(it, "limit")?.toInt() }
        val resetType = if (stack.getLore().any { annualStockPattern.matches(it) }) NeuNPC.NeuNPCOffer.ShopLimitResetType.YEARLY else NeuNPC.NeuNPCOffer.ShopLimitResetType.DAILY
        val offer = NeuNPC.NeuNPCOffer(
            cost = cost,
            result = result,
            category = inventoryName,
            limit = limit?.let { NeuNPC.NeuNPCOffer.LimitData(it, resetType) }
        )
        npcData.addOffer(offer)
    }
}
