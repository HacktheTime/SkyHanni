package de.hype.bingonet.shared.tutorials.steps.storagestep

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.inventory.EquipmentApi
import at.hannibal2.skyhanni.features.inventory.EquipmentSlot
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeApi
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import de.hype.bingonet.shared.tutorials.TaggedItemCheck
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class EquipArmorTutorialStep(
    val helmet: TaggedItemCheck?,
    val chestplate: TaggedItemCheck?,
    val leggings: TaggedItemCheck?,
    val boots: TaggedItemCheck?,
    val necklace: TaggedItemCheck?,
    val cloak: TaggedItemCheck?,
    val belt: TaggedItemCheck?,
    val gloves: TaggedItemCheck?,
) : TutorialStep() {

    fun check(): Boolean {
        val data = WardrobeApi.storage ?: return false
        val currentSlot = data.currentSlot
        val helmetSelected =
            helmet?.let { helmet -> data.data.get(currentSlot)?.armor?.any { it?.let { helmet.check(it) } ?: false } } ?: true
        val chestplateSelected =
            chestplate?.let { chestplate -> data.data.get(currentSlot)?.armor?.any { it?.let { chestplate.check(it) } ?: false } } ?: true
        val leggingsSelected =
            leggings?.let { leggings -> data.data.get(currentSlot)?.armor?.any { it?.let { leggings.check(it) } ?: false } } ?: true
        val bootsSelected =
            boots?.let { boots -> data.data.get(currentSlot)?.armor?.any { it?.let { boots.check(it) } ?: false } } ?: true

        val necklaceSelected = necklace?.let {
            EquipmentApi.getEquipment(EquipmentSlot.NECKLACE)?.let { stack -> it.check(stack) } ?: false
        } ?: true

        val cloakSelected = cloak?.let {
            EquipmentApi.getEquipment(EquipmentSlot.CLOAK)?.let { stack -> it.check(stack) } ?: false
        } ?: true
        val beltSelected = belt?.let {
            EquipmentApi.getEquipment(EquipmentSlot.BELT)?.let { stack -> it.check(stack) } ?: false
        } ?: true
        val glovesSelected = gloves?.let {
            EquipmentApi.getEquipment(EquipmentSlot.GLOVES)?.let { stack -> it.check(stack) } ?: false
        } ?: true
        return helmetSelected && chestplateSelected && leggingsSelected && bootsSelected
            && necklaceSelected && cloakSelected && beltSelected && glovesSelected
    }

    override fun getStepName(tutorial: Tutorial): String {
        return "Equip Armor"
    }

    override fun getStepDescription(tutorial: Tutorial): String? {
        return null
        //TODO show user the items he has to equip via display names.
    }

    override fun isComplete(tutorial: Tutorial): Boolean {
        if (check()) {
            complete()
            return true
        } else {
            return false
        }
    }

    override fun getRequirements(): List<TutorialNode> = emptyList()

    override fun onActivate(tutorial: Tutorial) {
        ChatUtils.chatPrompt(
            message = "Incorrect Armor Equipped. Press %KEY% to open the Warderobe",
            keyBind = SkyHanniMod.feature.tutorials
                .chatPromptKey,
            code = {
                HypixelCommands.openWardrobe()
            },
        )
    }
}
