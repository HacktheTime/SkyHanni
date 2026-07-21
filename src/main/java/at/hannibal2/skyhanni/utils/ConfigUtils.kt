package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigGuiManager
import at.hannibal2.skyhanni.config.MoulConfigEditorComponent
import at.hannibal2.skyhanni.features.pets.PetDisplayConfigGuiManager
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.lang.reflect.Field
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField

object ConfigUtils {

    private const val UNKNOWN_EDITOR_INDEX = -1

    private val editorProviders = listOf<() -> MoulConfigEditor<*>>(
        ConfigGuiManager::getEditorInstance,
        PetDisplayConfigGuiManager::getEditorInstance,
    )
    private val editorIndexCache = mutableMapOf<Field, Int>()

    /**
     * Migrates a Boolean to an Enum Constant.
     *
     * @param element The JsonElement to migrate
     * @param trueValue The enum value it should map to if the value is true
     * @param falseValue The enum value it should map to if the value is false
     * @return The migrated JsonElement
     */
    fun <T : Enum<T>> migrateBooleanToEnum(element: JsonElement, trueValue: T, falseValue: T): JsonElement {
        require(element is JsonPrimitive) { "Expected a JsonPrimitive but got ${element.javaClass.simpleName}" }
        return JsonPrimitive(if (element.asBoolean) trueValue.name else falseValue.name)
    }

    private fun KProperty0<*>.tryFindEditor(editor: MoulConfigEditor<*>): ProcessedOption? {
        // Java reflection is used because MoulConfig is relocated at build time, causing Kotlin reflection
        // (this.javaField) to fail to resolve property descriptors in the production build.
        val receiver = (this as? CallableReference)?.boundReceiver
            ?.takeIf { it !== CallableReference.NO_RECEIVER }
            ?: return null
        val field = generateSequence(receiver.javaClass as Class<*>?) { it.superclass }
            .firstNotNullOfOrNull { clazz ->
                runCatching { clazz.getDeclaredField(name) }.getOrNull()
            } ?: return null
        return editor.getOptionFromField(field)
    }

    fun KProperty0<*>.jumpToEditor() {
        if (tryJumpToEditor(ConfigGuiManager.getEditorInstance())) return

        ErrorManager.crashInDevEnv("Can not open config $name")
        ErrorManager.logErrorStateWithData(
            "Can not open the config",
            "error while trying to jump to an editor element",
            "this.name" to this.name,
            "this.toString()" to this.toString(),
            "this" to this,
        )
    }

    private fun KProperty0<*>.tryJumpToEditor(editor: MoulConfigEditor<*>): Boolean {
        val option = tryFindEditor(editor) ?: return false
        return editor.jumpToOption(option)
    }

    fun canJumpToEditor(field: Field): Boolean =
        field.findEditorIndex() != UNKNOWN_EDITOR_INDEX

    fun jumpToEditor(field: Field): Boolean {
        val editor = field.findEditor() ?: return false
        val option = editor.getOptionFromField(field) ?: return false
        return editor.jumpToOption(option)
    }

    fun clearEditorCache() {
        editorIndexCache.clear()
    }

    private fun Field.findEditor(): MoulConfigEditor<*>? =
        editorProviders.getOrNull(findEditorIndex())?.invoke()

    private fun Field.findEditorIndex(): Int = editorIndexCache.getOrPut(this) {
        editorProviders.indexOfFirst { editorProvider ->
            editorProvider().getOptionFromField(this) != null
        }
    }

    private fun MoulConfigEditor<*>.jumpToOption(option: ProcessedOption): Boolean {
        search("")
        if (!goToOption(option)) return false
        openEditor(this)
        return true
    }

    fun openEditor(editor: MoulConfigEditor<*>) {
        SkyHanniMod.screenToOpen = createConfigScreen(editor)
    }

    internal fun createConfigScreen(editor: MoulConfigEditor<*>, previousScreen: Screen? = null) =
        MoulConfigScreenComponent(Component.empty(), GuiContext(MoulConfigEditorComponent(editor)), previousScreen)

    /**
     * Open the config editor and navigate to the given java field declared on [owner].
     * This is useful when a KMutableProperty1 belongs to a non-singleton class and we can't safely
     * set the value by creating a new instance.
     */
    fun openEditorForField(owner: Class<*>, fieldName: String) {
        val editor = ConfigGuiManager.getEditorInstance()
        val field = runCatching { owner.getDeclaredField(fieldName) }.getOrNull() ?: return
        field.isAccessible = true
        val option = editor.getOptionFromField(field) ?: return
        editor.search("")
        if (!editor.goToOption(option)) return
        openEditor(editor)
    }

    /**
     * Bind a mutable Kotlin property to a receiver instance and open its editor option if available.
     * Useful for object singletons where we can obtain a bound property reference.
     */
    fun <T> KMutableProperty1<T, *>.jumpToEditor(receiver: T) {
        val editor = ConfigGuiManager.getEditorInstance()
        val field = this.javaField ?: return
        val option = editor.getOptionFromField(field) ?: return
        editor.search("")
        if (!editor.goToOption(option)) return
        openEditor(editor)
    }

    val configScreenCurrentlyOpen: Boolean
        get() = MinecraftCompat.screen is MoulConfigScreenComponent

    fun String.asStructuredText() = StructuredText.of(this)
}
