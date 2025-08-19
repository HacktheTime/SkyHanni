package de.hype.bingonet.shared.tutorials.steps.guisteps

import de.hype.bingonet.sharedcompilation.sbenums.BNNEUItem


class CraftItemTutorialStep(val item: BNNEUItem, val requiredAmount: Int = 1) {
    val crafted: Int = 0

    //TODO check requirement, show if unlock needed + missing items via item search.
    // Item search should be done recursively for items needed going. for example if you need
    // x e lapis blocks check how much e lapis and then ways to get e lapis
    // adding both lapis and lapis block normal e recipe.
    // If possible consider de crafting maybe?
    // Issue with decrafting like lapis with lapis being craftable from the block.

    fun hasResources() {
        val recipies = item.getCraftingRecipies()
        recipies.map { it.ingredients }
    }
}
