package de.hype.bingonet.shared.tutorials.steps.misc

import at.hannibal2.skyhanni.utils.NeuInternalName
import de.hype.bingonet.shared.constants.Rarity
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Pure data class for pet equipping tutorial steps.
 * Step completes when a pet of the specified type and minimum rarity is equipped.
 */
class EquipPetTutorialStep(
    val petType: NeuInternalName,
    val minimumPetRarity: Rarity,
) : TutorialStep()
