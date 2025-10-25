package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.guisteps.GuiClickSlotTutorialStep
import java.util.regex.Pattern
class GuiClickSlotEditor : TutorialNodeEditor {
    private val guiRegexInput = TextInput()
    private val indexInput = TextInput()
    private val descriptionInput = TextInput()
    override fun supports(node: TutorialNode): Boolean = node is GuiClickSlotTutorialStep
    override fun title(node: TutorialNode): String = "Edit 'GUI Click Slot' Step"
    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as GuiClickSlotTutorialStep
        if (guiRegexInput.textBox.isEmpty()) guiRegexInput.textBox = step.guiName.pattern()
        if (indexInput.textBox.isEmpty()) indexInput.textBox = step.slotIndex.toString()
        if (descriptionInput.textBox.isEmpty()) descriptionInput.textBox = step.description.orEmpty()
        return listOf(
            Renderable.text("§fEdit 'GUI Click Slot' Step"),
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7GUI Regex: §f")); add(Renderable.textBox("", guiRegexInput, 300, bypassChecks = true)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Slot Index: §f")); add(Renderable.textBox("", indexInput, 100, bypassChecks = true)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Description: §f")); add(Renderable.textBox("", descriptionInput, 300, bypassChecks = true)) },
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val gui = runCatching { Pattern.compile(guiRegexInput.finalText().trim()) }.getOrElse { step.guiName }
                    val idx = indexInput.finalText().trim().filter { it.isDigit() }.toIntOrNull() ?: step.slotIndex
                    val desc = descriptionInput.finalText().trim().ifEmpty { null }
                    val newNode = GuiClickSlotTutorialStep(gui, idx, desc)
                    try { TutorialStepLogic.populateNodeIds(newNode, tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
        )
    }
}
