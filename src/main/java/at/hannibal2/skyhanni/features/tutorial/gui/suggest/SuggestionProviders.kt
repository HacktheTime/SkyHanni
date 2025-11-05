package at.hannibal2.skyhanni.features.tutorial.gui.suggest

import at.hannibal2.skyhanni.data.ProfileStorageData
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.steps.MessageTutorialStep
import de.hype.bingonet.shared.tutorials.steps.TextTutorialStep
import de.hype.bingonet.shared.tutorials.steps.guisteps.GuiItemTutorialStep
import de.hype.bingonet.shared.tutorials.steps.itemstep.EnchantTutorialStep

object SuggestionProviders {
    fun filterContains(options: List<String>, query: String): List<String> {
        if (query.isBlank()) return options.take(10)
        val q = query.lowercase()
        return options.filter { it.lowercase().contains(q) }.take(10)
    }

    fun titlesFromTutorial(tutorial: Tutorial): List<String> {
        return tutorial.steps.flatMap { node ->
            when (node) {
                is TextTutorialStep -> listOf(node.getStepName(tutorial))
                is MessageTutorialStep -> listOf(node.name)
                else -> emptyList()
            }
        }
    }

    fun descriptionsFromTutorial(tutorial: Tutorial): List<String> {
        return tutorial.steps.flatMap { node ->
            when (node) {
                is TextTutorialStep -> listOfNotNull(node.description)
                is MessageTutorialStep -> listOfNotNull(node.description)
                else -> emptyList()
            }
        }
    }

    fun regexPatternsFromTutorial(tutorial: Tutorial): List<String> {
        return tutorial.steps.flatMap { node ->
            when (node) {
                is MessageTutorialStep -> listOf(node.criteria.pattern())
                else -> emptyList()
            }
        }
    }

    fun islands(): List<String> = Islands.entries.map { it.getDisplayName() }

    fun tagNames(): List<String> = ProfileStorageData.profileSpecific?.itemTags?.keys?.sorted().orEmpty()

    fun tagNamesFromSteps(tutorial: Tutorial): List<String> {
        return tutorial.steps.flatMap { node ->
            when (node) {
                is de.hype.bingonet.shared.tutorials.steps.itemstep.TagItemTutorialStep -> listOf(node.tagName)
                else -> emptyList()
            }
        }
    }

    fun enchantIdsFromTutorial(tutorial: Tutorial): List<String> {
        // try to find any EnchantTutorialStep and return their keys
        val ids = mutableSetOf<String>()
        tutorial.steps.forEach { node ->
            if (node is EnchantTutorialStep) ids.addAll(node.enchantIds.keys)
            if (node is GuiItemTutorialStep) {
                // nothing specific
            }
        }
        return ids.toList().sorted()
    }

    fun reforges(): List<String> = emptyList()
}
