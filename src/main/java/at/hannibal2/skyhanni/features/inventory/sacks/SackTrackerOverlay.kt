package at.hannibal2.skyhanni.features.inventory.sacks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.InternalNameArgumentType
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.InternalNameArgumentType.Companion.itemName
import at.hannibal2.skyhanni.data.SackApi
import at.hannibal2.skyhanni.data.SackApi.getAmountInSacksOrNull
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import com.mojang.brigadier.arguments.IntegerArgumentType
import de.hype.bingonet.environment.displayName

@SkyHanniModule
object SackTrackerOverlay {
    val toTrack: MutableMap<NeuInternalName, UInt?> get() = SkyHanniMod.feature.inventory.sackTrackingOverlayToTrack
    val position get() = SkyHanniMod.feature.inventory.sackTrackingOverlayPosition

    private var tempStorage: List<Renderable> = emptyList()

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (toTrack.isEmpty()) return
        position.renderRenderables(tempStorage, 0, "Sack Tracker Overlay")
    }

    fun update(){
        val renderables = mutableListOf<Renderable>()
        for ((sack, amountNullable) in toTrack) {
            val displayName = sack.displayName
            val wanted = amountNullable
            val sackAmount = sack.getAmountInSacksOrNull() ?: 0
            val amountString = if (wanted == null) {
                "§a$sackAmount"
            } else if (sackAmount >= wanted.toInt()) {
                "§aDONE"
            } else {
                "§c$sackAmount§r/§a$wanted"
            }
            renderables.add(Renderable.text("$displayName§r: $amountString"))
        }
        tempStorage = renderables
    }

    @HandleEvent
    fun onSackUpdate(event: SackChangeEvent) {
        update()
    }

    @HandleEvent
    fun registerCommands(event: CommandRegistrationEvent){
        event.registerBrigadier("shtracksack"){
            description = "Add Sack Items to track the Count of with Count."
            literal("add"){
                arg("item", sackItemArgument()) { itemArg ->
                    argCallback("amount", IntegerArgumentType.integer(0)){
                        toTrack[getArg(itemArg)] = it.toUInt()
                        update()
                    }

                    callback {
                        toTrack[getArg(itemArg)] = null
                        update()
                    }
                }
            }

            literal("remove"){
                argCallback("item", sackItemArgument()) {
                    toTrack.remove(it)
                    update()
                }
            }
            literalCallback("clear"){
                toTrack.clear()
                update()
            }
            literalCallback("removecompleted"){
                toTrack.removeIf(
                    predicate = { (sack, wanted) ->
                        val sackAmount = sack.getAmountInSacksOrNull() ?: 0
                        wanted != null && sackAmount >= wanted.toInt()
                    },
                )
                update()
            }
        }
    }


    fun sackItemArgument() : InternalNameArgumentType = itemName { return@itemName SackApi.sackListInternalNames
        .contains(it.internalName) }
}
