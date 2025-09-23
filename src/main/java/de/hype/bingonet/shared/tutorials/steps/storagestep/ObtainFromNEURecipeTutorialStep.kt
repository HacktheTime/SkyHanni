package de.hype.bingonet.shared.tutorials.steps.storagestep
import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventoryAndSacks
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getRecipes
import at.hannibal2.skyhanni.utils.PrimitiveRecipe
import at.hannibal2.skyhanni.utils.RecipeType
import de.hype.bingonet.shared.tutorials.ResourceContributor
import de.hype.bingonet.shared.tutorials.ResourceItemCheck
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import kotlin.math.ceil

/**
 * Obtain an item strictly by a selected NEU recipe (crafting or npc_shop/trade), with a per-ingredient obtain map.
 * Each ingredient can specify a list of nodes to perform to gather it; if null, default to NPC purchases when possible.
 */
class ObtainFromNEURecipe(
    val item: NeuInternalName,
    val requiredAmount: Int = 1,
    val obtainMap: Map<NeuInternalName, ObtainWay?>,
    val preferOnCurrentIsland: Boolean = true,
) : TutorialStep(), ResourceContributor {

    data class ObtainWay(val nodes: List<TutorialNode>)

    override fun getStepName(tutorial: Tutorial): String = "Obtain ${item.internalName} x$requiredAmount (by recipe)"

    

    

    

    

    

    

    

    

    

    

override fun getStepDescription(tutorial: Tutorial): String? {
        val config = SkyHanniMod.feature.tutorials
        val id = item
        val recipe = manualRecipe ?: pickRecipe(id) ?: return null
        val desc = mutableListOf<String>()

        // Compaction/crafting hint when all low-tier resources are available now
        if (config.showCompactionHints) {
            val perUnit = flattenIngredients(recipe)
            val canCraftNow = perUnit.all { (mat, per) ->
                val have = mat.getAmountInInventoryAndSacks()
                have >= ceil(per).toInt()
            }
            if (canCraftNow) {
                val name = id.itemNameWithoutColor
                val verb = if (id.asString().startsWith("ENCHANTED_")) "compact" else "craft"
                desc += "Hint: You can $verb $name now."
            }
        }

        // NPC buy hint only when obtain way is null
        if (config.npcBuyHintsOnlyWhenObtainWayNull) {
            val perUnit = flattenIngredients(recipe)
            val npcs = perUnit.keys.filter { ing ->
                val r = getRecipes(ing)
                r.any { it.recipeType == RecipeType.NPC_SHOP }
            }.filter { ing -> obtainMap[ing] == null }
            if (npcs.isNotEmpty()) {
                val first = npcs.first().itemNameWithoutColor
                desc += "Hint: You can buy $first from an NPC (default mapping)."
            }
        }

        return if (desc.isEmpty()) null else desc.joinToString("\n")
    }

fun setManualRecipe(recipe: PrimitiveRecipe?) { manualRecipe = recipe }

override fun getRequirements(): List<TutorialNode> {
        val id = item
        val have = id.getAmountInInventoryAndSacks()
        val needUnits = (requiredAmount - have).coerceAtLeast(0)
        if (needUnits <= 0) return emptyList()
        val recipe = manualRecipe ?: pickRecipe(id) ?: return emptyList()
        val perUnit = flattenIngredients(recipe)
        val reqs = mutableListOf<TutorialNode>()
        for ((mat, per) in perUnit) {
            val total = ceil(per * needUnits).toInt()
            if (total <= 0) continue
            val way = obtainMap[mat]
            if (way == null) {
                // Default: obtain via NPC or any available route
                reqs += ObtainTutorialStep(
                    check = ResourceItemCheck(item = mat, amount = total, displayText = mat.itemNameWithoutColor),
                )
            } else {
                reqs += way.nodes
            }
        }
        return reqs
    }

override fun check(tutorial: Tutorial): Boolean {
        val id = item
        val have = id.getAmountInInventoryAndSacks()
        return have >= requiredAmount
    }

override fun getRequiredResources(tutorial: Tutorial): Map<NeuInternalName, Double> {
        val target = item
        val have = target.getAmountInInventoryAndSacks()
        val needUnits = (requiredAmount - have).coerceAtLeast(0)
        if (needUnits <= 0) return emptyMap()

        val recipe = manualRecipe ?: pickRecipe(target)
        val perUnit = flattenIngredients(recipe ?: return mapOf(target to needUnits.toDouble()))
        // Planned progress scaling
        val config = SkyHanniMod.feature.tutorials
        val planned = (config.tutorialPlannedProgressPercent.coerceIn(1, 100) / 100.0)
        return perUnit.mapValues { it.value * needUnits * planned }
    }

override fun validate(tutorial: Tutorial): List<String> {
        val issues = mutableListOf<String>()
        if (requiredAmount <= 0) issues += "ObtainFromNEURecipe for ${item.internalName} has non-positive amount"
        val target = item
        val recipe = manualRecipe ?: pickRecipe(target)
        if (recipe == null) {
            issues += "No NEU recipe found for ${item.internalName}"
            return issues
        }
        val perUnit = flattenIngredients(recipe)
        perUnit.keys.forEach { ing ->
            if (!obtainMap.containsKey(ing)) issues += "Missing obtain way mapping (nullable or list) for ingredient ${ing.itemNameWithoutColor}"
        }
        // Validate nested nodes
        obtainMap.values.filterNotNull().forEach { way -> way.nodes.forEach { issues += it.validate(tutorial) } }
        return issues
    }

private fun pickRecipe(target: NeuInternalName): PrimitiveRecipe? {
        val list = getRecipes(target)
        if (list.isEmpty()) return null
        // Prefer crafting if we have more of its primitive mats; else NPC by lower coin price
        val crafting = list.filter { it.recipeType == RecipeType.CRAFTING }
        val scoredCraft = crafting.maxByOrNull { r -> r.ingredients.sumOf { it.internalName.getAmountInInventoryAndSacks().toLong() } }
        val npcs = list.filter { it.recipeType == RecipeType.NPC_SHOP }
        val cheapestNpc = npcs.minByOrNull { r -> r.ingredients.filter { it.isCoin() }.sumOf { it.count } }
        return scoredCraft ?: cheapestNpc ?: list.firstOrNull()
    }

private fun flattenIngredients(recipe: PrimitiveRecipe): Map<NeuInternalName, Double> =
        flattenIngredientsInternal(recipe, mutableSetOf())

private fun flattenIngredientsInternal(recipe: PrimitiveRecipe, visiting: MutableSet<NeuInternalName>): Map<NeuInternalName, Double> {
        val result = mutableMapOf<NeuInternalName, Double>()
        for (ing in recipe.ingredients) {
            val internal = ing.internalName
            if (!visiting.add(internal)) continue // guard recursion
            val subRecipes = getRecipes(internal)
            val crafting = subRecipes.firstOrNull { it.recipeType == RecipeType.CRAFTING }
            if (crafting == null) {
                result.merge(internal, ing.count) { a, b -> a + b }
            } else {
                val sub = flattenIngredientsInternal(crafting, visiting)
                sub.forEach { (k, v) -> result.merge(k, v * ing.count) { a, b -> a + b } }
            }
            visiting.remove(internal)
        }
        return result
    }

private var manualRecipe: PrimitiveRecipe? = null
}
