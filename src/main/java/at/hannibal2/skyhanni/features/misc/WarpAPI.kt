package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object WarpAPI {
    @Suppress("DEPRECATION")
    fun warp(warp: String) {
        HypixelCommands.warp(warp)
    }

    val offCooldown: Boolean get() = lastWarp.passedSince() > 3.seconds
    var lastWarp = SimpleTimeMark.farPast()

    /**
     * Warps when off Cooldown.
     */
    fun warpOffCooldown(warp: String) {
        val passedSince = lastWarp.passedSince()
        if (passedSince < 3.seconds) {
            SkyHanniMod.launchCoroutine("WarpAPI.warpOffCooldown: $warp") {
                delay(3.seconds - passedSince)
                warp(warp)
            }
        } else {
            warp(warp)
        }

    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        lastWarp = SimpleTimeMark.now()
    }

    fun setWarpCooldown() {
        lastWarp = SimpleTimeMark.now()
    }
}
