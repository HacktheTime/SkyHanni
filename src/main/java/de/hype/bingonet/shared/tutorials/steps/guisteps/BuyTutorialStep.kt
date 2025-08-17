package de.hype.bingonet.shared.tutorials.steps.guisteps

import de.hype.bingonet.shared.tutorials.steps.GUIBasedTutorialStep
import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import java.util.regex.Pattern

class BuyTutorialStep(
    val items: Map<String, ItemInfo>,
    /**
     * Suggest NPC via NEU repo + dont forget trades and shops like garden ig?
     */
    guiName: Pattern
) : GUIBasedTutorialStep(guiName) {
    override fun getStepName(): String {
        TODO("Not yet implemented")
    }

    override fun getStepDescription(): String? {
        TODO("Not yet implemented")
    }

    data class ItemInfo(
        val count: Int,
        var done: Boolean = false,
        var price: Double? = null
    )
}

//TODO neu repo contains a list of items the npcs sell. using the gui name maybe try to match npc name to it track and show the cost as well as on how to get the coins maybe

//TODO defaults for some buy items + automatic item tagging for things like the axes since they have an item uuid?
