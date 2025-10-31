package de.hype.bingonet.shared.tutorials.steps

import java.util.regex.Pattern

/**
 * Pure data class for GUI-based tutorial steps.
 * Base class for steps that require specific GUI to be open.
 */
abstract class GUIBasedTutorialStep(
    val guiName: Pattern,
) : TutorialStep()
