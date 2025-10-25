package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.MinionTutorialStep
class MinionEditor : TutorialNodeEditor {
    private val slotsInput = TextInput()
    override fun supports(node: TutorialNode): Boolean = node is MinionTutorialStep
    override fun title(node: TutorialNode): String = "Edit 'Minion' Step"
    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as MinionTutorialStep
        if (slotsInput.textBox.isEmpty()) slotsInput.textBox = step.slots.toString()
        return listOf(
            Renderable.text("§fEdit 'Minion' Step"),
            Renderable.horizontal(spacing = 6) {
                add(Renderable.text("§7Slots: §f"))
                add(Renderable.textBox("", slotsInput, 100, bypassChecks = true))
            },
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val n = slotsInput.finalText().trim().filter { it.isDigit() }.toIntOrNull() ?: step.slots
                    val newNode = MinionTutorialStep(n)
                    try { TutorialStepLogic.populateNodeIds(newNode, tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
        )
    }
}
