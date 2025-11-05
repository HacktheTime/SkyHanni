package at.hannibal2.skyhanni.features.tutorial.gui

import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.paths.AsyncTutorialFork
import de.hype.bingonet.shared.tutorials.paths.OptionalTutorialFork
import de.hype.bingonet.shared.tutorials.paths.SelectPathTutorialFork

/**
 * Utility functions to modify Tutorial node trees immutably.
 */
object TutorialTreeOps {
    fun removeNodeById(nodes: List<TutorialNode>, targetId: String): List<TutorialNode> {
        val out = mutableListOf<TutorialNode>()
        for (n in nodes) {
            if (n.nodeId == targetId) continue
            when (n) {
                is AsyncTutorialFork -> {
                    val inner = removeNodeById(n.pathNodes, targetId)
                    out += if (inner === n.pathNodes) n else AsyncTutorialFork(inner)
                }
                is SelectPathTutorialFork -> {
                    var changed = false
                    val newPaths = n.paths.map { sp ->
                        val inner = removeNodeById(sp.pathNodes, targetId)
                        if (inner !== sp.pathNodes) changed = true
                        SelectPathTutorialFork.SelectedPathTutorialFork(sp.option, inner)
                    }
                    out += if (changed) SelectPathTutorialFork(newPaths) else n
                }
                is OptionalTutorialFork -> {
                    var changed = false
                    val newPaths = n.paths.map { (path, hidden) ->
                        val inner = removeNodeById(path, targetId)
                        if (inner !== path) changed = true
                        inner to hidden
                    }
                    out += if (changed) OptionalTutorialFork(newPaths) else n
                }
                else -> out += n
            }
        }
        return out
    }

    /**
     * Removes a node by id and returns the updated list and the removed node (if found).
     */
    fun extractNodeById(nodes: List<TutorialNode>, targetId: String): Pair<List<TutorialNode>, TutorialNode?> {
        val out = mutableListOf<TutorialNode>()
        var removed: TutorialNode? = null
        for (n in nodes) {
            if (removed == null && n.nodeId == targetId) {
                removed = n
                continue
            }
            when (n) {
                is AsyncTutorialFork -> {
                    val (inner, rem) = extractNodeById(n.pathNodes, targetId)
                    if (rem != null) {
                        removed = rem
                        out += AsyncTutorialFork(inner)
                    } else {
                        out += n
                    }
                }
                is SelectPathTutorialFork -> {
                    var changed = false
                    val newPaths = n.paths.map { sp ->
                        val (inner, rem) = extractNodeById(sp.pathNodes, targetId)
                        if (rem != null) { changed = true; removed = rem }
                        SelectPathTutorialFork.SelectedPathTutorialFork(sp.option, inner)
                    }
                    out += if (changed) SelectPathTutorialFork(newPaths) else n
                }
                is OptionalTutorialFork -> {
                    var changed = false
                    val newPaths = n.paths.map { (path, hidden) ->
                        val (inner, rem) = extractNodeById(path, targetId)
                        if (rem != null) { changed = true; removed = rem }
                        inner to hidden
                    }
                    out += if (changed) OptionalTutorialFork(newPaths) else n
                }
                else -> out += n
            }
        }
        return out to removed
    }

    fun replaceNodeById(
        nodes: List<TutorialNode>,
        targetId: String,
        replacement: () -> TutorialNode,
    ): List<TutorialNode> {
        val out = mutableListOf<TutorialNode>()
        var replaced = false
        for (n in nodes) {
            if (!replaced && n.nodeId == targetId) {
                out += replacement()
                replaced = true
                continue
            }
            when (n) {
                is AsyncTutorialFork -> {
                    val inner = replaceNodeById(n.pathNodes, targetId, replacement)
                    out += if (inner === n.pathNodes) n else AsyncTutorialFork(inner)
                    replaced = replaced || (inner !== n.pathNodes)
                }
                is SelectPathTutorialFork -> {
                    var changed = false
                    val newPaths = n.paths.map { sp ->
                        val inner = replaceNodeById(sp.pathNodes, targetId, replacement)
                        if (inner !== sp.pathNodes) changed = true
                        SelectPathTutorialFork.SelectedPathTutorialFork(sp.option, inner)
                    }
                    out += if (changed) SelectPathTutorialFork(newPaths) else n
                    replaced = replaced || changed
                }
                is OptionalTutorialFork -> {
                    var changed = false
                    val newPaths = n.paths.map { (path, hidden) ->
                        val inner = replaceNodeById(path, targetId, replacement)
                        if (inner !== path) changed = true
                        inner to hidden
                    }
                    out += if (changed) OptionalTutorialFork(newPaths) else n
                    replaced = replaced || changed
                }
                else -> out += n
            }
        }
        return out
    }

