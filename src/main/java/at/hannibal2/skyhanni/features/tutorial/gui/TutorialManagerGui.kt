package at.hannibal2.skyhanni.features.tutorial.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.tutorial.TutorialOverlayDisplay
import at.hannibal2.skyhanni.features.tutorial.TutorialManager
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyhanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.paths.AsyncTutorialFork
import de.hype.bingonet.shared.tutorials.paths.OptionalTutorialFork
import de.hype.bingonet.shared.tutorials.paths.SelectPathTutorialFork
import de.hype.bingonet.shared.tutorials.paths.TutorialFork
import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import de.hype.bingonet.shared.tutorials.steps.itemstep.EnchantTutorialStep
import de.hype.bingonet.shared.tutorials.steps.itemstep.ReforgeTutorialStep
import de.hype.bingonet.shared.tutorials.steps.itemstep.TagItemTutorialStep
import de.hype.bingonet.shared.tutorials.steps.location.GoToIslandTutorialStep
import de.hype.bingonet.shared.tutorials.steps.location.GoToPositionTutorialStep

/**
 * Interactive manager GUI for Tutorials: shows the tutorial tree, lets you select paths, skip goals,
 * toggle description/hidden paths, and view node details. Overlay stays in sync.
 */
class TutorialManagerGui : SkyhanniBaseScreen() {

    private val config get() = SkyHanniMod.feature.tutorials

    private val sizeX = 520
    private val sizeY = 300

    private var selectedNodeId: String? = null

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val guiLeft = (width - sizeX) / 2
        val guiTop = (height - sizeY) / 2
        val mx = mouseX - guiLeft
        val my = mouseY - guiTop

