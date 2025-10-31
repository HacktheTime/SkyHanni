package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialStepLogic
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionDropdown
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionProviders
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.TaggedItemCheck
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.itemstep.EnchantTutorialStep
class EnchantEditor : TutorialNodeEditor {
    private val tagInput = TextInput()
    private val enchantIdInput = TextInput()
    private val enchantLevelInput = TextInput()
    private var editMap: MutableMap<String, Int>? = null
    override fun supports(node: TutorialNode): Boolean = node is EnchantTutorialStep
    override fun title(node: TutorialNode): String = "Edit 'Enchant' Step"
    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as EnchantTutorialStep
        if (tagInput.textBox.isEmpty()) tagInput.textBox = step.item.tag
        if (editMap == null) editMap = step.enchantIds.toMutableMap()
        val map = editMap ?: mutableMapOf()
        val list = mutableListOf<Renderable>()
        list += Renderable.text("§fEdit 'Enchant' Step")
        list += Renderable.searchBox(Renderable.text(""), "§7Tag Name: §f", {}, tagInput, hideIfNoText = false, bypassChecks = true)
        list += SuggestionDropdown.below(tagInput, optionsProvider = { SuggestionProviders.filterContains(SuggestionProviders.tagNames() + SuggestionProviders.tagNamesFromSteps(tutorial), tagInput.finalText()) })
        // Current enchants overview
        if (map.isNotEmpty()) {
            list += Renderable.text("§7Current Enchants:")
            map.entries.sortedBy { it.key }.forEach { (id, lvl) ->
                list += Renderable.horizontal(spacing = 8) {
                    add(Renderable.text("§7- §f$id §8→ §f$lvl"))
                    add(Renderable.clickable("§cRemove", onLeftClick = { map.remove(id) }))
                }
            }
        } else {
            list += Renderable.text("§8(no enchants yet)")
        }
        // Add/update row
        list += Renderable.text(" ")
        list += Renderable.searchBox(Renderable.text(""), "§7Enchant ID: §f", {}, enchantIdInput, hideIfNoText = false, bypassChecks = true)
        list += SuggestionDropdown.below(enchantIdInput, optionsProvider = { SuggestionProviders.filterContains(SuggestionProviders.enchantIdsFromTutorial(tutorial), enchantIdInput.finalText()) })
        list += Renderable.searchBox(Renderable.text(""), "§7Level: §f", {}, enchantLevelInput, hideIfNoText = false, bypassChecks = true)
        list += Renderable.clickable("§aAdd/Update Enchant", onLeftClick = {
            val id = enchantIdInput.finalText().trim()
            val lvl = enchantLevelInput.finalText().trim().toIntOrNull()
            if (id.isNotEmpty() && lvl != null) {
                editMap?.put(id, lvl)
                enchantIdInput.textBox = ""
                enchantLevelInput.textBox = ""
        })
        list += Renderable.horizontal(spacing = 8) {
            add(Renderable.clickable("§aSave", onLeftClick = {
                val newNode = EnchantTutorialStep(
                    TaggedItemCheck(step.item.displayText, step.item.descriptionText, tagInput.finalText().trim()),
                    (editMap ?: emptyMap()).toMap(),
                )
                try { TutorialStepLogic.populateNodeIds(newNode, tutorial) } catch (_: Throwable) {}
                onSave(newNode)
                editMap = null
            }))
            add(Renderable.clickable("§cCancel", onLeftClick = { onCancel(); editMap = null }))
        return list
    }
}
