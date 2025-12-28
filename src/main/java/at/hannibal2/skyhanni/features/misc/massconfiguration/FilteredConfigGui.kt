package at.hannibal2.skyhanni.features.misc.massconfiguration

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.BlockingMoulConfigProcessor
import at.hannibal2.skyhanni.features.misc.update.UpdateManager
import at.hannibal2.skyhanni.utils.ConfigUtils
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import java.lang.reflect.Field

/** Opens a MoulConfig editor that renders only whitelisted option paths. */
object FilteredConfigGui {

    // Debugging helper; set to true to enable debug prints from this file.
    private const val DEBUG = true
    private fun dbg(msg: String) {
        if (DEBUG) println("[FilteredConfigGui] $msg")
    }

    fun open(allowedPaths: Set<String>) {
        if (allowedPaths.isEmpty()) return
        // Use provided set as-is; allow suffix matching to align nested paths with allowed tails.
        val allowedExact = allowedPaths.toSet()
        val normalizedAllowed = allowedExact.flatMap { ap ->
            val parts = ap.split('.')
            parts.indices.map { idx -> parts.take(idx + 1).joinToString(".") }
        }.toSet()
        println("[FilteredConfigGui] open called with allowedPaths (${allowedExact.size}): ${allowedExact.sorted()}")

        // Counting pass: determine which option full paths actually produce a visible editor.
        val counter = CountingMoulConfigProcessor(allowedExact, normalizedAllowed)
        // Register builtin editors for the counting processor so it can create option GUIs.
        BuiltinMoulConfigGuis.addProcessors(counter)
        UpdateManager.injectConfigProcessor(counter)
        val counterDriver = ConfigProcessorDriver(counter)
        counterDriver.warnForPrivateFields = false
        counterDriver.checkExpose = false
        counterDriver.processConfig(SkyHanniMod.feature)
        val usedPrefixes = counter.usedPrefixes
        println("[FilteredConfigGui] usedPrefixes (${usedPrefixes.size}): ${usedPrefixes.sorted()}")

        // Real pass: only start categories/accordions that contain visible options.
        val processor = FilteringMoulConfigProcessor(allowedExact, normalizedAllowed, usedPrefixes)
        BuiltinMoulConfigGuis.addProcessors(processor)
        UpdateManager.injectConfigProcessor(processor)
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.checkExpose = false
        driver.processConfig(SkyHanniMod.feature)

        val editor = MoulConfigEditor(processor)
        ConfigUtils.openEditor(editor)
    }

