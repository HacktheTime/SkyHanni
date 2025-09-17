package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.table.SearchableScrollTable.Companion.searchableScrollTable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.constants.Collections
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.requirement.CollectionLevelRequirement

class CollectionLevelRequirementEditor : TutorialNodeEditor {
    private val collectionInput = TextInput()
    private val minLevelInput = TextInput()
    private val requiredTotalInput = TextInput()

    override fun supports(node: TutorialNode): Boolean = node is CollectionLevelRequirement

    override fun title(node: TutorialNode): String = "Edit 'Collection Level' Requirement"

    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as CollectionLevelRequirement
        if (collectionInput.textBox.isEmpty()) collectionInput.textBox = step.collection.displayName
        if (minLevelInput.textBox.isEmpty()) minLevelInput.textBox = step.minLevel.toString()
        if (requiredTotalInput.textBox.isEmpty()) requiredTotalInput.textBox = step.requiredTotal?.toString().orEmpty()

        val collMap: Map<List<Renderable>, String?> = Collections.values.associate { c -> listOf(Renderable.text("§7• §f${c.displayName}")) to c.displayName }

        return listOf(
            Renderable.text("§fEdit 'Collection Level' Requirement"),
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Collection: §f")); add(Renderable.searchableScrollTable(collMap, 100, textInput = collectionInput, key = 361)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Min Level: §f")); add(Renderable.textBox("", minLevelInput, 100, bypassChecks = true)) },
            Renderable.horizontal(spacing = 6) { add(Renderable.text("§7Required Total (optional): §f")); add(Renderable.textBox("", requiredTotalInput, 140, bypassChecks = true)) },
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val collection = Collections.values.firstOrNull { it.displayName.equals(collectionInput.finalText().trim(), true) } ?:
                    step.collection
                    val minLevel = minLevelInput.finalText().trim().filter { it.isDigit() }.toIntOrNull() ?: step.minLevel
                    val total = requiredTotalInput.finalText().trim().filter { it.isDigit() }.toIntOrNull()
                    val newNode = CollectionLevelRequirement(collection, minLevel, total)
                    try { newNode.populateNodeIds(tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
        )
    }
}
