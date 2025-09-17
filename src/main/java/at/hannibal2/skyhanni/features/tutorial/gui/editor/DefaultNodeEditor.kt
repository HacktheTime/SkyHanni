package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode

/** Fallback editor when no specific editor exists. */
class DefaultNodeEditor : TutorialNodeEditor {
    override fun supports(node: TutorialNode): Boolean = true

    override fun title(node: TutorialNode): String = "Edit Node"

    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> = listOf(
        Renderable.text("§7No editor available for this node type."),
        Renderable.text("§8You can still duplicate, delete, or move it from the list."),
        Renderable.text(" "),
        Renderable.clickable("§cClose", onLeftClick = { onCancel() }),
    )
}
