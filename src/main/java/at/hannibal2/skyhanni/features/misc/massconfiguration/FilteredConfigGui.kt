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
        driver.processConfig(SkyHanniMod.feature)

        val editor = MoulConfigEditor(processor)
        ConfigUtils.openEditor(editor)
    }

    /** Hides any option whose path is not whitelisted. */
    private class FilteringMoulConfigProcessor(
        private val allowed: Set<String>,
    ) : BlockingMoulConfigProcessor() {

        private fun isBlocked(field: Field): Boolean {
            // Drop anything not public (accessor flags) and unsupported types
            if (!Modifier.isPublic(field.modifiers)) return true
            val className = field.declaringClass.name
            if (className.startsWith("at.hannibal2.skyhanni.config.features.About")) return true
            if (field.type == Unit::class.java) return true
            if (field.type == Runnable::class.java) return true
            return false
        }

        private fun matchesAllowed(field: Field): Boolean {
            val name = field.name
            return allowed.any { it.endsWith(".$name") || it == name }
        }

        private fun matchesAllowedPath(path: String): Boolean {
            return allowed.any { allow -> allow == path || allow.endsWith(".$path") }
        }

        override fun emitOption(baseObject: Any, field: Field, option: ConfigOption) {
            if (isBlocked(field)) return
            if (!matchesAllowed(field)) return
            runCatching { super.emitOption(baseObject, field, option) }
        }

        override fun createOptionGui(processedOption: ProcessedOption, field: Field, option: ConfigOption): GuiOptionEditor? {
            if (isBlocked(field)) return null
            val path = processedOption.getPath() ?: return null
            if (!matchesAllowedPath(path)) return null
            return super.createOptionGui(processedOption, field, option)
        }
    }
}
