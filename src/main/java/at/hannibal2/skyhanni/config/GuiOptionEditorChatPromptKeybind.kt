package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.KeyBind
import at.hannibal2.skyhanni.config.features.chat.ChatConfig
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import io.github.notenoughupdates.moulconfig.common.IMinecraft
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.KeyboardEvent
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorKeybind
import org.lwjgl.glfw.GLFW

/**
 * Editor for [KeyBind.key] fields.
 *
 * A [KeyBind] with key == 0 (or -1 from a legacy reset) has no override and uses the global
 * chat prompt key ([ChatConfig.defaultChatPrompt]). Instead of showing a meaningless "none"
 * button, this editor displays "DEFAULT (<global key>)" and offers a button to jump straight
 * to the global setting.
 */
class GuiOptionEditorChatPromptKeybind(
    private val base: GuiOptionEditorKeybind,
) : GuiOptionEditor(base.getOption()) {

    private var editingKeycode = false

    private val globalKey: Int
        get() = SkyHanniMod.feature.chat.defaultChatPrompt

    override fun getHeight(): Int = base.height

    override fun setGuiContext(guiContext: GuiContext) {
        base.setGuiContext(guiContext)
    }

    private fun keyName(key: Int): String = IMinecraft.INSTANCE.getKeyName(key).text

    /** Normalized override: 0 means "no override, use global default". */
    private fun currentKey(): Int {
        val key = option.get() as Int
        return if (key == 0 || key == GLFW.GLFW_KEY_UNKNOWN) 0 else key
    }

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        // normal config row: dark background, name and description rendered by the base editor
        super.render(context, x, y, width)
        val height = getHeight()
        val key = currentKey()
        val hasOverride = key != 0
        val mx = IMinecraft.INSTANCE.mouseX
        val my = IMinecraft.INSTANCE.mouseY

        // keybind button at the same position the vanilla moulconfig keybind editor uses
        val keyBtnX = x + width / 6 - 24
        val keyBtnY = y + height - 21
        val keyBtnW = 48
        val keyBtnH = 16
        val resetX = keyBtnX + keyBtnW + 3
        val resetY = keyBtnY + 3
        val resetW = 10
        val resetH = 11
        val globalX = resetX + resetW + 4
        val globalW = 40

        val keyLabel = when {
            editingKeycode -> "> ${keyName(if (hasOverride) key else globalKey)} <"
            hasOverride -> keyName(key)
            else -> "DEFAULT (${keyName(globalKey)})"
        }
        drawButton(context, keyBtnX, keyBtnY, keyBtnW, keyBtnH, keyLabel, mx, my, inEditMode = editingKeycode)
        drawResetButton(context, resetX, resetY, resetW, resetH)
        drawButton(context, globalX, keyBtnY, globalW, keyBtnH, "§bGlobal", mx, my)
        if (mx in keyBtnX..(keyBtnX + keyBtnW) && my in keyBtnY..(keyBtnY + keyBtnH)) {
            context.scheduleDrawTooltip(
                mx, my,
                listOf(if (hasOverride) "Click to change this key, Esc to reset".asStructuredText() else "Click to set a custom key".asStructuredText()),
            )
        }
        if (mx in resetX..(resetX + resetW) && my in resetY..(resetY + resetH)) {
            context.scheduleDrawTooltip(mx, my, listOf("Reset to the global default".asStructuredText()))
        }
        if (mx in globalX..(globalX + globalW) && my in keyBtnY..(keyBtnY + keyBtnH)) {
            context.scheduleDrawTooltip(mx, my, listOf("Jump to the global chat prompt key".asStructuredText()))
        }
    }

    private fun drawResetButton(context: RenderContext, x: Int, y: Int, width: Int, height: Int) {
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), BTN_BG)
        context.drawColoredRect(x.toFloat(), (y + height - 1).toFloat(), (x + width).toFloat(), (y + height).toFloat(), BTN_BORDER)
        val font = IMinecraft.INSTANCE.defaultFontRenderer
        context.drawStringCenteredScaledMaxWidth(
            "x".asStructuredText(),
            font,
            (x + width / 2).toFloat(),
            (y + height / 2).toFloat(),
            true,
            width,
            TEXT_COLOR,
        )
    }

    private fun drawButton(
        context: RenderContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        label: String,
        mouseX: Int,
        mouseY: Int,
        inEditMode: Boolean = false,
    ) {
        val bg = if (inEditMode) EDIT_BG else if (mouseX in x..(x + width) && mouseY in y..(y + height)) BTN_HOVER else BTN_BG
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), bg)
        context.drawColoredRect(x.toFloat(), (y + height - 1).toFloat(), (x + width).toFloat(), (y + height).toFloat(), BTN_BORDER)
        val font = IMinecraft.INSTANCE.defaultFontRenderer
        context.drawStringCenteredScaledMaxWidth(
            label.asStructuredText(),
            font,
            (x + width / 2).toFloat(),
            (y + height / 2).toFloat(),
            true,
            width - 6,
            TEXT_COLOR,
        )
    }

    override fun mouseInput(
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        mouseEvent: MouseEvent?,
    ): Boolean {
        if (mouseEvent !is MouseEvent.Click) return editingKeycode
        val height = getHeight()
        val keyBtnX = x + width / 6 - 24
        val keyBtnY = y + height - 21
        val keyBtnW = 48
        val keyBtnH = 16
        val resetX = keyBtnX + keyBtnW + 3
        val resetY = keyBtnY + 3
        val resetW = 10
        val resetH = 11
        val globalX = resetX + resetW + 4
        val globalW = 40

        if (mouseEvent.mouseState) {
            if (editingKeycode && mouseEvent.mouseButton != -1) {
                // capture mouse buttons as well
                editingKeycode = false
                option.set(mouseEvent.mouseButton)
                return true
            }
            if (mouseEvent.mouseButton == 0) {
                if (mouseX in keyBtnX..(keyBtnX + keyBtnW) && mouseY in keyBtnY..(keyBtnY + keyBtnH)) {
                    editingKeycode = true
                    return true
                }
                if (mouseX in resetX..(resetX + resetW) && mouseY in resetY..(resetY + resetH)) {
                    option.set(0)
                    return true
                }
                if (mouseX in globalX..(globalX + globalW) && mouseY in keyBtnY..(keyBtnY + keyBtnH)) {
                    ConfigUtils.openEditorForField(SkyHanniMod.feature.chat::class.java, "defaultChatPrompt")
                    return true
                }
            }
        }
        return editingKeycode
    }

    override fun keyboardInput(event: KeyboardEvent?): Boolean {
        if (event is KeyboardEvent.KeyPressed) {
            if (editingKeycode) {
                if (event.pressed) return true
                editingKeycode = false
                val keycode = event.keycode
                if (keycode == GLFW.GLFW_KEY_ESCAPE || keycode == 0) {
                    option.set(0)
                } else {
                    option.set(keycode)
                }
                return true
            }
            return false
        }
        return editingKeycode
    }

    companion object {
        private const val BTN_BG = 0x3420202E.toInt()
        private const val BTN_HOVER = 0x45373757.toInt()
        private const val BTN_BORDER = 0xFF373764.toInt()
        private const val EDIT_BG = 0x34235A23.toInt()
        private const val TEXT_COLOR = 0xFFFFFFFF.toInt()
    }
}
