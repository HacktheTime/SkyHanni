package de.hype.bingonet.shared.tutorials.steps.misc

import at.hannibal2.skyhanni.data.effect.EffectApi
import at.hannibal2.skyhanni.utils.compat.EffectsCompat
import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import net.minecraft.potion.Potion
import kotlin.time.Duration

//TODO change it to support a specific type.
class AwaitGodSplashTutorialStep(val minimumDuration: Duration) : TutorialStep {

    override fun getStepDescription(): String? {
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
