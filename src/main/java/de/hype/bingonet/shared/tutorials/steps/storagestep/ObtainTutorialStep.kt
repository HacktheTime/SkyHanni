package de.hype.bingonet.shared.tutorials.steps.storagestep

import de.hype.bingonet.shared.constants.ObtainSource
import de.hype.bingonet.shared.tutorials.ItemCheck
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Pure data class for item obtaining tutorial steps.
 * Step completes when the specified item is obtained.
 * 
 * @param forceInventory If true, requires the item to be in inventory
 * @param forceNPCLeftOver If set, requires keeping this amount when buying from NPC
 * @param tagName If set, tags the obtained item with this name
 */
class ObtainTutorialStep(
    val check: ItemCheck,
    val obtainSource: List<ObtainSource> = emptyList(),
    val forceInventory: Boolean = false,
    val forceNPCLeftOver: Int? = null,
    val tagName: String? = null,
) : TutorialStep()
