package de.hype.bingonet.shared.tutorials.steps

/**
 * Pure data class for text tutorial steps.
 * Contains only data properties - no methods or logic.
 */
class TextTutorialStep(
    val name: String,
    val description: String,
) : TutorialStep()
