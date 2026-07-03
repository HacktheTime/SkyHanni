package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket

data class ErrorReportPacket(
    val original: Throwable,
    val fullErrorData: String,
    val minecraftVersion: String,
    val skyHanniVersion: String,
    val modIdentifier: String,
    val extraData: List<Pair<String, String?>>,
) : AbstractPacket()
