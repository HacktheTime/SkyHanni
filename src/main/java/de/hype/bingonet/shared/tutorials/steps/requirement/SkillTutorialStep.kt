package de.hype.bingonet.shared.tutorials.steps.requirement

import de.hype.bingonet.shared.constants.Skills
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Pure data class for skill level requirement steps.
 * Step completes when the specified skill reaches the target level.
 */
class SkillTutorialStep(
    val skill: Skills,
    val level: Int,
) : TutorialStep()
