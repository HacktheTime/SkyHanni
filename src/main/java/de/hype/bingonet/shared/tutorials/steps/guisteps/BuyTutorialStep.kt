package de.hype.bingonet.shared.tutorials.steps.guisteps

import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class BuyTutorialStep(
    items: Map<String, ItemInfo>,
) : TutorialStep() {
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
