package de.hype.bingonet.shared.tutorials.steps.itemstep

import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Pure data class for item tagging tutorial steps.
 * Step completes when an item is tagged with the specified tag name.
 */
class TagItemTutorialStep(
    val tagName: String,
    val explenation: String,
) : TutorialStep()
