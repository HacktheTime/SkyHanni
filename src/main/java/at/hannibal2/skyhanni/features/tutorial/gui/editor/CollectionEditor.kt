package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.table.SearchableScrollTable.Companion.searchableScrollTable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.constants.Collections
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.CollectionTutorialStep

class CollectionEditor : TutorialNodeEditor {
    private val collectionInput = TextInput()
    private val amountInput = TextInput()
    private var forGoal = false

    override fun supports(node: TutorialNode): Boolean = node is CollectionTutorialStep

    override fun title(node: TutorialNode): String = "Edit 'Collection' Step"

    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as CollectionTutorialStep
        if (collectionInput.textBox.isEmpty()) collectionInput.textBox = step.collection.displayName
        if (amountInput.textBox.isEmpty()) amountInput.textBox = step.amount.toString()
        forGoal = step.forCollectionGoal

        val collMap: Map<List<Renderable>, String?> = Collections.entries.associate { c -> listOf(Renderable.text("§7• §f${c.displayName}")) to c.displayName }

        val toggle = Renderable.clickable(
            "§7For Collection Goal: ${if (forGoal) "§aYES" else "§cNO"}",
            tips = listOf("§7If set, count applies to collection goal context"),
            onLeftClick = { forGoal = !forGoal },
        )

        return listOf(
            Renderable.text("§fEdit 'Collection' Step"),
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Collection: §f")); add(Renderable.searchableScrollTable(collMap, 100, textInput = collectionInput, key = 351)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Amount: §f")); add(Renderable.textBox("", amountInput, 120, bypassChecks = true)) },
            toggle,
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val collection = Collections.entries.firstOrNull { it.displayName.equals(collectionInput.finalText().trim(), true) } ?: step.collection
                    val amt = amountInput.finalText().trim().filter { it.isDigit() }.toIntOrNull() ?: step.amount
                    val newNode = CollectionTutorialStep(collection, amt, forGoal)
                    try { newNode.populateNodeIds(tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
        )
    }
}
