package de.hype.bingonet.shared.tutorials.steps.guisteps

import de.hype.bingonet.shared.tutorials.steps.GUIBasedTutorialStep
import java.util.regex.Pattern

/**
 * Pure data class for GUI item inspection tutorial steps.
 * Step completes when the item in the specified slot has/doesn't have certain lore.
 */
class GuiItemTutorialStep(
    guiName: Pattern,
    val itemIndex: Int,
    val has: Regex?,
    val doesntHave: Regex?,
    val description: String? = null,
) : GUIBasedTutorialStep(guiName)
