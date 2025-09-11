package at.hannibal2.skyhanni.events.mining

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.mining.eventtracker.MiningEventData

class NewMiningEvent(
    val data: MiningEventData,
) : SkyHanniEvent()
