package de.hype.bingonet.shared.tutorials.steps.storagestep

import at.hannibal2.skyhanni.features.inventory.storage.ItemTagManager
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventoryAndSacks
import at.hannibal2.skyhanni.utils.InventoryUtils.isTopInventory
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getRecipes
import at.hannibal2.skyhanni.utils.PrimitiveRecipe
import at.hannibal2.skyhanni.utils.RecipeType
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.objects.Position
import de.hype.bingonet.shared.tutorials.ItemCheck
import de.hype.bingonet.shared.tutorials.ResourceContributor
import de.hype.bingonet.shared.tutorials.ResourceItemCheck
import de.hype.bingonet.shared.tutorials.TaggedItemCheck
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import de.hype.bingonet.shared.tutorials.steps.requirement.ObtainCoinsTutorialStep
import kotlin.math.ceil
import net.minecraft.item.ItemStack

/**
 * Unified obtain step: replaces Buy/Store/Retrieve/Has flows.
 * - forceInventory: require item to be in player inventory (not just stored)
 * - forceNPCLeftOver: if set, keep at least this much of NPC daily limit for future planning
 * - tagName: optional tag to assign automatically when a matching stack is found; each item gets at most 1 tag
 */
class ObtainTutorialStep(
    val check: ItemCheck,
    val obtainSource: List<ObtainSource> = emptyList(),
    val forceInventory: Boolean = false,
    val forceNPCLeftOver: Int? = null,
    val tagName: String? = null,
) : TutorialStep(), ResourceContributor {

    override fun getStepName(tutorial: Tutorial): String = "Obtain ${check.displayText}"

    override fun getStepDescription(tutorial: Tutorial): String? = check.descriptionText

    override fun getRequirements(): List<TutorialNode> {
        val coinReq = computeCoinRequirementOrNull()
        return if (coinReq != null) listOf(coinReq) else emptyList()
    }

    override fun check(tutorial: Tutorial): Boolean {
        val stacks = getCandidateStacks()
        val matched = stacks.filter { it != null && check.check(it) }.mapNotNull { it }
        if (matched.isEmpty()) return false
        tagName?.let { ensureTaggedFirstMatch(matched, it) }
        return true
    }

    private fun getCandidateStacks(): List<ItemStack?> {
        val own = InventoryUtils.getItemsInOwnInventoryWithNull()?.toList().orEmpty()
        if (forceInventory) return own
        val top = InventoryUtils.getItemsInOpenChestWithNull().filter { it.isTopInventory() }.map { it.stack }
        return own + top
    }

    private fun ensureTaggedFirstMatch(matched: List<ItemStack>, tag: String) {
        // Tag the first untagged matching stack if the tag is not in use yet
        if (ItemTagManager.getUuidByTag(tag) != null) return
        val single = matched.firstOrNull() ?: return
        val uuid = single.getItemUuid() ?: return
        if (ItemTagManager.getTagByUuid(uuid) != null) return
        ItemTagManager.addTag(uuid, tag, single.displayName ?: tag)
    }

    private fun computeCoinRequirementOrNull(): ObtainCoinsTutorialStep? {
        val res = check as? ResourceItemCheck ?: return null
        val target = res.item
        val want = res.amount
        val have = target.getAmountInInventoryAndSacks()
        val need = (want - have).coerceAtLeast(0)
        if (need <= 0) return null
        val npcRecipes = getRecipes(target).filter { it.recipeType == RecipeType.NPC_SHOP }
        if (npcRecipes.isEmpty()) return null
        val best: PrimitiveRecipe? = npcRecipes.minByOrNull { r -> r.ingredients.filter { it.isCoin() }.sumOf { it.count } }
        val coinPerUnit = best?.ingredients?.filter { it.isCoin() }?.sumOf { it.count } ?: 0.0
        if (coinPerUnit <= 0.0) return null
        val limit = best?.limit ?: Int.MAX_VALUE
        val maxBuyable = minOf(limit, need)
        if (maxBuyable <= 0) return null
        val coinsNeeded = ceil(coinPerUnit * maxBuyable).toLong()
        if (coinsNeeded <= 0L) return null
        return ObtainCoinsTutorialStep(coinsNeeded)
    }

    override fun getRequiredResources(tutorial: Tutorial): Map<NeuInternalName, Double> {
        val res = check as? ResourceItemCheck ?: return emptyMap()
        val target = res.item
        val want = res.amount
        val have = target.getAmountInInventoryAndSacks()
        val need = (want - have).coerceAtLeast(0)
        return if (need > 0) mapOf(target to need.toDouble()) else emptyMap()
    }

    override fun validate(tutorial: Tutorial): List<String> {
        val issues = mutableListOf<String>()
        if (tagName != null && (check as? TaggedItemCheck)?.tag == tagName) issues += "Obtain Step match requests tag addition but item check" +
            " is Tag based with same tag!"
        if (check.displayText.isBlank()) issues += "Obtain step missing display text"
        if (check is ResourceItemCheck && check.amount <= 0) issues += "Obtain step for ${check.item} has non-positive amount"
        return issues
    }

    // Obtain source definitions for routing and hints
    sealed interface ObtainSource

    data class LocationBasedObtainSource(
        val comment: String,
        val island: Islands,
        val position: Position,
    ) : ObtainSource

    data class FishingObtainSource(
        val comment: String,
        val seaCreatureId: String,
    ) : ObtainSource
}
