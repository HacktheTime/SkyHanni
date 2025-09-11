package de.hype.bingonet.shared.tutorials.steps.requirement

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.storage.PlayerSpecificStorage
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TabListData
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class ObtainCoinsTutorialStep(
    val amount: Int
) : TutorialStep() {
    override fun getStepName(): String {
        return "Obtain ${amount.formatCoin(false)} Coins"
    }

    override fun getStepDescription(tutorial: Tutorial): String {
        //TODO tips for money making based on users options.
        //Arachne, Enderman, Relics?
        //Maybe show user options what they could sell to npc based on most expensive to least expensive.
    }

    @HandleEvent
    fun onPurseChange(event: PurseChangeEvent){
        event.purse + CustomScoreboardUtils.getBank()
    }
}
