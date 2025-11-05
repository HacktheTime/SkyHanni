package de.hype.bingonet.shared.tutorials.steps.misc

import at.hannibal2.skyhanni.data.effect.EffectApi
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import kotlin.time.Duration

//TODO change it to support a specific type.
class AwaitGodSplashTutorialStep(val minimumDuration: Duration) : TutorialStep() {

    override fun getStepDescription(tutorial: Tutorial): String? {
        return null
    }

    override fun getStepName(tutorial: Tutorial): String {
        return "Get a God Splash."
    }

    override fun getRequirements(): List<TutorialNode> = emptyList()

    override fun onActivate(tutorial: Tutorial) {
        check(tutorial)
    }

    override fun isComplete(tutorial: Tutorial): Boolean {
        return check(tutorial)
    }

    override fun check(@Suppress("UNUSED_PARAMETER") tutorial: Tutorial): Boolean {
        if (EffectApi.getGodSplashDuration() < minimumDuration) return false
        complete()
        return true
    }
}
