package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialStepLogic
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionDropdown
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.constants.Rarity
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.misc.EquipPetTutorialStep
import at.hannibal2.skyhanni.utils.NeuInternalName
class EquipPetEditor : TutorialNodeEditor {
    private val petInternalInput = TextInput()
    private val rarityInput = TextInput()
    override fun supports(node: TutorialNode): Boolean = node is EquipPetTutorialStep
    override fun title(node: TutorialNode): String = "Edit 'Equip Pet' Step"
    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as EquipPetTutorialStep
        if (petInternalInput.textBox.isEmpty()) petInternalInput.textBox = step.petType.internalName
        if (rarityInput.textBox.isEmpty()) rarityInput.textBox = step.minimumPetRarity.name
        val rarities = Rarity.entries.map { it.name }
        return listOf(
            Renderable.text("§fEdit 'Equip Pet' Step"),
            Renderable.horizontal(spacing = 6) {
                add(Renderable.text("§7Pet Internal: §f"))
                add(Renderable.textBox("", petInternalInput, 220, bypassChecks = true))
            },
                add(Renderable.text("§7Minimum Rarity: §f"))
                add(Renderable.textBox("", rarityInput, 160, bypassChecks = true))
            SuggestionDropdown.below(rarityInput) { rarities.filter { it.contains(rarityInput.finalText(), ignoreCase = true) }.take(10) },
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val pet = petInternalInput.finalText().trim().toInternalName()
                    val rarity = runCatching { Rarity.valueOf(rarityInput.finalText().trim().uppercase()) }.getOrElse { step.minimumPetRarity }
                    val newNode = EquipPetTutorialStep(pet, rarity)
                    try { TutorialStepLogic.populateNodeIds(newNode, tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
        )
    }
}
