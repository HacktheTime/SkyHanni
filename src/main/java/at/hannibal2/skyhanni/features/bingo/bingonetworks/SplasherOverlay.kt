package at.hannibal2.skyhanni.features.bingo.bingonetworks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.mob.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.bingo.bingonet.SplashManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.isOnBingo
import at.hannibal2.skyhanni.utils.EntityUtils.isOnIronman
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.constants.StatusConstants
import net.minecraft.world.entity.player.Player


@SkyHanniModule
object SplasherOverlay {

    private const val SCALE = 0.5714286f

    private val config get() = SkyHanniMod.feature.event.bingo.bingoNetworks.splasherConfig
    private var renderables: List<Renderable>? = null

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val renderables = renderables ?: return
        config.splasherOverlayPosition.renderRenderables(renderables, posLabel = "Splasher Overlay")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun refreshData(event: SecondPassedEvent) {
        if (!isEnabled()) {
            this.renderables = null
            return
        }
        val splash = SplashManager.getSplashInServer(true)
        if (splash == null) {
            this.renderables = null
            return
        }

        val renderables = mutableListOf<Renderable>()
        @Suppress("USELESS_ELVIS") //may be null while its local only with no update packet yet.
        val status = splash.status?: StatusConstants.WAITING
        renderables.add(Renderable.text("Status: ${status.displayName}"))
        val players =
            EntityUtils.getEntitiesNearby<Player>(
                5.0,
                predicate = {
                    it.isRealPlayer() && !it.isLocalPlayer
                },
            )
        renderables.add(Renderable.text("Hub: ${players.size}/${HypixelData.getMaxPlayersForCurrentServer()}"))
        val bingos = players.filter { it.isOnBingo() }
        val iman = players.filter { it.isOnIronman() }
        val leechers = players.filter { !(it.isOnBingo() || it.isOnIronman()) }
        renderables.add(
            Renderable.text("Participants: §6Ⓑ: ${bingos.size} §r| §8♲: ${iman.size} §r| §7N: ${leechers.size} §r| Total: ${players.size}"),
        )
        if (leechers.isNotEmpty()) {
            renderables.add(Renderable.text("Leechers:"))
            leechers.forEach {
                renderables.add(Renderable.text(it.displayName.string))
            }
        }
        this.renderables = renderables
    }

    private fun isEnabled() = config.useSplasherOverlay
}
