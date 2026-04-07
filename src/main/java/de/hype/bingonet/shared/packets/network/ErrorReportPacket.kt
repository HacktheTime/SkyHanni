package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket

class ErrorReportPacket(
    val original: Throwable,
    val fullErrorData: String,
    val mcVersion: String,
    val shVersion: String,
    val extraData: List<Pair<String, String?>>
) : AbstractPacket()
