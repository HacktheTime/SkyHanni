package de.hype.bingonet.shared.tutorials.steps.requirement

import at.hannibal2.skyhanni.utils.NeuInternalName
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Pure data class for NEU recipe unlock requirement steps.
 * Step completes when the specified recipe is unlocked.
 */
class NeuRecipeRequirement(
    val recipe: NeuInternalName,
) : TutorialStep()
