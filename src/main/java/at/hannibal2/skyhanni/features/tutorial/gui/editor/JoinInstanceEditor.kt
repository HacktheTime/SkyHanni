package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.constants.BasicCatacombsType
import de.hype.bingonet.shared.constants.MasterModCatacombsType
import de.hype.bingonet.shared.constants.KuudraType
import de.hype.bingonet.shared.constants.SkyblockInstance
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.location.JoinInstanceTutorialStep

/** Simple editor to pick a SkyblockInstance by text. Falls back to the existing value when not recognized. */
class JoinInstanceEditor : TutorialNodeEditor {
    private val instanceInput = TextInput()

    override fun supports(node: TutorialNode): Boolean = node is JoinInstanceTutorialStep

    override fun title(node: TutorialNode): String = "Edit 'Join Instance' Step"

    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as JoinInstanceTutorialStep
        if (instanceInput.textBox.isEmpty()) instanceInput.textBox = step.instance.displayName

        return listOf(
            Renderable.text("§fEdit 'Join Instance' Step"),
            Renderable.horizontal(spacing = 6) {
                add(Renderable.text("§7Instance (name or id): §f"))
                add(Renderable.textBox("", instanceInput, 280, bypassChecks = true))
            },
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val q = instanceInput.finalText().trim().lowercase()
                    val pick: SkyblockInstance? = listOf(
                        BasicCatacombsType.values().toList(),
                        MasterModCatacombsType.values().toList(),
                        KuudraType.values().toList(),
                    ).flatten().firstOrNull { inst ->
                        inst.displayName.lowercase().contains(q) || inst.joinId.lowercase().contains(q)
                    }
                    val newNode = JoinInstanceTutorialStep(pick ?: step.instance)
                    try { newNode.populateNodeIds(tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
        )
    }
}
