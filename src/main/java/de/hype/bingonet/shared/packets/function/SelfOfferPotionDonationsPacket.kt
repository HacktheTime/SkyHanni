package de.hype.bingonet.shared.packets.function

import at.hannibal2.skyhanni.utils.NeuInternalName
import de.hype.bingonet.environment.packetconfig.AbstractPacket

/**
 * Used to tell Bingo Net the amount of Potions you have to offer.
 */
data class SelfOfferPotionDonationsPacket(
    val amounts : Map<NeuInternalName, Int>,
    val extraMessage: String? = null
) : AbstractPacket()
