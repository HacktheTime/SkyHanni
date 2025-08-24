package de.hype.bingonet.shared.tutorials.steps

import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.constants.MiningEvents
import de.hype.bingonet.shared.tutorials.Tutorial

class AwaitMiningEvent(
    val event: MiningEvents,
    val islands: Islands?,
) : TutorialStep() {
    override fun getStepName(): String {
        return "Waiting for $event${islands.let { if (it == null) ""; else " on ${it.getDisplayName()}" }}"
    }

    override fun getStepDescription(tutorial: Tutorial): String? {
        return null
    }

    override fun isComplete(tutorial: Tutorial): Boolean {
        //TODO add mining event check
    }

}
