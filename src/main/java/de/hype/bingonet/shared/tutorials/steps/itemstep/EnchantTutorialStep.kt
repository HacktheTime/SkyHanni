package de.hype.bingonet.shared.tutorials.steps.itemstep

import de.hype.bingonet.shared.tutorials.TaggedItemCheck
import de.hype.bingonet.shared.tutorials.steps.GUIBasedTutorialStep
import java.util.regex.Pattern

/**
 * Pure data class for enchanting tutorial steps.
 * Step completes when the specified item is enchanted with the target enchants.
 * 
 * @param item The tagged item to enchant
 * @param enchantIds Map of enchant internal names to levels.
 *                   Positive for minimum level. 0 or negative for exact absolute level.
 */
class EnchantTutorialStep(
    val item: TaggedItemCheck,
    val enchantIds: Map<String, Int>,
) : GUIBasedTutorialStep(Pattern.compile("Enchant Items"))
