package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionDropdown
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionProviders
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.table.SearchableScrollTable.Companion.searchableScrollTable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.itemstep.TagItemTutorialStep

class TagItemEditor : TutorialNodeEditor {
    private val tagInput = TextInput()
    private val explInput = TextInput()

    override fun supports(node: TutorialNode): Boolean = node is TagItemTutorialStep

    override fun title(node: TutorialNode): String = "Edit Tag Item Step"

    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as TagItemTutorialStep
        if (tagInput.textBox.isEmpty()) tagInput.textBox = step.tagName
        if (explInput.textBox.isEmpty()) explInput.textBox = step.explenation

        val tags = ProfileStorageData.profileSpecific?.itemTags?.keys?.sorted().orEmpty()
        val content: Map<List<Renderable>, String?> = tags.associate { tag -> listOf(Renderable.text("§7• §f$tag")) to tag }

        return listOf(
            Renderable.text("§fEdit 'Tag Item' Step"),
            Renderable.searchBox(
                content = Renderable.searchableScrollTable(content, height = 80, textInput = tagInput, key = 201),
                searchPrefix = "§7Tag Name: §f",
                onUpdateSize = {},
                textInput = tagInput,
                hideIfNoText = false,
                bypassChecks = true,
            ),
            SuggestionDropdown.below(tagInput) { SuggestionProviders.filterContains(SuggestionProviders.tagNames() + SuggestionProviders.tagNamesFromSteps(tutorial), tagInput.finalText()) },
            Renderable.searchBox(Renderable.text(""), "§7Explanation: §f", {}, explInput, hideIfNoText = false, bypassChecks = true),
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(
                    Renderable.clickable(
                        "§aSave",
                        onLeftClick = {
                            val newNode = TagItemTutorialStep(tagInput.finalText().trim(), explInput.finalText().trim())
                            try {
                                newNode.populateNodeIds(tutorial)
                            } catch (_: Throwable) {
                            }
                            onSave(newNode)
                        },
                    ),
                )
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
        )
    }
}
