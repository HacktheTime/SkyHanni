package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionDropdown
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionProviders
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TextTutorialStep
class TextStepEditor : TutorialNodeEditor {
    private val nameInput = TextInput()
    private val descInput = TextInput()
    override fun supports(node: TutorialNode): Boolean = node is TextTutorialStep
    override fun title(node: TutorialNode): String = "Edit Text Step"
    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as TextTutorialStep
        if (nameInput.textBox.isEmpty()) nameInput.textBox = step.name
        if (descInput.textBox.isEmpty()) descInput.textBox = step.description
        return listOf(
            Renderable.text("§fEdit Text Step"),
            Renderable.searchBox(Renderable.text(""), "§7Title: §f", {}, nameInput, hideIfNoText = false, bypassChecks = true),
            SuggestionDropdown.below(nameInput) { SuggestionProviders.filterContains(SuggestionProviders.titlesFromTutorial(tutorial), nameInput.finalText()) },
            Renderable.searchBox(Renderable.text(""), "§7Description: §f", {}, descInput, hideIfNoText = false, bypassChecks = true),
            SuggestionDropdown.below(descInput) { SuggestionProviders.filterContains(SuggestionProviders.descriptionsFromTutorial(tutorial), descInput.finalText()) },
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val newNode = TextTutorialStep(nameInput.finalText(), descInput.finalText())
                    try { TutorialStepLogic.populateNodeIds(newNode, tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
        )
    }
}
