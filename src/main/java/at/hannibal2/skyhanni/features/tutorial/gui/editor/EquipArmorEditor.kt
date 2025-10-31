package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialStepLogic
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.TaggedItemCheck
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.storagestep.EquipArmorTutorialStep
class EquipArmorEditor : TutorialNodeEditor {
    private val helmetInput = TextInput()
    private val chestInput = TextInput()
    private val legsInput = TextInput()
    private val bootsInput = TextInput()
    private val necklaceInput = TextInput()
    private val cloakInput = TextInput()
    private val beltInput = TextInput()
    private val glovesInput = TextInput()
    override fun supports(node: TutorialNode): Boolean = node is EquipArmorTutorialStep
    override fun title(node: TutorialNode): String = "Edit 'Equip Armor' Step"
    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as EquipArmorTutorialStep
        if (helmetInput.textBox.isEmpty()) helmetInput.textBox = step.helmet?.tag.orEmpty()
        if (chestInput.textBox.isEmpty()) chestInput.textBox = step.chestplate?.tag.orEmpty()
        if (legsInput.textBox.isEmpty()) legsInput.textBox = step.leggings?.tag.orEmpty()
        if (bootsInput.textBox.isEmpty()) bootsInput.textBox = step.boots?.tag.orEmpty()
        if (necklaceInput.textBox.isEmpty()) necklaceInput.textBox = step.necklace?.tag.orEmpty()
        if (cloakInput.textBox.isEmpty()) cloakInput.textBox = step.cloak?.tag.orEmpty()
        if (beltInput.textBox.isEmpty()) beltInput.textBox = step.belt?.tag.orEmpty()
        if (glovesInput.textBox.isEmpty()) glovesInput.textBox = step.gloves?.tag.orEmpty()
        fun toTagged(text: String): TaggedItemCheck? {
            val t = text.trim().ifEmpty { return null }
            return TaggedItemCheck(displayText = t, descriptionText = null, tag = t)
        }
        return listOf(
            Renderable.text("§fEdit 'Equip Armor' Step"),
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Helmet tag: §f")); add(Renderable.textBox("", helmetInput, 200, bypassChecks = true)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Chestplate tag: §f")); add(Renderable.textBox("", chestInput, 200, bypassChecks = true)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Leggings tag: §f")); add(Renderable.textBox("", legsInput, 200, bypassChecks = true)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Boots tag: §f")); add(Renderable.textBox("", bootsInput, 200, bypassChecks = true)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Necklace tag: §f")); add(Renderable.textBox("", necklaceInput, 200, bypassChecks = true)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Cloak tag: §f")); add(Renderable.textBox("", cloakInput, 200, bypassChecks = true)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Belt tag: §f")); add(Renderable.textBox("", beltInput, 200, bypassChecks = true)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Gloves tag: §f")); add(Renderable.textBox("", glovesInput, 200, bypassChecks = true)) },
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val newNode = EquipArmorTutorialStep(
                        helmet = toTagged(helmetInput.finalText()),
                        chestplate = toTagged(chestInput.finalText()),
                        leggings = toTagged(legsInput.finalText()),
                        boots = toTagged(bootsInput.finalText()),
                        necklace = toTagged(necklaceInput.finalText()),
                        cloak = toTagged(cloakInput.finalText()),
                        belt = toTagged(beltInput.finalText()),
                        gloves = toTagged(glovesInput.finalText()),
                    )
                    try { TutorialStepLogic.populateNodeIds(newNode, tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
        )
    }
}
