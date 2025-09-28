package de.hype.bingonet.shared.tutorials.steps.misc

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class DailyEnchantingXpTutorialStep : TutorialStep() {

    override fun getStepName(tutorial: Tutorial): String {
        return "Get 500k from the Enchanting XP."
    }

    override fun getStepDescription(tutorial: Tutorial): String {
        return "Use a carrier to splash you grands as fast as you pick up the orbs and press q above the enchant with the highest xp needed to (dis)enchant that you can do with 1 grand. For example enderman Slayer / First Strike 4 (NOT 5)"
    }

    override fun getRequirements(): List<TutorialNode> = emptyList()

    override fun isComplete(tutorial: Tutorial): Boolean {
        TODO("Not yet implemented")
    }
}