    private class FilteringMoulConfigProcessor(
        private val allowedExact: Set<String>,
        private val normalizedAllowed: Set<String>,
        private val usedPrefixes: Set<String>,
    ) : BlockingMoulConfigProcessor() {

        private val pathStack = ArrayDeque<String>()
        private val skippedCategories = ArrayDeque<Boolean>()
        private val skippedAccordions = ArrayDeque<Boolean>()
        private var skipDepth = 0

        private fun currentPath(): String = pathStack.joinToString(".")

        // Returns true if any used option path lives inside the subtree rooted at `prefix`.
        private fun hasAllowedInSubtree(prefix: String): Boolean {
            if (prefix.isEmpty()) return true
            return usedPrefixes.any { it == prefix || it.startsWith("$prefix.") }
        }

        // Match when the tail of the path equals an allowed entry (supports nested categories with different roots).
        private fun pathRelatedToAllowed(path: String): Boolean {
            val pathParts = path.split('.')
            return allowedExact.any { ap ->
                val allowParts = ap.split('.')
                val common = allowParts.zip(pathParts).reversed().takeWhile { it.first == it.second }.size
                when {
                    allowParts.size == 1 || pathParts.size == 1 -> allowParts.size == pathParts.size && allowParts.last() == pathParts.last()
                    else -> common >= 2 // need at least two matching segments from the end (ignores missing leading category like "event")
                }
            }
        }

        private fun matchesAllowedFull(path: String): Boolean = pathRelatedToAllowed(path)

        private fun matchesAllowedAny(paths: List<String?>): Boolean = paths.filterNotNull().any { pathRelatedToAllowed(it) }

        // Diagnostic helper: return allowed paths that relate to any candidate path
        private fun collectMatchingAllowed(paths: List<String?>): List<String> {
            val res = mutableListOf<String>()
            for (p in paths.filterNotNull()) {
                val pathParts = p.split('.')
                allowedExact.forEach { ap ->
                    val allowParts = ap.split('.')
                    val common = allowParts.zip(pathParts).reversed().takeWhile { it.first == it.second }.size
                    val matches = when {
                        allowParts.size == 1 || pathParts.size == 1 -> allowParts.size == pathParts.size && allowParts.last() == pathParts.last()
                        else -> common >= 2
                    }
                    if (matches) res.add(ap)
                }
            }
            return res.distinct()
        }

        override fun pushPath(fieldPath: String) {
            pathStack.addLast(fieldPath)
            super.pushPath(fieldPath)
        }

        override fun popPath() {
            super.popPath()
            pathStack.removeLastOrNull()
        }

        override fun setCategoryParent(field: Field) {
            if (skipDepth > 0) return
            try {
                super.setCategoryParent(field)
            } catch (_: Throwable) {
            }
        }

        override fun beginCategory(baseObject: Any?, field: Field?, name: String, description: String) {
            val prefix = listOfNotNull(currentPath().takeIf { it.isNotEmpty() }, field?.name).joinToString(".")
            dbg("beginCategory prefix=$prefix allowedCount=${allowedExact.size} skipDepth=$skipDepth")
            val skip = if (skipDepth > 0) {
                true
            } else if (pathStack.isEmpty()) {
                val fieldName = field?.name
                val hasTop = fieldName != null && (
                    usedPrefixes.any { up -> up == fieldName || up.startsWith("$fieldName.") }
                        || allowedExact.any { ap -> ap.startsWith("$fieldName.") || ap.split('.').contains(fieldName) }
                    )
                dbg(" -> top-level hasAllowed=$hasTop")
                !hasTop
            } else {
                !hasAllowedInSubtree(prefix)
            }
            dbg(" -> hasAllowed=${!skip && skipDepth == 0}")
            skippedCategories.addLast(skip)
            if (skip) {
                skipDepth++
                return
            }
            super.beginCategory(baseObject, field, name, description)
        }

        override fun endCategory() {
            val skipped = skippedCategories.removeLastOrNull() ?: false
            if (skipped) {
                if (skipDepth > 0) skipDepth--
                return
            }
            if (skipDepth > 0) return
            super.endCategory()
        }

        override fun beginAccordion(baseObject: Any?, field: Field?, o: ConfigOption?, id: Int) {
            if (skipDepth > 0) {
                skippedAccordions.addLast(true)
                skipDepth++
                return
            }
            val prefix = listOfNotNull(currentPath().takeIf { it.isNotEmpty() }, field?.name).joinToString(".")
            dbg("beginAccordion prefix=$prefix")
            val skip = !hasAllowedInSubtree(prefix)
            dbg(" -> hasAllowed=${!skip}")
            skippedAccordions.addLast(skip)
            if (skip) {
                skipDepth++
                return
            }
            super.beginAccordion(baseObject, field, o, id)
        }

        override fun endAccordion() {
            val skipped = skippedAccordions.removeLastOrNull() ?: false
            if (skipped) {
                if (skipDepth > 0) skipDepth--
                return
            }
            if (skipDepth > 0) return
            super.endAccordion()
        }

        override fun emitOption(baseObject: Any, field: Field, option: ConfigOption) {
            if (skipDepth > 0) return
            if (field.type.name == "kotlin.Unit") return
            val fullPath = listOfNotNull(currentPath().takeIf { it.isNotEmpty() }, field.name).joinToString(".")
            val matchFull = matchesAllowedFull(fullPath)
            dbg("emitOption fullPath=$fullPath matchFull=$matchFull skipDepth=$skipDepth fieldType=${field.type.name}")
            if (!matchFull) return
            try {
                super.emitOption(baseObject, field, option)
            } catch (_: Throwable) {
            }
        }

        override fun createOptionGui(processedOption: ProcessedOption, field: Field, option: ConfigOption): GuiOptionEditor? {
            val fullPath = listOfNotNull(currentPath().takeIf { it.isNotEmpty() }, field.name).joinToString(".")
            val processedPath = processedOption.getPath()
            val catPrefix = processedOption.category.parentCategoryId
            val candidates =
                listOf(fullPath, processedPath, catPrefix, catPrefix?.let { if (processedPath != null) "$it.$processedPath" else null })
            val matchedAny = matchesAllowedAny(candidates)
            dbg("createOptionGui fullPath=$fullPath processedPath=$processedPath catPrefix=$catPrefix candidates=$candidates matchedAny=$matchedAny skipDepth=$skipDepth")
            if (!matchedAny) {
                val matches = collectMatchingAllowed(candidates)
                dbg(" -> no match for candidates=$candidates; matchingAllowedPaths=${matches}")
                return null
            }
            if (skipDepth > 0) return null
            val editor = try {
                super.createOptionGui(processedOption, field, option)
            } catch (_: Throwable) {
                null
            }
            if (editor == null) return null
            // Use the earlier matchedAny (which includes fallback rules) to decide visibility
            val matching = collectMatchingAllowed(candidates)
            dbg(" -> candidatesChecked=${candidates.filterNotNull()} matchingAllowedPaths=$matching allowedPathsSize=${allowedExact.size}")
            dbg("createdEditor fullPath=$fullPath processedPath=$processedPath editor=${editor.javaClass.simpleName}")
            return editor
        }
    }

