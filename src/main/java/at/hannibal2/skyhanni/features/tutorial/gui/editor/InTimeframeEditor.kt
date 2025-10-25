package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.misc.InTimeframeTutorialStep
import java.time.Instant
class InTimeframeEditor : TutorialNodeEditor {
    private val startInput = TextInput()
    private val endInput = TextInput()
    override fun supports(node: TutorialNode): Boolean = node is InTimeframeTutorialStep
    override fun title(node: TutorialNode): String = "Edit 'In Timeframe' Step"
    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as InTimeframeTutorialStep
        if (startInput.textBox.isEmpty()) startInput.textBox = step.start.epochSecond.toString()
        if (endInput.textBox.isEmpty()) endInput.textBox = step.end.epochSecond.toString()
        return listOf(
            Renderable.text("§fEdit 'In Timeframe' Step"),
            Renderable.horizontal(spacing = 6) {
                add(Renderable.text("§7Start (epoch seconds): §f"))
                add(Renderable.textBox("", startInput, 200, bypassChecks = true))
            },
                add(Renderable.text("§7End (epoch seconds): §f"))
                add(Renderable.textBox("", endInput, 200, bypassChecks = true))
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val startSecs = startInput.finalText().trim().toLongOrNull() ?: step.start.epochSecond
                    val endSecs = endInput.finalText().trim().toLongOrNull() ?: step.end.epochSecond
                    val newNode = InTimeframeTutorialStep(Instant.ofEpochSecond(startSecs), Instant.ofEpochSecond(endSecs))
                    try { TutorialStepLogic.populateNodeIds(newNode, tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
        )
    }
}
