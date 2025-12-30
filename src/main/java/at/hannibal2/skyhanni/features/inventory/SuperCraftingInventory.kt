package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getSingleLineLore
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternGroup
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SuperCraftingInventory {
    val config = SkyHanniMod.feature.inventory.superCraftingCoinWaste
    val invDetector = InventoryDetector(
        onOpenInventory = { updateSideData() },
        checkInventoryName = { name ->
            name.matches(".* Recipe".toRegex())
        },
        onCloseInventory = {
            lastProfit = null
        },
    )

    @HandleEvent
    fun renderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!invDetector.isInside()) return
        val lastProfit = lastProfit
        if (lastProfit != null) {
            if (lastProfit <= config.warnCoinWaste*100_000L) else {
                //Invert again to get a positive value again.
                val revert = -lastProfit*100_000L
                config.warnCoinWastePosition.renderRenderables(
                    listOf(
                        at.hannibal2.skyhanni.utils.renderables.Renderable.text(
                            "§cLoosing ${String.format("%,.1f", revert)} coins by Self Crafting",
                        ),
                    ),
                    posLabel = "Super Crafting Loss"
                )
            }
        }
    }

    var lastProfit: Double? = null

    @HandleEvent
    fun onClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!invDetector.isInside()) return
        if (HypixelData.noTrade) return
        updateSideData()
        if (event.clickedButton != 0) return
        if (event.blockWasteClick()) {
            SoundUtils.playErrorSound()
            TitleManager.sendTitle(
                "§cCraft-click Prevented (Big Loss Detected)",
                subtitleText = "§7Hold §eControl §7to bypass",
                duration = 1.seconds,
                location = TitleManager.TitleLocation.INVENTORY,
            )
        }
    }

    fun updateSideData() {
        lastProfit = getProfit()
    }

    fun getProfit(): Double? {
        val craftCount = getSuperCraftingCount() ?: return null
        val materials = getRecipeMaterials()
        if (materials.containsKey(NeuInternalName.NONE)) return null
        val resultItem = getResultItem()
        if (resultItem == NeuInternalName.NONE) return null

        val recipeMultiplier = resultItem.second

        val itemsPrice = materials.mapValues {
            it.value * (craftCount / recipeMultiplier)
        }.mapValues {
            val price = it.key.getPriceOrNull(ItemPriceSource.BAZAAR_INSTANT_SELL) ?: return null
            it.value * price
        }.sumAllValues()

        val resultItemPrice = resultItem.first.getPriceOrNull(ItemPriceSource.BAZAAR_INSTANT_BUY) ?: return null
        val totalResultPrice = resultItemPrice * craftCount

        return totalResultPrice - itemsPrice
    }

    fun getRecipeMaterials(): Map<NeuInternalName, Int> {
        val slots = InventoryUtils.getItemsInOpenChestWithNull()
        return listOf(
            slots.get(10), slots.get(11), slots.get(12),
            slots.get(19), slots.get(20), slots.get(21),
            slots.get(28), slots.get(29), slots.get(30),
        ).map { it.item.getInternalName() to it.item.count }.groupBy { it.first }.mapValues { it.value.sumOf { it.second } }
    }

    fun getSuperCraftingCount(): Int? {
        val slots = InventoryUtils.getItemsInOpenChestWithNull()
        val pickaxeSlot = slots.get(32)
        val lore = pickaxeSlot.item.getSingleLineLore()
        val craftingCount = craftingCount.matchMatcher(lore) {
            return groupOrNull("count")?.toIntOrNull()
        }
        return craftingCount
    }

    fun getResultItem(): Pair<NeuInternalName, Int> {
        val slots = InventoryUtils.getItemsInOpenChestWithNull()
        val resultSlot = slots.get(24)
        return resultSlot.item.getInternalName() to resultSlot.item.count
    }

    val craftingPatternGroup = RepoPatternGroup("supercraftinginventory")
    val craftingCount by craftingPatternGroup.pattern(
        "crafting.count",
        ".*Crafting (?<count>\\d+) item.*",
    )

    private fun GuiContainerEvent.SlotClickEvent.blockWasteClick(): Boolean {
        if (!config.warnCoinWasteEnabled) return false
        if (KeyboardManager.isControlKeyDown()) return false
        val profit = getProfit() ?: return false
        if (profit >= -config.warnCoinWaste * 100_000L) return false
        return true
    }
}