    fun addNodeInsideFork(
        nodes: List<TutorialNode>,
        forkId: String,
        pathIndex: Int,
        node: TutorialNode,
    ): List<TutorialNode> {
        val out = nodes.toMutableList()
        for (i in nodes.indices) {
            when (val n = nodes[i]) {
                is AsyncTutorialFork -> {
                    if (n.nodeId == forkId) {
                        val list = n.pathNodes.toMutableList()
                        list.add(node)
                        out[i] = AsyncTutorialFork(list)
                        return out
                    }
                    val inner = addNodeInsideFork(n.pathNodes, forkId, pathIndex, node)
                    if (inner !== n.pathNodes) {
                        out[i] = AsyncTutorialFork(inner)
                        return out
                    }
                }
                is SelectPathTutorialFork -> {
                    if (n.nodeId == forkId) {
                        if (pathIndex !in n.paths.indices) return nodes
                        val newPaths = n.paths.toMutableList()
                        val p = n.paths[pathIndex]
                        val list = p.pathNodes.toMutableList()
                        list.add(node)
                        newPaths[pathIndex] = SelectPathTutorialFork.SelectedPathTutorialFork(p.option, list)
                        out[i] = SelectPathTutorialFork(newPaths)
                        return out
                    }
                    // Recurse into each selectable path
                    for (j in n.paths.indices) {
                        val p = n.paths[j]
                        val inner = addNodeInsideFork(p.pathNodes, forkId, pathIndex, node)
                        if (inner !== p.pathNodes) {
                            val newPaths = n.paths.toMutableList()
                            newPaths[j] = SelectPathTutorialFork.SelectedPathTutorialFork(p.option, inner)
                            out[i] = SelectPathTutorialFork(newPaths)
                            return out
                        }
                    }
                }
                is OptionalTutorialFork -> {
                    if (n.nodeId == forkId) {
                        if (pathIndex !in n.paths.indices) return nodes
                        val newPaths = n.paths.toMutableList()
                        val (path, hidden) = n.paths[pathIndex]
                        val list = path.toMutableList()
                        list.add(node)
                        newPaths[pathIndex] = list to hidden
                        out[i] = OptionalTutorialFork(newPaths)
                        return out
                    }
                    // Recurse into each optional path
                    for (j in n.paths.indices) {
                        val (path, hidden) = n.paths[j]
                        val inner = addNodeInsideFork(path, forkId, pathIndex, node)
                        if (inner !== path) {
                            val newPaths = n.paths.toMutableList()
                            newPaths[j] = inner to hidden
                            out[i] = OptionalTutorialFork(newPaths)
                            return out
                        }
                    }
                }
                else -> {}
            }
        }
        return nodes
    }

    /**
     * Same as [addNodeInsideFork] but reports whether the tree was modified.
     */
    fun addNodeInsideForkWithResult(
        nodes: List<TutorialNode>,
        forkId: String,
        pathIndex: Int,
        node: TutorialNode,
    ): Pair<List<TutorialNode>, Boolean> {
        val out = nodes.toMutableList()
        for (i in nodes.indices) {
            when (val n = nodes[i]) {
                is AsyncTutorialFork -> {
                    if (n.nodeId == forkId) {
                        val list = n.pathNodes.toMutableList()
                        list.add(node)
                        out[i] = AsyncTutorialFork(list)
                        return out to true
                    }
                    val (inner, changed) = addNodeInsideForkWithResult(n.pathNodes, forkId, pathIndex, node)
                    if (changed) {
                        out[i] = AsyncTutorialFork(inner)
                        return out to true
                    }
                }
                is SelectPathTutorialFork -> {
                    if (n.nodeId == forkId) {
                        if (pathIndex !in n.paths.indices) return nodes to false
                        val newPaths = n.paths.toMutableList()
                        val p = n.paths[pathIndex]
                        val list = p.pathNodes.toMutableList()
                        list.add(node)
                        newPaths[pathIndex] = SelectPathTutorialFork.SelectedPathTutorialFork(p.option, list)
                        out[i] = SelectPathTutorialFork(newPaths)
                        return out to true
                    }
                    for (j in n.paths.indices) {
                        val p = n.paths[j]
                        val (inner, changed) = addNodeInsideForkWithResult(p.pathNodes, forkId, pathIndex, node)
                        if (changed) {
                            val newPaths = n.paths.toMutableList()
                            newPaths[j] = SelectPathTutorialFork.SelectedPathTutorialFork(p.option, inner)
                            out[i] = SelectPathTutorialFork(newPaths)
                            return out to true
                        }
                    }
                }
                is OptionalTutorialFork -> {
                    if (n.nodeId == forkId) {
                        if (pathIndex !in n.paths.indices) return nodes to false
                        val newPaths = n.paths.toMutableList()
                        val (path, hidden) = n.paths[pathIndex]
                        val list = path.toMutableList()
                        list.add(node)
                        newPaths[pathIndex] = list to hidden
                        out[i] = OptionalTutorialFork(newPaths)
                        return out to true
                    }
                    for (j in n.paths.indices) {
                        val (path, hidden) = n.paths[j]
                        val (inner, changed) = addNodeInsideForkWithResult(path, forkId, pathIndex, node)
                        if (changed) {
                            val newPaths = n.paths.toMutableList()
                            newPaths[j] = inner to hidden
                            out[i] = OptionalTutorialFork(newPaths)
                            return out to true
                        }
                    }
                }
                else -> {}
            }
        }
        return nodes to false
    }
}