    // Counting processor: tracks which option full paths (and their prefixes) yield a visible editor
    private class CountingMoulConfigProcessor(
        private val allowedExact: Set<String>,
        private val normalizedAllowed: Set<String>,
    ) : BlockingMoulConfigProcessor() {
        val usedPrefixes = mutableSetOf<String>()
        private val pathStack = ArrayDeque<String>()

        private fun currentPath(): String = pathStack.joinToString(".")

        // Match when the tail of the path equals an allowed entry
        private fun pathRelatedToAllowed(path: String): Boolean {
            val pathParts = path.split('.')
            return allowedExact.any { ap ->
                val allowParts = ap.split('.')
                val common = allowParts.zip(pathParts).reversed().takeWhile { it.first == it.second }.size
                when {
                    allowParts.size == 1 || pathParts.size == 1 -> allowParts.size == pathParts.size && allowParts.last() == pathParts.last()
                    else -> common >= 2
                }
            }
        }

        private fun matchesAllowedAny(paths: List<String?>): Boolean = paths.filterNotNull().any { pathRelatedToAllowed(it) }

        override fun pushPath(fieldPath: String) {
            pathStack.addLast(fieldPath)
            super.pushPath(fieldPath)
        }

        override fun popPath() {
            super.popPath()
            pathStack.removeLastOrNull()
        }

        override fun emitOption(baseObject: Any, field: Field, option: ConfigOption) {
            // Skip kotlin.Unit fields (often private/transient) to avoid creating editors for them
            if (field.type.name == "kotlin.Unit") return
            // Previously we attempted to do a "plausible" pre-search filter here which
            // could omit options from the counting pass. Removing that heuristics means
            // we will attempt to create editors for all emitted options so usedPrefixes
            // accurately reflects what actually creates a GUI editor.
            val fullPath = listOfNotNull(currentPath().takeIf { it.isNotEmpty() }, field.name).joinToString(".")
            dbg("Counting.emitOption fullPath=$fullPath (no pre-search filtering)")
            try {
                super.emitOption(baseObject, field, option)
            } catch (_: Throwable) {
                // ignore
            }
        }

        override fun createOptionGui(processedOption: ProcessedOption, field: Field, option: ConfigOption): GuiOptionEditor? {
            val fullPath = listOfNotNull(currentPath().takeIf { it.isNotEmpty() }, field.name).joinToString(".")
            val processedPath = processedOption.getPath()
            val catPrefix = processedOption.category.parentCategoryId
            val candidates =
                listOf(fullPath, processedPath, catPrefix, catPrefix?.let { if (processedPath != null) "$it.$processedPath" else null })
            val matched = matchesAllowedAny(candidates)
            dbg("Counting.createOptionGui fullPath=$fullPath processedPath=$processedPath catPrefix=$catPrefix candidates=$candidates matched=$matched")
            if (!matched) return null
            val editor = try {
                super.createOptionGui(processedOption, field, option)
            } catch (_: Throwable) {
                null
            }
            if (editor != null) {
                val optPath = processedPath ?: fullPath
                val pathsToPrefix = buildList {
                    add(optPath)
                    add(fullPath)
                    add(currentPath())
                    catPrefix?.let { add(it) }
                }
                pathsToPrefix.filter { !it.isNullOrBlank() }.forEach { p ->
                    val parts = p.split('.').filter { it.isNotBlank() }
                    for (i in 1..parts.size) usedPrefixes.add(parts.take(i).joinToString("."))
                    // Also add the leaf segment to catch top-level categories whose names are only present in a parent chain (e.g., "event")
                    if (parts.isNotEmpty()) usedPrefixes.add(parts.last())
                }
                // Also add parent prefixes of the allowed path(s) that matched, so missing leading segments (e.g., "event") are preserved
                val matchingAllowed = mutableSetOf<String>()
                candidates.filterNotNull().forEach { cand ->
                    allowedExact.forEach { ap ->
                        val allowParts = ap.split('.')
                        val candParts = cand.split('.')
                        val minSize = minOf(allowParts.size, candParts.size)
                        val matches = if (minSize == 1) {
                            allowParts.size == candParts.size && allowParts.last() == candParts.last()
                        } else {
                            allowParts.takeLast(minSize) == candParts.takeLast(minSize)
                        }
                        if (matches) matchingAllowed.add(ap)
                    }
                }
                matchingAllowed.forEach { ap ->
                    val parts = ap.split('.')
                    for (i in 1..parts.size) usedPrefixes.add(parts.take(i).joinToString("."))
                }
            }
            return editor
        }
    }
}
