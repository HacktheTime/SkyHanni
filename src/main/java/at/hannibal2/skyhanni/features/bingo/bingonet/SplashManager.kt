package at.hannibal2.skyhanni.features.bingo.bingonet

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.PartyApi.joinParty
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.misc.WarpAPI
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import de.hype.bingonet.BNConnection
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.objects.BNRole
import de.hype.bingonet.shared.objects.SplashData
import de.hype.bingonet.shared.packets.function.RequestDynamicSplashInvitePacket
import de.hype.bingonet.shared.packets.function.SplashUpdatePacket
import de.hype.bingonet.toLorenz
import kotlinx.coroutines.delay
import java.awt.Color
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// Not needed since the SH Message Event is asked for by the Player. Not needed to be a module.
@SkyHanniModule
object SplashManager {
    var splashPool: MutableMap<Int, DisplaySplash> = HashMap<Int, DisplaySplash>()
    val config get() = SkyHanniMod.feature.event.bingo.bingoNetworks

    fun addSplash(splash: SplashData, source: SplashSource): Boolean {
        if (source == SplashSource.BB) {
            if (splashPool.values.any { it.funder == splash.funder && it.hubSelectorData?.hubNumber == splash.hubSelectorData?.hubNumber }) {
                return true //This avoids duplicate splash announcement messages when dual sending and BN Data is better.
            }
        }

        val existed = splashPool.containsKey(splash.splashId)
        splashPool[splash.splashId] = DisplaySplash(splash)
        DelayedRun.runDelayed(
            5.minutes,
            {
                splashPool.remove(splash.splashId)
            },
        )
        return existed
    }

    fun addSplashAndDisplay(splash: SplashData, source: SplashSource) {
        if (!addSplash(splash, source)) display(splash.splashId, source)
    }

    fun updateSplash(packet: SplashUpdatePacket) {
        val splash = splashPool.get(packet.splashId)
        if (splash != null) {
            splash.status = packet.status
            if (splash.alreadyDisplayed) {
                if (SkyHanniMod.feature.event.bingo.bingoNetworks.showSplashStatusUpdates && splash.hubSelectorData != null) {
                    ChatUtils.chat("§6The Splash in ${splash.hubSelectorData.hubType.getDisplayName()} §a#${splash.hubSelectorData?.hubNumber ?: splash.serverID}§6 is now §d${packet.status.displayName}§r.")
                }
            }
        }
    }

    fun getSplashInServer(mustBeFromSelf: Boolean, serverId: String? = HypixelData.serverId): DisplaySplash? {
        if (serverId == null) return null
        return splashPool.values.filter { it.serverID == serverId }
            .filter { !mustBeFromSelf || it.announcer.equals(PlayerUtils.getName(), ignoreCase = true) || config.splasherConfig
                .altAccounts.lowercase().split(",").contains(it.announcer.lowercase()) }.minByOrNull { it.receivedTime }
    }

    enum class SplashSource {
        BN,
        BB,
        BSC,
        DISCORD_OS,
    }

