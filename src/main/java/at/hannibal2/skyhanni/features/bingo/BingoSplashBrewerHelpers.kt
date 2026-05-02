package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.GetFromSackApi
import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
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
import at.hannibal2.skyhanni.utils.compat.WorldCompat
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.toLorenzVec
import de.hype.bingonet.environment.displayName
import de.hype.bingonet.shared.constants.Formatting
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.BrewingStandBlock
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.WoolCarpetBlock
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.concurrent.atomics.AtomicInt

@SkyHanniModule
object BingoSplashBrewerHelpers {
    val config get() = SkyHanniMod.feature.event.bingo.bingoNetworks.splasherConfig.brewerUtils
    private val openedBrewingStands: MutableMap<LorenzVec, Boolean> = mutableMapOf()
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
    private var lastChestClick: LorenzVec? = null

    @Volatile
    private var lastBrewingStandData: BrewingStandData? = null

    private val rainbowOrder = Formatting.rainbowOrder

    val allBestPotions: Set<NeuInternalName> by lazy {
        EnoughUpdatesManager.getInternalNames().filter { it.isPotion() }.groupBy { it.internalName.replace("[0-9]+".toRegex(), "") }.map {
            it.value.maxBy { it.internalName.replace("\\D".toRegex(), "").toIntOrNull() ?: 0 }
        }.toSet()
    }

