package de.hype.bingonet.shared.tutorials.steps.misc

import at.hannibal2.skyhanni.data.effect.EffectApi
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import kotlin.time.Duration

//TODO change it to support a specific type.
class AwaitGodSplashTutorialStep(val minimumDuration: Duration) : TutorialStep {

    override fun getStepDescription(tutorial: Tutorial): String? {
        return null
    }

    override fun getStepName(): String {
        return "Get a God Splash."
    }

    override fun onActivate() {
        check()
    }

    fun check() {
        if (EffectApi.getGodSplashDuration() >= minimumDuration) complete()
    }
}
