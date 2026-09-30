package at.hannibal2.skyhanni.api.hypixeltools

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NeuItems

@SkyHanniModule
object HypixelToolsObservationEvents {
    private var lastNpcId: String? = null
    private var lastNpcName: String? = null
    private val dialogue = mutableListOf<String>()

    private val npcDialoguePattern = Regex("""^\s*(?:NPC|§[0-9a-fk-or]NPC)\s*[>:]\s*(.+?)\s*:\s*(.+)$""")

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        val match = npcDialoguePattern.matchEntire(event.cleanMessage) ?: return
        val name = match.groupValues[1].trim()
        val line = match.groupValues[2].trim()
        lastNpcName = name
        lastNpcId = "NPC_${name.uppercase().replace(Regex("[^A-Z0-9]+"), "_")}"
        dialogue += line
        HypixelToolsRepository.observeNpc(lastNpcId!!, name, dialogue = dialogue)
    }

    @HandleEvent
    fun onContainerClick(event: GuiContainerEvent.SlotClickEvent) {
        event.item?.let { NeuItems.getItemReference(it) }
        val npcId = lastNpcId ?: return
        val name = lastNpcName ?: npcId
        val inventory = event.container.slots.mapNotNull {
            NeuItems.getItemReference(it.item)?.getId()
        }.distinct()
        HypixelToolsRepository.observeNpc(
            id = npcId,
            displayName = name,
            dialogue = dialogue,
            guiName = InventoryUtils.openInventoryName(),
            inventory = inventory,
        )
    }
}
