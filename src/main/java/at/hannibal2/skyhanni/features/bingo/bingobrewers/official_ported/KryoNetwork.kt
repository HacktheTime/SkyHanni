@file:Suppress("PackageDirectoryMismatch")

package com.github.indigopolecat.kryo

import com.esotericsoftware.kryonet.EndPoint
import kotlin.jvm.java

object KryoNetwork {
    fun register(endPoint: EndPoint) {
        val kryo = endPoint.getKryo()
        kryo.register(EncryptedString::class.java)
        kryo.register(ByteArray::class.java)
        kryo.register(ServerPublicKey::class.java)
        kryo.register(Authentication::class.java)
        kryo.register(ClientSymmetricKey::class.java)
        kryo.register(ConnectionIGN::class.java)
        kryo.register(SplashNotification::class.java)
        kryo.register(ArrayList::class.java)
        kryo.register(PlayerCount::class.java)
        kryo.register(PlayerCountBroadcast::class.java)
        kryo.register(HashMap::class.java)
        kryo.register(ClientRequestLowestBINPrices::class.java)
        kryo.register(ServerSendLowestBINPrices::class.java)
        kryo.register(ClientSendCHItems::class.java)
        kryo.register(ServerSendCHItems::class.java)
        kryo.register(SubscribeToCHServer::class.java)
        kryo.register(ChestInfo::class.java)
        kryo.register(CHChestItem::class.java)
        kryo.register(LinkedHashSet::class.java)
        kryo.register(RequestWarpToServer::class.java)
        kryo.register(BackgroundWarpTask::class.java)
        kryo.register(RegisterToWarpServer::class.java)
        kryo.register(DoneWithWarpTask::class.java)
        kryo.register(CancelWarpRequest::class.java)
        kryo.register(AbortWarpTask::class.java)
        kryo.register(QueuePosition::class.java)
        kryo.register(ServerSummary::class.java)
        kryo.register(ServersSummary::class.java)
        kryo.register(UpdateServers::class.java)
        kryo.register(RequestLiveUpdatesForServerInfo::class.java)
        kryo.register(WarningBannerInfo::class.java)
        kryo.register(ClientReceiveServerConstantValues::class.java)
        kryo.register(JoinAlert::class.java)
        kryo.register(WarperInfo::class.java)
        kryo.register(RequestQueuePosition::class.java)
    }

    // TODO: add constructors to classes
    class ServerPublicKey {
        var public_key: String? = null
    }

    class ClientSymmetricKey {
        var symmetric_key: String? = null
    }

    class Authentication {
        var AuthID: EncryptedString? = null
    }

    class ConnectionIGN {
        var IGN: EncryptedString? = null
        var version: EncryptedString? = null
        var uuid: EncryptedString? = null
        var connections: Int = 0
        var accountInformation: HashMap<Any?, Any?> = HashMap<Any?, Any?>() // for future Misc. purposes
    }

    class EncryptedString {
        var string: String? = null
        var iv: ByteArray? = null
    }

    class SplashNotification {
        var timestamp: Long = 0

        var hub: String? = null
        var serverID: String? = null
        var isPrivate: Boolean = false
        var dungeonHub: Boolean = false

        var splasher: String? = null
        var splasherRealIGN: Boolean = false

        var partyHost: String? = null

        var note: ArrayList<String?>? = null

        var location: String? = null
        var splash: String? = null
        var remove: Boolean = false
    }

    class PlayerCount {
        var splashID: String? = null
        var playerCount: Int = 0
        var hub: String? = null
        var serverID: String? = null
    }

    class PlayerCountBroadcast {
        var playerCount: Int = 0
        var serverID: String? = null
    }


    // Request the lbin of any item on ah/bz by item id
    // If they don't exist, they won't be included in the response
    class ClientRequestLowestBINPrices {
        var items: ArrayList<String?>? = null
    }

    class ServerSendLowestBINPrices {
        var lbinMap: HashMap<String?, Int?>? = null
    }

