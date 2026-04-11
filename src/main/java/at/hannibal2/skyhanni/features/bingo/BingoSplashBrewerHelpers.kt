package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.block.BrewingStandBlock
import net.minecraft.world.level.block.WoolCarpetBlock
import org.lwjgl.glfw.GLFW
import java.awt.Color

@SkyHanniModule
object BingoSplashBrewerHelpers {
    val config get() = SkyHanniMod.feature.event.bingo.bingoNetworks.splasherConfig.brewerUtils
    val screenDetector = InventoryDetector(
        onOpenInventory = {
            lastBrewingStandData = lastBrewingStandClick?.getBrewingStandData()
        },
        onCloseInventory = {
            lastBrewingStandData = null
        },
        checkInventoryName = { it == "Brewing Stand" },
    )

    @Volatile
    private var lastBrewingStandClick: LorenzVec? = null

    @Volatile
    private var lastBrewingStandData: BrewingStandData? = null

    @HandleEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (event.getBlockState.block is BrewingStandBlock) {
            if (config.requireWitchPet) {
                val currentPet = CurrentPetApi.currentPet
                val isWitchPet = currentPet?.fauxInternalName?.internalName?.matches("WITCH;[3-9]".toRegex()) == true
                if ((currentPet?.level != 100 || !isWitchPet) && !GLFW.GLFW_KEY_LEFT_CONTROL.isKeyHeld()) {
                    ChatUtils.clickToActionOrDisable(
                        "§cYou enabled Witch Requirement and dont have With Pet equipped! Equip a Witch Pet " +
                            "or Press Control while clicking to bypass this!",
                        config::requireWitchPet, "Open Pet Menu",
                        {
                            HypixelCommands.pet()
                        },
                    )
                    event.cancel()
                    return
                }
            }
            lastBrewingStandClick = event.flatPosition
        }
    }

    @HandleEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.highlightCorrectItem) return
        if (!screenDetector.isInside()) return
        val lastBrewingStandData = lastBrewingStandData ?: return
        for (slot in InventoryUtils.getSlotsInOwnInventory()) {
            if (slot.item == null) continue
            val internalName = slot.item.getInternalNameOrNull() ?: continue
            if (lastBrewingStandData.material == internalName || lastBrewingStandData.inputBottle == internalName) {
                slot.highlight(Color.GREEN)
            }
        }
    }

    @HandleEvent
    fun keybindPressed(event: KeyDownEvent) {
        if (!screenDetector.isInside()) return
        val lastBrewingStandData = lastBrewingStandData ?: return
        if (config.insertKeyBind == event.keyCode && event.keyCode.isKeyClicked()) {
            val standInv = InventoryUtils.getItemsInOpenChestWithNull()
            val selfItems = InventoryUtils.getSlotsInOwnInventoryWithNull()

            val missingBrews = BREWING_STAND_BOTTLE_SLOT_INDEXES.any { standInv.get(it).item.isEmpty } && lastBrewingStandData
                .inputBottle != null && selfItems.any { it.item.getInternalNameOrNull() == lastBrewingStandData.inputBottle }

            val missingMaterial =
                lastBrewingStandData.material != null && BREWING_STAND_MATERIAL_SLOT_INDEX.let { standInv[it] }.item.isEmpty
            val hasMaterial =
                lastBrewingStandData.material != null && selfItems.any { it.item.getInternalNameOrNull() == lastBrewingStandData.material }

            if (missingBrews) {
                val slotToClick = selfItems.find { it.item.getInternalNameOrNull() == lastBrewingStandData.inputBottle }!!.index
                InventoryUtils.clickSlot(slotToClick, clickType = GuiContainerEvent.ClickType.SHIFT)
            } else if (missingMaterial) {
                if (hasMaterial) {
                    val slotToClick = selfItems.first { it.item.getInternalNameOrNull() == lastBrewingStandData.material }.index
                    InventoryUtils.clickSlot(slotToClick, clickType = GuiContainerEvent.ClickType.SHIFT)
                } else {
                    val hasFreeSpace = selfItems.any { it.item.isEmpty }
                    if (!hasFreeSpace) {
                        ChatUtils.chat("§cNo free space in inventory to get brewing material from sacks!")
                        return
                    }
                    GetFromSackApi.getFromSack(lastBrewingStandData.material, config.gfsMaterialCount)
                }
            }
        }
    }


    private fun LorenzVec.getBrewingStandData(): BrewingStandData? {
        val carpet = this.getAssociatedCarpet() ?: return null

        fun getItemFrame(filter: (NeuInternalName) -> Boolean): NeuInternalName? {
            return EntityUtils.getEntitiesInBox(
                this, 5.0,
            ) { itemFrame: ItemFrame ->
                return@getEntitiesInBox itemFrame.item.getInternalNameOrNull()?.let(filter) == true
            }.minBy { it.distanceTo(LorenzVec(this.x + 0.5, this.y + 0.5, this.z + 0.5)) }.item.getInternalNameOrNull()
        }
        return when (carpet) {
            Carpets.LIGHT_BLUE, Carpets.BLUE -> {
                val bottle = getItemFrame {
                    !isMaterial(it)
                }
                val ingredient = getItemFrame {
                    isMaterial(it)
                }
                BrewingStandData(this, ingredient, bottle)
            }
            Carpets.CYAN -> {
                val bottle = getItemFrame {
                    !isMaterial(it)
                }
                BrewingStandData(this, null, bottle)
            }
            Carpets.ORANGE -> BrewingStandData(this, "ENCHANTED_REDSTONE_LAMP".toInternalName(), null)
            Carpets.YELLOW -> BrewingStandData(this, "ENCHANTED_GLOWSTONE".toInternalName(), null)
            Carpets.RED -> BrewingStandData(this, "ENCHANTED_REDSTONE_BLOCK".toInternalName(), null)
            Carpets.DARK_GRAY -> BrewingStandData(this, "ENCHANTED_GUNPOWDER".toInternalName(), null)
            Carpets.LIGHT_GRAY -> BrewingStandData(this, null, null)
            Carpets.MAGENTA -> BrewingStandData(this, "NETHER_STALK".toInternalName(), null)
        }
    }

    private fun LorenzVec.getAssociatedCarpet(): Carpets? {
        (this.modifyPosition(x = 1).getBlockAt() as? WoolCarpetBlock)?.getColorEnum()?.let { return it }
        (this.modifyPosition(x = -1).getBlockAt() as? WoolCarpetBlock)?.getColorEnum()?.let { return it }
        (this.modifyPosition(z = 1).getBlockAt() as? WoolCarpetBlock)?.getColorEnum()?.let { return it }
        (this.modifyPosition(z = -1).getBlockAt() as? WoolCarpetBlock)?.getColorEnum()?.let { return it }
        return null
    }

    private fun WoolCarpetBlock.getColorEnum(): Carpets? {
        return Carpets.entries.firstOrNull { it.color == this.color }
    }

    enum class Carpets(
        val color: DyeColor,
    ) {
        CYAN(DyeColor.CYAN),
        BLUE(DyeColor.BLUE),
        LIGHT_BLUE(DyeColor.LIGHT_BLUE),
        ORANGE(DyeColor.ORANGE),
        YELLOW(DyeColor.YELLOW),
        RED(DyeColor.RED),
        MAGENTA(DyeColor.MAGENTA),
        LIGHT_GRAY(DyeColor.LIGHT_GRAY),
        DARK_GRAY(DyeColor.GRAY),
    }

    fun LorenzVec.modifyPosition(x: Int = 0, y: Int = 0, z: Int = 0): LorenzVec {
        return LorenzVec(this.x + x, this.y + y, this.z + z)
    }

    private data class BrewingStandData(
        val pos: LorenzVec,
        val material: NeuInternalName?,
        val inputBottle: NeuInternalName?,
    )

    private fun isMaterial(item: NeuInternalName): Boolean {
        return item.getItemStack().getLoreComponent().map { it.string }.any { it.contains("Brewing Ingredient", true) }
    }

    const val BREWING_STAND_MATERIAL_SLOT_INDEX = 13
    val BREWING_STAND_BOTTLE_SLOT_INDEXES = listOf(38, 40, 42)
}