    fun display(splashId: Int, source: SplashSource) {
        val splash = splashPool.get(splashId)
        if (splash == null) return
        if (source == SplashSource.DISCORD_OS) ChatUtils.chat("§cPrefilled the following Splash Data via Discord Notification:")
        if (splash.hubSelectorData == null) {
            ChatUtils.chatPrompt(
                "§d${splash.announcer} is Splashing in a §4PRIVATE§r Lobby.",
                SkyHanniMod.feature.event.bingo.bingoNetworks.splashHubWarp,
                {
                    joinParty(splash, source)
                },
            )
        } else {
            var islandType: String
            if (splash.hubSelectorData.hubType == Islands.DUNGEON_HUB) {
                islandType = "§dDUNGEON HUB§f"
            } else {
                islandType = "Hub"
            }

            ChatUtils.chatPrompt(
                "§d${splash.announcer}§r is Splashing in $islandType #${splash.hubSelectorData.hubNumber}§r${
                    splash.locationInHub?.displayString.let {
                        if (it != null) " at $it " else " "
                    }
                }(§aPress %KEY% to warp to a §d${splash.hubSelectorData.hubType}§r) " +
                    "§7| §6${splash.extraMessage ?: ""}",
                SkyHanniMod.feature.event.bingo.bingoNetworks.splashHubWarp,
                {
                    SkyHanniMod.launchCoroutine("Splash Hub Warp Helper") {
                        prepareHubWarp(splash, source)
                    }
                },
            )
        }
    }

    class DisplaySplash(packet: SplashData) : SplashData(packet) {
        var alreadyDisplayed: Boolean = false
        var receivedTime: Instant = Instant.now()
    }

    private fun prepareHubWarp(splash: SplashData, source: SplashSource) {
        if (splash.hubSelectorData == null) {
            joinParty(splash, source)
        } else {
            val currentIsland = HypixelData.skyBlockIsland
            if (splash.hubSelectorData.hubType == Islands.HUB) {
                if (BNConnection.roles.contains(BNRole.DEBUG)) ChatUtils.chat("§e[Debug] Current Island: $currentIsland")
                if (
                    currentIsland == IslandType.DUNGEON_HUB ||
                    currentIsland == IslandType.THE_FARMING_ISLANDS ||
                    currentIsland == IslandType.THE_PARK ||
                    currentIsland == IslandType.SPIDER_DEN ||
                    currentIsland == IslandType.GOLD_MINES
                ) {
                    // Double warp needed
                    SkyHanniMod.launchCoroutine("Hub double warp", 2.seconds) {
                        while (HypixelData.skyBlockIsland != IslandType.HUB) {
                            delay(250)
                        }
                        WarpAPI.warp(Islands.HUB.warpArgument!!)
                    }
                }
                WarpAPI.warp(Islands.HUB.warpArgument!!)
            } else {
                WarpAPI.warp(splash.hubSelectorData.hubType.warpArgument ?: error("Illegal Island Type."))
            }
        }
    }

    private var awaitingPartyInvite: String? = null
    private fun joinParty(splash: SplashData, source: SplashSource) {
        if (source == SplashSource.BN) {
            BNConnection.sendPacket(RequestDynamicSplashInvitePacket(splash.splashId))
            ChatUtils.chat("Party Request sent.")
            awaitingPartyInvite = splash.announcer
        } else if (source == SplashSource.BB) {
            val partyHost = splash.partyhost?:splash.announcer
            PartyApi.leaveParty()
            joinParty(partyHost)
        } else if (source == SplashSource.DISCORD_OS) {
            val partyHost = splash.partyhost
            if (partyHost != null) joinParty(partyHost)
            else ChatUtils.userError("This Discord-detected splash party host could not be parsed.")
        }
    }

    val SplashData.partyhost: String?
        get() {
            // extramessage should contain /p join {partyHost}
            val extraMessage = this.extraMessage ?: return null
            return "/p join (?<name>\\w+)".toPattern().matchMatcher(extraMessage) { group("name") }
        }

    @HandleEvent
    fun handlePartyInvite(message: SkyHanniChatEvent.Allow) {
        val awaitingPartyInvite = awaitingPartyInvite ?: return
        PartyApi.receivedInvitePattern.matchMatcher(message.cleanMessage) {
            val name = group("name")
            if (name == awaitingPartyInvite) {
                PartyApi.leaveParty()
                PartyApi.acceptParty(name)
            }
        }
    }

    private var waypointPos: LorenzVec? = null
    private var data: DisplaySplash? = null

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onIslandChange(event: IslandJoinEvent) {
        val data = getSplashInServer(false)
        val waypointPos = data?.locationInHub?.coords?.toLorenz()
        this.waypointPos = waypointPos
        this.data = data
        if (data != null && config.renderSplashLocationWaypoint && waypointPos != null) {
            val location = data.locationInHub
            IslandGraphs.pathFind(
                waypointPos,
                location.displayString,
                condition = { true },
            )
        }
    }

    @HandleEvent
    fun worldRender(event: SkyHanniRenderWorldEvent) {
        val waypoint = waypointPos ?: return
        event.drawWaypointFilled(waypoint, Color.YELLOW, true)
        event.drawString(waypoint, "§6Splash Location", true, Color.WHITE)
    }
}
