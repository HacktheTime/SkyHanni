package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialStepLogic
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionDropdown
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionProviders
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.table.SearchableScrollTable.Companion.searchableScrollTable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.location.GoToPositionTutorialStep
class GoToPositionEditor : TutorialNodeEditor {
    private val islandInput = TextInput()
    private var allowSkip = true
    override fun supports(node: TutorialNode): Boolean = node is GoToPositionTutorialStep
    override fun title(node: TutorialNode): String = "Edit 'Go To Position' Step"
    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as GoToPositionTutorialStep
        if (islandInput.textBox.isEmpty()) islandInput.textBox = step.island.getDisplayName()
        allowSkip = step.allowSkip
        val content: Map<List<Renderable>, String?> = Islands.entries.associate { isl ->
            val label = Renderable.hoverTips(
                "§f${isl.getDisplayName()}",
                listOf("§7Key: §f${isl.name}", isl.warpArgument?.let { "§7Warp: §f/$it" } ?: "§7No warp"),
            )
            listOf(Renderable.text("§7• "), label) to isl.getDisplayName()
        }
        val toggle = Renderable.clickable(
            "§7Allow Skip: ${if (allowSkip) "§aYES" else "§cNO"}",
            tips = listOf("§7If off, user can't skip this step"),
            onLeftClick = { allowSkip = !allowSkip },
        )
        return listOf(
            Renderable.text("§fEdit 'Go To Position' Step"),
            Renderable.searchBox(
                content = Renderable.searchableScrollTable(content, height = 120, textInput = islandInput, key = 221),
                searchPrefix = "§7Filter: §f",
                onUpdateSize = {},
                textInput = islandInput,
                hideIfNoText = false,
                bypassChecks = true,
            ),
            SuggestionDropdown.below(islandInput, optionsProvider = { SuggestionProviders.filterContains(SuggestionProviders.islands(), islandInput.finalText()) }),
            toggle,
            Renderable.text("§8Positions are managed via routing and left empty here (internal routing will be used)."),
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val q = islandInput.finalText().trim().lowercase()
                    val pick = Islands.entries.firstOrNull { isl ->
                        isl.getDisplayName().lowercase().contains(q) || isl.name.lowercase().contains(q)
                    } ?: step.island
                    val newNode = GoToPositionTutorialStep(null, pick, allowSkip)
                    try { TutorialStepLogic.populateNodeIds(newNode, tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
    }
}
