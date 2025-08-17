package de.hype.bingonet.shared.tutorials.steps.requirement

import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class ObtainCoinsTutorialStep(
    val amount: Int
) : TutorialStep() {
    override fun getStepName(): String {
        return "Obtain ${amount.formatCoin(false)} Coins"
    }

    override fun getStepDescription(): String? {
        //TODO tips for money making based on users options.
        //Arachne, Enderman, Relics?
        //Maybe show user options what they could sell to npc based on most expensive to least expensive.
    }
}
