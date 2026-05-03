@file:Suppress("NoUnusedImports")

package de.hype.bingonet

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.features.event.bingo.BBSplashMessageConfigureScreen
import at.hannibal2.skyhanni.config.features.event.bingo.BingoNetSystem
import at.hannibal2.skyhanni.config.features.inventory.hubselector.HubSelectorKeybinds
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.toBNIsland
import at.hannibal2.skyhanni.features.bingo.bingonet.BNRegistrationScreen
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import de.hype.bingonet.BNConnection.reconnectToBNServer
import de.hype.bingonet.environment.packetconfig.InterceptPacketInfo
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.constants.StatusConstants
import de.hype.bingonet.shared.objects.BNRole
import de.hype.bingonet.shared.objects.SplashData
import de.hype.bingonet.shared.objects.SplashLocation
import de.hype.bingonet.shared.objects.SplashLocations
import de.hype.bingonet.shared.packets.function.SplashNotifyPacket
import de.hype.bingonet.shared.packets.function.SplashTimeRequestPacket
import de.hype.bingonet.shared.packets.network.BingoChatMessagePacket
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
@Suppress("LongMethod", "NoUnusedImports")
object BNCommands {
    val config get() =  SkyHanniMod.feature.event.bingo.bingoNetworks
    val bnConfig get() =  SkyHanniMod.feature.event.bingo.bingoNetworks.bingoNet

    @HandleEvent
    fun registerCommands(event: CommandRegistrationEvent) {
        event.registerBrigadier("bnreconnectserver") {
            description = "(Re)Starts the Connection to the Bingo Net Server."
            category = CommandCategory.BINGO_NET
            arg(
                "server",
                BrigadierArguments.string(),
                BingoNetSystem.entries.map { it.name } + "disconnect",
            ) {
                callback {
                    connectServerCommand(getArg(it))
                }
            }
            simpleCallback {
                connectServerCommand(null)
            }
            literalCallback("state"){
                if (BNConnection.isConnected){
                    val system = BNConnection.connectedSystem
                    ChatUtils.chat("§aConnected§r to Bingo Net Server §e${system?.name ?: "Unknown"}§r.")
                } else {
                    ChatUtils.chat("§cDisconnected§r from Bingo Net.")
                }
            }
        }

        event.registerBrigadier("bc") {
            description = "Send a Message to Bingo Net Chat."
            category = CommandCategory.BINGO_NET
            argCallback(
                "message",
                BrigadierArguments.greedyString(),
            ) {
                BNConnection.sendPacket(BingoChatMessagePacket(null, "", it, 0))
            }
        }

        if (BNConnection.roles.contains(BNRole.SPLASHER)) {
            event.registerBrigadier("bnsplash") {
                description = "Announce a Splash (Announces a Splash for the Lobby your currently in)."
                category = CommandCategory.BINGO_NET
                arg("location", BrigadierArguments.string(), SplashLocations.values().map { it.getCommandArgNames() }) { loc ->
                    arg("extraMessage", BrigadierArguments.greedyString()) { extra ->
                        callback {
                            val location = SplashLocations.values().find { it.getName() == getArg(loc) }
                            val message = getArg(extra)
                            if (location == null) {
                                ChatUtils.userError("Invalid splash location provided: ${getArg(loc)}. Only use a suggested location!")
                                return@callback
                            }
                            sendSplash(location, message, false)
                        }
                    }
                    callback {
                        val location = SplashLocations.values().find { it.getName() == getArg(loc) }
                        if (location == null) {
                            ChatUtils.userError("Invalid splash location provided: ${getArg(loc)}. Only use a suggested location!")
                            return@callback
                        }
                        sendSplash(location, null, false)
                    }
                }
            }
            event.registerBrigadier("bnsplashdynamic") {
                description = "Announce a Dynamic Hub Splash"
                category = CommandCategory.BINGO_NET
                arg("location", BrigadierArguments.string(), SplashLocations.values().map { it.getName() }) { loc ->
                    arg("extraMessage", BrigadierArguments.greedyString()) { extra ->
                        callback {
                            val location = SplashLocations.values().find { it.getName() == getArg(loc) }
                            val message = getArg(extra)
                            if (location == null) {
                                ChatUtils.userError("Invalid splash location provided: ${getArg(loc)}. Only use a suggested location!")
                                return@callback
                            }
                            sendSplash(location, message, true)
                        }
                    }
                    callback {
                        val location = SplashLocations.values().find { it.getName() == getArg(loc) }
                        if (location == null) {
                            ChatUtils.userError("Invalid splash location provided: ${getArg(loc)}. Only use a suggested location!")
                            return@callback
                        }
                        sendSplash(location, null, true)
                    }
                }
            }
            event.registerBrigadier("bnrequestpottimes") {
                category = CommandCategory.BINGO_NET
                description = "For Splashers: Shows a summary of the current remaining God Pot Durations of all mod users."
                simpleCallback {
                    val packet = SplashTimeRequestPacket()
                    BNConnection.sendPacket(packet)
                    ChatUtils.chat("The Request has been send. The Server will send you a Response with the report shortly.")
                }
            }
        }

        event.registerBrigadier(
            "bnregistryscreen",
            {
                category = CommandCategory.DEVELOPER_DEBUG
                description = "Opens the Bingo Net Registration Screen"
                simpleCallback {
                    BNRegistrationScreen.openHelper()
                }
            },
        )

        event.registerBrigadier(
            "bndebugbreakpoint",
            {
                category = CommandCategory.DEVELOPER_DEBUG
                description = "Opens the Bingo Net Registration Screen"
                simpleCallback {
                    SkyHanniMod.launchCoroutine("BN Debug Breakpoint") {
                        debugBreakpoint()
                    }
                }
            },
        )
    }