    class ClientSendCHItems {
        var items: ArrayList<CHChestItem?> = ArrayList<CHChestItem?>()
        var x: Int = 0
        var y: Int = 0
        var z: Int = 0
        var server: String? = null
        var day: Int = 0
    }

    class CHChestItem {
        var name: String? = null
        var count: String? = null
        var numberColor: Int? = null
        var itemColor: Int? = null
    }

    // TODO: combine with warp register into one packet sent every time you join a lobby
    class SubscribeToCHServer {
        var server: String? = null
        var day: Int = 0
        var unsubscribe: Boolean = false
    }

    class ServerSendCHItems {
        var chestMap: ArrayList<ChestInfo?> = ArrayList<ChestInfo?>()
        var server: String? = null // used to confirm that the server is correct
        var day: Int = 0 // server's last known day
        var lastReceivedDayInfo: Long = Long.MAX_VALUE
    }

    class ChestInfo {
        var x: Int = 0
        var y: Int = 0
        var z: Int = 0
        var items: ArrayList<CHChestItem?> = ArrayList<CHChestItem?>()
    }

    class RequestWarpToServer {
        var server: String? = null
        var serverType: String? = null // Crystal Hollows, Dwarven Mines, etc.
    }

    class BackgroundWarpTask {
        var server: String? = null // confirm
        var accountsToWarp: HashMap<String?, String?> = HashMap<String?, String?>()
    }

    class RegisterToWarpServer {
        var server: String? = null
        var unregister: Boolean = false
    }

    class DoneWithWarpTask {
        var successful: Boolean = true
        var ignsWarped: ArrayList<String?> = ArrayList<String?>()
    }

    class CancelWarpRequest {
        var server: String? = null
    }

    // tell a warper to abort a warp
    // client can also send it to the server to indicate it cannot perform a warp to the server at all
    class AbortWarpTask {
        var ign: EncryptedString? = null
        var ineligible: Boolean = false
    }

    class QueuePosition {
        var positionInWarpQueue: Int = 0
    }

    class ServerSummary {
        var server: String? = null
        var serverType: String? = null
        var availablePlayersToWarp: Int = 0
        var lastUpdated: Long = 0
        var condensedItems: HashMap<String?, Any?> = HashMap<String?, Any?>()
    }

    class ServersSummary {
        var serverInfo: HashMap<String?, ServerSummary?> = HashMap<String?, ServerSummary?>()
    }

    class UpdateServers {
        var serversAndLastUpdatedTime: HashMap<String?, Long?> = HashMap<String?, Long?>()
    }

    class RequestLiveUpdatesForServerInfo {
        var unrequest: Boolean = false
    }

    class WarningBannerInfo {
        var text: EncryptedString? = null
        var textColor: Int = 0xFFFFFF
        var backgroundColor: Int = 0x000000
    }

    class ClientReceiveServerConstantValues {
        var constants: HashMap<String?, Any?> = HashMap<String?, Any?>()
    }

    class JoinAlert {
        var joinAlertChat: String? = null
        var joinAlertTitle: String? = null
    }

    class WarperInfo {
        var ign: EncryptedString? = null
    }

    class RequestQueuePosition {
        var server: String? = null
    }

//     class ServerSummary {
//         var server: String? = null
//         var serverType: String? =
//             null // Ex. Crystal Hollows, just so the warp network can be implemented for other servers in the future easily, add a check to make sure it's "Crystal Hollows"
//         var availablePlayersToWarp: Int = 0 // how many players are in the lobby that can warp
//         var lastUpdated: Long = 0
//         var condensedItems: HashMap<String?, CrystalHollowsItemTotal?> = HashMap<String?, CrystalHollowsItemTotal?>()
//
//         companion object {
//             fun sendUpdatedSummariesRequest() {
//                 val updateServers = UpdateServers()
//                 for (summary in ServerConnection.serverSummaries.values()) {
//                     updateServers.serversAndLastUpdatedTime.put(summary.server, summary.lastUpdated)
//                 }
//                 sendTCP(updateServers)
//             }
//         }
//     }
}
