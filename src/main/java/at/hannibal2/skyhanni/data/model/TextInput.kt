package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.insert
import at.hannibal2.skyhanni.utils.StringUtils.removeWordsAtEnd
import kotlinx.coroutines.runBlocking
import net.minecraft.client.settings.KeyBinding
import org.apache.commons.lang3.SystemUtils
import org.lwjgl.input.Keyboard
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

open class TextInput {

    var textBox: String = ""
    private var carriage: Int? = null
    // Selection state (null when no selection)
    private var selectionStart: Int? = null
    private var selectionEnd: Int? = null

    fun editText(textColor: LorenzColor = LorenzColor.WHITE, carriageColor: LorenzColor = LorenzColor.GREEN) = textBox.let {
        with(carriage) {
            if (this == null) it
            else it.insert(this, "${carriageColor.getChatColor()}|${textColor.getChatColor()}")
        }
    }.replace("(?<!§.\\|)§(?!.\\|§.)".toRegex(), "&&")

    fun editTextWithAlwaysCarriage() = textBox.let {
        with(carriage) {
            if (this == null) it.plus('|')
            else it.insert(this, '|')
        }
    }.replace("§", "&&")

    fun finalText() = textBox.replace("&&", "§")

    fun makeActive() = if (!isActive) activate(this) else Unit
    fun disable() = if (isActive) Companion.disable() else Unit
    fun handle() =
        //#if MC < 1.21
        handleTextInput()
    //#else
    //$$ handleTextInput(null)
    //#endif

    fun clear() {
        textBox = ""
        carriage = null
        selectionStart = null
        selectionEnd = null
    }

    val isActive get() = activeInstance == this

    private val updateEvents = mutableMapOf<Int, (TextInput) -> Unit>()

    protected fun update() {
        updateEvents.forEach { (_, it) -> it(this) }
    }

    fun registerToEvent(key: Int, event: (TextInput) -> Unit) {
        updateEvents[key] = event
    }

    fun removeFromEvent(key: Int) {
        updateEvents.remove(key)
    }

