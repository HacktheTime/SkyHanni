package de.hype.bingonet.shared.tutorials

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.paths.AsyncTutorialFork
import de.hype.bingonet.shared.tutorials.paths.OptionalTutorialFork
import de.hype.bingonet.shared.tutorials.paths.SelectPathTutorialFork
import de.hype.bingonet.shared.tutorials.paths.TutorialFork
import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import java.awt.Color

/**
 * Builds a Renderable representing the current state of a Tutorial.
 * Uses SkyHanni Renderable overlay widgets for easy registration and movement.
 */
internal object TutorialRenderableBuilder {

    /** Whether hidden optional paths should be shown; toggled via GUI. */
    @JvmStatic
    var showHiddenOptionalPaths: Boolean = false

    /** Whether descriptions for steps should be shown. Toggled via overlay or config. */
    @JvmStatic
    var showDescriptions: Boolean = true

    private val colHeader = Color(200, 200, 255)
    private val colAsync = Color(255, 200, 0)
    private val colRecommended = Color(0, 255, 180)
    private val colOptional = Color(180, 180, 180)
    private val colSelected = Color(120, 255, 120)

    fun build(tutorial: Tutorial, showDescriptions: Boolean): Renderable {
        this.showDescriptions = showDescriptions
        val content = buildNodesList(tutorial, tutorial.steps, indent = 0, markNextAsRequiresAsync = false)
        val header = Renderable.text("§b§lTutorial: §f${tutorial.name}")
        val desc = if (tutorial.description.isNotBlank()) Renderable.text("§7${tutorial.description}") else null
        val list = mutableListOf<Renderable>()
        list.add(header)
        if (desc != null) list.add(desc)

        // Protected resources section to help Sell Protection UI
        val cfg = SkyHanniMod.feature.tutorials
        val protected = tutorial.requiredResources
        if (cfg.tutorialProtectRequiredItems && protected.isNotEmpty()) {
            list.add(Renderable.text("§dProtected resources (sell-protect):"))
            protected.entries.take(8).forEach { (key, amount) ->
                list.add(Renderable.text("§7- §f${key.itemNameWithoutColor} §8x ${amount.toInt()}"))
            }
            if (protected.size > 8) {
                list.add(Renderable.text("§8… and ${protected.size - 8} more"))
            }
        }

        list.addAll(content)
        return Renderable.vertical(list)
    }

    private fun buildNodesList(
        tutorial: Tutorial,
        nodes: List<TutorialNode>,
        indent: Int,
        markNextAsRequiresAsync: Boolean,
    ): List<Renderable> {
        val lines = mutableListOf<Renderable>()
        var pendingAsync = false
        nodes.forEachIndexed { idx, node ->
            when (node) {
                is TutorialStep -> {
                    val completed = node.completed || node.isComplete(tutorial)
                    val active = node.isActive
                    val bullet = when {
                        completed -> "§a✔"
                        active -> "§e➤"
                        else -> "§7•"
                    }
                    val name = safeStepName(node, tutorial)
                    val recSuffix = if (markNextAsRequiresAsync || pendingAsync) " §b(§e⚑ needs async§b)" else ""
                    val line = renderIndented(indent) {
                        listOf(
                            Renderable.text("$bullet"),
                            Renderable.text(" §f$name$recSuffix"),
                        )
                    }
                    lines.add(line)
                    if (showDescriptions) {
                        val d = safeStepDesc(node, tutorial)
                        if (!d.isNullOrBlank()) {
                            lines.add(renderIndented(indent + 1) { listOf(Renderable.text("§7$d")) })
                        }
                    }
                    pendingAsync = false
                }

                is TutorialFork -> {
                    when (node) {
                        is AsyncTutorialFork -> {
                            // Do not show directly; mark the next main step as requiring async
                            // But still show its inner sub-steps under an async header so user can track progress
                            val headerText = "§6⟳ §e${node.getHeader(tutorial)}"
                            lines.add(renderIndented(indent) { listOf(Renderable.text(headerText)) })
                            // Show sub steps
                            val subs = node.getNodes(tutorial)
                            lines.addAll(buildNodesList(tutorial, subs, indent + 1, markNextAsRequiresAsync))
                            // Mark next non-async step as requiring async
                            pendingAsync = true
                        }

                        is OptionalTutorialFork -> {
                            val headerText = "§7◇ §8${node.getHeader(tutorial)}"
                            lines.add(renderIndented(indent) { listOf(Renderable.text(headerText)) })
                            // Determine furthest path
                            val furthest = node.paths.maxBy { pair -> pair.first.count { it.isComplete(tutorial) } }
                            node.paths.forEach { (path, hidden) ->
                                if (!hidden || showHiddenOptionalPaths) {
                                    val isFurthest = path === furthest.first
                                    val pathHeader = if (isFurthest) "§a▶ Furthest option" else "§7– Option"
                                    lines.add(renderIndented(indent + 1) { listOf(Renderable.text(pathHeader)) })
                                    lines.addAll(buildNodesList(tutorial, path, indent + 2, markNextAsRequiresAsync))
                                } else {
                                    // hidden path marker (not shown unless toggled)
                                }
                            }
                        }

                        is SelectPathTutorialFork -> {
                            val selected = tutorial.getSelectedOption(node.paths)
                            if (selected == null) {
                                val headerText = "§e⌛ §6Waiting for path selection"
                                lines.add(renderIndented(indent) { listOf(Renderable.text(headerText)) })
                                // Show options to pick (names only)
                                node.paths.forEach { opt ->
                                    val optText = "§e• §f${opt.option.name}"
                                    lines.add(renderIndented(indent + 1) { listOf(Renderable.text(optText)) })
                                }
                            } else {
                                val headerText = "§a◆ §2Selected Path: §f${selected.option.name}"
                                lines.add(renderIndented(indent) { listOf(Renderable.text(headerText)) })
                                lines.addAll(buildNodesList(tutorial, selected.pathNodes, indent + 1, markNextAsRequiresAsync))
                            }
                        }

                        else -> {
                            val headerText = "§9§l${node.getHeader(tutorial)}"
                            lines.add(renderIndented(indent) { listOf(Renderable.text(headerText)) })
                            val subs = node.getNodes(tutorial)
                            lines.addAll(buildNodesList(tutorial, subs, indent + 1, markNextAsRequiresAsync))
                        }
                    }
                }
            }
        }
        return lines
    }

    private fun renderIndented(indent: Int, children: () -> List<Renderable>): Renderable {
        val prefix = "  ".repeat(indent)
        val prefixRenderable = Renderable.text(prefix)
        return Renderable.horizontal(listOf(prefixRenderable) + children(), 0)
    }

    private fun safeStepName(step: TutorialStep, tutorial: Tutorial): String = try {
        step.getStepName(tutorial)
    } catch (e: Throwable) {
        "Step"
    }

    private fun safeStepDesc(step: TutorialStep, tutorial: Tutorial): String? = try {
        step.getStepDescription(tutorial)
    } catch (e: Throwable) {
        null
    }
}