    fun connectServerCommand(arg: String? = null) {
        val system = BingoNetSystem.entries.find { it.name == arg }
        if (system == null) {
            BNConnection.disconnect()
            return
        }
        SkyHanniMod.launchCoroutine("BN Connect to $system") {
            BNConnection.reconnectToBNServer(false, system)
        }
    }

    fun sendSplash(location: SplashLocation, extraMessage: String? = null, dynamic: Boolean) {
        val serverId = HypixelData.serverId ?: return
        var hubData =
            HubSelectorKeybinds.getHubNumberById(serverId, HypixelData.skyBlockIsland.toBNIsland() ?: error("Not on a Skyblock Island!"))
        if (dynamic) {
            hubData = null
        } else if (hubData == null) {
            ChatUtils.userError("Server ID $serverId not found in Cache or Hub Selector Data outdated. Please open the Hub Selector once.")
            return
        }
        val dualAnnounce =
            hubData != null && (config.splasherConfig.splashAnnounceMessageToClipboard || config.splasherConfig.openSplashChannelInBrowser)
        if (dualAnnounce) {
            if (!location.getName().equals("Bea") && hubData.hubType == Islands.HUB) {
                ChatUtils.userError("BB always splashes at Bea. Due to this you must splash at Bea if Dual announce is enabled")
                return
            }
        }

        val splashData = SplashData(
            announcer = "",
            locationInHub = location,
            extraMessage = extraMessage,
            lessWaste = config.splasherConfig.lessWaste,
            serverID = serverId,
            hubSelectorData = hubData,
            status = StatusConstants.WAITING,
        )
        if (dualAnnounce) {
            val interceptor = object : InterceptPacketInfo<SplashNotifyPacket>(
                clazz = SplashNotifyPacket::class.java,
                cancelPacket = false,
                blockIntercepts = false,
                ignoreIfIntercepted = false,
                blockExecutionForCompletion = true,
            ) {
                override fun run(packet: SplashNotifyPacket) {
                    val username = MinecraftCompat.localPlayer.name.string
                    val hype = username.equals("Hype_the_Time")
                    val mention = if (!hype) {
                        "<@&916461777863180328>" // BB-Splash-Ping
                    } else {
                        "€here"
                    }
                    val message: String = config.splasherConfig.bbSplashMessage
                        .replace(BBSplashMessageConfigureScreen.SERVER_ID, serverId)
                        .replace(BBSplashMessageConfigureScreen.EXTRA_MESSAGE, extraMessage ?: "")
                        .replace(BBSplashMessageConfigureScreen.SPLASHER, username)
                        .replace(
                            BBSplashMessageConfigureScreen.HUB,
                            if (hubData.hubType != Islands.HUB) {
                                "**${hubData.hubType.name}** #${hubData.hubNumber} at ${location.displayString}"
                            } else {
                                "Hub #${hubData.hubNumber}"
                            },
                        ).replace(BBSplashMessageConfigureScreen.ROLE_MENTIONS, mention)
                    if (message.isEmpty()) {
                        ChatUtils.userError("BB Splash Message is empty or invalid. Please configure it first.")
                        return
                    }
                    //We are waiting for info whether the splash is funded or not since terms of the funding.
                    if (packet.splash.funder != null || packet.splash.lessWaste) {
                        SkyHanniMod.launchCoroutine("Announce Splash to BB") {
                            ChatUtils.chat("Waiting 25 seconds due too BN Funding or Lesswaste being active...")
                            delay(25.seconds) // You have to wait 30 BUT it will take some time to post anyway.
                            val space = HypixelData.getMaxPlayersForCurrentServer()
                            if (space < 20) {
                                ChatUtils.chat("Announcing in BB not recommended due to little space left. ($space)")
                                return@launchCoroutine
                            }
                            if (config.splasherConfig.openSplashChannelInBrowser) OSUtils.openBrowser(
                                "https://discord" +
                                    ".com/channels/1448345970784993301/916074669973594123",
                            )
                            if (config.splasherConfig.splashAnnounceMessageToClipboard) OSUtils.copyToClipboard(message)
                        }
                    } else {
                        if (config.splasherConfig.openSplashChannelInBrowser) OSUtils.openBrowser(
                            "https://discord" +
                                ".com/channels/1448345970784993301/916074669973594123",
                        )
                        if (config.splasherConfig.splashAnnounceMessageToClipboard) OSUtils.copyToClipboard(message)
                    }
                }
            }
            BNConnection.packetIntercepts.add(interceptor)
        }
        BNConnection.sendPacket(SplashNotifyPacket(splashData))
    }

    suspend fun debugBreakpoint() {
        ChatUtils.chat("Debug Breakpoint reached.")
    }

}
