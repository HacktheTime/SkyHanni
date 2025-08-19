package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventory
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import de.hype.bingonet.environment.displayName

class NeuNPC private constructor(
    val neuItem: NeuInternalName
) {
    val isNPC by lazy {
        neuItem.displayName
    }

    val items by lazy {
        val recipies = EnoughUpdatesManager.getRecipesFor(neuItem).filter { it.recipeType== RecipeType.NPC_SHOP }

    }

    data class NPCOffer private constructor(
        val recipe: PrimitiveRecipe,
    ){
        val cost by lazy {
            return@lazy recipe.ingredients.map { it.internalName to it.count }.groupBy { it.first }.mapValues { it.value.sumOf { it.second } }
        }
    }


    companion object{
        val namePattern = ".*\\((?:(?:rift )?npc|monster|mayor)\\)".toPattern()
        fun NeuInternalName.toNPC(): NeuNPC? {
            if (!namePattern.matches(this.itemNameWithoutColor)) return null
            return NeuNPC(this)
        }
    }
}
