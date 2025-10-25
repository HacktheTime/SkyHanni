package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.location.SubAreaTutorialStep
class SubAreaEditor : TutorialNodeEditor {
    private val areaInput = TextInput()
    override fun supports(node: TutorialNode): Boolean = node is SubAreaTutorialStep
    override fun title(node: TutorialNode): String = "Edit 'Sub-Area' Step"
    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as SubAreaTutorialStep
        if (areaInput.textBox.isEmpty()) areaInput.textBox = step.area
        return listOf(
            Renderable.text("§fEdit 'Sub-Area' Step"),
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Area Name: §f")); add(Renderable.textBox("", areaInput, 280, bypassChecks = true)) },
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val newNode = SubAreaTutorialStep(areaInput.finalText().trim())
                    try { TutorialStepLogic.populateNodeIds(newNode, tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
        )
    }
}
