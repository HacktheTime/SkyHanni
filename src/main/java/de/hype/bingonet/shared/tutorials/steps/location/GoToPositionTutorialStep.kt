package de.hype.bingonet.shared.tutorials.steps.location

import de.hype.bingonet.shared.objects.Position
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class GoToPositionTutorialStep(
    /**
     * If null use SkyHannis internal routing System
     */
    val node: List<Position>?,
    val allowSkip: Boolean
    ) : TutorialStep {
}
