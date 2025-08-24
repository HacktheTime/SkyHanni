package at.hannibal2.skyhanni.events.tutorials

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class TutorialStepCompleteEvent(val step: TutorialStep): SkyHanniEvent() {
}
