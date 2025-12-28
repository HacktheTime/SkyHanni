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
    private fun dbg(msg: String) { if (DEBUG) println("[FilteredConfigGui] $msg") }

    fun open(allowedPaths: Set<String>) {
        if (allowedPaths.isEmpty()) return
        println("[FilteredConfigGui] open called with allowedPaths (${allowedPaths.size}): ${allowedPaths.sorted()}")

        // Counting pass: determine which option full paths actually produce a visible editor.
        val counter = CountingMoulConfigProcessor(allowedPaths)
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
        val processor = FilteringMoulConfigProcessor(allowedPaths, usedPrefixes)
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
        allowed: Set<String>,
        private val usedPrefixes: Set<String>,
    ) : BlockingMoulConfigProcessor() {

        private val allowedPaths = allowed.toSet()
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

        // Return true if `path` and any allowed path are in the same ancestor/descendant relation
        private fun pathRelatedToAllowed(path: String): Boolean {
            return allowedPaths.any { ap ->
                // keep only strict ancestor/descendant/direct-equality relations; remove fallback suffix/search matching
                if (ap == path || ap.startsWith("$path.") || path.startsWith("$ap.")) return@any true
                false
            }
        }

        private fun matchesAllowedFull(path: String): Boolean {
            return pathRelatedToAllowed(path)
        }

        private fun matchesAllowedAny(paths: List<String?>): Boolean {
            return paths.filterNotNull().any { path -> pathRelatedToAllowed(path) }
        }

        // Diagnostic helper: return allowed paths that relate to any candidate path
        private fun collectMatchingAllowed(paths: List<String?>): List<String> {
            val res = mutableListOf<String>()
            for (p in paths.filterNotNull()) {
                for (ap in allowedPaths) {
                    if (ap == p || ap.startsWith("$p.") || p.startsWith("$ap.")) {
                        res.add(ap)
                    }
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
            try { super.setCategoryParent(field) } catch (_: Throwable) {}
        }

        override fun beginCategory(baseObject: Any?, field: Field?, name: String, description: String) {
            val prefix = listOfNotNull(currentPath().takeIf { it.isNotEmpty() }, field?.name).joinToString(".")
            dbg("beginCategory prefix=$prefix allowedCount=${allowedPaths.size} skipDepth=$skipDepth")
            val skip = if (skipDepth > 0) {
                true
            } else if (pathStack.isEmpty()) {
                val fieldName = field?.name
                val hasTop = fieldName != null && (
                    usedPrefixes.any { up -> up == fieldName || up.startsWith("$fieldName.") } ||
                        allowedPaths.any { ap -> ap == fieldName || ap.startsWith("$fieldName.") }
                )
                dbg(" -> top-level hasAllowed=$hasTop")
                !hasTop
            } else {
                !hasAllowedInSubtree(prefix)
            }
            dbg(" -> hasAllowed=${!skip && skipDepth==0}")
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
            try { super.emitOption(baseObject, field, option) } catch (_: Throwable) {}
        }

        override fun createOptionGui(processedOption: ProcessedOption, field: Field, option: ConfigOption): GuiOptionEditor? {
            val fullPath = listOfNotNull(currentPath().takeIf { it.isNotEmpty() }, field.name).joinToString(".")
            val processedPath = processedOption.getPath()
            val catPrefix = processedOption.category.parentCategoryId
            val candidates = listOf(fullPath, processedPath, catPrefix, catPrefix?.let { if (processedPath != null) "$it.$processedPath" else null })
            val matchedAny = matchesAllowedAny(candidates)
            dbg("createOptionGui fullPath=$fullPath processedPath=$processedPath catPrefix=$catPrefix candidates=$candidates matchedAny=$matchedAny skipDepth=$skipDepth")
            if (!matchedAny) {
                val matches = collectMatchingAllowed(candidates)
                dbg(" -> no match for candidates=$candidates; matchingAllowedPaths=${matches}")
                return null
            }
            if (skipDepth > 0) return null
            val editor = try { super.createOptionGui(processedOption, field, option) } catch (_: Throwable) { null }
            if (editor == null) return null
            // Use the earlier matchedAny (which includes fallback rules) to decide visibility
            val matching = collectMatchingAllowed(candidates)
            dbg(" -> candidatesChecked=${candidates.filterNotNull()} matchingAllowedPaths=$matching allowedPathsSize=${allowedPaths.size}")
            dbg("createdEditor fullPath=$fullPath processedPath=$processedPath editor=${editor.javaClass.simpleName}")
            return editor
        }
    }

    // Counting processor: tracks which option full paths (and their prefixes) yield a visible editor
    private class CountingMoulConfigProcessor(
        allowed: Set<String>,
    ) : BlockingMoulConfigProcessor() {
        private val allowedPaths = allowed.toSet()
        val usedPrefixes = mutableSetOf<String>()
        private val pathStack = ArrayDeque<String>()

        private fun currentPath(): String = pathStack.joinToString(".")

        // Return true if `path` and any allowed path are in the same ancestor/descendant relation
        private fun pathRelatedToAllowed(path: String): Boolean {
            return allowedPaths.any { ap ->
                if (ap == path || ap.startsWith("$path.") || path.startsWith("$ap.")) return@any true
                // removed fallback suffix/search matching here to avoid fuzzy matches in final filtering
                false
            }
        }

        private fun matchesAllowedAny(paths: List<String?>): Boolean {
            return paths.filterNotNull().any { path -> pathRelatedToAllowed(path) }
        }

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
            val candidates = listOf(fullPath, processedPath, catPrefix, catPrefix?.let { if (processedPath != null) "$it.$processedPath" else null })
            val matched = matchesAllowedAny(candidates)
            dbg("Counting.createOptionGui fullPath=$fullPath processedPath=$processedPath catPrefix=$catPrefix candidates=$candidates matched=$matched")
            if (!matched) return null
            val editor = try { super.createOptionGui(processedOption, field, option) } catch (_: Throwable) { null }
            if (editor != null) {
                // Record the processed/opt path prefixes so Filtering can decide which
                // categories/accordions contain visible options.
                val optPath = processedPath ?: fullPath
                val parts = optPath.split('.')
                for (i in 1..parts.size) {
                    usedPrefixes.add(parts.take(i).joinToString("."))
                }
                dbg("Counting.createdEditor optPath=$optPath addedPrefixes=${parts}")
            }
            return editor
        }
    }
}
