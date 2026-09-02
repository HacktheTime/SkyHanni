package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.KeyboardEvent
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import kotlin.reflect.jvm.javaField

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
) : GuiOptionEditor(base.getOption()), ConfigBannerProvider {
    // Button bounds for mouse handling when blocked
    private var btnX1 = 0
    private var btnY1 = 0
    private var btnX2 = 0
    private var btnY2 = 0
    private var warningBannerHeightCache = WARN_BANNER_MIN_HEIGHT
    private var lastState: UiState = UiState(consentAllows = true, mainToggleEnabled = true, blocked = false)
    private var hoverTooltip: List<String>? = null

    override fun bannerOffset(): Int =
        warningBannerHeightCache + ((base as? ConfigBannerProvider)?.bannerOffset() ?: 0)

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        val state = resolveState().also { lastState = it }
        val font = context.minecraft.defaultFontRenderer
        val pad = (base.height * 0.08f).toInt().coerceAtLeast(2)
        val warningHeight = kotlin.math.max(font.height + pad * 2, WARN_BANNER_MIN_HEIGHT)
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
            // If the dependency has a main toggle field, show that with instruction
            thirdParty.mainToggleField?.let { mt ->
                add("")
                add("§6Main toggle: §f${mt.name}")
                add("§7This toggle may itself depend on other options; enabling it can unlock features.")
            }
        }

        if (dueToMainToggle) {
            val buttonHeight = font.height + pad
            val label = "Jump to Main Toggle"
            val btnW = (font.getStringWidth(label) + pad * 4).coerceAtLeast((width * 0.25f).toInt())
            val btnX = x + width - btnW - pad
            val btnY = y + (warningHeight - buttonHeight) / 2
            btnX1 = btnX; btnY1 = btnY; btnX2 = btnX + btnW; btnY2 = btnY + buttonHeight

            // Use same button visuals as dependency editor for consistency
            drawEnableButton(context, btnX, btnY, btnW, buttonHeight, label.asStructuredText())
        } else {
            btnX1 = 0; btnY1 = 0; btnX2 = 0; btnY2 = 0
        }

        val baseY = y + warningHeight
        base.render(context, x, baseY, width)

        if (state.blocked && !dueToMainToggle) {
            context.drawColoredRect(x.toFloat(), baseY.toFloat(), (x + width).toFloat(), (baseY + base.height).toFloat(), 0x55000000)
        }
    }

    private fun drawEnableButton(
        context: RenderContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        label: io.github.notenoughupdates.moulconfig.common.text.StructuredText,
        mouseX: Int = -1,
        mouseY: Int = -1,
    ) {
        // re-use styles from GuiOptionEditorDependencies
        val bg = 0xFF2E7D32.toInt()
        val border = 0xFF1B5E20.toInt()
        val topHighlight = 0xFF66BB6A.toInt()
        val hoverOverlay = 0x44333333
        // fill
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), bg)
        // top highlight
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + 1).toFloat(), topHighlight)
        // border
        context.drawColoredRect(x.toFloat(), (y + height - 1).toFloat(), (x + width).toFloat(), (y + height).toFloat(), border)
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + 1).toFloat(), (y + height).toFloat(), border)
        context.drawColoredRect((x + width - 1).toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), border)

        val font = context.minecraft.defaultFontRenderer
        context.drawStringScaledMaxWidth(label, font, x + 4, y + (height - font.height) / 2, true, width - 8, -0x1)

        val mx = io.github.notenoughupdates.moulconfig.common.IMinecraft.INSTANCE.mouseX
        val my = io.github.notenoughupdates.moulconfig.common.IMinecraft.INSTANCE.mouseY
        if (mx >= x && mx <= x + width && my >= y && my <= y + height) {
            context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), hoverOverlay)
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
                    // Jump to the third-party main toggle in the config
                    thirdParty.mainToggleField?.javaField?.let { field ->
                        ConfigUtils.openEditorForField(field.declaringClass, field.name)
                    }
                    return true
                }
                val insideBanner = mouseX in x..(x + width) && mouseY in y..(y + warningBannerHeightCache)
                if (insideBanner && clicked) {
                    thirdParty.mainToggleField?.javaField?.let { field ->
                        ConfigUtils.openEditorForField(field.declaringClass, field.name)
                    }
                    return true
                }
                return base.mouseInput(x, baseY, width, mouseX, mouseY, mouseEvent)
            }
            return insideRow && clicked
        }
        if (!insideRow) return false
        // Not blocked: clicking anywhere on the warning banner jumps to the main toggle
        val insideBanner = mouseY in y..(y + warningBannerHeightCache)
        if (insideBanner && clicked) {
            thirdParty.mainToggleField?.javaField?.let { field ->
                ConfigUtils.openEditorForField(field.declaringClass, field.name)
            }
            return true
        }
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
        return !state.blocked && base.keyboardInput(event)
    }

    override fun getHeight(): Int {
        val top = warningBannerHeightCache
        return base.height + top
    }

    override fun setGuiContext(guiContext: GuiContext) {
        base.setGuiContext(guiContext)
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
