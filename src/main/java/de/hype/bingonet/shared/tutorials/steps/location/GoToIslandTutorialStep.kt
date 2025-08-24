package de.hype.bingonet.shared.tutorials.steps.location

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.toBNIsland
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.utils.HypixelCommands
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class GoToIslandTutorialStep(val island: Islands) : TutorialStep {
    override fun getStepName(): String {
        TODO("Not yet implemented")
    }

    override fun getStepDescription(tutorial: Tutorial): String? {
        TODO("Not yet implemented")
    }

    override fun onActivate() {
        if (HypixelData.skyBlockIsland.toBNIsland()==island) complete()
        chatPromptSuggestion("Next Task: Travel to $island Do you want to warp there now?"){
            island.warpArgument?.let {
                HypixelCommands.warp(it)
            }
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent){
        if (isActive&&event.newIsland.toBNIsland()==island) complete()
    }
}
