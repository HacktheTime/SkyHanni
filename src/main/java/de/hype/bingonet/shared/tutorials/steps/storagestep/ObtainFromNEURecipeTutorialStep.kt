package de.hype.bingonet.shared.tutorials.steps.storagestep

import at.hannibal2.skyhanni.utils.NeuInternalName
import de.hype.bingonet.shared.constants.ObtainWay
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Pure data class for NEU recipe-based obtaining tutorial steps.
 * Step completes when the item is obtained following NEU recipe logic.
 */
class ObtainFromNEURecipeTutorialStep(
    val item: NeuInternalName,
    val requiredAmount: Int = 1,
    val obtainMap: Map<NeuInternalName, ObtainWay?>,
    val preferOnCurrentIsland: Boolean = true,
) : TutorialStep()