    fun NeuInternalName.isPotion(): Boolean {
        return this.internalName.contains("POTION", true)
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onBlockClick(event: BlockClickEvent) {
        if (event.blockState.block is BrewingStandBlock) {
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
            openedBrewingStands[event.position] = true
        } else if (event.blockState.block is ChestBlock) {
            lastChestClick = event.flatPosition
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onIslandJoinEvent(event: IslandJoinEvent) {
        val isFresh = (WorldCompat.worldTime ?: 0) <= 10_000L
        if (isFresh) {
            openedBrewingStands.clear()
        } else if (openedBrewingStands.isNotEmpty()) {
            ChatUtils.clickableChat(
                "Your Private Island was loaded before you joined. It is unclear however if it was continously " +
                    "loaded while you were away. Click here if you want to reset the opened brewing stands data and reopen them all.",
                {
                    openedBrewingStands.replaceAll { k, v ->
                        return@replaceAll false
                    }
                },
            )
        }
    }


    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onWorldRender(event: SkyHanniRenderWorldEvent) {
        openedBrewingStands.filterNot { it.value }.forEach { (vec, bool) ->
            event.drawWaypointFilled(vec, Color.RED, true)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.highlightCorrectItem) return
        if (screenDetector.isInside()) {
            val lastBrewingStandData = lastBrewingStandData ?: return
            for (slot in InventoryUtils.getSlotsInOwnInventory()) {
                if (slot.item == null) continue
                val internalName = slot.item.getInternalNameOrNull() ?: continue
                if (lastBrewingStandData.material == internalName || lastBrewingStandData.inputBottle == internalName) {
                    slot.highlight(Color.GREEN)
                }
            }
        } else {
            val potion = lastChestClick?.getItemFrame { it.isPotion() }?:return
            val xpBoost = potion.internalName.contains("_XP_BOOST")
            val potionType = potion.internalName.split(";").first()
            InventoryUtils.getItemsInOpenChest().forEach {
                val internalName = it.item.getInternalNameOrNull()?:return@forEach
                var color :Color? = null
                if (xpBoost){
                    if (internalName.internalName.startsWith(potionType)){
                        color = Color.YELLOW
                    }
                }
                if (internalName == potion) {
                    val allowedValues = listOf("1:00:00", "11:15", "54:00", "30:00")
                    val maxLength = it.item.getLoreComponent().any { line ->
                        allowedValues.any { allowedValue -> line.string.contains(allowedValue) }
                    }
                    if (maxLength && it.item.displayName.string.contains("Splash", true)) {
                        color = Color.GREEN
                    }
                }
                if (color != null) {
                    it.highlight(color)
                }
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun keybindPressed(event: KeyDownEvent) {
        if (!screenDetector.isInside()) return
        val lastBrewingStandData = lastBrewingStandData ?: return
        if (config.insertKeyBind == event.keyCode && event.keyCode.isKeyClicked()) {
            val standInv = InventoryUtils.getItemsInOpenChestWithNull()
            val selfItems = InventoryUtils.getSlotsInOwnInventoryWithNull()
            val brewingMaterial = lastBrewingStandData.material
            val materialCount = config.gfsMaterialCount.let {
                if (brewingMaterial?.internalName.equals("ENCHANTED_CAKE")) return@let 1
                else it
            }

            val missingBrews = BREWING_STAND_BOTTLE_SLOT_INDEXES.any { standInv.get(it).item.isEmpty } && lastBrewingStandData
                .inputBottle != null && selfItems.any { it.item.getInternalNameOrNull() == lastBrewingStandData.inputBottle }
            val materialSlot = BREWING_STAND_MATERIAL_SLOT_INDEX.let { standInv[it] }
            val missingMaterial = materialSlot.item.isEmpty
            val wrongMaterial = !missingMaterial && materialSlot.item.count != materialCount
            val filtered = selfItems.filter { it.item.getInternalNameOrNull() == lastBrewingStandData.material }
            val gfsDiff = if (filtered.any { it.item.count == materialCount }) {
                0
            } else {
                var count = 0
                var foundAnyUpgradeable = false
                for (it in filtered) {
                    if (it.item.count > materialCount) {
                        count += it.getMaxStackSize(it.item) - it.item.count
                    } else {
                        count += materialCount - it.item.count
                        foundAnyUpgradeable = true
                        break
                    }
                }
                if (!foundAnyUpgradeable) count += materialCount
                count
            }

            if (missingBrews) {
                val slotToClick = selfItems.find { it.item.getInternalNameOrNull() == lastBrewingStandData.inputBottle }!!.index
                InventoryUtils.clickSlot(slotToClick, clickType = GuiContainerEvent.ClickType.SHIFT)
            } else if (brewingMaterial != null) {
                if (wrongMaterial) {
                    InventoryUtils.clickSlot(BREWING_STAND_MATERIAL_SLOT_INDEX, clickType = GuiContainerEvent.ClickType.SHIFT)
                } else if (gfsDiff != 0 && missingMaterial) {
                    GetFromSackApi.getFromSack(brewingMaterial, gfsDiff)
                } else {
                    val slotToClick =
                        selfItems.firstOrNull {
                            it.item.getInternalNameOrNull() == brewingMaterial && it.item.count ==
                                materialCount
                        }?.index
                    slotToClick?.let {
                        InventoryUtils.clickSlot(it, clickType = GuiContainerEvent.ClickType.SHIFT)
                    }
                }
            }
        }
    }

    @HandleEvent
    fun registerCommand(event: CommandRegistrationEvent) {
        event.registerBrigadier("shhighlightmissingbrewingstands") {
            description = "Imports the Brewing stands for the Brewer Utils for Bingo Splashers"
            category = CommandCategory.USERS_ACTIVE
            callback {
                CoroutineSettings("Brewingstand Auto Detection for Brewer Utils").launch {
                    autoDetectBrewingStands()
                }
            }
        }
    }

    @OptIn(AllEntitiesGetter::class)
    private fun autoDetectBrewingStands() {
        val itemFrames = EntityUtils.getEntities<ItemFrame>().filter { it.item.item == Items.OAK_SIGN }.toList()
        val size = itemFrames.size
        if (size == 0) {
            ChatUtils.userError("There is no marker for the mod to use. Please add the Oak Sign marker or go near it.")
            return
        }
        val actualValidItemFrames = AtomicInt(0)
        val rotations = listOf(1 to 0, 0 to 1, -1 to 0, 0 to -1)
        fun LorenzVec.isBrewingStand(): Boolean {
            return this.getBlockAt() == Blocks.BREWING_STAND
        }
        itemFrames.forEach {
            val originPosition = it.position().toLorenzVec().roundToBlock()
            var firstBrewingStand: Pair<LorenzVec, Pair<Int, Int>>? = null
            for (rotation in rotations) {
                val new = originPosition.add(rotation.first, 0, rotation.second)
                if (new.isBrewingStand()) {
                    firstBrewingStand = new to rotation
                    break
                }
            }
            if (firstBrewingStand == null) {
                for (rotation in rotations) {
                    val new = originPosition.add(rotation.first * 2, 0, rotation.second * 2)
                    if (new.isBrewingStand()) {
                        firstBrewingStand = new to rotation
                        break
                    }
                }
            }
            if (firstBrewingStand == null) return@forEach
            var columnDirection: Pair<Int, Int>? = null
            rotations.forEach {
                val newPos = firstBrewingStand.first.add(it.first, 1, it.second)
                if (newPos.isBrewingStand()) {
                    columnDirection = it
                }
            }
            if (columnDirection == null) return@forEach
            val rowDirection = firstBrewingStand.second
            val first = firstBrewingStand.first

            //Use the column and row direction to find more brewing stands recursively in their directions.
            var currentColumn = first
            var secondTry = false
            while (currentColumn.isBrewingStand() || !secondTry) {
                var row = currentColumn
                if (row.isBrewingStand()) {
                    openedBrewingStands.putIfAbsent(row, false)
                    row = row.add(columnDirection.first, 1, columnDirection.second)
                    secondTry = false
                } else {
                    row = row.add(columnDirection.first, 1, columnDirection.second)
                    secondTry = true
                }
                while (row.isBrewingStand()) {
                    val it = row
                    openedBrewingStands.putIfAbsent(it, false)
                    row = row.add(columnDirection.first, 1, columnDirection.second)
                }
                currentColumn = currentColumn.add(rowDirection.first, 0, rowDirection.second)
            }
        }
    }

    private fun LorenzVec.getItemFrame(filter: (NeuInternalName) -> Boolean): NeuInternalName? {
        return EntityUtils.getEntitiesInBox(
            this, 5.0,
        ) { itemFrame: ItemFrame ->
            return@getEntitiesInBox itemFrame.item.getInternalNameOrNull()?.let(filter) == true
        }.minByOrNull { it.distanceTo(LorenzVec(this.x + 0.5, this.y + 0.5, this.z + 0.5)) }?.item?.getInternalNameOrNull()
    }

    private fun LorenzVec.getBrewingStandData(): BrewingStandData? {
        val carpet = this.getAssociatedCarpet() ?: return null
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
