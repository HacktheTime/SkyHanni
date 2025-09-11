package de.hype.bingonet.shared.tutorials.steps.itemstep

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.misc.items.enchants.Enchant
import de.hype.bingonet.shared.tutorials.SingleItemCheck
import de.hype.bingonet.shared.tutorials.TaggedItemCheck
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.steps.GUIBasedTutorialStep
import java.util.regex.Pattern

class EnchantTutorialStep(
    val item: TaggedItemCheck,
    /**
     * Map of enchant internal names to levels.
     *
     * Positive for minimum level. Aka if you want the user to get 5 but they have 6 enter 5
     * 0 or negative. Tells the user to get the exact absolute level. 0 = disenchant
     */
    val enchantIds : Map<String, Int>,
    ) : GUIBasedTutorialStep(Pattern.compile("Enchant Items")){

    constructor(
        item: TaggedItemCheck,
        enchants: Map<Enchant, Int>
    ): this(
        item, enchantIds = enchants.mapKeys { it.key.nbtName }
    )

    fun calcGrandsNeeded(){

    }

    override fun getStepDescription(tutorial: Tutorial): String {

    }

    val assumeGrandCarrier get() = SkyHanniMod.feature

    //TODO render overlay, highlight items, highlight missing enchants once item is inserted. show how many grands are needed for a enchant from table.

    //TODO only allow for tagged items to be enchanted. for daily 500k its not needed since extra step type. then when entering library auto scan items and open the tagged items with highlight.
}
