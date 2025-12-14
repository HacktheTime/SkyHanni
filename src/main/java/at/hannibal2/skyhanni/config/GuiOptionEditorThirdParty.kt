package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.KeyboardEvent
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import java.awt.Color
import kotlin.math.max

/**
 * Unified GUI wrapper for third-party dependencies.
 * - Blocked mode: overlays reddish background with warning + CTA to enable/allow.
 * - Warn mode: shows a red/orange ribbon; interaction allowed.
 */
class GuiOptionEditorThirdParty(
    private val base: GuiOptionEditor,
    private val thirdParty: ThirdParty,
    private val usesMainToggle: Boolean,
    private val requiresMainToggle: Boolean,
    private val extraMessage: String,
) : GuiOptionEditor(base.getOption()) {
    // Button bounds for mouse handling when blocked
    private var btnX1 = 0
    private var btnY1 = 0
    private var btnX2 = 0
    private var btnY2 = 0
    private var warningBannerHeightCache = WARN_BANNER_MIN_HEIGHT
    private var lastState: UiState = UiState(consentAllows = true, mainToggleEnabled = true, blocked = false)
    private var hoverTooltip: List<String>? = null

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        val state = resolveState().also { lastState = it }
        val font = context.minecraft.defaultFontRenderer
        val pad = (base.height * 0.08f).toInt().coerceAtLeast(2)
        val warningHeight = max(font.height + pad * 2, WARN_BANNER_MIN_HEIGHT)
        warningBannerHeightCache = warningHeight
        val bannerBottom = y + warningHeight
        val dueToMainToggle = usesMainToggle && requiresMainToggle && !state.mainToggleEnabled
        val bannerColor = when {
            state.blocked && !dueToMainToggle -> 0x55FF5555
            else -> 0x33000000
        }
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), bannerBottom.toFloat(), bannerColor)
        val warn = (
            if (state.blocked) {
                if (dueToMainToggle) "§c⚠ Requires Third Party:  ${thirdParty.displayName}" else "§cBlocked: Allow ${
                    thirdParty.displayName
                }"
            } else {
                "§c⚠ Uses third party: ${thirdParty.displayName}" +
                    if (extraMessage.isNotBlank()) " – $extraMessage" else ""
            }
            ).asStructuredText()
        val textY = y + (warningHeight - font.height) / 2
        context.drawStringScaledMaxWidth(warn, font, x + pad, textY, true, width - pad * 2, -0x1)
        hoverTooltip = buildList {
            add("Third Party: §c${thirdParty.displayName}")
            add("§6-§c⚠§6 The SkyHanni Team has no access nor control for the Server this Feature depends on.")
            thirdParty.description.takeIf { it.isNotBlank() }?.let { add("§7$it") }
            if (extraMessage.isNotBlank() && extraMessage != thirdParty.description) add("§7$extraMessage")
        }

        if (dueToMainToggle) {
            val buttonHeight = font.height + pad
            val label = "Enable ${thirdParty.displayName}"
            val btnW = (font.getStringWidth(label) + pad * 4).coerceAtLeast((width * 0.25f).toInt())
            val btnX = x + width - btnW - pad
            val btnY = y + (warningHeight - buttonHeight) / 2
            btnX1 = btnX; btnY1 = btnY; btnX2 = btnX + btnW; btnY2 = btnY + buttonHeight
            val bgCol = 0xFFEAFFF0.toInt()
            val borderCol = 0xFF1F8B44.toInt()
            context.drawColoredRect(btnX.toFloat(), btnY.toFloat(), (btnX + btnW).toFloat(), (btnY + buttonHeight).toFloat(), bgCol.toInt())
            context.drawColoredRect(btnX.toFloat(), btnY.toFloat(), (btnX + btnW).toFloat(), (btnY + 1).toFloat(), borderCol)
            context.drawColoredRect(
                btnX.toFloat(),
                (btnY + buttonHeight - 1).toFloat(),
                (btnX + btnW).toFloat(),
                (btnY + buttonHeight).toFloat(),
                borderCol,
            )
            context.drawColoredRect(btnX.toFloat(), btnY.toFloat(), (btnX + 1).toFloat(), (btnY + buttonHeight).toFloat(), borderCol)
            context.drawColoredRect(btnX.toFloat(), btnY.toFloat(), (btnX + btnW - 1).toFloat(), (btnY + buttonHeight).toFloat(), borderCol)
            context.drawStringScaledMaxWidth(
                label.asStructuredText(),
                font,
                btnX + pad,
                btnY + (buttonHeight - font.height) / 2,
                true,
                btnW - pad * 2,
                borderCol,
            )
        } else {
            btnX1 = 0; btnY1 = 0; btnX2 = 0; btnY2 = 0
        }

        val baseY = y + warningHeight
        base.render(context, x, baseY, width)

        if (state.blocked && !dueToMainToggle) {
            context.drawColoredRect(x.toFloat(), baseY.toFloat(), (x + width).toFloat(), (baseY + base.height).toFloat(), 0x55000000)
        }
    }

    override fun mouseInput(
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        mouseEvent: MouseEvent?,
    ): Boolean {
        hoverTooltip = null
        val state = lastState
        val topPad = if (!state.blocked) warningBannerHeightCache else 0
        val baseY = y + topPad
        val totalHeight = getHeight()
        val insideRow = mouseX in x..(x + width) && mouseY in y..(y + totalHeight)
        val clicked = mouseEvent is MouseEvent.Click && mouseEvent.mouseState
        val dueToMainToggle = usesMainToggle && requiresMainToggle && !state.mainToggleEnabled
        if (state.blocked) {
            if (dueToMainToggle) {
                val insideBtn = mouseX in btnX1..btnX2 && mouseY in btnY1..btnY2
                if (insideBtn && clicked) {
                    thirdParty.setEnabled(true)
                    try {
                        SkyHanniMod.configManager.recreateConfig()
                    } catch (_: Throwable) { /* ignore */
                    }
                    return true
                }
                return base.mouseInput(x, baseY, width, mouseX, mouseY, mouseEvent)
            }
            return insideRow && clicked
        }
        if (!insideRow) return false
        return base.mouseInput(x, baseY, width, mouseX, mouseY, mouseEvent)
    }

    override fun mouseInputOverlay(
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        mouseEvent: MouseEvent?,
    ): Boolean {
        return base.mouseInputOverlay(x, y + warningBannerHeightCache, width, mouseX, mouseY, mouseEvent)
    }

    override fun renderOverlay(context: RenderContext, x: Int, y: Int, width: Int) {
        hoverTooltip?.takeIf { it.isNotEmpty() }?.let { tips ->
            val mx = io.github.notenoughupdates.moulconfig.common.IMinecraft.INSTANCE.mouseX
            val my = io.github.notenoughupdates.moulconfig.common.IMinecraft.INSTANCE.mouseY
            val bannerBottom = y + warningBannerHeightCache
            if (mx in x..(x + width) && my in y..bannerBottom) {
                RenderableTooltips.setTooltipForRender(tips.map(StringRenderable::from))
            }
        }
        base.renderOverlay(context, x, y + warningBannerHeightCache, width)
    }

    override fun keyboardInput(event: KeyboardEvent?): Boolean {
        val state = lastState
        if (state.blocked) return false
        return base.keyboardInput(event)
    }

    override fun getHeight(): Int {
        val top = warningBannerHeightCache
        return base.height + top
    }

    private fun resolveState(): UiState {
        val consentAllows = ThirdPartyPolicy.isConsented(thirdParty) || usesMainToggle
        val mainToggleEnabled = !usesMainToggle || thirdParty.isEnabled()
        val blocked = (requiresMainToggle && usesMainToggle && !mainToggleEnabled) || !consentAllows
        return UiState(consentAllows, mainToggleEnabled, blocked)
    }

    private data class UiState(
        val consentAllows: Boolean,
        val mainToggleEnabled: Boolean,
        val blocked: Boolean,
    )

    companion object {
        private const val WARN_BANNER_MIN_HEIGHT = 16
    }
}
