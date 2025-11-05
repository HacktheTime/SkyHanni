package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.misc.AwaitGodSplashTutorialStep
import kotlin.time.Duration.Companion.seconds

/** Simple editor to adjust the minimum God Splash duration. */
class AwaitGodSplashEditor : TutorialNodeEditor {
    private val durationInput = TextInput()

    override fun supports(node: TutorialNode): Boolean = node is AwaitGodSplashTutorialStep

    override fun title(node: TutorialNode): String = "Edit 'Await God Splash' Step"

    override fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable> {
        val step = node as AwaitGodSplashTutorialStep
        if (durationInput.textBox.isEmpty()) durationInput.textBox = step.minimumDuration.inWholeSeconds.toString()

        return listOf(
            Renderable.text("§fEdit 'Await God Splash' Step"),
            Renderable.horizontal(spacing = 6) {
                add(Renderable.text("§7Minimum seconds: §f"))
                add(Renderable.textBox("", durationInput, 120, bypassChecks = true))
            },
            Renderable.text(" "),
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aSave", onLeftClick = {
                    val secs = durationInput.finalText().trim().toLongOrNull() ?: step.minimumDuration.inWholeSeconds
                    val newNode = AwaitGodSplashTutorialStep(secs.seconds)
                    try { newNode.populateNodeIds(tutorial) } catch (_: Throwable) {}
                    onSave(newNode)
                }))
                add(Renderable.clickable("§cCancel", onLeftClick = { onCancel() }))
            },
        )
    }
}
