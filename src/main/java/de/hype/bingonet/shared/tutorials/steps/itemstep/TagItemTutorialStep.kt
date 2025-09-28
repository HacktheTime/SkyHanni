package de.hype.bingonet.shared.tutorials.steps.itemstep

import at.hannibal2.skyhanni.features.inventory.storage.ItemTagManager
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class TagItemTutorialStep(val tagName: String, val explenation: String) : TutorialStep() {
    override fun getStepName(tutorial: Tutorial): String {
        return "Tag Item with $tagName"
    }

    override fun getStepDescription(tutorial: Tutorial): String {
        return "${explenation}\nRun /shtagitem $tagName"
    }

    override fun getRequirements(): List<TutorialNode> = emptyList()


    //This Events gets completed by a hook into the Tag Manager.

    override fun onActivate(tutorial: Tutorial) {
        if (ItemTagManager.hasTag(tagName)) {
            chatPromptSuggestion("You have a Item tagged with $tagName already. Use the Keybind or redo /shtagitem $tagName with a new item.") {
                complete()
            }
        }
    }
}
