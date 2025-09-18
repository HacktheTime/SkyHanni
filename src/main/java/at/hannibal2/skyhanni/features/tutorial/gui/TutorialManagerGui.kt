package at.hannibal2.skyhanni.features.tutorial.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.features.tutorial.TutorialManager
import at.hannibal2.skyhanni.features.tutorial.TutorialOverlayDisplay
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyhanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.container.table.SearchableScrollTable.Companion.searchableScrollTable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.paths.AsyncTutorialFork
import de.hype.bingonet.shared.tutorials.paths.OptionalTutorialFork
import de.hype.bingonet.shared.tutorials.paths.SelectPathTutorialFork
import de.hype.bingonet.shared.tutorials.paths.TutorialFork
import de.hype.bingonet.shared.tutorials.steps.TextTutorialStep
import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import de.hype.bingonet.shared.tutorials.steps.itemstep.EnchantTutorialStep
import de.hype.bingonet.shared.tutorials.steps.itemstep.ReforgeTutorialStep
import de.hype.bingonet.shared.tutorials.steps.itemstep.TagItemTutorialStep
import de.hype.bingonet.shared.tutorials.steps.location.GoToIslandTutorialStep
import de.hype.bingonet.shared.tutorials.steps.location.GoToPositionTutorialStep
import at.hannibal2.skyhanni.features.tutorial.gui.editor.TutorialEditorRegistry
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionDropdown
import at.hannibal2.skyhanni.features.tutorial.gui.suggest.SuggestionProviders
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.renderables.interactables.DragItem
import at.hannibal2.skyhanni.utils.renderables.interactables.DragNDrop.draggable
import at.hannibal2.skyhanni.utils.renderables.interactables.DragNDrop.droppable
import at.hannibal2.skyhanni.utils.renderables.interactables.Droppable
import de.hype.bingonet.shared.tutorials.steps.storagestep.ObtainTutorialStep
import de.hype.bingonet.shared.tutorials.TaggedItemCheck
import de.hype.bingonet.shared.tutorials.steps.requirement.ObtainCoinsTutorialStep
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.OSUtils

class TutorialManagerGui : SkyhanniBaseScreen() {

    private val config get() = SkyHanniMod.feature.tutorials

    private val sizeX = 520
    private val sizeY = 300

    private var selectedNodeId: String? = null

    // Modes
    private var editTutorialMode = false
    private var createTutorialMode = false
    private var addNodeMode = false
    private var editNodeMode = false

    // Target for adding nodes (null=root; else forkId and path index)
    private var addTargetForkId: String? = null
    private var addTargetPathIndex: Int? = null

    // clipboard throttle
    private var lastClipboardActionMs: Long = 0L

    // Tutorial metadata inputs
    private val nameInput = TextInput()
    private val descInput = TextInput()
    private var profileDependentDraft: Boolean = false

    // Node creation selector
    private enum class NewNodeType { TEXT, GO_TO_ISLAND, TAG_ITEM, ASYNC_FORK }
    private var pendingNodeType: NewNodeType? = null

    // Node-specific inputs
    private val textStepNameInput = TextInput()
    private val textStepDescInput = TextInput()

