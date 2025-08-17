package de.hype.bingonet.shared.tutorials.steps.itemstep

import at.hannibal2.skyhanni.SkyHanniMod
import de.hype.bingonet.shared.tutorials.steps.GUIBasedTutorialStep
import java.util.regex.Pattern

class EnchantTutorialStep : GUIBasedTutorialStep(Pattern.compile("Enchant Items")){
    override fun getStepName(): String {
        return "Enchant Items."
    }

    fun calcGrandsNeeded()

    override fun getStepDescription(): String? {

    }

    val assumeGrandCarrier get() = SkyHanniMod.feature

    //TODO render overlay, highlight items, highlight missing enchants once item is inserted. show how many grands are needed for a enchant from table.
}
