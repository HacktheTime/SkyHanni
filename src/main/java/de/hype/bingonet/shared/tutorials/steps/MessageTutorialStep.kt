package de.hype.bingonet.shared.tutorials.steps

import java.util.regex.Pattern

/**
 * Pure data class for message-based tutorial steps.
 * Step completes when a chat message matching the criteria pattern is received.
 */
class MessageTutorialStep(
    val criteria: Pattern,
    val name: String,
    val description: String,
) : TutorialStep()
