package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialStepLogic
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.guisteps.GuiItemTutorialStep
import java.util.regex.Pattern
class GuiItemEditor : TutorialNodeEditor {
    private val guiRegexInput = TextInput()
    private val indexInput = TextInput()
    private val hasRegexInput = TextInput()
    private val doesntRegexInput = TextInput()
    private val descriptionInput = TextInput()
    override fun supports(node: TutorialNode): Boolean = node is GuiItemTutorialStep
    override fun title(node: TutorialNode): String = "Edit 'GUI Item' Step"
    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as GuiItemTutorialStep
        if (guiRegexInput.textBox.isEmpty()) guiRegexInput.textBox = step.guiName.pattern()
        if (indexInput.textBox.isEmpty()) indexInput.textBox = step.itemIndex.toString()
        if (hasRegexInput.textBox.isEmpty()) hasRegexInput.textBox = step.has?.pattern.orEmpty()
        if (doesntRegexInput.textBox.isEmpty()) doesntRegexInput.textBox = step.doesntHave?.pattern.orEmpty()
        if (descriptionInput.textBox.isEmpty()) descriptionInput.textBox = step.description.orEmpty()
        return listOf(
            Renderable.text("§fEdit 'GUI Item' Step"),
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7GUI Regex: §f")); add(Renderable.textBox("", guiRegexInput, 300, bypassChecks = true)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Item Index: §f")); add(Renderable.textBox("", indexInput, 100, bypassChecks = true)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Must Match (Regex): §f")); add(Renderable.textBox("", hasRegexInput, 300, bypassChecks = true)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Disallow (Regex): §f")); add(Renderable.textBox("", doesntRegexInput, 300, bypassChecks = true)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Description: §f")); add(Renderable.textBox("", descriptionInput, 300, bypassChecks = true)) },
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val gui = runCatching { Pattern.compile(guiRegexInput.finalText().trim()) }.getOrElse { step.guiName }
                    val idx = indexInput.finalText().trim().filter { it.isDigit() }.toIntOrNull() ?: step.itemIndex
                    val has = hasRegexInput.finalText().trim().takeIf { it.isNotEmpty() }?.let { runCatching { Regex(it) }.getOrNull() }
                    val doesnt = doesntRegexInput.finalText().trim().takeIf { it.isNotEmpty() }?.let { runCatching { Regex(it) }.getOrNull() }
                    val desc = descriptionInput.finalText().trim().ifEmpty { null }
                    val newNode = GuiItemTutorialStep(gui, idx, has, doesnt, desc)
                    try { TutorialStepLogic.populateNodeIds(newNode, tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
        )
    }
}
