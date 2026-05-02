package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor

class GuiOptionEditorBlocked(private val base: GuiOptionEditor, private val extraMessage: String) : GuiOptionEditor(base.getOption()) {

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        // Render base then overlay a reddish block with legible text (no overlap)
        base.render(context, x, y, width)

        val font = context.minecraft.defaultFontRenderer
        val pad = (height * 0.08f).toInt().coerceAtLeast(2)

        // Reddish background
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), 0x66AA0000.toInt())

        val title = ("\u26A0 This option is currently not available.").asStructuredText()
        val body = extraMessage.asStructuredText()

        var ty = y + pad
        context.drawStringScaledMaxWidth(title, font, x + pad, ty, true, width - pad * 2, -0x1)
        ty += font.height + pad
        context.drawStringScaledMaxWidth(body, font, x + pad, ty, true, width - pad * 2, -0x1)
    }

    override fun mouseInput(x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int): Boolean {
        return false
    }

    override fun keyboardInput(): Boolean {
        return false
    }

    override fun getHeight(): Int {
        return base.height
    }

    companion object {
        val blockedTexture: MyResourceLocation = MyResourceLocation(
            "skyhanni", "config_blocked.png",
        )
    }
}
