package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.api.ReforgeApi
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionDropdown
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionProviders
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.table.SearchableScrollTable.Companion.searchableScrollTable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.TaggedItemCheck
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.itemstep.ReforgeTutorialStep

class ReforgeEditor : TutorialNodeEditor {
    private val tagInput = TextInput()
    private val reforgeInput = TextInput()

    override fun supports(node: TutorialNode): Boolean = node is ReforgeTutorialStep

    override fun title(node: TutorialNode): String = "Edit 'Reforge' Step"

    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as ReforgeTutorialStep
        if (tagInput.textBox.isEmpty()) tagInput.textBox = step.item.tag
        if (reforgeInput.textBox.isEmpty()) reforgeInput.textBox = step.reforgeName

        val reforges = try { ReforgeApi.reforges.map { it.name }.distinct().sorted() } catch (_: Throwable) { emptyList() }
        val content: Map<List<Renderable>, String?> = reforges.associate { name -> listOf(Renderable.text("§7• §f$name")) to name }

        return listOf(
            Renderable.text("§fEdit 'Reforge' Step"),
            Renderable.searchBox(Renderable.text(""), "§7Tag Name: §f", {}, tagInput, hideIfNoText = false, bypassChecks = true),
            SuggestionDropdown.below(tagInput) { SuggestionProviders.filterContains(SuggestionProviders.tagNames() + SuggestionProviders.tagNamesFromSteps(tutorial), tagInput.finalText()) },
            Renderable.searchBox(
                content = Renderable.searchableScrollTable(content, height = 100, textInput = reforgeInput, key = 231),
                searchPrefix = "§7Reforge: §f",
                onUpdateSize = {},
                textInput = reforgeInput,
                hideIfNoText = false,
                bypassChecks = true,
            ),
            SuggestionDropdown.below(reforgeInput) { SuggestionProviders.filterContains(SuggestionProviders.reforges(), reforgeInput.finalText()) },
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val newNode = ReforgeTutorialStep(
                        TaggedItemCheck(step.item.displayText, step.item.descriptionText, tagInput.finalText().trim()),
                        reforgeInput.finalText().trim(),
                    )
                    try { newNode.populateNodeIds(tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
        )
    }
}
