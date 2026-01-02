package at.hannibal2.skyhanni.features.bingo.bingobrewers

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.event.bingo.BingoNetworksConfig
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.features.chat.CurrentChatDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import de.hype.bingonet.environment.packetconfig.PacketUtils.gson
import java.io.IOException

@SkyHanniModule
object BingoBrewersClient {
    private var client: Client? = null
    private var listener: Listener? = null
    private val config = SkyHanniMod.feature.event.bingo.bingoNetworks

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
            SkyHanniMod.launchCoroutine("Init BingoBrewersClient") {
                if (client?.isConnected != true) connect()
            }
        } else {
            stop()
        }
    }


    @Throws(IOException::class)
    @Synchronized
    private fun connect() {
        stop()
        if (!isEnabled()){
            ChatUtils.chatAndOpenConfig("Bingo Brewers is not enabled right now. Please enable it first,", SkyHanniMod.feature.event.bingo
                .bingoNetworks::useBB)
            return
        }
        val client = Client(16384, 16384)
        listener = getListener()
        BingoBrewersPackets.registerPackets(client)
        client.addListener(listener)
        client.start()
        client.connect(10000, "bingobrewers.com", 8282, 7070)
        this.client = client
        val response = BingoBrewersPackets.ConnectionIgn()
        // IDK your server side indigo. I wanted to avoid issues on your side if I change anything since I dont have your code to look at. Otherwise I would have said sth like v0.3.8-compatible or sth.
        response.hello = "${PlayerUtils.getName()}|v0.3.8|Beta|${PlayerUtils.getUuid()}"
        println("Sending BingoBrewers Hello " + response.hello)
        client.sendTCP(response)
        ChatUtils.chat("§aConnected to Bingo Brewers server!")
    }


    private fun getListener(): Listener {
        return object : Listener() {
            override fun received(connection: Connection?, `object`: Any) {
                SkyHanniMod.launchCoroutine("BingoBrewers Packet handling") {
                    if (`object`.javaClass.`package`.name.contains("com.esotericsoftware.kryonet")) return@launchCoroutine
                    if (`object` is BingoBrewersPackets.BingoBrewersPacket<*>) {
                        if (SkyHanniMod.feature.event.bingo.bingoNetworks.showPacketTraffic) println("BN Bingobrewrs: ${gson.toJson(`object`)}")
                        try {
                            val packet = `object`
                            packet.executeUnparsed(packet, client ?: error("BingoBrewersClient client is null but received a packet!"))
                        } catch (e: Exception) {
                            ChatUtils.chat("Error handling a Packet from Bingo Brewers. Please report this to BINGO NET")
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun disconnected(connection: Connection?) {
                reconnect()
            }
        }
    }

    fun stop() {
        if (client?.isConnected == true) {
            ChatUtils.chat("§cDisconnected from Bingo Brewers server.")
        }
        client?.stop()
        client?.close()
    }

    fun reconnect() {
        var waitTime: Float
        var repeat: Boolean

        waitTime = ((3000 * Math.random()).toInt() + 2000).toFloat()

        repeat = true
        while (repeat) {
            try {
                ChatUtils.chat("Reconnecting to Bingo Brewers server...")
                connect()
                repeat = false
            } catch (e: Exception) {
                client?.close()
                client?.removeListener(listener)
                try {
                    println("Reconnect failed. Trying again in $waitTime milliseconds.")
                    Thread.sleep(waitTime.toInt().toLong())
                } catch (ex: InterruptedException) {
                    throw RuntimeException(ex)
                }
                // keep reconnects under 45s between
                if (waitTime * 1.5 < 45000) {
                    waitTime *= 1.5f
                } else {
                    waitTime = (45000 - (5000 * Math.random() + 1000).toInt()).toFloat() // slightly vary time
                }
            }
        }
    }

    fun sendTCP(data: Any) {
        client?.sendTCP(data)
    }

    @HandleEvent
    fun commandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier(
            "bingobrewersreconnect",
            {
                category = CommandCategory.BINGO_NET
                description = "Reload the Bingo Brewers Client"
                literalCallback("reconnect"){
                    connect()
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
