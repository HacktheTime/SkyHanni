package at.hannibal2.skyhanni.features.bingo.bingobrewers.official_ported

import at.hannibal2.skyhanni.features.bingo.bingobrewers.official_ported.ServerConnection.client
import at.hannibal2.skyhanni.features.bingo.bingobrewers.official_ported.ServerConnection.connectionsThisSession
import at.hannibal2.skyhanni.features.bingo.bingobrewers.official_ported.ServerConnection.decryptString
import at.hannibal2.skyhanni.features.bingo.bingobrewers.official_ported.ServerConnection.encryptObjectPublicKey
import at.hannibal2.skyhanni.features.bingo.bingobrewers.official_ported.ServerConnection.encryptString
import at.hannibal2.skyhanni.features.bingo.bingobrewers.official_ported.ServerConnection.generateAESKey
import at.hannibal2.skyhanni.features.bingo.bingobrewers.official_ported.ServerConnection.loadPublicKeyFromBase64
import at.hannibal2.skyhanni.features.bingo.bingobrewers.official_ported.ServerConnection.reconnect
import at.hannibal2.skyhanni.features.bingo.bingobrewers.official_ported.ServerConnection.sendTCP
import at.hannibal2.skyhanni.features.bingo.bingonet.SplashManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.MojangUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import com.esotericsoftware.kryonet.Connection
import com.github.indigopolecat.kryo.KryoNetwork
import com.mojang.authlib.exceptions.AuthenticationException
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.objects.SplashData
import de.hype.bingonet.shared.objects.SplashLocations
import java.security.NoSuchAlgorithmException
import java.util.UUID
import javax.crypto.SecretKey

object PacketProcessing {
    private var CLIENT_INSTANCE: ServerConnection? = null