    private val islandInput = TextInput()
    private val tagNameInput = TextInput()
    private val tagExplInput = TextInput()

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        // Global clipboard shortcuts (copy/paste tutorial JSON) when no text input is active
        try {
            if (!TextInput.isActive()) {
                // throttle repeated activation
                val now = System.currentTimeMillis()
                if (KeyboardManager.isCopyingKeysDown() && now - lastClipboardActionMs > 800) {
                    lastClipboardActionMs = now
                    val json = ProfileStorageData.profileSpecific?.tutorialManager?.exportActiveTutorialJson()
                    if (json != null) {
                        OSUtils.copyToClipboard(json)
                        ChatUtils.chat("§aCopied tutorial JSON to clipboard")
                    } else ChatUtils.chat("§cNo active tutorial to copy")
                }
                if (KeyboardManager.isPastingKeysDown() && now - lastClipboardActionMs > 800) {
                    lastClipboardActionMs = now
                    SkyHanniMod.launchCoroutine {
                        val clip = OSUtils.readFromClipboard() ?: ""
                        if (clip.isBlank()) { ChatUtils.chat("§cClipboard is empty"); return@launchCoroutine }
                        val ok = ProfileStorageData.profileSpecific?.tutorialManager?.importTutorialFromJson(clip) ?: false
                        if (ok) {
                            TutorialOverlayDisplay.markDirty()
                            SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "tutorial-paste")
                        }
                    }
                }
            }
        } catch (_: Throwable) {}

        val guiLeft = (width - sizeX) / 2
        val guiTop = (height - sizeY) / 2
        val xTranslate = guiLeft
        val yTranslate = guiTop

        // Draw background and a floating rectangle like other GUIs
        drawDefaultBackground(mouseX, mouseY, partialTicks)
        DrawContextUtils.translate(xTranslate - 2.0, yTranslate - 2.0, 0.0)
        GuiRenderUtils.drawFloatingRectDark(0, 0, sizeX, sizeY)
        DrawContextUtils.translate(-(xTranslate - 2.0), -(yTranslate - 2.0), 0.0)

        // Translate to inner content with a small top padding
        DrawContextUtils.translate(xTranslate.toFloat(), yTranslate.toFloat() + 5f, 0f)

        val tutorial = TutorialManager.activeTutorial
        val leftPanel = buildLeftPanel(tutorial)
        val rightPanel = buildRightPanel(tutorial)
        val content = Renderable.horizontal(spacing = 10) {
            add(leftPanel)
            add(rightPanel)
        }

        Renderable.withMousePosition(mouseX - xTranslate, mouseY - yTranslate) {
            content.renderXYAligned(0, 0, sizeX, sizeY)
        }

        DrawContextUtils.translate(-xTranslate.toFloat(), -yTranslate.toFloat() + -5f, 0f)
    }

    private fun buildLeftPanel(tutorial: Tutorial?): Renderable {
        val list = mutableListOf<Renderable>()
        list.add(Renderable.text("§b§lTutorial Manager"))
        list.add(
            Renderable.clickable(
                "§eShow Descriptions: §f${if (config.showStepDescriptions) "§aON" else "§cOFF"}",
                tips = listOf("§7Toggles description rendering in lists"),
                onLeftClick = {
                    config.showStepDescriptions = !config.showStepDescriptions
                    TutorialOverlayDisplay.markDirty()
                },
                bypassChecks = true,
            ),
        )
        list.add(
            Renderable.clickable(
                "§eShow Hidden Optional Paths: §f${if (config.showHiddenOptionalPaths) "§aON" else "§cOFF"}",
                tips = listOf("§7Reveal hidden options inside Optional forks"),
                onLeftClick = {
                    config.showHiddenOptionalPaths = !config.showHiddenOptionalPaths
                    TutorialOverlayDisplay.markDirty()
                },
                bypassChecks = true,
            ),
        )
        if (tutorial != null) {
            list.add(
                Renderable.clickable(
                    if (editTutorialMode) "§cExit Edit Mode" else "§aEdit Tutorial",
                    tips = listOf("§7Edit name/description and manage structure"),
                    onLeftClick = {
                        editTutorialMode = !editTutorialMode
                        createTutorialMode = false
                        addNodeMode = false
                        editNodeMode = false
                        if (editTutorialMode) {
                            nameInput.textBox = tutorial.name
                            descInput.textBox = tutorial.description
                            profileDependentDraft = tutorial.profileDependend
                        }
                    },
                    bypassChecks = true,
                ),
            )
        }
        list.add(
            Renderable.clickable(
                "§cClose",
                tips = listOf("§7Click to close"),
                onLeftClick = { mc.displayGuiScreen(null) },
                bypassChecks = true,
            ),
        )
        list.add(Renderable.text(" "))

        if (tutorial != null) {
            // New: global root drop target
            list.add(
                Renderable.droppable(
                    display = Renderable.hoverTips("§7⇩ Drop here to move node to root", listOf("§7Drag a node handle and drop to append to root"), bypassChecks = true),
                    drop = object : Droppable {
                        override fun validTarget(item: Any?): Boolean = item is NodeDrag
                        override fun handle(drop: Any?) { val d = drop as? NodeDrag ?: return; moveNodeToRoot(tutorial, d.nodeId) }
                    },
                    bypassChecks = true,
                ),
            )
            list.add(
                Renderable.clickable(
                    "§a+ Add Node to Root",
                    tips = listOf("§7Append a step or fork to the root path"),
                    onLeftClick = {
                        addNodeMode = true
                        editNodeMode = false
                        pendingNodeType = null
                        addTargetForkId = null
                        addTargetPathIndex = null
                        selectedNodeId = null
                    },
                    bypassChecks = true,
                ),
            )
            list.add(buildTree(tutorial))
        } else {
            list.add(Renderable.text("§7No active tutorial."))
            list.add(
                Renderable.clickable(
                    if (createTutorialMode) "§cCancel Creation" else "§aCreate New Tutorial",
                    tips = listOf("§7Start a new tutorial"),
                    onLeftClick = {
                        createTutorialMode = !createTutorialMode
                        editTutorialMode = false
                        addNodeMode = false
                        editNodeMode = false
                        if (createTutorialMode) {
                            nameInput.textBox = ""
                            descInput.textBox = ""
                            profileDependentDraft = false
                        }
                    },
                    bypassChecks = true,
                ),
            )
        }
        return Renderable.vertical(list, spacing = 2)
    }

    private fun buildRightPanel(tutorial: Tutorial?): Renderable {
        val out = mutableListOf<Renderable>()
        out.add(Renderable.text("§b§lDetails"))
        if (tutorial == null) {
            if (createTutorialMode) out.addAll(buildCreateTutorialForm()) else out.add(Renderable.text("§7No tutorial loaded."))
            return Renderable.vertical(out, spacing = 2)
        }
        if (addNodeMode) {
            out.addAll(buildAddNodeWizard(tutorial))
            return Renderable.vertical(out, spacing = 2)
        }
        if (editTutorialMode) {
            out.addAll(buildEditTutorialForm(tutorial))
            return Renderable.vertical(out, spacing = 2)
        }
        val selectedNode = selectedNodeId?.let { tutorial.getNodeReference(it) }
        if (selectedNode == null) {
            out.add(Renderable.text("§7Select a node on the left to view details."))
            return Renderable.vertical(out, spacing = 2)
        }

        if (editNodeMode) {
            val editor = TutorialEditorRegistry.editorFor(selectedNode)
            val form = editor.buildEditor(
                tutorial,
                selectedNode,
                onSave = { replacement ->
                    // Replace node in tutorial
                    val newSteps = TutorialTreeOps.replaceNodeById(tutorial.steps, selectedNode.nodeId) { replacement }
                    applyNewSteps(tutorial, newSteps)
                    editNodeMode = false
                    selectedNodeId = null
                },
                onCancel = {
                    editNodeMode = false
                },
            )
            out.addAll(form)
            return Renderable.vertical(out, spacing = 2)
        }

        // Normal details + actions
        out.addAll(buildDetails(tutorial, selectedNode))
        out.add(Renderable.text(" "))
        out.add(
            Renderable.horizontal(spacing = 6) {
                add(
                    Renderable.clickable(
                        "§a✎ Edit Node",
                        tips = listOf("§7Open the editor for this node"),
                        onLeftClick = { editNodeMode = true },
                    ),
                )
                add(
                    Renderable.clickable(
                        "§7↑ Move Up",
                        tips = listOf("§7Move this node up in its list"),
                        onLeftClick = {
                            val moved = moveNode(tutorial, -1, selectedNode.nodeId)
                            if (moved) selectedNodeId = selectedNode.nodeId
                        },
                    ),
                )
                add(
                    Renderable.clickable(
                        "§7↓ Move Down",
                        tips = listOf("§7Move this node down in its list"),
                        onLeftClick = {
                            val moved = moveNode(tutorial, +1, selectedNode.nodeId)
                            if (moved) selectedNodeId = selectedNode.nodeId
                        },
                    )
                )
                add(
                    Renderable.clickable(
                        "§7⌘ Duplicate",
                        tips = listOf("§7Duplicate this node after itself"),
                        onLeftClick = { duplicateNodeInTree(tutorial, selectedNode.nodeId) },
                    ),
                )
                add(
                    Renderable.clickable(
                        "§c✖ Delete",
                        tips = listOf("§7Remove this node"),
                        onLeftClick = {
                            val newSteps = TutorialTreeOps.removeNodeById(tutorial.steps, selectedNode.nodeId)
                            applyNewSteps(tutorial, newSteps)
                            selectedNodeId = null
                        },
                    ),
                )
            },
        )

        // For forks, offer to duplicate a path
        when (selectedNode) {
            is SelectPathTutorialFork -> {
                out.add(Renderable.text(" "))
                out.add(Renderable.text("§eDuplicate a Path:"))
                selectedNode.paths.forEachIndexed { idx, opt ->
                    out.add(
                        Renderable.clickable(
                            "§7⌘ Duplicate '${opt.option.name}'",
                            tips = listOf("§7Creates a copy of this path after itself"),
                            onLeftClick = { duplicatePathInTree(tutorial, selectedNode.nodeId, idx) },
                        ),
                    )
                }
            }
            is OptionalTutorialFork -> {
                out.add(Renderable.text(" "))
                out.add(Renderable.text("§eDuplicate an Option:"))
                selectedNode.paths.forEachIndexed { idx, _ ->
                    out.add(
                        Renderable.clickable(
                            "§7⌘ Duplicate Option ${idx + 1}",
                            tips = listOf("§7Creates a copy of this option after itself"),
                            onLeftClick = { duplicatePathInTree(tutorial, selectedNode.nodeId, idx) },
                        ),
                    )
                }
            }
        }

        return Renderable.vertical(out, spacing = 2)
    }

    private fun buildTree(tutorial: Tutorial): Renderable {
        val items = mutableListOf<Renderable>()
        val header = mutableListOf<Renderable>()
        header.add(Renderable.text("§f§l${tutorial.name}"))
        if (tutorial.description.isNotBlank()) header.add(Renderable.text("§7${tutorial.description}"))
        // New: header drop target to append to root
        header.add(
            Renderable.droppable(
                display = Renderable.hoverTips("§8[ Drop node here to append to root ]", listOf("§7Drag a node handle and drop here"), bypassChecks = true),
                drop = object : Droppable {
                    override fun validTarget(item: Any?): Boolean = item is NodeDrag
                    override fun handle(drop: Any?) { val d = drop as? NodeDrag ?: return; moveNodeToRoot(tutorial, d.nodeId) }
                },
                bypassChecks = true,
            ),
        )
        items.add(Renderable.vertical(header, spacing = 1))
        items.addAll(buildNodesList(tutorial, tutorial.steps, indent = 0, markNextAsRequiresAsync = false))
        return Renderable.vertical(items, spacing = 1)
    }

    private data class NodeDrag(val nodeId: String, val label: String) : DragItem<NodeDrag> {
        override fun get(): NodeDrag = this
        override fun onRender(mouseX: Int, mouseY: Int) {
            // Lightweight drag preview
            Renderable.text("§7≡ §f$label").render(mouseX, mouseY)
        }
    }

    private fun makeDragHandle(nodeId: String, label: String): Renderable =
        Renderable.draggable(
            display = Renderable.text("§7≡ "),
            item = { NodeDrag(nodeId, label) },
            bypassChecks = true,
        )

    private fun wrapDroppableRow(
        tutorial: Tutorial,
        row: Renderable,
        targetNodeId: String,
        siblingIds: Set<String>,
    ): Renderable = Renderable.droppable(
        display = row,
        drop = object : Droppable {
            override fun validTarget(item: Any?): Boolean {
                val drag = item as? NodeDrag ?: return false
                return drag.nodeId != targetNodeId && siblingIds.contains(drag.nodeId)
            }

            override fun handle(drop: Any?) {
                val drag = drop as? NodeDrag ?: return
                reorderNodeWithinSameParent(tutorial, drag.nodeId, targetNodeId)
            }
        },
        bypassChecks = true,
    )

    private fun reorderNodeWithinSameParent(tutorial: Tutorial, movingId: String, targetId: String) {
        val (newSteps, changed) = reorderRec(tutorial.steps, movingId, targetId)
        if (changed) applyNewSteps(tutorial, newSteps)
    }

    private fun reorderRec(
        nodes: List<TutorialNode>,
        movingId: String,
        targetId: String,
    ): Pair<List<TutorialNode>, Boolean> {
        // Same-level reorder
        val ids = nodes.map { it.nodeId }
        val mi = ids.indexOf(movingId)
        val ti = ids.indexOf(targetId)
        if (mi != -1 && ti != -1) {
            if (mi == ti) return nodes to false
            val list = nodes.toMutableList()
            val moving = list.removeAt(mi)
            val insertAt = if (mi < ti) ti - 1 else ti
            list.add(insertAt, moving)
            return list to true
        }
        // Recurse into forks
        nodes.forEachIndexed { idx, n ->
            when (n) {
                is AsyncTutorialFork -> {
                    val (inner, changed) = reorderRec(n.pathNodes, movingId, targetId)
                    if (changed) {
                        val copy = nodes.toMutableList()
                        copy[idx] = AsyncTutorialFork(inner)
                        return copy to true
                    }
                }
                is SelectPathTutorialFork -> {
                    n.paths.forEachIndexed { pIdx, sp ->
                        val (inner, changed) = reorderRec(sp.pathNodes, movingId, targetId)
                        if (changed) {
                            val newPaths = n.paths.mapIndexed { j, p -> if (j == pIdx) SelectPathTutorialFork.SelectedPathTutorialFork(p.option, inner) else p }
                            val copy = nodes.toMutableList()
                            copy[idx] = SelectPathTutorialFork(newPaths)
                            return copy to true
                        }
                    }
                }
                is OptionalTutorialFork -> {
                    n.paths.forEachIndexed { pIdx, pair ->
                        val (inner, changed) = reorderRec(pair.first, movingId, targetId)
                        if (changed) {
                            val newPaths = n.paths.mapIndexed { j, p -> if (j == pIdx) (inner to p.second) else p }
                            val copy = nodes.toMutableList()
                            copy[idx] = OptionalTutorialFork(newPaths)
                            return copy to true
                        }
                    }
                }
                else -> {}
            }
        }
        return nodes to false
    }

    private fun lineWithIndent(indent: Int, children: () -> List<Renderable>): Renderable {
        val prefix = "  ".repeat(indent)
        return Renderable.horizontal { add(Renderable.text(prefix)); children().forEach { add(it) } }
    }

    private fun safeStepName(step: TutorialStep, tutorial: Tutorial): String = try { step.getStepName(tutorial) } catch (_: Throwable) { "Step" }
    private fun safeStepDesc(step: TutorialStep, tutorial: Tutorial): String? = try { step.getStepDescription(tutorial) } catch (_: Throwable) { null }

    private fun buildDetails(tutorial: Tutorial, node: TutorialNode): List<Renderable> {
        val out = mutableListOf<Renderable>()
        when (node) {
            is TutorialStep -> {
                out.add(Renderable.text("§f${safeStepName(node, tutorial)}"))
                val desc = safeStepDesc(node, tutorial)
                if (!desc.isNullOrBlank()) out.add(Renderable.text("§7$desc"))
                out.add(Renderable.text(" "))
                if (!node.completed) out.add(Renderable.clickable("§cSkip this step", onLeftClick = { tutorial.skipNode(node); TutorialOverlayDisplay.markDirty() }, bypassChecks = true)) else out.add(Renderable.text("§aAlready completed"))
                when (node) {
                    is ReforgeTutorialStep -> {
                        out.add(Renderable.text("§eType: §fReforge"))
                        out.add(Renderable.text("§7Tagged Item: §f${node.item.tag}"))
                        out.add(Renderable.text("§7Wanted Reforge: §f${node.reforgeName}"))
                    }
                    is EnchantTutorialStep -> {
                        out.add(Renderable.text("§eType: §fEnchant"))
                        out.add(Renderable.text("§7Tagged Item: §f${node.item.tag}"))
                        if (node.enchantIds.isNotEmpty()) {
                            out.add(Renderable.text("§7Wanted Enchants:"))
                            node.enchantIds.entries.sortedBy { it.key }.forEach { (id, lvl) -> out.add(Renderable.text("§7 - §f$id §8→ §f$lvl")) }
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
                        out.add(Renderable.text("§7Route nodes: §f${node.node?.size ?: 0}"))
                    }
                }
                out.add(Renderable.text(" "))
                out.add(
                    Renderable.clickable(
                        "§a+ Insert new node after this (root only)",
                        tips = listOf("§7Insert below on root path"),
                        onLeftClick = {
                            addNodeMode = true
                            pendingNodeType = null
                            addTargetForkId = null
                            addTargetPathIndex = null
                        },
                    ),
                )
            }
            is SelectPathTutorialFork -> {
                out.add(Renderable.text("§f${node.getHeader(tutorial)}"))
                val selected = tutorial.getSelectedOption(node.paths)
                if (selected == null) {
                    out.add(Renderable.text("§6Waiting for path selection"))
                    node.paths.forEach { opt -> out.add(Renderable.clickable("§eChoose: §f${opt.option.name}", onLeftClick = { tutorial.selectPath(opt.option.id); TutorialOverlayDisplay.markDirty() }, bypassChecks = true)) }
                } else out.add(Renderable.text("§aSelected: §f${selected.option.name}"))
                out.add(Renderable.text(" "))
                out.add(Renderable.text("§eAdd Child to Path:"))
                node.paths.forEachIndexed { idx, opt ->
                    out.add(
                        Renderable.clickable(
                            "§7- §f${opt.option.name}",
                            tips = listOf(opt.option.guideMakerDescription ?: "§7No description"),
                            onLeftClick = { addNodeMode = true; pendingNodeType = null; addTargetForkId = node.nodeId; addTargetPathIndex = idx },
                        ),
                    )
                }
            }
            is OptionalTutorialFork -> {
                out.add(Renderable.text("§f${node.getHeader(tutorial)}"))
                out.add(Renderable.text("§7Both options shown; furthest path is highlighted."))
                out.add(Renderable.text("§7Hidden options are ${if (config.showHiddenOptionalPaths) "§aVISIBLE" else "§cHIDDEN"}."))
                out.add(Renderable.text(" "))
                out.add(Renderable.text("§eAdd Child to Option:"))
                node.paths.forEachIndexed { idx, pair ->
                    val hidden = pair.second
                    val tag = if (hidden) "§8(hidden)" else ""
                    out.add(
                        Renderable.clickable(
                            "§7- §fOption ${idx + 1} $tag",
                            tips = listOf("§7Adds to this option"),
                            onLeftClick = { addNodeMode = true; pendingNodeType = null; addTargetForkId = node.nodeId; addTargetPathIndex = idx },
                        ),
                    )
                }
            }
            is AsyncTutorialFork -> {
                out.add(Renderable.text("§f${node.getHeader(tutorial)}"))
                out.add(Renderable.text("§6Runs asynchronously; sub-steps progress in parallel."))
                out.add(Renderable.text("§7Next main step marked as needing async."))
                out.add(Renderable.text(" "))
                out.add(
                    Renderable.clickable(
                        "§a+ Add Child to Async",
                        tips = listOf("§7Append inside this async fork"),
                        onLeftClick = { addNodeMode = true; pendingNodeType = null; addTargetForkId = node.nodeId; addTargetPathIndex = 0 },
                    ),
                )
            }
            is TutorialFork -> out.add(Renderable.text("§f${node.getHeader(tutorial)}"))
        }
        out.add(Renderable.text(" "))
        out.add(Renderable.clickable("§7Clear selection", onLeftClick = { selectedNodeId = null; editNodeMode = false }, bypassChecks = true))
        return out
    }

    private fun buildCreateTutorialForm(): List<Renderable> {
        val out = mutableListOf<Renderable>()
        out.add(Renderable.text("§fCreate a new Tutorial"))
        out.add(Renderable.text(" "))
        // Name field
        out.add(
            Renderable.horizontal(spacing = 6) {
                add(Renderable.text("§7Name: §f"))
                add(Renderable.textBox("", nameInput, 260, bypassChecks = true))
            },
        )
        // Description field
        out.add(
            Renderable.horizontal(spacing = 6) {
                add(Renderable.text("§7Description: §f"))
                add(Renderable.textBox("", descInput, 260, bypassChecks = true))
            },
        )
        out.add(Renderable.clickable("§7Profile dependent: ${if (profileDependentDraft) "§aYES" else "§cNO"}", tips = listOf("§7If on, progress is per-profile"), onLeftClick = { profileDependentDraft = !profileDependentDraft }, bypassChecks = true))
        out.add(Renderable.text(" "))
        out.add(
            Renderable.horizontal(spacing = 8) {
                add(
                    Renderable.clickable(
                        Renderable.hoverTips("§aCreate", listOf("§7Create tutorial with given meta")),
                        onLeftClick = {
                            val name = nameInput.finalText().trim(); val desc = descInput.finalText().trim()
                            if (name.isNotEmpty()) {
                                val t = Tutorial(mutableListOf(), name, desc, profileDependentDraft)
                                t.onLoad()
                                ProfileStorageData.profileSpecific?.tutorialManager?.activeTutorial = t
                                TutorialOverlayDisplay.markDirty()
                                SkyHanniMod.launchCoroutine { SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "tutorial-create") }
                                createTutorialMode = false
                            }
                        },
                        bypassChecks = true,
                    ),
                )
                add(Renderable.clickable("§cCancel", onLeftClick = { createTutorialMode = false }, bypassChecks = true))
            },
        )
        return out
    }

    private fun buildEditTutorialForm(tutorial: Tutorial): List<Renderable> {
        val out = mutableListOf<Renderable>()
        out.add(Renderable.text("§fEdit Tutorial Metadata"))
        out.add(Renderable.text(" "))
        if (nameInput.textBox.isEmpty()) nameInput.textBox = tutorial.name
        if (descInput.textBox.isEmpty()) descInput.textBox = tutorial.description
        // Name field
        out.add(
            Renderable.horizontal(spacing = 6) {
                add(Renderable.text("§7Name: §f"))
                add(Renderable.textBox("", nameInput, 260, bypassChecks = true))
            },
        )
        // Description field
        out.add(
            Renderable.horizontal(spacing = 6) {
                add(Renderable.text("§7Description: §f"))
                add(Renderable.textBox("", descInput, 260, bypassChecks = true))
            },
        )
        out.add(Renderable.clickable("§7Profile dependent: ${if (profileDependentDraft) "§aYES" else "§cNO"}", tips = listOf("§7If on, progress is per-profile"), onLeftClick = { profileDependentDraft = !profileDependentDraft }, bypassChecks = true))
        out.add(Renderable.text(" "))
        out.add(
            Renderable.horizontal(spacing = 8) {
                add(
                    Renderable.clickable(
                        Renderable.hoverTips("§aSave", listOf("§7Apply changes")),
                        onLeftClick = {
                            val name = nameInput.finalText().ifBlank { tutorial.name }
                            val desc = descInput.finalText()
                            val newT = Tutorial(tutorial.steps.toMutableList(), name, desc, profileDependentDraft)
                            newT.onLoad()
                            ProfileStorageData.profileSpecific?.tutorialManager?.activeTutorial = newT
                            TutorialOverlayDisplay.markDirty()
                            SkyHanniMod.launchCoroutine { SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "tutorial-edit") }
                            editTutorialMode = false
                        },
                        bypassChecks = true,
                    ),
                )
                add(Renderable.clickable("§cCancel", onLeftClick = { editTutorialMode = false }, bypassChecks = true))
            },
        )
        return out
    }

    private fun applyNewSteps(tutorial: Tutorial, newSteps: List<TutorialNode>) {
        val newT = Tutorial(newSteps.toMutableList(), tutorial.name, tutorial.description, tutorial.profileDependend)
        newT.onLoad()
        ProfileStorageData.profileSpecific?.tutorialManager?.activeTutorial = newT
        TutorialOverlayDisplay.markDirty()
        SkyHanniMod.launchCoroutine { SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "tutorial-apply-steps") }
    }

    private fun buildAddNodeWizard(tutorial: Tutorial): List<Renderable> {
        val out = mutableListOf<Renderable>()
        out.add(Renderable.text("§fAdd a new Node"))
        val targetText = if (addTargetForkId == null) "root" else "fork $addTargetForkId"
        out.add(Renderable.text("§7Target: §f$targetText"))
        out.add(Renderable.text(" "))
        if (pendingNodeType == null) {
            out.add(Renderable.text("§eChoose a node type:"))
            // Keep forms for these types
            out.add(Renderable.clickable(Renderable.hoverTips("§fText Step", listOf("§7Custom instruction; manual skip"), bypassChecks = true), onLeftClick = { pendingNodeType = NewNodeType.TEXT }, bypassChecks = true))
            out.add(Renderable.clickable(Renderable.hoverTips("§fGo To Island", listOf("§7Warp/select an island"), bypassChecks = true), onLeftClick = { pendingNodeType = NewNodeType.GO_TO_ISLAND }, bypassChecks = true))
            out.add(Renderable.clickable(Renderable.hoverTips("§fTag Item", listOf("§7Ask user to tag an item"), bypassChecks = true), onLeftClick = { pendingNodeType = NewNodeType.TAG_ITEM }, bypassChecks = true))
            out.add(Renderable.clickable(Renderable.hoverTips("§fAsync Fork", listOf("§7Parallel sub-steps"), bypassChecks = true), onLeftClick = { pendingNodeType = NewNodeType.ASYNC_FORK }, bypassChecks = true))

            // Quick-add common nodes with safe defaults
            out.add(Renderable.clickable(Renderable.hoverTips("§fObtain (sample)", listOf("§7Adds a sample Obtain step using a placeholder tag"), bypassChecks = true), onLeftClick = {
                val chk = TaggedItemCheck("Sample Item", "Placeholder sample item", "SAMPLE_TAG")
                addNodeToTarget(tutorial, ObtainTutorialStep(chk))
            }, bypassChecks = true))

            out.add(Renderable.clickable(Renderable.hoverTips("§fObtain Coins", listOf("§7Adds a coin requirement step (1000)"), bypassChecks = true), onLeftClick = {
                addNodeToTarget(tutorial, ObtainCoinsTutorialStep(1000))
            }, bypassChecks = true))

            out.add(Renderable.clickable(Renderable.hoverTips("§fEnchant (sample)", listOf("§7Adds an Enchant step with empty enchant map"), bypassChecks = true), onLeftClick = {
                val chk = TaggedItemCheck("Sample Item", "Placeholder", "SAMPLE_TAG")
                addNodeToTarget(tutorial, EnchantTutorialStep(chk, emptyMap()))
            }, bypassChecks = true))

            out.add(Renderable.clickable(Renderable.hoverTips("§fReforge (sample)", listOf("§7Adds a Reforge step targeting 'Default'"), bypassChecks = true), onLeftClick = {
                val chk = TaggedItemCheck("Sample Item", "Placeholder", "SAMPLE_TAG")
                addNodeToTarget(tutorial, ReforgeTutorialStep(chk, "Default"))
            }, bypassChecks = true))
             out.add(Renderable.text(" "))
             out.add(Renderable.clickable("§cBack", onLeftClick = { addNodeMode = false }))
             return out
         }
        when (pendingNodeType) {
            NewNodeType.TEXT -> out.addAll(buildAddTextStepForm(tutorial))
            NewNodeType.GO_TO_ISLAND -> out.addAll(buildAddGoToIslandForm(tutorial))
            NewNodeType.TAG_ITEM -> out.addAll(buildAddTagItemForm(tutorial))
            NewNodeType.ASYNC_FORK -> out.addAll(buildAddAsyncForkForm(tutorial))
            else -> {}
        }
        return out
    }

    private fun buildAddTextStepForm(tutorial: Tutorial): List<Renderable> {
        val out = mutableListOf<Renderable>()
        out.add(Renderable.text("§fNew Text Step"))
        out.add(Renderable.searchBox(Renderable.text(""), "§7Title: §f", {}, textStepNameInput, hideIfNoText = false, ySpacing = 4, bypassChecks = true))
        out.add(SuggestionDropdown.below(textStepNameInput, optionsProvider = { SuggestionProviders.filterContains(SuggestionProviders.titlesFromTutorial(tutorial), textStepNameInput.finalText()) }))
        out.add(Renderable.searchBox(Renderable.text(""), "§7Description: §f", {}, textStepDescInput, hideIfNoText = false, ySpacing = 4, bypassChecks = true))
        out.add(SuggestionDropdown.below(textStepDescInput, optionsProvider = { SuggestionProviders.filterContains(SuggestionProviders.descriptionsFromTutorial(tutorial), textStepDescInput.finalText()) }))
        out.add(Renderable.text(" "))
        out.add(
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aAdd", onLeftClick = {
                    val name = textStepNameInput.finalText().trim().ifEmpty { "Instruction" }
                    val desc = textStepDescInput.finalText().trim()
                    addNodeToTarget(tutorial, TextTutorialStep(name, desc))
                }))
                add(Renderable.clickable("§cBack", onLeftClick = { pendingNodeType = null }))
            },
        )
        return out
    }

    private fun buildAddGoToIslandForm(tutorial: Tutorial): List<Renderable> {
        val out = mutableListOf<Renderable>()
        out.add(Renderable.text("§fNew 'Go To Island' Step"))
        val content: Map<List<Renderable>, String?> = Islands.entries.associate { isl ->
            val label = Renderable.hoverTips("§f${isl.getDisplayName()}", listOf("§7Key: §f${isl.name}", isl.warpArgument?.let { "§7Warp: §f/$it" } ?: "§7No warp"), bypassChecks = true)
            listOf(Renderable.text("§7• "), label) to isl.getDisplayName()
        }
        out.add(
            Renderable.searchBox(
                content = Renderable.searchableScrollTable(content, height = 120, textInput = islandInput, key = 31, xSpacing = 4, ySpacing = 1),
                searchPrefix = "§7Filter: §f",
                onUpdateSize = {},
                textInput = islandInput,
                hideIfNoText = false,
                ySpacing = 6,
                bypassChecks = true,
            ),
        )
        out.add(SuggestionDropdown.below(islandInput, optionsProvider = { SuggestionProviders.filterContains(SuggestionProviders.islands(), islandInput.finalText()) }))
        out.add(Renderable.text(" "))
        out.add(Renderable.text("§7Tip: Click an island in the list to select"))
        out.add(
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aAdd with selected", tips = listOf("§7Adds if filter uniquely matches"), onLeftClick = {
                    val q = islandInput.finalText().trim().lowercase()
                    val matches = Islands.entries.filter { isl ->
                        isl.getDisplayName().lowercase().contains(q) || isl.name.lowercase().contains(q)
                    }
                    if (matches.size == 1) {
                        addNodeToTarget(tutorial, GoToIslandTutorialStep(matches.first()))
                    }
                }))
                add(Renderable.clickable("§cBack", onLeftClick = { pendingNodeType = null }))
            },
        )
        return out
    }

    private fun buildAddTagItemForm(tutorial: Tutorial): List<Renderable> {
        val out = mutableListOf<Renderable>()
        out.add(Renderable.text("§fNew 'Tag Item' Step"))
        val tags = ProfileStorageData.profileSpecific?.itemTags?.keys?.sorted().orEmpty()
        val content: Map<List<Renderable>, String?> = tags.associate { tag -> listOf(Renderable.text("§7• §f$tag")) to tag }
        out.add(
            Renderable.searchBox(
                content = Renderable.searchableScrollTable(content, height = 80, textInput = tagNameInput, key = 41, xSpacing = 2, ySpacing = 0),
                searchPrefix = "§7Tag Name: §f",
                onUpdateSize = {},
                textInput = tagNameInput,
                hideIfNoText = false,
                ySpacing = 4,
                bypassChecks = true,
            ),
        )
        out.add(SuggestionDropdown.below(tagNameInput, optionsProvider = { SuggestionProviders.filterContains(SuggestionProviders.tagNames() + SuggestionProviders.tagNamesFromSteps(tutorial), tagNameInput.finalText()) }))
        out.add(Renderable.searchBox(Renderable.text(""), "§7Explanation: §f", {}, tagExplInput, hideIfNoText = false, ySpacing = 4, bypassChecks = true))
        out.add(Renderable.text(" "))
        out.add(
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aAdd", onLeftClick = {
                    val tag = tagNameInput.finalText().trim()
                    val expl = tagExplInput.finalText().trim().ifEmpty { "Tag your item so later steps can reference it" }
                    if (tag.isNotEmpty()) addNodeToTarget(tutorial, TagItemTutorialStep(tag, expl))
                }))
                add(Renderable.clickable("§cBack", onLeftClick = { pendingNodeType = null }))
            },
        )
        return out
    }

    private fun buildAddAsyncForkForm(tutorial: Tutorial): List<Renderable> {
        val out = mutableListOf<Renderable>()
        out.add(Renderable.text("§fNew Async Fork"))
        out.add(Renderable.text("§7Creates an empty async group; add children afterwards."))
        out.add(Renderable.text(" "))
        out.add(
            Renderable.horizontal(spacing = 8) {
                add(Renderable.clickable("§aAdd", tips = listOf("§7Create async fork"), onLeftClick = { addNodeToTarget(tutorial, AsyncTutorialFork(emptyList())) }, bypassChecks = true))
                add(Renderable.clickable("§cBack", onLeftClick = { pendingNodeType = null }, bypassChecks = true))
            },
        )
        return out
    }

    private fun addNodeToTarget(tutorial: Tutorial, node: TutorialNode) {
        try { node.populateNodeIds(tutorial) } catch (_: Throwable) {}
        val newSteps = if (addTargetForkId == null) tutorial.steps.toMutableList().apply { add(node) } else TutorialTreeOps.addNodeInsideFork(tutorial.steps, addTargetForkId!!, addTargetPathIndex ?: 0, node)
        applyNewSteps(tutorial, newSteps)
        addNodeMode = false
        pendingNodeType = null
        addTargetForkId = null
        addTargetPathIndex = null
        TutorialOverlayDisplay.markDirty()
    }

    private fun moveNode(tutorial: Tutorial, delta: Int, targetId: String): Boolean {
        val (newSteps, moved) = moveNodeRec(tutorial.steps, delta, targetId)
        if (moved) applyNewSteps(tutorial, newSteps)
        return moved
    }

    private fun moveNodeRec(nodes: List<TutorialNode>, delta: Int, targetId: String): Pair<List<TutorialNode>, Boolean> {
        val idx = nodes.indexOfFirst { it.nodeId == targetId }
        if (idx != -1) {
            val newIdx = idx + delta
            if (newIdx in nodes.indices) {
                val copy = nodes.toMutableList()
                val tmp = copy[idx]
                copy[idx] = copy[newIdx]
                copy[newIdx] = tmp
                return copy to true
            }
            return nodes to false
        }
        for (n in nodes) when (n) {
            is AsyncTutorialFork -> {
                val (inner, moved) = moveNodeRec(n.pathNodes, delta, targetId)
                if (moved) return AsyncTutorialFork(inner).let { nodes.map { if (it === n) it.copyAsync(inner) else it } } to true
            }
            is SelectPathTutorialFork -> {
                n.paths.forEachIndexed { i, sp ->
                    val (_, moved) = moveNodeRec(sp.pathNodes, delta, targetId)
                    if (moved) {
                        val newPaths = n.paths.mapIndexed { j, p -> if (j == i) SelectPathTutorialFork(newPaths) else p }
                        return nodes.map { if (it === n) SelectPathTutorialFork(newPaths) else it } to true
                    }
                }
            }
            is OptionalTutorialFork -> {
                n.paths.forEachIndexed { i, pair ->
                    val (_, moved) = moveNodeRec(pair.first, delta, targetId)
                    if (moved) {
                        val newPaths = n.paths.mapIndexed { j, p -> if (j == i) OptionalTutorialFork(newPaths) else p }
                        return nodes.map { if (it === n) OptionalTutorialFork(newPaths) else it } to true
                    }
                }
            }
            else -> {}
        }
        return nodes to false
    }

    private fun TutorialNode.copyAsync(newChildren: List<TutorialNode>): TutorialNode = when (this) {
        is AsyncTutorialFork -> AsyncTutorialFork(newChildren)
        else -> this
    }

    private fun duplicateNodeInTree(tutorial: Tutorial, targetId: String): Boolean {
        val (newSteps, done) = duplicateNodeRec(tutorial, tutorial.steps, targetId)
        if (done) applyNewSteps(tutorial, newSteps)
        return done
    }

    private fun duplicateNodeRec(tutorial: Tutorial, nodes: List<TutorialNode>, targetId: String): Pair<List<TutorialNode>, Boolean> {
        val idx = nodes.indexOfFirst { it.nodeId == targetId }
        if (idx != -1) {
            val dup = duplicateNode(tutorial, nodes[idx]) ?: return nodes to false
            val copy = nodes.toMutableList()
            copy.add(idx + 1, dup)
            return copy to true
        }
        for (n in nodes) when (n) {
            is AsyncTutorialFork -> {
                val (inner, done) = duplicateNodeRec(tutorial, n.pathNodes, targetId)
                if (done) return nodes.map { if (it === n) AsyncTutorialFork(inner) else it } to true
            }
            is SelectPathTutorialFork -> {
                n.paths.forEachIndexed { i, sp ->
                    val (_, done) = duplicateNodeRec(tutorial, sp.pathNodes, targetId)
                    if (done) {
                        val newPaths = n.paths.mapIndexed { j, p -> if (j == i) SelectPathTutorialFork(newPaths) else p }
                        return nodes.map { if (it === n) SelectPathTutorialFork(newPaths) else it } to true
                    }
                }
            }
            is OptionalTutorialFork -> {
                n.paths.forEachIndexed { i, pair ->
                    val (_, done) = duplicateNodeRec(tutorial, pair.first, targetId)
                    if (done) {
                        val newPaths = n.paths.mapIndexed { j, p -> if (j == i) OptionalTutorialFork(newPaths) else p }
                        return nodes.map { if (it === n) OptionalTutorialFork(newPaths) else it } to true
                    }
                }
            }
            else -> {}
        }
        return nodes to false
    }

    private fun duplicateNode(tutorial: Tutorial, node: TutorialNode): TutorialNode? = when (node) {
        is TextTutorialStep -> TextTutorialStep(node.name, node.description).also { it.populateNodeIds(tutorial) }
        is TagItemTutorialStep -> TagItemTutorialStep(node.tagName, node.explenation).also { it.populateNodeIds(tutorial) }
        is GoToIslandTutorialStep -> GoToIslandTutorialStep(node.island).also { it.populateNodeIds(tutorial) }
        is ReforgeTutorialStep -> ReforgeTutorialStep(node.item, node.reforgeName).also { it.populateNodeIds(tutorial) }
        is EnchantTutorialStep -> EnchantTutorialStep(node.item, node.enchantIds.toMap()).also { it.populateNodeIds(tutorial) }
        is AsyncTutorialFork -> AsyncTutorialFork(node.pathNodes.mapNotNull { duplicateNode(tutorial, it) }).also { it.populateNodeIds(tutorial) }
        is SelectPathTutorialFork -> SelectPathTutorialFork(
            node.paths.map { sp -> SelectPathTutorialFork.SelectedPathTutorialFork(sp.option, sp.pathNodes.mapNotNull { duplicateNode(tutorial, it) }) },
        ).also { it.populateNodeIds(tutorial) }
        is OptionalTutorialFork -> OptionalTutorialFork(
            node.paths.map { (path, hidden) -> path.mapNotNull { duplicateNode(tutorial, it) } to hidden },
        ).also { it.populateNodeIds(tutorial) }
        else -> null
    }

    private fun duplicatePathInTree(tutorial: Tutorial, forkId: String, pathIndex: Int): Boolean {
        val (newSteps, done) = duplicatePathRec(tutorial, tutorial.steps, forkId, pathIndex)
        if (done) applyNewSteps(tutorial, newSteps)
        return done
    }

    private fun duplicatePathRec(
        tutorial: Tutorial,
        nodes: List<TutorialNode>,
        forkId: String,
        pathIndex: Int,
    ): Pair<List<TutorialNode>, Boolean> {
        val out = nodes.toMutableList()
        for (i in nodes.indices) {
            when (val n = nodes[i]) {
                is SelectPathTutorialFork -> if (n.nodeId == forkId) {
                    if (pathIndex !in n.paths.indices) return nodes to false
                    val sp = n.paths[pathIndex]
                    val duplicatedNodes = sp.pathNodes.mapNotNull { duplicateNode(tutorial, it) }
                    val opt = SelectPathTutorialFork.SelectPathTutorialOption(
                        sp.option.id + "_copy",
                        sp.option.name + " (Copy)",
                        sp.option.guideMakerDescription,
                    )
                    val newList = n.paths.toMutableList()
                    newList.add(pathIndex + 1, SelectPathTutorialFork.SelectedPathTutorialFork(opt, duplicatedNodes))
                    out[i] = SelectPathTutorialFork(newList)
                    return out to true
                } else {
                    val (_, done) = duplicatePathRec(tutorial, n.getAllInternalNodes(), forkId, pathIndex)
                    if (done) return out to true
                }
                is OptionalTutorialFork -> if (n.nodeId == forkId) {
                    if (pathIndex !in n.paths.indices) return nodes to false
                    val (path, hidden) = n.paths[pathIndex]
                    val duplicatedNodes = path.mapNotNull { duplicateNode(tutorial, it) }
                    val newList = n.paths.toMutableList()
                    newList.add(pathIndex + 1, duplicatedNodes to hidden)
                    out[i] = OptionalTutorialFork(newList)
                    return out to true
                } else {
                    val (_, done) = duplicatePathRec(tutorial, n.getAllInternalNodes(), forkId, pathIndex)
                    if (done) return out to true
                }
                is AsyncTutorialFork -> {
                    val (_, done) = duplicatePathRec(tutorial, n.pathNodes, forkId, pathIndex)
                    if (done) return out to true
                }
                else -> {}
            }
        }
        return nodes to false
    }

    private fun buildNodesList(
        tutorial: Tutorial,
        nodes: List<TutorialNode>,
        indent: Int,
        markNextAsRequiresAsync: Boolean,
    ): List<Renderable> {
        val lines = mutableListOf<Renderable>()
        val siblingIds = nodes.map { it.nodeId }.toSet()
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
                    val lineContent = lineWithIndent(indent) { listOf(Renderable.text(bullet), Renderable.text(" §f$name$recSuffix")) }
                    val clickableRow = Renderable.clickable(
                        Renderable.hoverTips(lineContent, listOf("§7Left-click to select", "§7Right panel shows details & actions"), bypassChecks = true),
                        onLeftClick = { selectedNodeId = node.nodeId },
                        bypassChecks = true,
                    )
                    val rowWithHandle = Renderable.horizontal(spacing = 2) {
                        add(makeDragHandle(node.nodeId, name))
                        add(clickableRow)
                    }
                    lines.add(wrapDroppableRow(tutorial, rowWithHandle, node.nodeId, siblingIds))
                    if (config.showStepDescriptions) {
                        val d = safeStepDesc(node, tutorial)
                        if (!d.isNullOrBlank()) lines.add(lineWithIndent(indent + 1) { listOf(Renderable.text("§7$d")) })
                    }
                    pendingAsync = false
                }
                is TutorialFork -> {
                    when (node) {
                        is AsyncTutorialFork -> {
                            val header = lineWithIndent(indent) { listOf(Renderable.text("§6⟳ §e${node.getHeader(tutorial)}")) }
                            val clickableRow = Renderable.clickable(
                                Renderable.hoverTips(header, listOf("§7Async fork: parallel sub-steps"), bypassChecks = true),
                                onLeftClick = { selectedNodeId = node.nodeId },
                                bypassChecks = true,
                            )
                            val rowWithHandle = Renderable.horizontal(spacing = 2) {
                                add(makeDragHandle(node.nodeId, node.getHeader(tutorial)))
                                add(clickableRow)
                            }
                            lines.add(wrapDroppableRow(tutorial, rowWithHandle, node.nodeId, siblingIds))
                            // New: droppable zone to move a node into this async fork
                            lines.add(lineWithIndent(indent + 1) {
                                listOf(
                                    Renderable.droppable(
                                        display = Renderable.hoverTips("§8[ Drop node here to move into this async ]", listOf("§7Appends to async group"), bypassChecks = true),
                                        drop = object : Droppable {
                                            override fun validTarget(item: Any?): Boolean = item is NodeDrag
                                            override fun handle(drop: Any?) { val d = drop as? NodeDrag ?: return; moveNodeIntoAsync(tutorial, node.nodeId, d.nodeId) }
                                        },
                                        bypassChecks = true,
                                    ),
                                )
                            })
                            lines.addAll(buildNodesList(tutorial, node.getNodes(tutorial), indent + 1, markNextAsRequiresAsync))
                            pendingAsync = true
                        }
                        is OptionalTutorialFork -> {
                            val header = lineWithIndent(indent) { listOf(Renderable.text("§7◇ §8${node.getHeader(tutorial)}")) }
                            val clickableRow = Renderable.clickable(
                                Renderable.hoverTips(header, listOf("§7Optional fork: complete any one option"), bypassChecks = true),
                                onLeftClick = { selectedNodeId = node.nodeId },
                                bypassChecks = true,
                            )
                            val rowWithHandle = Renderable.horizontal(spacing = 2) {
                                add(makeDragHandle(node.nodeId, node.getHeader(tutorial)))
                                add(clickableRow)
                            }
                            lines.add(wrapDroppableRow(tutorial, rowWithHandle, node.nodeId, siblingIds))
                            val furthest = node.paths.maxBy { pair -> pair.first.count { it.isComplete(tutorial) } }.first
                            node.paths.forEachIndexed { pathIndex, (path, hidden) ->
                                if (!hidden || config.showHiddenOptionalPaths) {
                                    val isFurthest = path === furthest
                                    val pathHeader = if (isFurthest) "§a▶ Furthest option" else "§7– Option ${pathIndex + 1}"
                                    // New: droppable on option header to move a node into this option
                                    val headerRow = lineWithIndent(indent + 1) { listOf(Renderable.text(pathHeader)) }
                                    lines.add(
                                        Renderable.droppable(
                                            display = headerRow,
                                            drop = object : Droppable {
                                                override fun validTarget(item: Any?): Boolean = item is NodeDrag
                                                override fun handle(drop: Any?) { val d = drop as? NodeDrag ?: return; moveNodeIntoOptional(tutorial, node.nodeId, pathIndex, d.nodeId) }
                                            },
                                            bypassChecks = true,
                                        ),
                                    )
                                    lines.addAll(buildNodesList(tutorial, path, indent + 2, markNextAsRequiresAsync))
                                }
                            }
                        }
                        is SelectPathTutorialFork -> {
                            val selected = tutorial.getSelectedOption(node.paths)
                            if (selected == null) {
                                val header = lineWithIndent(indent) { listOf(Renderable.text("§e⌛ §6Waiting for path selection")) }
                                val clickableRow = Renderable.clickable(
                                    Renderable.hoverTips(header, listOf("§7Select one of the listed paths"), bypassChecks = true),
                                    onLeftClick = { selectedNodeId = node.nodeId },
                                    bypassChecks = true,
                                )
                                val rowWithHandle = Renderable.horizontal(spacing = 2) { add(makeDragHandle(node.nodeId, node.getHeader(tutorial))); add(clickableRow) }
                                lines.add(wrapDroppableRow(tutorial, rowWithHandle, node.nodeId, siblingIds))
                                node.paths.forEachIndexed { idx, opt ->
                                    val row = lineWithIndent(indent + 1) { listOf(Renderable.text("§e• §f${opt.option.name} §7(click to choose)")) }
                                    // New: droppable into this selectable path
                                    lines.add(
                                        Renderable.droppable(
                                            display = Renderable.clickable(
                                                Renderable.hoverTips(row, listOf(opt.option.guideMakerDescription ?: "§7No description"), bypassChecks = true),
                                                onLeftClick = {
                                                    tutorial.selectPath(opt.option.id)
                                                    TutorialOverlayDisplay.markDirty()
                                                    selectedNodeId = node.nodeId
                                                },
                                                bypassChecks = true,
                                            ),
                                            drop = object : Droppable {
                                                override fun validTarget(item: Any?): Boolean = item is NodeDrag
                                                override fun handle(drop: Any?) { val d = drop as? NodeDrag ?: return; moveNodeIntoSelectPath(tutorial, node.nodeId, idx, d.nodeId) }
                                            },
                                            bypassChecks = true,
                                        ),
                                    )
                                }
                            } else {
                                val header = lineWithIndent(indent) { listOf(Renderable.text("§a◆ §2Selected Path: §f${selected.option.name}")) }
                                val clickableRow = Renderable.clickable(
                                    Renderable.hoverTips(header, listOf(selected.option.guideMakerDescription ?: "§7Selected path"), bypassChecks = true),
                                    onLeftClick = { selectedNodeId = node.nodeId },
                                    bypassChecks = true,
                                )
                                val rowWithHandle = Renderable.horizontal(spacing = 2) { add(makeDragHandle(node.nodeId, node.getHeader(tutorial))); add(clickableRow) }
                                lines.add(wrapDroppableRow(tutorial, rowWithHandle, node.nodeId, siblingIds))
                                // New: droppable into the selected path
                                lines.add(
                                    Renderable.droppable(
                                        display = lineWithIndent(indent + 1) { listOf(Renderable.text("§8[ Drop node here to move into selected path ]")) },
                                        drop = object : Droppable {
                                            override fun validTarget(item: Any?): Boolean = item is NodeDrag
                                            override fun handle(drop: Any?) { val d = drop as? NodeDrag ?: return; val idx = node.paths.indexOfFirst { it.option.id == selected.option.id }; if (idx != -1) moveNodeIntoSelectPath(tutorial, node.nodeId, idx, d.nodeId) }
                                        },
                                        bypassChecks = true,
                                    ),
                                )
                                lines.addAll(buildNodesList(tutorial, selected.pathNodes, indent + 1, markNextAsRequiresAsync))
                            }
                        }
                        else -> {
                            val header = lineWithIndent(indent) { listOf(Renderable.text("§9§l${node.getHeader(tutorial)}")) }
                            val clickableRow = Renderable.clickable(
                                Renderable.hoverTips(header, listOf("§7Fork grouping steps"), bypassChecks = true),
                                onLeftClick = { selectedNodeId = node.nodeId },
                                bypassChecks = true,
                            )
                            val rowWithHandle = Renderable.horizontal(spacing = 2) {
                                add(makeDragHandle(node.nodeId, node.getHeader(tutorial)))
                                add(clickableRow)
                            }
                            lines.add(wrapDroppableRow(tutorial, rowWithHandle, node.nodeId, siblingIds))
                            lines.addAll(buildNodesList(tutorial, node.getNodes(tutorial), indent + 1, markNextAsRequiresAsync))
                        }
                    }
                }
            }
        }
        return lines
    }

    // New: drag-n-drop move helpers across parents
    private fun moveNodeToRoot(tutorial: Tutorial, movingId: String) {
        val (without, removed) = TutorialTreeOps.extractNodeById(tutorial.steps, movingId)
        if (removed != null) applyNewSteps(tutorial, without.toMutableList().apply { add(removed) })
    }
    private fun moveNodeIntoAsync(tutorial: Tutorial, forkId: String, movingId: String) {
        val (without, removed) = TutorialTreeOps.extractNodeById(tutorial.steps, movingId)
        if (removed != null) TutorialTreeOps.addNodeInsideForkWithResult(without, forkId, 0, removed).also { (added, ok) -> if (ok) applyNewSteps(tutorial, added) }
    }
    private fun moveNodeIntoSelectPath(tutorial: Tutorial, forkId: String, pathIndex: Int, movingId: String) {
        val (without, removed) = TutorialTreeOps.extractNodeById(tutorial.steps, movingId)
        if (removed != null) TutorialTreeOps.addNodeInsideForkWithResult(without, forkId, pathIndex, removed).also { (added, ok) -> if (ok) applyNewSteps(tutorial, added) }
    }
    private fun moveNodeIntoOptional(tutorial: Tutorial, forkId: String, pathIndex: Int, movingId: String) {
        val (without, removed) = TutorialTreeOps.extractNodeById(tutorial.steps, movingId)
        if (removed != null) TutorialTreeOps.addNodeInsideForkWithResult(without, forkId, pathIndex, removed).also { (added, ok) -> if (ok) applyNewSteps(tutorial, added) }
    }

    // ...existing code...
}
//pre processor test
