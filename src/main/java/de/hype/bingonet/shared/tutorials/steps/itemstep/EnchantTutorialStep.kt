package de.hype.bingonet.shared.tutorials.steps.itemstep

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.features.misc.items.enchants.Enchant
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.isTopInventory
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import de.hype.bingonet.shared.tutorials.TaggedItemCheck
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.GUIBasedTutorialStep
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic
import java.awt.Color
import java.util.Locale
import java.util.regex.Pattern

class EnchantTutorialStep(
    val item: TaggedItemCheck,
    /**
     * Map of enchant internal names to levels.
     *
     * Positive for minimum level. Aka if you want the user to get 5 but they have 6 enter 5
     * 0 or negative. Tells the user to get the exact absolute level. 0 = disenchant
     */
    val enchantIds: Map<String, Int>,
) : GUIBasedTutorialStep(Pattern.compile("Enchant Items")) {

    constructor(
        enchants: Map<Enchant, Int>,
        item: TaggedItemCheck,
    ) : this(
        item, enchantIds = enchants.mapKeys { it.key.nbtName },
    )

    override fun getStepName(tutorial: Tutorial): String {
        return "Enchant ${item.displayText}"
    }

    override fun getStepDescription(tutorial: Tutorial): String? {
        return item.descriptionText
    }

    override fun getRequirements(): List<TutorialNode> = emptyList()

    override fun isComplete(tutorial: Tutorial): Boolean = completed

    private fun idToDisplayName(id: String): String {
        val plain = id.lowercase(Locale.US).replace("_", " ")
        return plain.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun parseNameAndLevel(name: String): Pair<String, Int?> {
        // Extract trailing roman numeral or number as level
        val parts = name.replace(Regex("§."), "").trim().split(" ")
        if (parts.isEmpty()) return name to null
        val last = parts.last()
        val level = romanOrNumberToInt(last)
        val base = if (level != null) parts.dropLast(1).joinToString(" ") else name
        return base.lowercase(Locale.US) to level
    }

    private fun romanOrNumberToInt(s: String): Int? {
        return s.toIntOrNull() ?: romanToIntOrNull(s)
    }

    private fun romanToIntOrNull(s: String): Int? {
        val map = mapOf(
            'I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000,
        )
        var sum = 0
        var prev = 0
        for (ch in s.uppercase(Locale.US)) {
            val v = map[ch] ?: return null
            sum += if (v > prev) v - 2 * prev else v
            prev = v
        }
        return sum
    }

    private fun findTargetStackUuid(): String? {
        val targetUuid = at.hannibal2.skyhanni.features.inventory.storage.ItemTagManager.getUuidByTag(item.tag)
        return targetUuid
    }

    private fun findCurrentItemStackUuid(): String? = findTargetStackUuid()

    private fun computeMissingLevels(currentLevel: Int?, target: Int): Set<Int> {
        if (target <= 0) {
            val exact = kotlin.math.abs(target)
            return if (currentLevel == exact) emptySet() else setOf(exact)
        }
        val cur = currentLevel ?: 0
        if (cur >= target) return emptySet()
        return (cur + 1..target).toSet()
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (ignoreEvent()) return

        // Highlight the desired item by UUID in the player's inventory and top inventory (if inserted)
        val targetUuid = findCurrentItemStackUuid()
        if (targetUuid != null) {
            (InventoryUtils.getSlotsInOwnInventory() + InventoryUtils.getItemsInOpenChestWithNull()).forEach { slot ->
                val stack = slot.stack ?: return@forEach
                if (stack.getItemUuid() == targetUuid) {
                    slot.highlight(Color.YELLOW)
                }
            }
        }

        // Determine current levels on the target item
        val currentLevels: Map<String, Int> = run {
            val allSlots = InventoryUtils.getItemsInOpenChestWithNull() + InventoryUtils.getSlotsInOwnInventory()
            val stack = allSlots.firstOrNull { it.stack?.getItemUuid() == targetUuid }?.stack
            stack?.getHypixelEnchantments().orEmpty()
        }

        val neededOverview: MutableSet<String> = mutableSetOf()
        val neededTiers: MutableMap<String, Set<Int>> = mutableMapOf()
        enchantIds.forEach { (id, targetLevel) ->
            val current = currentLevels[id]
            val missing = computeMissingLevels(current, targetLevel)
            if (missing.isNotEmpty()) {
                neededOverview += id
                neededTiers[id] = missing
            }
        }

        // Highlight entries in the top inventory that match needed enchants/tiers
        val topSlots = InventoryUtils.getItemsInOpenChestWithNull().filter { it.isTopInventory() }
        var anyHighlightedOnPage = false
        for (slot in topSlots) {
            val stack = slot.stack ?: continue
            val display = stack.displayName.replace(Regex("§."), "").trim()
            stack.getLore().joinToString("\n")
            val (baseNameLc, levelOpt) = parseNameAndLevel(display)

            // Try to match against needed enchants
            val matchedId = neededOverview.firstOrNull { id ->
                val expectedName = idToDisplayName(id).lowercase(Locale.US)
                baseNameLc == expectedName || display.lowercase(Locale.US).contains(expectedName)
            } ?: continue

            // Overview page: no explicit level
            if (levelOpt == null) {
                slot.highlight(Color.ORANGE)
                anyHighlightedOnPage = true
                continue
            }

            // Tier page: highlight tiers still needed
            val needed = neededTiers[matchedId].orEmpty()
            if (levelOpt in needed) {
                slot.highlight(Color.ORANGE)
                anyHighlightedOnPage = true
            }
        }

        // If nothing to highlight on this page but still missing overall, hint to swap pages
        if (!anyHighlightedOnPage && neededOverview.isNotEmpty()) {
            TutorialStepLogic.chatPromptSuggestion("Swap pages in the Enchanting menu to find the required enchants.") { }
        }

        // Completion check: all needed fulfilled
        if (neededOverview.isEmpty()) {
            complete()
        }
    }

    val assumeGrandCarrier get() = SkyHanniMod.feature

    //TODO render overlay, highlight items, highlight missing enchants once item is inserted. show how many grands are needed for a enchant from table.

    //TODO only allow for tagged items to be enchanted. for daily 500k its not needed since extra step type. then when entering library auto scan items and open the tagged items with highlight.
}
