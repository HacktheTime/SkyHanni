package at.hannibal2.skyhanni.features.tutorial

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.NeuInternalName

/**
 * SellProtection exposes a simple check to prevent selling items that are required
 * by the currently active tutorial. It reads the protected resources map computed
 * by the tutorial cache (Tutorial.requiredResources).
 */
object SellProtection {

    /** Returns the protected resource plan from the active tutorial, or empty when none. */
    fun getProtectedResources(): Map<NeuInternalName, Double> =
        TutorialManager.activeTutorial?.requiredResources ?: emptyMap()

    /** True if the given item is part of the protected resources set (amount > 0). */
    fun isProtected(item: NeuInternalName): Boolean =
        getProtectedResources()[item]?.let { it > 0.0 } == true

    /**
     * Returns true if a sale should be blocked for this item and amount.
     * Current policy: if config flag is on and the item is protected at all, block the sale.
     */
    fun shouldBlockSale(item: NeuInternalName, amount: Int): Boolean {
        if (!SkyHanniMod.feature.tutorials.tutorialProtectRequiredItems) return false
        if (amount <= 0) return false
        return isProtected(item)
    }
}
