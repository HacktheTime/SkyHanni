package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialStepLogic
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.requirement.ObtainCoinsTutorialStep
class ObtainCoinsEditor : TutorialNodeEditor {
    private val amountInput = TextInput()
    override fun supports(node: TutorialNode): Boolean = node is ObtainCoinsTutorialStep
    override fun title(node: TutorialNode): String = "Edit 'Obtain Coins' Step"
    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as ObtainCoinsTutorialStep
        if (amountInput.textBox.isEmpty()) amountInput.textBox = step.amount.toString()
        return listOf(
            Renderable.text("§fEdit 'Obtain Coins' Step"),
            Renderable.horizontal(spacing = 6) {
                add(Renderable.text("§7Amount: §f"))
                add(Renderable.textBox("", amountInput, 160, bypassChecks = true))
            },
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val raw = amountInput.finalText().trim().filter { it.isDigit() }
                    val amt = raw.toLongOrNull() ?: step.amount
                    val newNode = ObtainCoinsTutorialStep(amt)
                    try { TutorialStepLogic.populateNodeIds(newNode, tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
        )
    }
}