    @SkyHanniModule
    companion object {
        private var activeInstance: TextInput? = null

        fun isActive() = activeInstance != null

        fun activate(instance: TextInput) {
            activeInstance = instance
            //#if MC < 1.21
            timeSinceKeyEvent = Keyboard.getEventNanoseconds()
            //#endif
        }

        fun disable() {
            activeInstance = null
        }

        @HandleEvent
        fun onInventoryClose(event: InventoryCloseEvent) {
            disable()
        }

        @Suppress("UnusedParameter")
        fun onMinecraftInput(keyBinding: KeyBinding, cir: CallbackInfoReturnable<Boolean>) {
            if (activeInstance != null) {
                cir.returnValue = false
                return
            }
        }

        fun onGuiInput(
            //#if MC < 1.21
            ci: CallbackInfo,
            //#else
            //$$ ci: CallbackInfoReturnable<Boolean>
            //#endif
        ) {
            if (activeInstance != null) {
                if (Keyboard.KEY_ESCAPE.isKeyHeld()) {
                    disable()
                } else {
                    //#if MC < 1.21
                    ci.cancel()
                    //#else
                    //$$ ci.setReturnValue(false)
                    //#endif
                }
                return
            }
        }

        private var timeSinceKeyEvent = 0L

        private var carriage
            get() = activeInstance?.carriage
            set(value) {
                activeInstance?.carriage = value
            }

        private var textBox
            get() = activeInstance?.textBox.orEmpty()
            set(value) {
                activeInstance?.textBox = value
            }

        private var selectionStart
            get() = activeInstance?.selectionStart
            set(value) { activeInstance?.selectionStart = value }
        private var selectionEnd
            get() = activeInstance?.selectionEnd
            set(value) { activeInstance?.selectionEnd = value }

        private fun clearSelection() { selectionStart = null; selectionEnd = null }
        private fun hasSelection(): Boolean { val a = selectionStart; val b = selectionEnd; return a != null && b != null && a != b }
        private fun normalizedSelection(): Pair<Int, Int>? {
            val a = selectionStart; val b = selectionEnd
            if (a == null || b == null || a == b) return null
            val s = a.coerceAtMost(b).coerceIn(0, textBox.length)
            val e = a.coerceAtLeast(b).coerceIn(0, textBox.length)
            return s to e
        }

        private fun updated() {
            with(activeInstance) {
                if (this == null) return
                update()
            }
        }

        //#if MC > 1.21
        //$$ @HandleEvent
        //$$ fun onChar(event: at.hannibal2.skyhanni.events.minecraft.CharEvent) {
        //$$     handleTextInput(event.keyCode.toChar())
        //$$ }
        //#endif

        private fun handleTextInput(
            //#if MC > 1.21
            //$$ char: Char?,
            //#endif
        ) {
            // helpers
            fun caretOrEnd(): Int = carriage ?: textBox.length
            fun setCaret(pos: Int?) { carriage = pos?.coerceIn(0, textBox.length) }
            fun setCaretPos(pos: Int) { carriage = pos.coerceIn(0, textBox.length) }
            fun isWordChar(ch: Char): Boolean = Character.isLetterOrDigit(ch) || ch == '_'
            fun findWordStartLeft(caretPos: Int): Int {
                if (caretPos <= 0) return 0
                var j = caretPos - 1
                while (j >= 0 && !isWordChar(textBox[j])) j--
                while (j >= 0 && isWordChar(textBox[j])) j--
                return (j + 1).coerceAtLeast(0)
            }
            fun findWordEndRight(caretPos: Int): Int {
                val len = textBox.length
                if (caretPos >= len) return len
                var j = caretPos
                while (j < len && isWordChar(textBox[j])) j++
                while (j < len && !isWordChar(textBox[j])) j++
                return j.coerceAtMost(len)
            }

            // Clipboard
            if (KeyboardManager.isCopyingKeysDown()) {
                val sel = normalizedSelection()
                OSUtils.copyToClipboard(if (sel != null) textBox.substring(sel.first, sel.second) else textBox)
                return
            }
            if (KeyboardManager.isPastingKeysDown()) {
                runBlocking {
                    val paste = OSUtils.readFromClipboard()?.take(2024) ?: return@runBlocking
                    val sel = normalizedSelection()
                    if (sel != null) {
                        textBox = textBox.substring(0, sel.first) + paste + textBox.substring(sel.second)
                        setCaretPos(sel.first + paste.length)
                        clearSelection()
                    } else {
                        val c = caretOrEnd()
                        textBox = textBox.substring(0, c) + paste + textBox.substring(c)
                        setCaretPos(c + paste.length)
                    }
                    updated()
                }
                return
            }

            val ctrl = Keyboard.KEY_LCONTROL.isKeyHeld() || Keyboard.KEY_RCONTROL.isKeyHeld()
            val shift = Keyboard.KEY_LSHIFT.isKeyHeld() || Keyboard.KEY_RSHIFT.isKeyHeld()

            // Navigation keys
            if (Keyboard.KEY_A.isKeyClicked() && ctrl) {
                selectionStart = 0; selectionEnd = textBox.length
                setCaretPos(textBox.length)
                updated(); return
            }

            if (Keyboard.KEY_LEFT.isKeyClicked()) {
                val caret = caretOrEnd()
                val newCaret = if (ctrl) findWordStartLeft(caret) else (caret - 1).coerceAtLeast(0)
                if (shift) {
                    // extend selection from anchor (existing start) or caret
                    val anchor = selectionStart ?: caret
                    setCaretPos(newCaret); selectionStart = anchor; selectionEnd = newCaret
                } else {
                    setCaretPos(newCaret); clearSelection()
                }
                updated(); return
            }
            if (Keyboard.KEY_RIGHT.isKeyClicked()) {
                val caret = caretOrEnd()
                val newCaret = if (ctrl) findWordEndRight(caret) else (caret + 1).coerceAtMost(textBox.length)
                if (shift) {
                    val anchor = selectionStart ?: caret
                    setCaretPos(newCaret); selectionStart = anchor; selectionEnd = newCaret
                } else {
                    setCaretPos(newCaret); clearSelection()
                }
                updated(); return
            }
            if (Keyboard.KEY_HOME.isKeyClicked()) {
                val newCaret = 0
                if (shift) {
                    val anchor = selectionStart ?: caretOrEnd()
                    setCaretPos(newCaret); selectionStart = anchor; selectionEnd = newCaret
                } else {
                    setCaretPos(newCaret); clearSelection()
                }
                updated(); return
            }
            if (Keyboard.KEY_END.isKeyClicked()) {
                val newCaret = textBox.length
                if (shift) {
                    val anchor = selectionStart ?: caretOrEnd()
                    setCaretPos(newCaret); selectionStart = anchor; selectionEnd = newCaret
                } else {
                    setCaretPos(newCaret); clearSelection()
                }
                updated(); return
            }

            // Deletion
            //#if MC > 1.21
            //$$ if (GLFW.GLFW_KEY_BACKSPACE.isKeyClicked() || (SystemUtils.IS_OS_MAC && GLFW.GLFW_KEY_DELETE.isKeyClicked())) {
            //$$     // handled below similar to < 1.21
            //$$ }
            //$$ if (char==null) return
            //#endif

            //#if MC < 1.21
            if (timeSinceKeyEvent == Keyboard.getEventNanoseconds()) return
            timeSinceKeyEvent = Keyboard.getEventNanoseconds()
            val char: Char = Keyboard.getEventCharacter()
            //#endif

            if (char == '\b' || (char == Char(127) && SystemUtils.IS_OS_MAC)) {
                val sel = normalizedSelection()
                if (sel != null) {
                    textBox = textBox.removeRange(sel.first, sel.second)
                    setCaretPos(sel.first)
                    clearSelection(); updated(); return
                }
                val caret = caretOrEnd()
                if (caret > 0) {
                    if (ctrl) {
                        val newCaret = findWordStartLeft(caret)
                        textBox = textBox.removeRange(newCaret, caret)
                        setCaretPos(newCaret)
                    } else {
                        textBox = textBox.removeRange(caret - 1, caret)
                        setCaretPos(caret - 1)
                    }
                    updated(); return
                }
            }

            if (Keyboard.KEY_DELETE.isKeyClicked()) {
                val sel = normalizedSelection()
                if (sel != null) {
                    textBox = textBox.removeRange(sel.first, sel.second)
                    setCaretPos(sel.first)
                    clearSelection(); updated(); return
                }
                val caret = caretOrEnd()
                if (caret < textBox.length) {
                    if (ctrl) {
                        val newCaret = findWordEndRight(caret)
                        textBox = textBox.removeRange(caret, newCaret)
                        setCaretPos(caret)
                    } else {
                        textBox = textBox.removeRange(caret, caret + 1)
                        setCaretPos(caret)
                    }
                    updated(); return
                }
            }

            // character input
            when (char) {
                Char(0) -> return
                '\b' -> return // handled above
                Char(127) -> if (!SystemUtils.IS_OS_MAC) return else {}
                else -> {
                    if (Character.isISOControl(char)) return
                    val sel = normalizedSelection()
                    if (sel != null) {
                        textBox = textBox.substring(0, sel.first) + char + textBox.substring(sel.second)
                        setCaretPos(sel.first + 1)
                        clearSelection(); updated(); return
                    }
                    val caret = caretOrEnd()
                    textBox = textBox.insert(caret, char)
                    setCaretPos(caret + 1)
                    updated(); return
                }
            }
        }
    }
}
