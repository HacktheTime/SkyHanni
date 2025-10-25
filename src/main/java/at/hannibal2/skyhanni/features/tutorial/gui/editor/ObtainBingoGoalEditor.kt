package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.misc.ObtainBingoGoalTutorialStep
class ObtainBingoGoalEditor : TutorialNodeEditor {
    private val nameInput = TextInput()
    private var showOnActive: Boolean? = null
    override fun supports(node: TutorialNode): Boolean = node is ObtainBingoGoalTutorialStep
    override fun title(node: TutorialNode): String = "Edit 'Obtain Bingo Goal' Step"
    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as ObtainBingoGoalTutorialStep
        if (nameInput.textBox.isEmpty()) nameInput.textBox = step.displayName
        if (showOnActive == null) showOnActive = step.showOnActive
        val toggle = Renderable.clickable(
            "§7Show on Active: ${if (showOnActive == true) "§aYES" else "§cNO"}",
            tips = listOf("§7If off, won't appear as an alternative in Optional forks"),
            onLeftClick = { showOnActive = !(showOnActive ?: true) },
        )
        return listOf(
            Renderable.text("§fEdit 'Obtain Bingo Goal' Step"),
            Renderable.horizontal(spacing = 6) {
                add(Renderable.text("§7Goal Name: §f"))
                add(Renderable.textBox("", nameInput, 280, bypassChecks = true))
            },
            toggle,
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val newNode = ObtainBingoGoalTutorialStep(nameInput.finalText().trim(), showOnActive ?: true)
                    try { TutorialStepLogic.populateNodeIds(newNode, tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
    }
}
