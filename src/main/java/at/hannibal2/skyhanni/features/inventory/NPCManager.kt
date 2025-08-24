package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.features.dev.GuiNeuRepoDump
import at.hannibal2.skyhanni.features.dev.GuiNeuRepoDump.folder
import at.hannibal2.skyhanni.features.dev.GuiNeuRepoDump.gson
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuNPC
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.annotations.Expose
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.item.ItemStack
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object NPCManager {
    @Expose
    val limitData : MutableMap<NeuNPC.NeuNPCOffer, Pair<Int, SimpleTimeMark>> = HashMap()
    val npcPattern = RepoPattern.group("npc")
    val remainingPattern by npcPattern.pattern("limit.remaining", "§6(?<limit>\\d+) §7remaining")
    val annualStockPattern by npcPattern.pattern("limit.remaining", "§7Annual Stock §8Year (?<year>\\d+)")
    val costPattern by npcPattern.pattern("cost", "§7Cost")

   val buyableItemPattern by npcPattern.pattern("buy.check", "§eClick to ((trade)|(purchase))!")
    val coinPricePattern by npcPattern.pattern(
        "price.coin",
        "§6(?<price>[0-9,.]+) Coins",
    )
    val copperPricePattern by npcPattern.pattern(
        "price.copper",
        "§c(?<price>[0-9,.]+) Copper",
    )
    val chocolatePricePattern by npcPattern.pattern(
        "price.chocolate",
        "§6(?<price>[0-9,.]+) Chocolate",
    )

    val itemPricePattern by npcPattern.pattern(
        "price.item",
        "§.(?<item>.*?)( §8x(?<count>\\d+))?",
    )

    var lastClickedEntity: EntityOtherPlayerMP? = null
        private set
    var lastClickedNPC: NeuNPC? = null
        private set

    @HandleEvent
    fun updateLimit(event: InventoryUpdatedEvent){
        val npc = getLastNPC(event.inventoryName) ?: return
        lastClickedNPC = npc
        SkyHanniMod.launchCoroutine {
            val offers = npc.offers.get(event.inventoryName)
            if (offers.isNullOrEmpty()) return@launchCoroutine
            event.inventoryItems.forEach { (key, stack) ->
                offers.find { it.matches(stack) }?.let {
                    val lore = stack.getLore()
                    for (line in lore) {
                        val remaining = remainingPattern.matchGroup(line, "limit")?.toIntOrNull()
                        if (remaining != null) {
                            limitData[it] = Pair(remaining, SimpleTimeMark.now())
                            ChatUtils.chat("Updated limit for NPC ${npc.internalName} (${npc.displayName}) offer ${it.result.internalName}: $remaining remaining")
                        }
                    }
                }
            }
        }
    }

    /**
     * Warning this method is heavily dependent on toSkyblockString() accouting for custom formats like coins, copper etc that isnt "$displayname §8x64"
     */
    private fun NeuNPC.NeuNPCOffer.matches(stack: ItemStack): Boolean {
        if (stack.getInternalName() != this.result.internalName) return false
        return stack.getLore().containsAll(cost.map { it.toSkyblockString() }.toSet())
    }

    fun getRemainingStock(
        offer: NeuNPC.NeuNPCOffer,
    ): Int? {
        val offerLimit = offer.limit ?: return null
        val limitData = limitData[offer] ?: return offerLimit.limit
        val lastLimitUpdate = limitData.second
        val resetType = offerLimit.resetType
        val hasReset = when(resetType){
            NeuNPC.NeuNPCOffer.ShopLimitResetType.DAILY -> {
                val reset = Instant.now().atZone(ZoneOffset.UTC)
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0).toLocalDateTime()
               lastLimitUpdate.toLocalDateTime().isAfter(reset)
            }
            NeuNPC.NeuNPCOffer.ShopLimitResetType.YEARLY -> {
                limitData.second.toSkyBlockTime().year<SimpleTimeMark.now().toSkyBlockTime().year
            }
        }
        if (hasReset){
            this.limitData.remove(offer)
            return offerLimit.limit
        }
        return limitData.first
    }

    @HandleEvent
    fun onPunch(event: EntityClickEvent) {
        val entity = event.clickedEntity
        if (entity !is EntityOtherPlayerMP) return
        if (!entity.isNpc()) return
        lastClickedEntity = event.clickedEntity
        lastClickedNPC = event.getAsNPC()
    }

    fun getLastNPC(guiName: String): NeuNPC? {
        lastClickedNPC.let {
            if (it != null) return it
        }
        val internalId = (guiName.uppercase().replace(" ", "_").replace(Regex("[^A-Z_]+"), ""))
        val file = File(folder, "${internalId}_NPC.json")
        if (file.exists()) {
            val npc = gson.fromJson(file.bufferedReader().readText(), NeuNPC::class.java)
            return npc
        }
        return null
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiOpen(event: InventoryFullyOpenedEvent) {
        SkyHanniMod.launchCoroutine {
            delay(10.milliseconds)
            resetTask?.cancel()
            val npc = GuiNeuRepoDump.getLastNPC(event.inventoryName) ?: return@launchCoroutine
            lastClickedNPC = npc
        }
    }


    var resetTask: Job? = null

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiClose(event: InventoryCloseEvent) {
        resetTask?.cancel()
        resetTask = SkyHanniMod.launchCoroutine {
            try {
                delay(500.milliseconds)
                lastClickedEntity = null
                lastClickedNPC = null
                resetTask = null
            } catch (_: CancellationException) {

            }

        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiOpen(event: InventoryCloseEvent) {
        if (event.inventoryTitle == "?") return
        resetTask?.cancel()
    }
}

fun NeuNPC.NeuNPCOffer.getRemaining(): Int? {
    return NPCManager.getRemainingStock(this)
}