    fun processPacket(connection: Connection?, packet: Any?) {
        if (packet is KryoNetwork.ServerPublicKey) {
            val serverPublicKey = packet
            val public_key = serverPublicKey.public_key
            val symmetricKey: SecretKey

            if (public_key == ServerConnection.SERVER_PUBLIC_KEY) {
                try {
                    symmetricKey = generateAESKey(256)
                } catch (e: NoSuchAlgorithmException) {
                    throw RuntimeException(e)
                }

                ServerConnection.symmetricKey = symmetricKey

                val key = KryoNetwork.ClientSymmetricKey()
                key.symmetric_key = encryptObjectPublicKey(symmetricKey, loadPublicKeyFromBase64(public_key))
                sendTCP(key)
            } else {
                ChatUtils.chat("BB Server Public Key Outdated")
                client!!.close()
                client?.removeListener(CLIENT_INSTANCE)
                reconnect = true // by setting this to true, the client will assume it is already reconnecting and won't try to
                return
            }
        } else if (packet is KryoNetwork.Authentication) {
            val authentication = packet
            val serverAuthID: String = decryptString(authentication.AuthID!!)

            val clientAuthID = UUID.randomUUID().toString().replace("-".toRegex(), "")
            authentication.AuthID = encryptString(clientAuthID)

            try {
                // This is sending your session info to Mojang's servers as if you were joining a server,
                // this is used on the Bingo Brewers server to authenticate your IGN like an MC server normally would when you join.
                // Basically it's for authentication, it's not a rat, here's the exact same code in skytils:  https://github.com/Skytils/SkytilsMod/blob/1.x/src/main/kotlin/gg/skytils/skytilsmod/features/impl/handlers/MayorInfo.kt#L175
                MojangUtils.joinServer(
                    serverAuthID.substring(
                        0,
                        serverAuthID.length / 2 - 1,
                    ) + clientAuthID.substring(clientAuthID.length / 2),
                )
            } catch (e: AuthenticationException) {
                e.printStackTrace()
                return
            }

            sendTCP(authentication)

            val accountInfo = KryoNetwork.ConnectionIGN()
            accountInfo.IGN = encryptString(PlayerUtils.getName())
            accountInfo.uuid = encryptString(PlayerUtils.getRawUuid().toString())
            accountInfo.version = encryptString("v0.4")
            accountInfo.connections = connectionsThisSession

            sendTCP(accountInfo)


            try {
                Thread.sleep(300)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        } else if (packet is KryoNetwork.SplashNotification) {
            println("Received splash notification")

            val notif = packet
            val hub = notif.hub
            if (hub.isNullOrEmpty()) return // completely ignore splashes without a hub number
            val note = notif.note?.joinToString(" ").orEmpty()
            val splasher = notif.splasher ?: return
            val serverId = notif.serverID ?: return

            SplashManager.addSplashAndDisplay(
                SplashData(
                    splasher,
                    SplashLocations.MAP_BB,
                    note,
                    false,
                    serverId,
                    if (notif.isPrivate) {
                        null
                    } else {
                        SplashData.HubSelectorData(
                            hub.toInt(),
                            if (notif.dungeonHub) Islands.DUNGEON_HUB else Islands.HUB,
                        )
                    },
                ),
                SplashManager.SplashSource.BB,
            )

        } else if (packet is KryoNetwork.PlayerCountBroadcast) {
            // ignored for now
        } else if (packet is KryoNetwork.ClientReceiveServerConstantValues) {
            // ignored for now
        } else if (packet is KryoNetwork.ServerSendCHItems) {
            // ignored for now
        } else if (packet is KryoNetwork.QueuePosition) {
            // ignored for now
        } else if (packet is KryoNetwork.BackgroundWarpTask) {
            // ignored for now
        } else if (packet is KryoNetwork.WarningBannerInfo) {
            // ignored for now
        } else if (packet is KryoNetwork.AbortWarpTask) {
            // ignored for now
        } else if (packet is KryoNetwork.CancelWarpRequest) {
            // ignored for now
        } else if (packet is KryoNetwork.WarperInfo) {
            // ignored for now
        } else {
            // Keep unknown packet handling side-effect free on the network thread.
            println("[BB] Unhandled packet: ${packet?.javaClass?.name}")
//         }else if (packet is KryoNetwork.PlayerCountBroadcast) {
//             val playerCountBroadcast = packet
//
// //             for (info in SplashInfoHud.activeSplashes) {
// //                 if (playerCountBroadcast.serverID == info.serverID) {
// //                     info.lobbyPlayerCount = playerCountBroadcast.playerCount.toString()
// //                 }
// //             }
//         } else if (packet is ClientReceiveServerConstantValues) {
// //             val request = packet
// //             val constants = request.constants
// //
// //             if (constants.get("bingoRankCosts") != null && constants.get("bingoRankCosts") is HashMap<*, *>) {
// //                 var nope = false
// //                 for (entry in (constants.get("bingoRankCosts") as HashMap<*, *>).entries) {
// //                     if (entry.value !is Int || entry.key !is Int) {
// //                         nope = true
// //                         break
// //                     }
// //                 }
// //                 if (!nope) {
// //                     ChestInventories.rankPriceMap = constants.get("bingoRankCosts") as HashMap<Int?, Int?>?
// //                 }
// //             }
// //             if (constants.get("chItemRegex") != null && constants.get("chItemRegex") is String) {
// //                 CHChests.regex = constants.get("chItemRegex") as String?
// //             }
// //             if (constants.get("newMiscCHItems") != null && constants.get("newMiscCHItems") is ArrayList<*>) {
// //                 var nope = false
// //                 for (string in (constants.get("newMiscCHItems") as java.util.ArrayList<kotlin.Any?>?)!!) {
// //                     if (string !is String) {
// //                         nope = true
// //                         break
// //                     }
// //                 }
// //                 if (!nope) {
// //                     newMiscCHItems = constants.get("newMiscCHItems") as ArrayList<String?>?
// //                 }
// //             }
// //             if (constants.get("joinAlert" + version) != null && constants.get("joinAlert" + version) is KryoNetwork.JoinAlert) {
// //                 val joinAlert = constants.get("joinAlert" + version) as KryoNetwork.JoinAlert
// //                 if (joinAlert.joinAlertChat != null) {
// //                     joinChat = joinAlert.joinAlertChat
// //                 }
// //                 if (joinAlert.joinAlertTitle != null) {
// //                     joinTitle = TitleHud(joinAlert.joinAlertTitle, 0xFF5555, 10000, true)
// //                 }
// //             }
// //
// //             if (constants.get("CHItemOrder") != null && constants.get("CHItemOrder") is LinkedHashSet<*>) {
// //                 var nope = false
// //                 for (string in (constants.get("CHItemOrder") as java.util.LinkedHashSet<kotlin.Any?>?)!!) {
// //                     if (string !is String) {
// //                         nope = true
// //                         break
// //                     }
// //                 }
// //                 if (!nope) {
// //                     CHItemOrder = ArrayList<String?>(constants.get("CHItemOrder") as LinkedHashSet<String?>?)
// //                 }
// //             }
// //
// //             if (constants.get("itemNameRegexGroup") != null && constants.get("itemNameRegexGroup") is Int) {
// //                 if (constants.get("itemCountRegexGroup") != null && constants.get("itemCountRegexGroup") is Int) if (constants.get("itemNameColorRegexGroup") != null && constants.get(
// //                         "itemNameColorRegexGroup",
// //                     ) is Int
// //                 ) {
// //                     CHChests.itemCountRegexGroup = constants.get("itemCountRegexGroup") as Int?
// //                     CHChests.itemNameRegexGroup = constants.get("itemNameRegexGroup") as Int?
// //                     CHChests.itemNameColorRegexGroup = constants.get("itemNameColorRegexGroup") as Int?
// //                 }
// //             }
// //
// //             if (constants.get("signalLootChatMessage") != null && constants.get("signalLootChatMessage") is String) {
// //                 CHChests.signalLootChatMessage = constants.get("signalLootChatMessage") as String?
// //             }
// //
// //             if (constants.get("signalLootChatMessageEnd") != null && constants.get("signalLootChatMessageEnd") is String) {
// //                 CHChests.signalLootChatMessageEnd = constants.get("signalLootChatMessageEnd") as String?
// //             }
//         } else if (packet is ServerSendCHItems) {
// //             val CHItems = packet
// //             println("Received CH Chests for " + CHItems.server)
// //             val chests: ArrayList<KryoNetwork.ChestInfo> = CHItems.chestMap
// //             if (CHItems.server == PlayerInfo.currentServer) {
// //                 if (CHItems.day - 1 > PlayerInfo.day || System.currentTimeMillis() - (if (CHItems.lastReceivedDayInfo != null) CHItems.lastReceivedDayInfo else Long.MAX_VALUE) > 25200000) return  // ignore if the server is younger than last known, or it's been more than 7 hours since info was received
// //
// //                 for (chest in chests) {
// //                     val chWaypoints: CHWaypoints = CHWaypoints(chest.x, chest.y, chest.z, chest.items)
// //                     waypoints.add(chWaypoints)
// //
// //                     for (item in chest.items) {
// //                         CrystalHollowsItemTotal.sumItems(item)
// //                     }
// //
// //                     for (waypoint in CHWaypoints.filteredWaypoints) {
// //                         waypoint.filteredExpandedItems.clear()
// //                     }
// //
// //                     BingoBrewersConfig.filterPowder()
// //                     BingoBrewersConfig.filterGoblinEggs()
// //                     BingoBrewersConfig.filterRoughGemstones()
// //                     //BingoBrewersConfig.filterJasperGemstones();
// //                     BingoBrewersConfig.filterRobotParts()
// //                     BingoBrewersConfig.filterPrehistoricEggs()
// //                     BingoBrewersConfig.filterPickonimbus()
// //                     BingoBrewersConfig.filterMisc()
// //                     organizeWaypoints()
// //                 }
// //             }
// //         } else if (packet is ServersSummary) {
// //             val servers: ServersSummary = packet as ServersSummary
// //             serverSummaries.putAll(servers.serverInfo)
// //             // remove outdated entries
// //             for (server in serverSummaries.values()) {
// //                 if (server.serverType == null) {
// //                     serverSummaries.remove(server.server)
// //                 }
// //             }
//         } else if (packet is KryoNetwork.QueuePosition) {
//             // if you have to wait in the queue, this will give you your current position
//             // gonna leave it for you to implement because I think the permanent value should be stored in the class for rendering the menu
//             val position = packet
//             if (position.positionInWarpQueue == 0) {
//                 // server is telling the client there was an unknown error and there are no available warp clients
//             }
//         } else if (packet is KryoNetwork.BackgroundWarpTask) {
// //             val warpTask = packet
// //
// //             if (warpTask.server == PlayerInfo.currentServer && !warpTask.accountsToWarp.isEmpty() && accountsToWarp.isEmpty() && !warpTask.accountsToWarp.containsKey(
// //                     null,
// //                 ) && !warpTask.accountsToWarp.containsValue(null)
// //             ) {
// //                 accountsToWarp = ConcurrentHashMap<String?, String?>(warpTask.accountsToWarp)
// //                 Warping.server = warpTask.server
// //
// //                 println("Received warp task for: " + accountsToWarp.values())
// //
// //                 if (accountsToWarp.isEmpty()) {
// //                     Warping.abort(false)
// //                     return
// //                 }
// //
// //                 val client: Client? = getClient()
// //                 if (client != null) {
// //                     val confirm = KryoNetwork.BackgroundWarpTask()
// //                     confirm.accountsToWarp = HashMap<String?, String?>()
// //                     confirm.accountsToWarp.put(uuid, ign)
// //                     confirm.server = PlayerInfo.currentServer
// //                     client.sendTCP(confirm)
// //                     println("confirmed")
// //                 }
// //
// //                 if (Warping.warpThread != null) {
// //                     warpThread.stop = true
// //                     warpThread.notify()
// //                 }
// //                 Warping.warpThread = BackgroundWarpThread()
// //                 val warpThread: Thread = Thread(Warping.warpThread)
// //                 warpThread.start()
// //                 println("warp begun")
// //             } else {
// //                 println("something went wrong")
// //                 println("warpTask Server: " + warpTask.server + " player info server: " + PlayerInfo.currentServer)
// //                 println("current accounts: " + accountsToWarp.toString())
// //                 println("accounts to warp: " + warpTask.accountsToWarp.toString())
// //             }
//         } else if (packet is KryoNetwork.WarningBannerInfo) {
//         } else if (packet is KryoNetwork.AbortWarpTask) {
// //             Warping.PARTY_EMPTY_KICK = false
// //             Warping.kickParty = true
// //             accountsToWarp.clear()
// //             Warping.waitingOnLocation = true
// //             if (warpThread != null) {
// //                 warpThread.stop = true
// //                 warpThread.notify()
// //             }
//         } else if (packet is KryoNetwork.CancelWarpRequest) {
// //             requestedWarp = ""
// //             // sent by server if unable to fulfill a warp
//         } else if (packet is KryoNetwork.WarperInfo) {
// //             val warperInfo = packet
// //
// //             warperIGN = decryptString(warperInfo.ign)
// //
// //             timeOfInvite = System.currentTimeMillis()
// //
// //             if (partyInvites.contains(warperIGN) && System.currentTimeMillis() - timeOfInvite < 5000) {
// //                 // we have already received the packet telling us the ign and we can safely join
// //                 sendChatMessage("/p accept " + warperIGN)
// //                 partyInvites = ArrayList<Any?>()
// //                 warperIGN = null
// //                 timeOfInvite = 0
// //             } else if (System.currentTimeMillis() - timeOfInvite > 5000) {
// //                 warperIGN = null
// //                 partyInvites = ArrayList<Any?>()
// //                 timeOfInvite = 0
// //             }
// //         }
//         }
        }
    }
    fun setClientInstance(instance: ServerConnection?) {
        CLIENT_INSTANCE = instance
    }
}
