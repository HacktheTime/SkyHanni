package at.hannibal2.skyhanni.api.storage

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.SkyHanniInventoryContainer
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.ChestBlock
import java.util.NavigableMap
import java.util.TreeMap
import java.util.UUID
import kotlin.collections.drop
import kotlin.collections.forEachIndexed
import kotlin.collections.map
import kotlin.collections.toList

@SkyHanniModule
@Suppress("ReturnCount")
object StorageApi {
    val toHighlightResults = mutableListOf<StorageSearchResult>()
    var currentInventoryResults: List<StorageSearchResult> = emptyList()
    private val storage: NavigableMap<String, SkyHanniInventoryContainer>
        get() = ProfileStorageData.storageProfiles?.data ?: TreeMap()

    private var saveJob: Job? = null
    private val saveMutex = Mutex()

    /**
     * REGEX-TEST: Ender Chest
     * REGEX-TEST: Ender Chest (1/9)
     */
    private val enderChestPattern by RepoPattern.pattern(
        "storage.enderchest",
        "Ender Chest(?: \\((?<page>\\d+)/\\d+\\))?",
    )

    /**
     * REGEX-TEST: Jumbo Backpack§r (Slot #2)
     */
    private val backpackPattern by RepoPattern.pattern(
        "storage.backpack",
        ".* Backpack§r \\(Slot #(?<page>\\d+)\\)",
    )

    /**
     * REGEX-TEST: Rift Storage (1/2)
     * REGEX-TEST: Rift Storage
     */
    private val riftStoragePattern by RepoPattern.pattern(
        "storage.rift",
        "Rift Storage(?: \\((?<page>\\d+)/\\d+\\))?",
    )

    val accessStorage: Map<String, SkyHanniInventoryContainer> get() = storage
    val enderChest: Map<String, SkyHanniInventoryContainer>
        get() = StringUtils.subMapOfStringsStartingWith(
            "Ender Chest",
            storage,
        )
    val backpack: Map<String, SkyHanniInventoryContainer>
        get() = StringUtils.subMapOfStringsStartingWith(
            "Backpack",
            storage,
        )
    val riftStorage: Map<String, SkyHanniInventoryContainer>
        get() = StringUtils.subMapOfStringsStartingWith(
            "Rift Storage",
            storage,
        )
    private val mutableIslandChest: MutableMap<String, SkyHanniInventoryContainer>
        get() = StringUtils.subMapOfStringsStartingWith(
            "Private Island Chest",
            storage,
        )

