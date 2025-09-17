package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionDropdown
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.constants.Skills
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.requirement.SkillTutorialStep

class SkillEditor : TutorialNodeEditor {
    private val skillInput = TextInput()
    private val levelInput = TextInput()

    override fun supports(node: TutorialNode): Boolean = node is SkillTutorialStep

    override fun title(node: TutorialNode): String = "Edit 'Skill Level' Step"

    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as SkillTutorialStep
        if (skillInput.textBox.isEmpty()) skillInput.textBox = step.skill.displayName
        if (levelInput.textBox.isEmpty()) levelInput.textBox = step.level.toString()

        val skills = Skills.entries.map { it.displayName }

        return listOf(
            Renderable.text("§fEdit 'Skill Level' Step"),
            Renderable.horizontal(spacing = 6) {
                add(Renderable.text("§7Skill: §f"))
                add(Renderable.textBox("", skillInput, 200, bypassChecks = true))
            },
            SuggestionDropdown.below(skillInput) { skills.filter { it.contains(skillInput.finalText(), ignoreCase = true) }.take(10) },
            Renderable.horizontal(spacing = 6) {
                add(Renderable.text("§7Level: §f"))
                add(Renderable.textBox("", levelInput, 120, bypassChecks = true))
            },
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val sk = Skills.entries.firstOrNull { it.displayName.equals(skillInput.finalText().trim(), true) } ?: step.skill
                    val lvl = levelInput.finalText().trim().filter { it.isDigit() }.toIntOrNull() ?: step.level
                    val newNode = SkillTutorialStep(sk, lvl)
                    try { newNode.populateNodeIds(tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
        )
    }
}
