package de.hype.bingonet.shared.tutorials.steps.storagestep

import at.hannibal2.skyhanni.utils.NeuItems
import de.hype.bingonet.shared.tutorials.ItemInfo
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.steps.GUIBasedTutorialStep
import java.util.regex.Pattern

class BuyTutorialStep(
    val items: Map<IttemInfo, Int>,
    /**
     * Suggest NPC via NEU repo + dont forget trades and shops like garden ig?
     */
    guiName: Pattern,
) : GUIBasedTutorialStep(guiName) {
    override fun getStepName(): String {
        TODO("Not yet implemented")
    }

    override fun getStepDescription(tutorial: Tutorial): String {
        TODO("Not yet implemented")
    }

    data class BuyInfo(
        val item: ItemInfo,
        val count: Int,
        val addTags: List<String> = emptyList(),
    ) {
        fun isValid() : Boolean{
            return !(addTags.isNotEmpty() && (count != 1))
            //TODO Technically it would be better to check stackable state and tags and count.
        }
    }

    val requiredCoins : Int by lazy {
        NeuItems.findItemNameWithoutNPCs()
    }
}

//TODO neu repo contains a list of items the npcs sell. using the gui name maybe try to match npc name to it track and show the cost as well as on how to get the coins maybe

//TODO defaults for some buy items + automatic item tagging for things like the axes since they have an item uuid?
