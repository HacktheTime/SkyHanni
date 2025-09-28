package de.hype.bingonet.shared.tutorials

import at.hannibal2.skyhanni.utils.NeuInternalName

/**
 * Steps that can contribute "required resources" for the whole tutorial implement this.
 * Values are absolute amounts needed to safely protect/plan across the tutorial.
 */
interface ResourceContributor {
    fun getRequiredResources(tutorial: Tutorial): Map<NeuInternalName, Double>
}
