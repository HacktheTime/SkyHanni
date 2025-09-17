package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.table.SearchableScrollTable.Companion.searchableScrollTable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.paths.AsyncTutorialFork
import de.hype.bingonet.shared.tutorials.paths.OptionalTutorialFork
import de.hype.bingonet.shared.tutorials.paths.SelectPathTutorialFork
import de.hype.bingonet.shared.tutorials.paths.TutorialFork
import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import de.hype.bingonet.shared.tutorials.paths.RequireAsyncCompletionTutorialStep

class RequireAsyncCompletionEditor : TutorialNodeEditor {
    private val nodeInput = TextInput()

    override fun supports(node: TutorialNode): Boolean = node is RequireAsyncCompletionTutorialStep

    override fun title(node: TutorialNode): String = "Edit 'Require Async Completion' Step"

    private fun collectNodes(tutorial: Tutorial): List<TutorialNode> {
        val acc = ArrayList<TutorialNode>()
        fun walk(nodes: List<TutorialNode>) {
            for (n in nodes) {
                acc.add(n)
                when (n) {
                    is AsyncTutorialFork -> walk(n.pathNodes)
                    is OptionalTutorialFork -> n.paths.forEach { (p, _) -> walk(p) }
                    is SelectPathTutorialFork -> n.paths.forEach { sp -> walk(sp.pathNodes) }
                    is TutorialFork -> walk(n.getNodes(tutorial))
                    else -> {}
                }
            }
        }
        walk(tutorial.steps)
        return acc
    }

    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val nodes = collectNodes(tutorial)
        val labelMap: Map<List<Renderable>, String?> = nodes.associate { n ->
            val name = (n as? TutorialStep)?.let { runCatching { it.getStepName(tutorial) }.getOrNull() } ?: n.javaClass.simpleName
            listOf(Renderable.text("§7• §f$name")) to n.nodeId
        }
        return listOf(
            Renderable.text("§fPick a node to wait for completion"),
            Renderable.searchableScrollTable(labelMap, height = 120, textInput = nodeInput, key = 341),
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val targetId = nodeInput.finalText().trim()
                    val target = nodes.firstOrNull { it.nodeId == targetId } ?: nodes.firstOrNull()
                    if (target != null) {
                        val newNode = RequireAsyncCompletionTutorialStep(target)
                        try { newNode.populateNodeIds(tutorial) } catch (_: Throwable) {}
                        onSave(newNode)
                    }
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
        )
    }
}