        DrawContextUtils.pushMatrix()
        try {
            DrawContextUtils.translate(guiLeft.toFloat(), guiTop.toFloat(), 0f)
            GuiRenderUtils.drawRect(0, 0, sizeX, sizeY, 0x80000000.toInt())

            val tutorial = TutorialManager.activeTutorial

            val leftPanel = buildLeftPanel(tutorial)
            val rightPanel = buildRightPanel(tutorial)

            val content = Renderable.horizontal(spacing = 10) {
                add(leftPanel)
                add(rightPanel)
            }

            Renderable.withMousePosition(mx, my) {
                content.renderXYAligned(8, 8, sizeX - 16, sizeY - 16)
            }
        } finally {
            DrawContextUtils.popMatrix()
        }
    }

    private fun buildLeftPanel(tutorial: Tutorial?): Renderable {
        val list = mutableListOf<Renderable>()
        list.add(Renderable.text("§b§lTutorial Manager"))
        list.add(
            Renderable.clickable(
                "§eShow Descriptions: §f${if (config.showStepDescriptions) "§aON" else "§cOFF"}",
                onLeftClick = {
                    config.showStepDescriptions = !config.showStepDescriptions
                    TutorialOverlayDisplay.markDirty()
                },
            ),
        )
        list.add(
            Renderable.clickable(
                "§eShow Hidden Optional Paths: §f${if (config.showHiddenOptionalPaths) "§aON" else "§cOFF"}",
                onLeftClick = {
                    config.showHiddenOptionalPaths = !config.showHiddenOptionalPaths
                    TutorialOverlayDisplay.markDirty()
                },
            ),
        )
        list.add(
            Renderable.clickable(
                "§cClose",
                tips = listOf("§7Click to close"),
                onLeftClick = { mc.displayGuiScreen(null) },
            ),
        )
        list.add(Renderable.text(" "))

        if (tutorial != null) {
            list.add(buildTree(tutorial))
        } else {
            list.add(Renderable.text("§7No active tutorial."))
        }
        return Renderable.vertical(list, spacing = 2)
    }

    private fun buildRightPanel(tutorial: Tutorial?): Renderable {
        val rightPanel = mutableListOf<Renderable>()
        rightPanel.add(Renderable.text("§b§lDetails"))
        val selectedNode = tutorial?.let { t -> selectedNodeId?.let { t.getNodeReference(it) } }
        if (tutorial == null) {
            rightPanel.add(Renderable.text("§7No tutorial loaded."))
        } else if (selectedNode == null) {
            rightPanel.add(Renderable.text("§7Select a node on the left to view details."))
        } else {
            rightPanel.addAll(buildDetails(tutorial, selectedNode))
        }
        return Renderable.vertical(rightPanel, spacing = 2)
    }

    // Build a clickable tree of tutorial nodes with indentation and special-case markers.
    private fun buildTree(tutorial: Tutorial): Renderable {
        val items = mutableListOf<Renderable>()
        val header = mutableListOf<Renderable>()
        header.add(Renderable.text("§f§l${tutorial.name}"))
        if (tutorial.description.isNotBlank()) header.add(Renderable.text("§7${tutorial.description}"))
        items.add(Renderable.vertical(header, spacing = 1))
        items.addAll(buildNodesList(tutorial, tutorial.steps, indent = 0, markNextAsRequiresAsync = false))
        return Renderable.vertical(items, spacing = 1)
    }

    private fun buildNodesList(
        tutorial: Tutorial,
        nodes: List<TutorialNode>,
        indent: Int,
        markNextAsRequiresAsync: Boolean,
    ): List<Renderable> {
        val lines = mutableListOf<Renderable>()
        var pendingAsync = false
        nodes.forEach { node ->
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
                    val line = lineWithIndent(indent) {
                        listOf(
                            Renderable.text(bullet),
                            Renderable.text(" §f$name$recSuffix"),
                        )
                    }
                    lines.add(Renderable.clickable(line, onLeftClick = { selectedNodeId = node.nodeId }))
                    if (config.showStepDescriptions) {
                        val d = safeStepDesc(node, tutorial)
                        if (!d.isNullOrBlank()) {
                            lines.add(lineWithIndent(indent + 1) { listOf(Renderable.text("§7$d")) })
                        }
                    }
                    pendingAsync = false
                }

                is TutorialFork -> {
                    when (node) {
                        is AsyncTutorialFork -> {
                            val headerText = "§6⟳ §e${node.getHeader(tutorial)}"
                            val hdr = lineWithIndent(indent) { listOf(Renderable.text(headerText)) }
                            lines.add(Renderable.clickable(hdr, onLeftClick = { selectedNodeId = node.nodeId }))
                            val subs = node.getNodes(tutorial)
                            lines.addAll(buildNodesList(tutorial, subs, indent + 1, markNextAsRequiresAsync))
                            pendingAsync = true
                        }

                        is OptionalTutorialFork -> {
                            val headerText = "§7◇ §8${node.getHeader(tutorial)}"
                            val hdr = lineWithIndent(indent) { listOf(Renderable.text(headerText)) }
                            lines.add(Renderable.clickable(hdr, onLeftClick = { selectedNodeId = node.nodeId }))
                            val furthest = node.paths.maxBy { pair -> pair.first.count { it.isComplete(tutorial) } }.first
                            node.paths.forEach { (path, hidden) ->
                                if (!hidden || config.showHiddenOptionalPaths) {
                                    val isFurthest = path === furthest
                                    val pathHeader = if (isFurthest) "§a▶ Furthest option" else "§7– Option"
                                    lines.add(lineWithIndent(indent + 1) { listOf(Renderable.text(pathHeader)) })
                                    lines.addAll(buildNodesList(tutorial, path, indent + 2, markNextAsRequiresAsync))
                                }
                            }
                        }

                        is SelectPathTutorialFork -> {
                            val selected = tutorial.getSelectedOption(node.paths)
                            if (selected == null) {
                                val headerText = "§e⌛ §6Waiting for path selection"
                                val hdr = lineWithIndent(indent) { listOf(Renderable.text(headerText)) }
                                lines.add(Renderable.clickable(hdr, onLeftClick = { selectedNodeId = node.nodeId }))
                                node.paths.forEach { opt ->
                                    val optText = "§e• §f${opt.option.name} §7(click to choose)"
                                    val row = lineWithIndent(indent + 1) { listOf(Renderable.text(optText)) }
                                    lines.add(
                                        Renderable.clickable(
                                            row,
                                            onLeftClick = {
                                                tutorial.selectPath(opt.option.id)
                                                TutorialOverlayDisplay.markDirty()
                                                selectedNodeId = node.nodeId
                                            },
                                        ),
                                    )
                                }
                            } else {
                                val headerText = "§a◆ §2Selected Path: §f${selected.option.name}"
                                val hdr = lineWithIndent(indent) { listOf(Renderable.text(headerText)) }
                                lines.add(Renderable.clickable(hdr, onLeftClick = { selectedNodeId = node.nodeId }))
                                lines.addAll(buildNodesList(tutorial, selected.pathNodes, indent + 1, markNextAsRequiresAsync))
                            }
                        }

                        else -> {
                            val headerText = "§9§l${node.getHeader(tutorial)}"
                            val hdr = lineWithIndent(indent) { listOf(Renderable.text(headerText)) }
                            lines.add(Renderable.clickable(hdr, onLeftClick = { selectedNodeId = node.nodeId }))
                            val subs = node.getNodes(tutorial)
                            lines.addAll(buildNodesList(tutorial, subs, indent + 1, markNextAsRequiresAsync))
                        }
                    }
                }
            }
        }
        return lines
    }

    private fun lineWithIndent(indent: Int, children: () -> List<Renderable>): Renderable {
        val prefix = "  ".repeat(indent)
        val pref = Renderable.text(prefix)
        return Renderable.horizontal { add(pref); children().forEach { add(it) } }
    }

    private fun safeStepName(step: TutorialStep, tutorial: Tutorial): String = try {
        step.getStepName(tutorial)
    } catch (_: Throwable) {
        "Step"
    }

    private fun safeStepDesc(step: TutorialStep, tutorial: Tutorial): String? = try {
        step.getStepDescription(tutorial)
    } catch (_: Throwable) {
        null
    }

    private fun buildDetails(tutorial: Tutorial, node: TutorialNode): List<Renderable> {
        val out = mutableListOf<Renderable>()
        when (node) {
            is TutorialStep -> {
                out.add(Renderable.text("§f${safeStepName(node, tutorial)}"))
                val desc = safeStepDesc(node, tutorial)
                if (!desc.isNullOrBlank()) out.add(Renderable.text("§7$desc"))
                out.add(Renderable.text(" "))
                if (!node.completed) {
                    out.add(
                        Renderable.clickable("§cSkip this step", onLeftClick = {
                            tutorial.skipNode(node)
                            TutorialOverlayDisplay.markDirty()
                        }),
                    )
                } else {
                    out.add(Renderable.text("§aAlready completed"))
                }
                // Special-case details for known step types
                when (node) {
                    is ReforgeTutorialStep -> {
                        out.add(Renderable.text("§eType: §fReforge"))
                        out.add(Renderable.text("§7Tagged Item: §f${node.item.tag}"))
                        out.add(Renderable.text("§7Wanted Reforge: §f${node.reforgeInternalName}"))
                    }
                    is EnchantTutorialStep -> {
                        out.add(Renderable.text("§eType: §fEnchant"))
                        out.add(Renderable.text("§7Tagged Item: §f${node.item.tag}"))
                        if (node.enchantIds.isNotEmpty()) {
                            out.add(Renderable.text("§7Wanted Enchants:"))
                            node.enchantIds.entries.sortedBy { it.key }.forEach { (id, lvl) ->
                                out.add(Renderable.text("§7 - §f$id §8→ §f$lvl"))
                            }
                        }
                    }
                    is TagItemTutorialStep -> {
                        out.add(Renderable.text("§eType: §fTag Item"))
                        out.add(Renderable.text("§7Tag: §f${node.tagName}"))
                    }
                    is GoToIslandTutorialStep -> {
                        out.add(Renderable.text("§eType: §fGo To Island"))
                        out.add(Renderable.text("§7Island: §f${node.island}"))
                    }
                    is GoToPositionTutorialStep -> {
                        out.add(Renderable.text("§eType: §fGo To Position"))
                        out.add(Renderable.text("§7Island: §f${node.island}"))
                        val count = node.node?.size ?: 0
                        out.add(Renderable.text("§7Route nodes: §f$count"))
                    }
                }
            }

            is SelectPathTutorialFork -> {
                out.add(Renderable.text("§f${node.getHeader(tutorial)}"))
                val selected = tutorial.getSelectedOption(node.paths)
                if (selected == null) {
                    out.add(Renderable.text("§6Waiting for path selection"))
                    node.paths.forEach { opt ->
                        out.add(
                            Renderable.clickable("§eChoose: §f${opt.option.name}", onLeftClick = {
                                tutorial.selectPath(opt.option.id)
                                TutorialOverlayDisplay.markDirty()
                            }),
                        )
                    }
                } else {
                    out.add(Renderable.text("§aSelected: §f${selected.option.name}"))
                }
            }

            is OptionalTutorialFork -> {
                out.add(Renderable.text("§f${node.getHeader(tutorial)}"))
                out.add(Renderable.text("§7Both options shown; furthest path is highlighted in the list."))
                out.add(Renderable.text("§7Hidden options are ${if (config.showHiddenOptionalPaths) "§aVISIBLE" else "§cHIDDEN"}."))
            }

            is AsyncTutorialFork -> {
                out.add(Renderable.text("§f${node.getHeader(tutorial)}"))
                out.add(Renderable.text("§6This fork runs asynchronously; its sub-steps progress in parallel."))
                out.add(Renderable.text("§7The next main step is marked as needing async completion."))
            }

            is TutorialFork -> {
                out.add(Renderable.text("§f${node.getHeader(tutorial)}"))
            }
        }
        out.add(Renderable.text(" "))
        out.add(
            Renderable.clickable("§7Clear selection", onLeftClick = {
                selectedNodeId = null
            }),
        )
        return out
    }
}
