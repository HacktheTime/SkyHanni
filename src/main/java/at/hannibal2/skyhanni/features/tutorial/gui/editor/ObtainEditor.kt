package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialStepLogic
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.ItemCheck
import de.hype.bingonet.shared.tutorials.ResourceItemCheck
import de.hype.bingonet.shared.tutorials.TaggedItemCheck
import de.hype.bingonet.shared.tutorials.SingleItemCheck
import de.hype.bingonet.shared.tutorials.ItemCondition
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.storagestep.ObtainTutorialStep
class ObtainEditor : TutorialNodeEditor {
    private val displayInput = TextInput()
    private val descriptionInput = TextInput()
    private val amountInput = TextInput() // for resource checks
    private val forceInvToggle = TextInput() // simplified as toggle via text ON/OFF
    private val tagNameInput = TextInput()
    override fun supports(node: TutorialNode): Boolean = node is ObtainTutorialStep
    override fun title(node: TutorialNode): String = "Edit 'Obtain' Step"
    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as ObtainTutorialStep
        val chk = step.check
        if (displayInput.textBox.isEmpty()) displayInput.textBox = chk.displayText
        if (descriptionInput.textBox.isEmpty()) descriptionInput.textBox = chk.descriptionText.orEmpty()
        if (amountInput.textBox.isEmpty() && chk is ResourceItemCheck) amountInput.textBox = chk.amount.toString()
        if (forceInvToggle.textBox.isEmpty()) forceInvToggle.textBox = if (step.forceInventory) "ON" else "OFF"
        if (tagNameInput.textBox.isEmpty()) tagNameInput.textBox = step.tagName.orEmpty()
        val toggleHint = Renderable.text("§7Force in Inventory: type ON or OFF")
        return listOf(
            Renderable.text("§fEdit 'Obtain' Step (basic)"),
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Display: §f")); add(Renderable.textBox("", displayInput, 280, bypassChecks = true)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Description: §f")); add(Renderable.textBox("", descriptionInput, 300, bypassChecks = true)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Amount (if resource): §f")); add(Renderable.textBox("", amountInput, 120, bypassChecks = true)) },
            toggleHint,
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Force Inventory: §f")); add(Renderable.textBox("", forceInvToggle, 80, bypassChecks = true)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Tag Name (optional): §f")); add(Renderable.textBox("", tagNameInput, 180, bypassChecks = true)) },
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    // build concrete ItemCheck without mutating properties
                    val display = displayInput.finalText().trim().ifEmpty { chk.displayText }
                    val desc = descriptionInput.finalText().trim().ifEmpty { chk.descriptionText }
                    val newCheck: ItemCheck = when (chk) {
                        is ResourceItemCheck -> {
                            val amt = amountInput.finalText().trim().filter { it.isDigit() }.toIntOrNull() ?: chk.amount
                            ResourceItemCheck(chk.item, amt, display, desc)
                        }
                        is TaggedItemCheck -> {
                            val tag = tagNameInput.finalText().trim().ifEmpty { chk.tag }
                            TaggedItemCheck(display, desc, tag)
                        }
                        else -> SingleItemCheck(ItemCondition(), display, desc)
                    }
                    val force = forceInvToggle.finalText().trim().equals("ON", true)
                    val tag = tagNameInput.finalText().trim().ifEmpty { null }
                    val newNode = ObtainTutorialStep(newCheck, step.obtainSource, force, step.forceNPCLeftOver, tag)
                    try { TutorialStepLogic.populateNodeIds(newNode, tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
        )
    }
}
