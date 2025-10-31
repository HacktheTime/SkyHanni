package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialStepLogic
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.misc.DailyEnchantingXpTutorialStep
class DailyEnchantingXpEditor : TutorialNodeEditor {
    override fun supports(node: TutorialNode): Boolean = node is DailyEnchantingXpTutorialStep
    override fun title(node: TutorialNode): String = "Edit 'Daily Enchanting XP' Step"
    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        return listOf(
            Renderable.text("§fThis step has no parameters."),
            Renderable.text(" "),
            Renderable.clickable("§aSave", onLeftClick = { onSave(DailyEnchantingXpTutorialStep().also { try { TutorialStepLogic.populateNodeIds(it, tutorial) } catch (_: Throwable) {} }) }),
            Renderable.clickable("§cCancel", onLeftClick = { onCancel() }),
        )
    }
}
