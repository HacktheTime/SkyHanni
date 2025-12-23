package at.hannibal2.skyhanni.features.misc.massconfiguration

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.BlockingMoulConfigProcessor
import at.hannibal2.skyhanni.config.GuiOptionEditorHidden
import at.hannibal2.skyhanni.features.misc.update.UpdateManager
import at.hannibal2.skyhanni.utils.ConfigUtils
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/** Opens a MoulConfig editor that renders only whitelisted option paths. */
object FilteredConfigGui {

    fun open(allowedPaths: Set<String>) {
        if (allowedPaths.isEmpty()) return
        val processor = FilteringMoulConfigProcessor(allowedPaths)
        BuiltinMoulConfigGuis.addProcessors(processor)
        UpdateManager.injectConfigProcessor(processor)
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.checkExpose = false
        driver.processConfig(SkyHanniMod.feature)

        val editor = MoulConfigEditor(processor)
        ConfigUtils.openEditor(editor)
    }

    /** Hides any option whose path is not whitelisted. */
    private class FilteringMoulConfigProcessor(
        allowed: Set<String>,
    ) : BlockingMoulConfigProcessor() {

        private val allowedPaths = allowed.toSet()
        private val pathStack = ArrayDeque<String>()
        private val skippedCategories = ArrayDeque<Boolean>()
        private val skippedAccordions = ArrayDeque<Boolean>()
        private var skipDepth = 0

        private fun currentPath(): String = pathStack.joinToString(".")

        private fun suffixes(path: String): List<String> {
            val parts = path.split('.')
            return (parts.indices).map { idx -> parts.drop(idx).joinToString(".") }.filter { it.isNotEmpty() }
        }

        private fun hasAllowedInSubtree(prefix: String): Boolean {
            if (pathStack.isEmpty()) return allowedPaths.isNotEmpty()
            if (prefix.isEmpty()) return true
            return allowedPaths.any { allowed -> allowed == prefix || allowed.startsWith("$prefix.") || prefix.startsWith("$allowed.") }
        }

        private fun matchesAllowedFull(path: String): Boolean {
            return allowedPaths.any { allowed -> allowed == path || allowed.startsWith("$path.") || path.startsWith("$allowed.") }
        }

        private fun matchesAllowedAny(paths: List<String?>): Boolean {
            return paths.filterNotNull().any { path ->
                allowedPaths.any { allow -> allow == path || allow.startsWith("$path.") || path.startsWith("$allow.") }
            }
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
            super.setCategoryParent(field)
        }

        override fun beginCategory(baseObject: Any?, field: Field?, name: String, description: String) {
            val prefix = listOfNotNull(currentPath().takeIf { it.isNotEmpty() }, field?.name).joinToString(".")
            val skip = skipDepth > 0 || !hasAllowedInSubtree(prefix)
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
                skipDepth--
                return
            }
            if (skipDepth > 0) return
            super.endCategory()
        }

        override fun beginAccordion(baseObject: Any?, field: Field?, o: ConfigOption?, id: Int) {
            val prefix = listOfNotNull(currentPath().takeIf { it.isNotEmpty() }, field?.name).joinToString(".")
            val skip = skipDepth > 0 || !hasAllowedInSubtree(prefix)
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
                skipDepth--
                return
            }
            if (skipDepth > 0) return
            super.endAccordion()
        }

        override fun emitOption(baseObject: Any, field: Field, option: ConfigOption) {
            if (skipDepth > 0) return
            val fullPath = listOfNotNull(currentPath().takeIf { it.isNotEmpty() }, field.name).joinToString(".")
            if (!matchesAllowedFull(fullPath)) return
            try {
                super.emitOption(baseObject, field, option)
            } catch (_: RuntimeException) {
                // Ignore unsupported option editors in filtered mode
            }
        }

        override fun createOptionGui(processedOption: ProcessedOption, field: Field, option: ConfigOption): GuiOptionEditor? {
            if (skipDepth > 0) return null
            val fullPath = listOfNotNull(currentPath().takeIf { it.isNotEmpty() }, field.name).joinToString(".")
            val processedPath = processedOption.getPath()
            val catPrefix = processedOption.category.parentCategoryId
            val candidates = listOf(fullPath, processedPath, catPrefix?.let { if (processedPath != null) "$it.$processedPath" else null })
            if (!matchesAllowedAny(candidates)) return null
            return super.createOptionGui(processedOption, field, option)
        }
    }
}
