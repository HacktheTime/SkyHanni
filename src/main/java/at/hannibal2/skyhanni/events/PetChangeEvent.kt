package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.PetData
import io.github.moulberry.notenoughupdates.miscfeatures.PetInfoOverlay

class PetChangeEvent(
    val newPet: PetData?,
    val oldPet: PetData?,
) : SkyHanniEvent()
