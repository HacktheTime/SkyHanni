package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionDropdown
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionProviders
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.MessageTutorialStep
import java.util.regex.Pattern

class MessageStepEditor : TutorialNodeEditor {
    private val nameInput = TextInput()
    private val descInput = TextInput()
    private val regexInput = TextInput()

    override fun supports(node: TutorialNode): Boolean = node is MessageTutorialStep

    override fun title(node: TutorialNode): String = "Edit 'Message' Step"

    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as MessageTutorialStep
        if (nameInput.textBox.isEmpty()) nameInput.textBox = step.name
        if (descInput.textBox.isEmpty()) descInput.textBox = step.description
        if (regexInput.textBox.isEmpty()) regexInput.textBox = step.criteria.pattern()

        val preview = runCatching { Pattern.compile(regexInput.finalText()) }.fold(
            onSuccess = { Renderable.text("§7Pattern: §avalid") },
            onFailure = { Renderable.text("§7Pattern: §cinvalid (${it.message})") },
        )

        return listOf(
            Renderable.text("§fEdit 'Message' Step"),
            Renderable.searchBox(Renderable.text(""), "§7Name: §f", {}, nameInput, hideIfNoText = false, bypassChecks = true),
            SuggestionDropdown.below(nameInput){ SuggestionProviders.filterContains(SuggestionProviders.titlesFromTutorial(tutorial), nameInput.finalText()) },
            Renderable.searchBox(Renderable.text(""), "§7Description: §f", {}, descInput, hideIfNoText = false, bypassChecks = true),
            SuggestionDropdown.below(descInput){
                SuggestionProviders.filterContains(SuggestionProviders.descriptionsFromTutorial(tutorial), descInput.finalText())
            },
            Renderable.searchBox(Renderable.text(""), "§7Regex: §f", {}, regexInput, hideIfNoText = false, bypassChecks = true),
            SuggestionDropdown.below(regexInput){
                SuggestionProviders.filterContains(SuggestionProviders.regexPatternsFromTutorial(tutorial), regexInput.finalText())
            },
            preview,
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val pattern = runCatching { Pattern.compile(regexInput.finalText()) }.getOrNull()
                    if (pattern != null) {
                        val newNode = MessageTutorialStep(pattern, nameInput.finalText(), descInput.finalText())
                        try { newNode.populateNodeIds(tutorial) } catch (_: Throwable) {}
                        onSave(newNode)
                    }
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
        )
    }
}
