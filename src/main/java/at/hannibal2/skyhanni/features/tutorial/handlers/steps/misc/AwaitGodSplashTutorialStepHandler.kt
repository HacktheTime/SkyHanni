package at.hannibal2.skyhanni.features.tutorial.handlers.steps.misc

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.misc.AwaitGodSplashTutorialStep

class AwaitGodSplashTutorialStepHandler : TutorialStepHandler<AwaitGodSplashTutorialStep> {
    override fun getStepName(step: AwaitGodSplashTutorialStep, tutorial: Tutorial): String = "AwaitGodSplash step"
    override fun getStepDescription(step: AwaitGodSplashTutorialStep, tutorial: Tutorial): String? = null
    override fun getRequirements(step: AwaitGodSplashTutorialStep): List<TutorialNode> = emptyList()
    
    companion object {
        private val activeSteps = mutableSetOf<AwaitGodSplashTutorialStep>()
    }
}
