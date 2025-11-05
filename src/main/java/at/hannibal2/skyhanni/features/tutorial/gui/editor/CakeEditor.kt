package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.misc.CakeTutorialStep
import kotlin.time.Duration.Companion.hours

class CakeEditor : TutorialNodeEditor {
    private val hoursInput = TextInput()

    override fun supports(node: TutorialNode): Boolean = node is CakeTutorialStep

    override fun title(node: TutorialNode): String = "Edit 'Century Cakes' Step"

    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as CakeTutorialStep
        if (hoursInput.textBox.isEmpty()) hoursInput.textBox = (step.minimumDuration.inWholeHours).toString()

        return listOf(
            Renderable.text("§fEdit 'Century Cakes' Step"),
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Min Duration (hours): §f")); add(Renderable.textBox("", hoursInput, 120, bypassChecks = true)) },
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val h = hoursInput.finalText().trim().filter { it.isDigit() }.toLongOrNull() ?: step.minimumDuration.inWholeHours
                    val newNode = CakeTutorialStep(h.hours)
                    try { newNode.populateNodeIds(tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
        )
    }
}
