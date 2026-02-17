package at.hannibal2.skyhanni.events.player

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.utils.PlayerUtils

/**
 * Activates on any Player Death. You stands for the current player.
 */
class PlayerDeathEvent(val name: String, val reason: String, val chatEvent: SkyHanniChatEvent.Allow) : SkyHanniEvent() {
    val isSelf by lazy {
        name == PlayerUtils.getName()
    }
}
