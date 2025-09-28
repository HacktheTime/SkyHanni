package de.hype.bingonet.shared.tutorials

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.tutorials.TutorialStepCompleteEvent
import at.hannibal2.skyhanni.features.tutorial.gui.TutorialRenderableBuilder
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.renderables.Renderable
import de.hype.bingonet.shared.tutorials.paths.SelectPathTutorialFork
import de.hype.bingonet.shared.tutorials.paths.TutorialFork
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class Tutorial(
    private val _steps: MutableList<TutorialNode>,
    val name: String,
    val description: String,
    val profileDependend: Boolean = false,
) {
    // Selected route IDs for SelectPathTutorialForks
    private val selectedPathIds = mutableSetOf<String>()


    private var cachedShowDescriptions: Boolean? = null

    fun reset() {
        _steps.forEach { it.reset(this) }
        selectedPathIds.clear()
        invalidateRenderable()
    }

    val steps: List<TutorialNode> get() = _steps


    fun selectPath(optionId: String) {
        selectedPathIds.add(optionId)
        invalidateAndRebuild()
    }

    fun isPathSelected(optionId: String): Boolean = selectedPathIds.contains(optionId)

    fun skipNode(node: TutorialNode) {
        when (node) {
            is TutorialStep -> if (!node.completed) node.complete()
            is TutorialFork -> node.getAllInternalNodes().forEach { skipNode(it) }
            else -> {}
        }
        invalidateAndRebuild()
    }

    fun getNodeReference(nodeId: String): TutorialNode? {
        fun findNodeRecursively(nodes: List<TutorialNode>): TutorialNode? {
            for (it in nodes) {
                if (it.nodeId == nodeId) return it
                if (it is TutorialFork) {
                    val found = findNodeRecursively(it.getAllInternalNodes())
                    if (found != null) return found
                }
            }
            return null
        }
        return findNodeRecursively(_steps)
    }

    private val allStepsFlatMap: List<TutorialStep>
        get() {
            val list = mutableListOf<TutorialStep>()
            fun collect(nodes: List<TutorialNode>) {
                for (n in nodes) when (n) {
                    is TutorialStep -> list.add(n)
                    is TutorialFork -> collect(n.getAllInternalNodes())
                }
            }
            collect(_steps)
            return list
        }

    fun getActiveSteps(): List<TutorialStep> = allStepsFlatMap.filter { it.isActive }

    fun onProfileJoin() {
        allStepsFlatMap.forEach { it.refresh(this) }
    }

    fun generateNodeId(): String = "tutorial_node_${_steps.size + 1}"

    fun addNode(node: TutorialNode) {
        node.populateNodeIds(this)
        _steps.add(node)
        invalidateRenderable()
    }

    fun refresh() {
        _steps.forEach { it.refresh(this) }
        selectedPathIds.clear()
    }

    lateinit var requiredResources: Map<NeuInternalName, Double>
        private set


    private fun invalidateRenderable() {
    }

    private fun invalidateAndRebuild() {
        cachedShowDescriptions
        invalidateRenderable()
    }

    fun onLoad() {
    }


    fun getSelectedOption(options: List<SelectPathTutorialFork.SelectedPathTutorialFork>): SelectPathTutorialFork.SelectedPathTutorialFork? {
        options.forEach { if (selectedPathIds.contains(it.option.id)) return it }
        if (options.size == 1) return options.first()
        ChatUtils.chat(
            "§e[SkyHanni Tutorial]: The Currently loaded Tutorial has ${options.size} options for you to choose from on how to continue",
            prefix = false,
        )
        options.forEach {
            ChatUtils.clickableChat(
                "§e[SkyHanni Tutorial]: ${it.option.name} : ${it.option.guideMakerDescription}",
                { selectPath(it.option.id) },
            )
        }
        return null
    }

    @HandleEvent
    fun onTutorialStepComplete(event: TutorialStepCompleteEvent) {
        refreshCaches()
        invalidateAndRebuild()
    }

    fun refreshCaches() {
        SkyHanniMod.feature.tutorials.protectResourcesAcrossPaths
        val contributors = allStepsFlatMap.filterIsInstance<ResourceContributor>()
        val pending = contributors.filter { step ->
            val asStep = step as? TutorialStep
            asStep == null || !asStep.completed
        }
        val map = mutableMapOf<NeuInternalName, Double>()
        fun addAll(res: Map<NeuInternalName, Double>) {
            res.forEach { (k, v) -> map.merge(k, v) { a, b -> a + b } }
        }
        pending.forEach { addAll(it.getRequiredResources(this)) }
        requiredResources = map
        // todo: if includeAllPaths == true, incorporate optional path resource needs
    }

    fun getRenderable(showDescriptions: Boolean): Renderable {
        val cached = cachedRenderable
        if (cached != null && cachedShowDescriptions == showDescriptions) return cached
        val built = TutorialRenderableBuilder.build(this, showDescriptions)
        cachedRenderable = built
        cachedShowDescriptions = showDescriptions
        return built
    }

    private fun validate() {
        val errors = mutableListOf<String>()
        fun validateNode(node: TutorialNode) {
            errors += node.validate(this)
            when (node) {
                is TutorialStep -> node.getRequirements().forEach { validateNode(it) }
                is TutorialFork -> node.getAllInternalNodes().forEach { validateNode(it) }
            }
        }
        _steps.forEach { validateNode(it) }
        if (errors.isNotEmpty()) {
            ChatUtils.chat("§c[SkyHanni Tutorial] Found ${errors.size} configuration issue(s):", prefix = false)
            errors.forEach { ChatUtils.chat("§7- $it", prefix = false) }
        }
    }

    // Cached renderable of the tree to avoid rebuilding each frame
    private var cachedRenderable: Renderable? = null
}
