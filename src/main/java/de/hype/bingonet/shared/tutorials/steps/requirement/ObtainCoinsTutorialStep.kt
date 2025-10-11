package de.hype.bingonet.shared.tutorials.steps.requirement

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.PurseApi
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.features.tutorial.SellProtection
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class ObtainCoinsTutorialStep(
    val amount: Long,
) : TutorialStep() {

    private var lastKnownPurse: Double = PurseApi.currentPurse

    override fun getStepName(tutorial: Tutorial): String = "Have at least ${amount} Coins (Purse)"

    override fun getStepDescription(tutorial: Tutorial): String? {
        val need = (amount - lastKnownPurse).toLong()
        if (need <= 0L) return null
        // Build NPC sell suggestions from current inventory, excluding protected resources
        val protected = SellProtection.getProtectedResources().filterValues { it > 0.0 }.keys
        val counts = mutableMapOf<NeuInternalName, Int>()
        InventoryUtils.getItemsInOwnInventory().forEach { stack ->
            val internal = stack.getInternalNameOrNull() ?: return@forEach
            if (internal in protected) return@forEach
            val npc = internal.getNpcPriceOrNull() ?: return@forEach
            if (npc <= 0.0) return@forEach
            counts.merge(internal, stack.stackSize) { a, b -> a + b }
        }
        if (counts.isEmpty()) return null
        val ranked = counts.entries
            .map { (id, cnt) -> Triple(id, cnt, (id.getNpcPriceOrNull() ?: 0.0) * cnt) }
            .filter { it.third > 0.0 }
            .sortedByDescending { it.third }
            .take(8)
        if (ranked.isEmpty()) return null
        val header = "Tip: Sell to NPC (excludes protected):"
        val lines = ranked.map { (id, cnt, total) ->
            val per = id.getNpcPriceOrNull() ?: 0.0
            "- ${id.asString()} x$cnt = ${total.formatCoin()} (per ${per.formatCoin(true)})"
        }
        return (listOf(header) + lines).joinToString("\n")
    }

    override fun getRequirements(): List<TutorialNode> = emptyList()

    override fun check(tutorial: Tutorial): Boolean {
        return lastKnownPurse >= amount
    }

    override fun validate(tutorial: Tutorial): List<String> {
        return if (amount <= 0) listOf("ObtainCoins step has non-positive amount") else emptyList()
    }

    @HandleEvent
    fun onPurseChange(event: PurseChangeEvent) {
        lastKnownPurse = event.purse
        val t = at.hannibal2.skyhanni.features.tutorial.TutorialManager.activeTutorial ?: return
        if (!completed && check(t)) complete()
    }
}
