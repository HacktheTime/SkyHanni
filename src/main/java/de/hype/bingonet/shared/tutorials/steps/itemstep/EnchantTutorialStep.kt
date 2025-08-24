package de.hype.bingonet.shared.tutorials.steps.itemstep

import at.hannibal2.skyhanni.SkyHanniMod
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.steps.GUIBasedTutorialStep
import java.util.regex.Pattern

class EnchantTutorialStep : GUIBasedTutorialStep(Pattern.compile("Enchant Items")){
    override fun getStepName(): String {
        return "Enchant Items."
    }

    fun calcGrandsNeeded()

    override fun getStepDescription(tutorial: Tutorial): String? {

    }

    val assumeGrandCarrier get() = SkyHanniMod.feature

    //TODO render overlay, highlight items, highlight missing enchants once item is inserted. show how many grands are needed for a enchant from table.

    //TODO only allow for tagged items to be enchanted. for daily 500k its not needed since extra step type. then when entering library auto scan items and open the tagged items with highlight.
}