    var currentStorage: SkyHanniInventoryContainer? = null
        private set

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        enderChestPattern.matchMatcher(event.inventoryName) {
            val page = groupOrNull("page")?.toInt() ?: 1
            handleRead("Ender Chest $page", event.inventoryItemsWithNull.values)
            return
        }
        backpackPattern.matchMatcher(event.inventoryName) {
            val page = groupOrNull("page")?.toInt() ?: 1
            handleRead("Backpack $page", event.inventoryItemsWithNull.values)
            return
        }
        riftStoragePattern.matchMatcher(event.inventoryName) {
            val page = groupOrNull("page")?.toInt() ?: 1
            handleRead("Rift Storage $page", event.inventoryItemsWithNull.values)
            return
        }
        if (!IslandType.PRIVATE_ISLAND.isCurrent() || !isPrivateIslandStorageEnabled()) return
        if (InventoryUtils.isInNormalChest(event.inventoryName)) {
            handlePrivateIslandRead(event.inventoryItemsWithNull.values)
        }
    }

    private var shouldReCheck = false
    private var shouldSave = false

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiContainerSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (currentStorage == null) return
        shouldReCheck = true
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        if (!shouldReCheck) return
        currentStorage?.items = InventoryUtils.getItemsInOpenChestWithNull().map { it.item }.drop(9)
        shouldReCheck = false
        shouldSave = true
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed() {
        if (!shouldSave || saveJob?.isActive == true) return
        setupSaveJob()
    }

    private fun setupSaveJob() {
        saveJob = SkyHanniMod.launchIOCoroutineWithMutex("storage api save", saveMutex) {
            if (!shouldSave) return@launchIOCoroutineWithMutex
            SkyHanniMod.configManager.saveConfig(ConfigFileType.STORAGE, "Updated Items")
            shouldSave = false
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onMinutePassed(event: SecondPassedEvent) {
        if (!event.repeatSeconds(60) || !isPrivateIslandStorageEnabled()) return
        mutableIslandChest.removeIf { (_, chest) ->
            if (chest.primaryCords == null) {
                ErrorManager.logErrorStateWithData(
                    "Something went wrong during Private Island cleanup",
                    "Tried to remove a container that isn't a chest",
                    "Chest" to chest,
                    "Storage" to accessStorage,
                )
                return@removeIf false
            }
            when {
                chest.primaryCords.distanceSqToPlayer() > 30 * 30 -> false
                chest.primaryCords.getBlockAt() !is ChestBlock -> true
                chest.secondaryCords == null -> getNeighborBlocks(chest.primaryCords).any { it.second is ChestBlock }
                else -> chest.secondaryCords.getBlockAt() !is ChestBlock
            }.also {
                if (it) ChatUtils.debug("Removed Private Island Chest at: ${chest.primaryCords}")
            }
        }
    }

    private fun handleRead(name: String, inventory: Collection<ItemStack?>) {
        val saneInventory = inventory.drop(9)
        val old = storage[name]
        val stored: SkyHanniInventoryContainer
        if (old == null) {
            stored = SkyHanniInventoryContainer(name, 9, saneInventory)
            storage[name] = stored
            return
        } else {
            stored = old
            old.items = saneInventory
        }
        currentStorage = stored
        shouldSave = true
        currentInventoryResults = toHighlightResults.filter { it.storageName == name }
        toHighlightResults.removeAll(currentInventoryResults)
    }

    private fun handlePrivateIslandRead(inventory: Collection<ItemStack?>) {
        val primary = lastChestClicked ?: run {
            ErrorManager.logErrorStateWithData("Failed to save chest", "Failed to save chest on Private Island", "inventory" to inventory)
            return
        }
        val secondary = doubleChestCord
        val name = "Private Island Chest $primary"
        val saneInventory = inventory.toList()
        val old = storage[name]
        val stored: SkyHanniInventoryContainer
        if (old == null) {
            stored = SkyHanniInventoryContainer(name, 9, saneInventory, "Private Island Chest", primary, secondary)
            storage[name] = stored
            return
        } else {
            stored = old
            old.items = saneInventory
        }
        currentStorage = stored
        shouldSave = true
        currentInventoryResults = toHighlightResults.filter { it.storageName == name }
        toHighlightResults.removeAll(currentInventoryResults)
    }
    // TODO museum (existing?), vault, warderobe, builders wand, bags

    private var lastChestClicked: LorenzVec? = null
    private var doubleChestCord: LorenzVec? = null

    private fun getNeighborBlocks(position: LorenzVec) =
        listOf(position.add(x = 1), position.add(x = -1), position.add(z = 1), position.add(z = -1)).map {
            it to it.getBlockAt()
        }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onBlockClick(event: BlockClickEvent) {
        if (event.clickType != ClickType.RIGHT_CLICK) return
        if (!isPrivateIslandStorageEnabled()) return
        val chest = event.getBlockState.block as? ChestBlock ?: return
        val position = event.flatPosition
        // Double Chest Check
        val otherChest = getNeighborBlocks(position).firstOrNull { it.second == chest }?.first
        if (otherChest == null) {
            lastChestClicked = position
            doubleChestCord = null
        } else if (otherChest.lengthSquared() > position.lengthSquared()) {
            lastChestClicked = position
            doubleChestCord = otherChest
        } else {
            lastChestClicked = otherChest
            doubleChestCord = position
        }
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Storage Data")
        if (storage.isEmpty()) {
            event.addIrrelevant("Empty")
        } else {
            event.addIrrelevant(storage.values.sortedBy { it.internalName }.flatMap { it.getDebug() + listOf("") })
        }
    }

    private fun isPrivateIslandStorageEnabled() = SkyHanniMod.feature.inventory.savePrivateIslandChests

    /**
     * Creates a new search consumer for searching through all storage
     */
    fun search(): StorageSearchConsumer {
        return StorageSearchConsumer()
    }

    /**
     * Returns all storage data as a map of storage name to items
     */
    fun getAllStorageData(): Map<String, List<ItemStack?>> {
        return storage.mapValues { (_, container) -> container.items }
    }

    /**
     * Searches storage with a simple item name filter
     */
    fun searchByName(pattern: String, ignoreCase: Boolean = true): List<StorageSearchResult> {
        return search()
            .filterByName(pattern, ignoreCase)
            .getResults()
    }

    /**
     * Searches storage with a SkyBlock item ID filter
     */
    fun searchByItemId(itemId: String, ignoreCase: Boolean = true): List<StorageSearchResult> {
        return search()
            .filterByItemId(itemId, ignoreCase)
            .getResults()
    }

    /**
     * Searches storage with a SkyBlock UUID filter
     */
    fun searchByUuid(uuid: UUID): List<StorageSearchResult> {
        return search()
            .filterByUuid(uuid)
            .getResults()
    }

    /**
     * Searches storage by category
     */
    fun searchByCategory(vararg categories: StorageCategory): List<StorageSearchResult> {
        return search()
            .filterByCategory(*categories)
            .getResults()
    }

    /**
     * Gets the page number from a storage name
     */
    fun getPageFromStorageName(storageName: String): Int? {
        // First try patterns used for live inventory titles (with parentheses/page counts)
        enderChestPattern.matchMatcher(storageName) {
            return groupOrNull("page")?.toInt() ?: 1
        }
        backpackPattern.matchMatcher(storageName) {
            return groupOrNull("page")?.toInt() // backpack GUI always has an explicit slot number
        }
        riftStoragePattern.matchMatcher(storageName) {
            return groupOrNull("page")?.toInt() ?: 1
        }
        // Fallback: parse the stored container names we generate internally like
        // "Ender Chest 1", "Backpack 2", "Rift Storage 3"
        if (storageName.startsWith("Ender Chest ")) {
            return storageName.removePrefix("Ender Chest ").toIntOrNull()
        }
        if (storageName.startsWith("Backpack ")) {
            return storageName.removePrefix("Backpack ").toIntOrNull()
        }
        if (storageName.startsWith("Rift Storage ")) {
            return storageName.removePrefix("Rift Storage ").toIntOrNull()
        }
        return null
    }

    private val unparsePrivateIslandChestPattern by RepoPattern.pattern(
        "storage.privateislandchestunparse",
        "Private Island Chest LorenzVec\\(x=(?<x>-?\\d+(\\.\\d+)?), y=(?<y>-?\\d+(\\.\\d+)?), z=(?<z>-?\\d+(\\.\\d+)?)\\)",
    )

    /**
     * Gets the location from a private island chest storage name
     */
    fun getLocationFromStorageName(storageName: String): LorenzVec? {
        if (!storageName.startsWith("Private Island Chest")) return null

        // Extract coordinates from the storage name
        return unparsePrivateIslandChestPattern.matchMatcher(storageName) {
            val x = groupOrNull("x")?.toDoubleOrNull()
            val y = groupOrNull("y")?.toDoubleOrNull()
            val z = groupOrNull("z")?.toDoubleOrNull()
            return@matchMatcher if (x != null && y != null && z != null) LorenzVec(x, y, z) else null
        }
    }


    @HandleEvent
    fun itemBackgroundRender(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (currentInventoryResults.isEmpty()) return
        val offSet = currentInventoryResults.first().category.indexOffSet
        val slots = currentInventoryResults.map { it.slotIndex + offSet }.toHashSet()
        InventoryUtils.getItemsInOpenChestWithNull().forEachIndexed { index, slot ->
            if (slots.contains(index)) {
                slot.highlight(LorenzColor.YELLOW)
            }
        }
    }

    @HandleEvent
    fun onGuiContainerClose(event: InventoryCloseEvent) {
        currentInventoryResults = emptyList()
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun renderWaypoints(event: SkyHanniRenderWorldEvent) {
        toHighlightResults.filter { it.isPrivateIslandChest() }.forEach {
            val location = it.location ?: return@forEach
            event.drawWaypointFilled(location, java.awt.Color.YELLOW, true, false)
        }
    }
}

fun List<StorageSearchResult>.outputToChat() {
    StorageNavigationUtils.makeNavigatableChatList(this, allowServerChange = false)
}

