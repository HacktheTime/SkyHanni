package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionDropdown
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionProviders
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.table.SearchableScrollTable.Companion.searchableScrollTable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.location.GoToIslandTutorialStep
class GoToIslandEditor : TutorialNodeEditor {
    private val islandInput = TextInput()
    override fun supports(node: TutorialNode): Boolean = node is GoToIslandTutorialStep
    override fun title(node: TutorialNode): String = "Edit 'Go To Island' Step"
    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as GoToIslandTutorialStep
        if (islandInput.textBox.isEmpty()) islandInput.textBox = step.island.getDisplayName()
        val content: Map<List<Renderable>, String?> = Islands.entries.associate { isl ->
            val label = Renderable.hoverTips(
                "§f${isl.getDisplayName()}",
                listOf("§7Key: §f${isl.name}", isl.warpArgument?.let { "§7Warp: §f/$it" } ?: "§7No warp"),
            )
            listOf(Renderable.text("§7• "), label) to isl.getDisplayName()
        }
        return listOf(
            Renderable.text("§fEdit 'Go To Island' Step"),
            Renderable.searchBox(
                content = Renderable.searchableScrollTable(content, height = 120, textInput = islandInput, key = 211),
                searchPrefix = "§7Filter: §f",
                onUpdateSize = {},
                textInput = islandInput,
                hideIfNoText = false,
                bypassChecks = true,
            ),
            SuggestionDropdown.below(islandInput, optionsProvider = { SuggestionProviders.filterContains(SuggestionProviders.islands(), islandInput.finalText()) }),
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val q = islandInput.finalText().trim().lowercase()
                    val pick = Islands.entries.firstOrNull { isl ->
                        isl.getDisplayName().lowercase().contains(q) || isl.name.lowercase().contains(q)
                    } ?: step.island
                    val newNode = GoToIslandTutorialStep(pick)
                    try { TutorialStepLogic.populateNodeIds(newNode, tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
        )
    }
}
