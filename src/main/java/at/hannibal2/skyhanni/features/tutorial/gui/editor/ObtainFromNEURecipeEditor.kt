package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import de.hype.bingonet.shared.tutorials.steps.TutorialStepLogic
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.storagestep.ObtainFromNEURecipe
import at.hannibal2.skyhanni.utils.NeuInternalName
/** Basic editor for ObtainFromNEURecipe: allows changing target item and required amount. */
class ObtainFromNEURecipeEditor : TutorialNodeEditor {
    private val itemInput = TextInput()
    private val amountInput = TextInput()
    override fun supports(node: TutorialNode): Boolean = node is ObtainFromNEURecipe
    override fun title(node: TutorialNode): String = "Edit 'Obtain From NEU Recipe' Step"
    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as ObtainFromNEURecipe
        if (itemInput.textBox.isEmpty()) itemInput.textBox = step.item.internalName
        if (amountInput.textBox.isEmpty()) amountInput.textBox = step.requiredAmount.toString()
        return listOf(
            Renderable.text("§fEdit 'Obtain From NEU Recipe' Step"),
            Renderable.horizontal(spacing = 6) {
                add(Renderable.text("§7Item internal name: §f"))
                add(Renderable.textBox("", itemInput, 300, bypassChecks = true))
            },
                add(Renderable.text("§7Required amount: §f"))
                add(Renderable.textBox("", amountInput, 80, bypassChecks = true))
            Renderable.text("§8Note: obtain mappings and recipe selection are preserved where possible."),
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val q = itemInput.finalText().trim()
                    //Use NeuItems since NeuInternalName is not a Enum.
                    val picked: NeuInternalName = q.toInternalName()
                    val amt = amountInput.finalText().trim().toIntOrNull() ?: step.requiredAmount
                    val newNode = ObtainFromNEURecipe(picked, amt, step.obtainMap, step.preferOnCurrentIsland)
                    try { TutorialStepLogic.populateNodeIds(newNode, tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
        )
    }
}
