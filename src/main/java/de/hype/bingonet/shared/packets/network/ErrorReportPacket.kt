package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket

class ErrorReportPacket(
    original: Throwable,
    fullErrorData: String,
    mcVersion: String,
    shVersion: String,
    extraData: List<Pair<String, String?>>
) : AbstractPacket() {
}
