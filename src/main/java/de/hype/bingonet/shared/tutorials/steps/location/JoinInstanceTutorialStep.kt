package de.hype.bingonet.shared.tutorials.steps.location

import de.hype.bingonet.shared.constants.SkyblockInstance
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

/**
 * Pure data class for instance joining tutorial steps.
 * Step completes when the player joins the specified Skyblock instance.
 */
class JoinInstanceTutorialStep(
    val instance: SkyblockInstance,
) : TutorialStep()
