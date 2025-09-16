package de.hype.bingonet.shared.tutorials.steps

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.toSHIsland
import at.hannibal2.skyhanni.events.mining.NewMiningEvent
import at.hannibal2.skyhanni.features.mining.eventtracker.MiningEventDataReceive
import at.hannibal2.skyhanni.features.mining.eventtracker.MiningEventDisplay
import at.hannibal2.skyhanni.features.mining.eventtracker.toBN
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.constants.MiningEvents
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode

class AwaitMiningEvent(
    val event: MiningEvents,
    val islands: Islands?,
) : TutorialStep() {
    override fun getStepName(tutorial: Tutorial): String {
        return "Waiting for $event${islands.let { if (it == null) ""; else " on ${it.getDisplayName()}" }}"
    }

    override fun getStepDescription(tutorial: Tutorial): String? {
        return null
    }

    override fun getRequirements(): List<TutorialNode> = emptyList()

    @HandleEvent
    fun onNewMiningEvent(event: NewMiningEvent) {
        if (ignoreEvent()) return
        val runningEvents = event.data.runningEvents
        val islands = islands
        if (islands != null) {
            val event = runningEvents.get(islands.toSHIsland())?.lastOrNull()?.event?.toBN()
            if (event == this.event) {
                complete()
            }
        } else {
            runningEvents.forEach {
                val event = it.value.lastOrNull()?.event?.toBN()
                if (event == this.event) {
                    complete()
                    return
                }
            }
        }

    }

    override fun isComplete(tutorial: Tutorial): Boolean {
        return completed
        //TODO add mining event check
    }

}
