package at.hannibal2.skyhanni.features.bingo.bingobrewers

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.features.bingo.bingobrewers.BingoBrewersClient.isEnabled
import at.hannibal2.skyhanni.features.bingo.bingonet.SplashManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.constants.StatusConstants
import de.hype.bingonet.shared.objects.SplashData
import de.hype.bingonet.shared.objects.SplashLocations
import java.io.IOException
import java.net.Socket
import java.util.regex.Pattern

@SkyHanniModule
object BSCClient {
    val config = SkyHanniMod.feature.event.bingo.bingoNetworks
    val enabled get() = config.useBSC
    var client: Socket? = null
    lateinit var thread: Thread

    @HandleEvent
    fun event(event: ConfigLoadEvent) {
        init()
    }

    init {
        init()
    }

    @Synchronized
    fun init() {
        if (enabled) {
            SkyHanniMod.launchCoroutine("Init BSC Client") {
                if (client?.isConnected != true) connect()
            }
        } else {
            stop()
        }
    }


    @Throws(IOException::class)
    @Synchronized
    private fun connect() {
        client?.close()
        if (!isEnabled()){
            ChatUtils.chatAndOpenConfig("Bingo Splash Community is not enabled right now. Please enable it first,", SkyHanniMod.feature
                .event.bingo
                .bingoNetworks::useBSC)
            return
        }
        val client = Socket("bsn.morazzer.dev", 1807)
        this.client = client
        thread = Thread {
            while (!thread.isInterrupted && client.isConnected) {
                val inputStream = client.getInputStream()
                val buffer = ByteArray(1024)
                val read = inputStream.read(buffer)
                if (read == -1) {
                    ChatUtils.clickableChat(
                        "Error trying to connect to BSC Server",
                        onClick = {
                            connect()
                        },
                    )
                    BSCClient.client?.close()
                }
                val id = buffer[0]
                if (id == 0.toByte()) {
                    //Ignore
                } else if (id == 1.toByte()) {
                    ChatUtils.chat("§aConnected to Bingo Splash Community Splash Announcement Server!")
                    val message = String(buffer, 1, read - 1).let {
                        it.replace("<@&[0-9]+>".toRegex(), "")
                    }
                    val serverId = Pattern.compile("((mini|mega)[0-9]+[a-z])", Pattern.CASE_INSENSITIVE).matcher(message).let {
                        it.find()
                        it.group(1)
                    }
                    val hubNumber = Pattern.compile("(hub ([0-9]{1,2}))", Pattern.CASE_INSENSITIVE).matcher(message).let {
                        it.find()
                        it.group(2)?.toIntOrNull()
                    }
                    val island = if (message.contains("dhub", true) || message.contains("dungeon")) Islands.DUNGEON_HUB else Islands.HUB
                    val hubSelectorData = if (hubNumber != null) SplashData.HubSelectorData(hubNumber, island) else null
                    if (hubSelectorData == null) continue
                    val sploosh = SplashData(
                        announcer = "BSC",
                        locationInHub = SplashLocations.KAT,
                        extraMessage = message,
                        lessWaste = false,
                        serverID = serverId,
                        hubSelectorData = hubSelectorData,
                        status = StatusConstants.WAITING,
                        funder = null,
                    )
                    SplashManager.addSplash(sploosh, SplashManager.SplashSource.BSC)
                } else if (id == 2.toByte()) {
                    ChatUtils.chat("§cConnection limit on BSC server reached. Try again later with /bsc reconnect")
                    BSCClient.client?.close()
                    return@Thread
                }
            }
        }
        thread.start()
    }


    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("bsc") {
            description = "Copies information about the item in hand to the clipboard"
            category = CommandCategory.USERS_ACTIVE
            literalCallback("reconnect"){
                ChatUtils.chat("§eReconnecting to BSC Server...")
                connect()
            }
            literalCallback("stop"){
                ChatUtils.chat("§eDisconnecting from BSC Server...")
                stop()
            }
        }
    }

    fun stop() {
        client?.close()
        thread.interrupt()
    }
}
