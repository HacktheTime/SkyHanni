package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.KeyboardEvent
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import net.minecraft.client.Minecraft
import kotlin.math.max

/**
 * Decorator for a third-party main toggle row.
 * Adds a red badge and warning triangle but does not block the base control.
 */
class GuiOptionEditorThirdPartyMainToggle(
    private val base: GuiOptionEditor,
    private val thirdParty: ThirdParty,
    private val extraMessage: String,
) : GuiOptionEditor(base.getOption()) {
    private var bannerHeightCache = MIN_BANNER_HEIGHT
    private var hoverTooltip: List<String>? = null

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        val font = context.minecraft.defaultFontRenderer
        val pad = (base.height * 0.08f).toInt().coerceAtLeast(2)
        val lineH = font.height
        val bannerHeight = max(lineH + pad * 2, MIN_BANNER_HEIGHT)
        bannerHeightCache = bannerHeight

        val bannerBottom = y + bannerHeight
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), bannerBottom.toFloat(), 0x33000000)

        val label = ("§c\u26A0 Third-Party Main Toggle: ${thirdParty.displayName}" +
            (if (extraMessage.isNotBlank()) " – $extraMessage" else "") + "§r").asStructuredText()
        val ty = y + (bannerHeight - lineH) / 2
        context.drawStringScaledMaxWidth(label, font, x + pad, ty, true, width - pad * 2, -0x1)
        hoverTooltip = listOf("§c${thirdParty.displayName}", "§7Click to view features")

        base.render(context, x, bannerBottom, width)
    }

    override fun mouseInput(
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        mouseEvent: MouseEvent?,
    ): Boolean {
        val bannerHeight = bannerHeightCache
        val insideBanner = mouseX in x..(x + width) && mouseY in y..(y + bannerHeight)
        val isPrimaryClick = mouseEvent is MouseEvent.Click && mouseEvent.mouseState && mouseEvent.mouseButton == 0
        if (insideBanner && isPrimaryClick) {
            Minecraft.getInstance().setScreen(ThirdPartyFeaturesScreen(thirdParty))
            return true
        }
        return base.mouseInput(x, y + bannerHeight, width, mouseX, mouseY, mouseEvent)
    }

    override fun keyboardInput(event: KeyboardEvent?): Boolean = base.keyboardInput(event)

    override fun getHeight(): Int = base.height + bannerHeightCache

    companion object {
        private const val MIN_BANNER_HEIGHT = 18
    }
}
