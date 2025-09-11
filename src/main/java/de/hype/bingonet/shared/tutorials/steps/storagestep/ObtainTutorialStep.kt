package de.hype.bingonet.shared.tutorials.steps.storagestep

import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.features.fishing.SeaCreature
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.objects.Position
import de.hype.bingonet.shared.tutorials.ItemCheck
import de.hype.bingonet.shared.tutorials.TaggedItemCheck
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class ObtainTutorialStep(
    val check: ItemCheck,
    val obtainSource: List<ObtainSource>,
    val forceInventory: Boolean = false,
    val forceNPCLeftOver: Int? = null,
) : TutorialStep() {

    constructor(check: TaggedItemCheck) : this()

    override fun isComplete(tutorial: Tutorial): Boolean {
        TODO("Not yet implemented")
    }

    //TODO it should detect if more resources than what npc gives are needed


    class LocationBasedObtainSource(
        val comment: String,
        val island : Islands,
        val position: Position,
    )

    class FishingObtainSource(
        val comment: String,
        val seaCreature : SeaCreature
    ) {
        fun getIslands(){
            return seaCreature.re
        }
    }
}
