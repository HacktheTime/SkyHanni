package de.hype.bingonet.shared.tutorials.steps.guisteps

import de.hype.bingonet.shared.tutorials.steps.GUIBasedTutorialStep
import java.util.regex.Pattern

/**
 * Pure data class for GUI click slot tutorial steps.
 * Step completes when the specified slot in the GUI is clicked.
 */
class GuiClickSlotTutorialStep(
    guiName: Pattern,
    val slotIndex: Int,
    val description: String? = null,
) : GUIBasedTutorialStep(guiName)
