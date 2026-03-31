package at.hannibal2.skyhanni.features.bingo.bingobrewers

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.features.bingo.bingobrewers.official.ServerConnection
import at.hannibal2.skyhanni.features.bingo.bingobrewers.official.ServerConnection.client
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils

@SkyHanniModule
object BingoBrewersClient {
    private val config get() = SkyHanniMod.feature.event.bingo.bingoNetworks

    fun isEnabled() = config.useBB

    init {
        init()
    }

    @HandleEvent
    fun event(event: ConfigLoadEvent) {
        init()
    }

    @Synchronized
    fun init() {
        if (isEnabled()) {
            ServerConnection.init()
        } else {
            stop()
        }
    }

    fun stop(silent: Boolean = false) {
        if (ServerConnection.isConnected == true && !silent) {
            ChatUtils.chat("§cDisconnected from Bingo Brewers server.")
        }
        ServerConnection.close()
    }
    fun sendTCP(data: Any) {
        client?.sendTCP(data)
    }

    @HandleEvent
    fun commandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier(
            "bnbingobrewers",
            {
                category = CommandCategory.BINGO_NET
                description = "Reload the Bingo Brewers Client"
                literalCallback("reconnect"){
                    stop()
                    ServerConnection.connect()
                }
                callback {
                    ChatUtils.chat("§cMissing Argument!")
                }
                literalCallback("stop"){
                    stop()
                }
                literalCallback("state"){
                    if (BSCClient.client?.isConnected == true){
                        ChatUtils.chat("§aConnected to BB Server")
                    } else {
                        ChatUtils.chat("§cNot connected to BB Server")
                    }
                }
            },
        )
    }


}
