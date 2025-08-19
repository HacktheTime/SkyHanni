package de.hype.bingonet.shared.tutorials

import at.hannibal2.skyhanni.utils.ChatUtils
import de.hype.bingonet.shared.tutorials.paths.SelectPathTutorialFork
import de.hype.bingonet.shared.tutorials.paths.TutorialFork
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class Tutorial(
    val steps: List<TutorialNode>,
    val name: String,
    val description: String,
    val profileDependend: Boolean = false,
) {
    //TODO add support for changing config values 1 by 1. Could be useful for bingo to make it so a user gets some sh options configured automatically for this Bingo BUT.
    // THIS HAS TO BE THOUGHT THROUGH EXTREMELY TO AVOID SECURITY ISSUES
    // Should also have support for undoing those changes by storing previous config.

    //TODO the node ids (unique!) have to be populated still
    private val selectedPathIds = mutableSetOf<String>()


    fun reset() {
        steps.forEach { it.reset() }
        selectedPathIds.clear()
    }

    fun getSelectedOption(options: List<SelectPathTutorialFork.SelectedPathTutorialFork>): SelectPathTutorialFork.SelectedPathTutorialFork? {
        options.forEach {
            if (selectedPathIds.contains(it.option.id)) {
                return it
            }
        }
        if (options.size == 1) return null
        ChatUtils.chat(
            "§e[SkyHanni Tutorial]: The Currently loaded Tutorial has ${options.size} options for you to choose from on how to continue",
            prefix = false,
        )
        options.forEach {
            ChatUtils.clickableChat(
                "§e[SkyHanni Tutorial]: ${it.option.name} : ${it.option.guideMakerDescription}",
                {
                    addSelected(it.option)
                },
            )
        }
    }

    private fun addSelected(option: SelectPathTutorialFork.SelectPathTutorialOption) {
        selectedPathIds.add(option.id)
    }

    fun getNodeReference(nodeId: String): TutorialNode {

    }

    private val allStepsFlatMap: List<TutorialStep> get() {
        steps.map {
            if (it is TutorialFork) {
                return@map it.getNodes(this)
            }
            return@map listOf(it as TutorialStep)
        }
    }
    fun getActiveSteps(): List<TutorialStep> {
       return allStepsFlatMap.filter { it.isActive }
    }

    fun onProfileJoin(){
        allStepsFlatMap.forEach { it.refresh() }
    }
}
