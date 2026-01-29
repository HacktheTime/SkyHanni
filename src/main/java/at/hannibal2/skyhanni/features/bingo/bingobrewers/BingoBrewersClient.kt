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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

@SkyHanniModule
object BingoBrewersClient {
    private var client: Client? = null
    private var listener: Listener? = null
    private var reconnectJob: Job? = null
    @Volatile private var isConnecting = false
    @Volatile private var isStopping = false
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
        if (!isEnabled()) {
            ChatUtils.chatAndOpenConfig(
                "Bingo Brewers is not enabled right now. Please enable it first,",
                SkyHanniMod.feature.event.bingo.bingoNetworks::useBB,
            )
            return
        }
        if (isConnecting || client?.isConnected == true) return
        isConnecting = true
        try {
            stop(silent = true)
            val client = Client(16384, 16384)
            listener = getListener()
            BingoBrewersPackets.registerPackets(client)
            client.addListener(listener)
            client.start()
            client.connect(10000, "bingobrewers.com", 8282, 7070)
            this.client = client
            val response = BingoBrewersPackets.ConnectionIgn()
            response.hello = "${'$'}{PlayerUtils.getName()}|v0.3.8|Beta|${'$'}{PlayerUtils.getUuid()}"
            println("Sending BingoBrewers Hello " + response.hello)
            client.sendTCP(response)
            reconnectJob?.cancel()
            reconnectJob = null
            ChatUtils.chat("§aConnected to Bingo Brewers server!")
        } finally {
            isConnecting = false
        }
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
                scheduleReconnect()
            }
        }
    }

    fun stop(silent: Boolean = false) {
        isStopping = true
        reconnectJob?.cancel()
        reconnectJob = null
        if (client?.isConnected == true && !silent) {
            ChatUtils.chat("§cDisconnected from Bingo Brewers server.")
        }
        client?.stop()
        client?.close()
        client = null
        listener = null
        isStopping = false
    }

    private fun scheduleReconnect() {
        if (!isEnabled() || isStopping) return
        if (reconnectJob?.isActive == true || isConnecting) return
        reconnectJob = SkyHanniMod.launchCoroutine("BingoBrewers reconnect") {
            var waitTime = ((3000 * Math.random()).toInt() + 2000).toLong()
            while (isEnabled() && !isStopping) {
                ChatUtils.chat("§eReconnecting to Bingo Brewers server...")
                try {
                    connect()
                    return@launchCoroutine
                } catch (e: Exception) {
                    client?.close()
                    client?.removeListener(listener)
                    println("Reconnect failed. Trying again in ${'$'}waitTime milliseconds.")
                    delay(waitTime)
                    waitTime = (waitTime * 3 / 2).coerceAtMost(45000L)
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
                    stop()
                    scheduleReconnect()
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
