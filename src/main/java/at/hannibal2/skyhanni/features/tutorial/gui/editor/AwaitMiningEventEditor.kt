package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionDropdown
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.table.SearchableScrollTable.Companion.searchableScrollTable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.constants.MiningEvents
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.AwaitMiningEvent
class AwaitMiningEventEditor : TutorialNodeEditor {
    private val eventInput = TextInput()
    private val islandInput = TextInput()
    private var anyIsland = false
    override fun supports(node: TutorialNode): Boolean = node is AwaitMiningEvent
    override fun title(node: TutorialNode): String = "Edit 'Await Mining Event' Step"
    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as AwaitMiningEvent
        if (eventInput.textBox.isEmpty()) eventInput.textBox = step.event.displayName
        anyIsland = step.islands == null
        if (!anyIsland && islandInput.textBox.isEmpty()) islandInput.textBox = step.islands?.getDisplayName().orEmpty()
        val eventOptions: Map<List<Renderable>, String?> = MiningEvents.entries.associate { e ->
            listOf(Renderable.text("§7• §f${e.displayName}")) to e.displayName
        }
        val islandOptions: Map<List<Renderable>, String?> = Islands.entries.associate { isl ->
            listOf(Renderable.text("§7• §f${isl.getDisplayName()}")) to isl.getDisplayName()
        val toggleAny = Renderable.clickable(
            "§7Any Island: ${if (anyIsland) "§aYES" else "§cNO"}",
            tips = listOf("§7If YES, event can happen on any island"),
            onLeftClick = { anyIsland = !anyIsland },
        )
        return listOf(
            Renderable.text("§fEdit 'Await Mining Event' Step"),
            Renderable.searchBox(
                content = Renderable.searchableScrollTable(eventOptions, height = 90, textInput = eventInput, key = 301, xSpacing = 2, ySpacing = 0),
                searchPrefix = "§7Event: §f",
                onUpdateSize = {},
                textInput = eventInput,
                hideIfNoText = false,
                ySpacing = 4,
                bypassChecks = true,
            ),
            SuggestionDropdown.below(eventInput) { MiningEvents.entries.map { it.displayName }.filter { it.contains(eventInput.finalText(), ignoreCase = true) }.take(10) },
            toggleAny,
            Renderable.horizontal(spacing = 6) {
                add(Renderable.text("§7Island: §f"))
                add(Renderable.searchableScrollTable(islandOptions, height = 90, textInput = islandInput, key = 302))
            },
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val event = MiningEvents.getByDisplayName(eventInput.finalText().trim()) ?: step.event
                    val isl = if (anyIsland) null else Islands.entries.firstOrNull { it.getDisplayName().equals(islandInput.finalText().trim(), true) } ?: step.islands
                    val newNode = AwaitMiningEvent(event, isl)
                    try { TutorialStepLogic.populateNodeIds(newNode, tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
    }
}
