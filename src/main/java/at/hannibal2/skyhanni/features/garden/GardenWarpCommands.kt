package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.features.garden.sensitivity.LockMouseLook
import at.hannibal2.skyhanni.features.misc.WarpAPI
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenWarpCommands {

    private val config get() = GardenApi.config.gardenCommands

    /**
     * REGEX-TEST: /tp 3
     * REGEX-TEST: /tp barn
     */
    private val tpPlotPattern by RepoPattern.pattern(
        "garden.warpcommand.tpplot",
        "/tp (?<plot>.*)",
    )


    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!config.warpCommands) return

        val message = event.message.lowercase()

        if (message == "/home") {
            event.cancel()
            WarpAPI.warp("garden")
            ChatUtils.chat("§aTeleported you to the spawn location!", prefix = false)
        }

        if (message == "/barn") {
            event.cancel()
            HypixelCommands.teleportToPlot("barn")
            LockMouseLook.unlockMouse()
        }

        tpPlotPattern.matchMatcher(event.message) {
            event.cancel()
            val plotName = group("plot")
            HypixelCommands.teleportToPlot(plotName)
            LockMouseLook.unlockMouse()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onKeyDown(event: KeyDownEvent) {
        if (Minecraft.getInstance().screen != null) return

        when (event.keyCode) {
            config.homeHotkey -> {
                WarpAPI.warpOffCooldown("garden")
            }

            config.sethomeHotkey -> {
                HypixelCommands.setHome()
            }

            config.barnHotkey -> {
                if (!WarpAPI.offCooldown) return
                WarpAPI.setWarpCooldown()
                LockMouseLook.unlockMouse()
                HypixelCommands.teleportToPlot("barn")
            }

            else -> return
        }
    }
}
