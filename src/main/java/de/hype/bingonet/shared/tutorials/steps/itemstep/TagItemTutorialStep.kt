package de.hype.bingonet.shared.tutorials.steps.itemstep

import at.hannibal2.skyhanni.features.inventory.storage.ItemTagManager
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class TagItemTutorialStep(val tagName: String, val explenation: String ) : TutorialStep {
    override fun getStepName(): String {
        return "Tag Item with $tagName"
    }

    override fun getStepDescription(): String {
        return "${explenation}\nRun /shtagitem $tagName"
    }

    override fun onActivate() {
        if (ItemTagManager.hasTag(tagName) ?: false) {
            chatPromptSuggestion("You have a Item tagged with $tagName already. Use the Keybind or redo /shtagitem $tagName with a new item."){
                complete()
            }
        }
    }

    //This Events gets completed by a hook into the Tag Manager.
}
