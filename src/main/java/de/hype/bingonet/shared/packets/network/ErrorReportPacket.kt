package de.hype.bingonet.shared.packets.network
import de.hype.bingonet.shared.packets.base.ExpectReplyPacket

data class ErrorReportPacket(
    val original: Throwable,
    val fullErrorData: String,
    val minecraftVersion: String,
    val skyHanniVersion: String,
    val modIdentifier: String,
    val extraData: List<Pair<String, String?>>,
) : ExpectReplyPacket<ErrorReportedIdPacket>()

data class ErrorReportedIdPacket(
    val errorReportId: String,
    val isNew: Boolean,
    val url: String
) : ExpectReplyPacket.ReplyPacket()
