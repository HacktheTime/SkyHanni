package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import io.github.notenoughupdates.moulconfig.common.IMinecraft
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.KeyboardEvent
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import kotlin.math.max
import kotlin.reflect.jvm.javaField

/**
 * Decorator for a third-party main toggle row.
 *
 * Shows a banner with the third-party name; clicking it expands a sticky panel with
 * a disclaimer that third-party servers are not controlled by the SkyHanni team, a
 * description, the custom note from the annotation, and clickable links (website,
 * privacy policy, terms of service, discord) plus a button to view the features.
 */
class GuiOptionEditorThirdPartyMainToggle(
    private val base: GuiOptionEditor,
    private val thirdParty: ThirdParty,
    private val extraMessage: String,
) : GuiOptionEditor(base.getOption()), ConfigBannerProvider {
    private class PanelRow(
        val text: String,
        val action: (() -> Unit)?,
        val tooltip: String?,
    )

    private var bannerHeight = MIN_BANNER_HEIGHT
    private var panelHeight = 0
    private var expanded = false
    private var hoverTooltip: List<String>? = null
    private val rowHits = mutableListOf<Pair<IntRange, PanelRow>>()

    override fun bannerOffset(): Int =
        bannerHeight + panelHeight + ((base as? ConfigBannerProvider)?.bannerOffset() ?: 0)

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        val font = context.minecraft.defaultFontRenderer
        val pad = (base.height * 0.08f).toInt().coerceAtLeast(2)
        val bannerH = max(font.height + pad * 2, MIN_BANNER_HEIGHT)
        bannerHeight = bannerH

        val bannerBottom = y + bannerH
        context.drawColoredRect(
            x.toFloat(), y.toFloat(), (x + width).toFloat(), bannerBottom.toFloat(),
            if (expanded) BANNER_BG_ACTIVE else BANNER_BG,
        )
        val label = ("§c⚠ Third-Party: ${thirdParty.displayName} §7- §b" +
            if (expanded) "click to hide details" else "click for details").asStructuredText()
        val ty = y + (bannerH - font.height) / 2
        context.drawStringScaledMaxWidth(label, font, x + pad, ty, true, width - pad * 2, TEXT_COLOR)
        hoverTooltip = listOf(
            "§c⚠ ${thirdParty.displayName}",
            if (expanded) "§7Click to hide details" else "§7Click to show details",
        )

        rowHits.clear()
        var contentY = bannerBottom
        if (expanded) {
            val panelTop = contentY
            val rowPad = 3
            var cursorY = panelTop

            val rows = buildList {
                add(PanelRow("§cThird-party servers are not controlled by the SkyHanni team.", null, null))
                add(PanelRow("§7${thirdParty.description}", null, null))
                if (extraMessage.isNotBlank()) {
                    add(PanelRow("§e$extraMessage", null, null))
                }
                add(PanelRow("§7Server access: " + if (thirdParty.serverAccess) "§aYes" else "§cNo", null, null))
                add(PanelRow(
                    "§7Source access: " +
                        if (thirdParty.sourceAccess) "§aYes" else "§cNo §7(the client part is open source)",
                    null, null,
                ))
                thirdParty.websiteUrl?.let { add(PanelRow("§bOpen Website: $it", { OSUtils.openBrowser(it) }, "§bClick to open in your browser")) }
                thirdParty.privacyPolicyUrl?.let { add(PanelRow("§bPrivacy Policy: $it", { OSUtils.openBrowser(it) }, "§bClick to open in your browser")) }
                thirdParty.termsOfServiceUrl?.let { add(PanelRow("§bTerms of Service: $it", { OSUtils.openBrowser(it) }, "§bClick to open in your browser")) }
                thirdParty.discordUrl?.let { add(PanelRow("§bDiscord: $it", { OSUtils.openBrowser(it) }, "§bClick to open in your browser")) }
                add(PanelRow(
                    "§bView features in config",
                    {
                        val main = thirdParty.mainToggleField?.javaField
                        if (main != null) {
                            ConfigUtils.openFilteredConfig(UsedByResolver.transitiveUsedBy(main.declaringClass, main.name))
                        } else {
                            MinecraftCompat.screen = ThirdPartyFeaturesScreen(thirdParty, MinecraftCompat.screen)
                        }
                    },
                    "§7Open the config with only this third party and its dependents",
                ))
            }

            rows.forEach { row ->
                val rowTop = cursorY
                val wrapped = font.splitText(row.text.asStructuredText(), width - 12)
                val rowHeight = wrapped.size * (font.height + 1) + rowPad * 2
                val isHover = row.action != null && isRowHovered(x, width, rowTop, rowHeight)
                context.drawColoredRect(
                    x.toFloat(), rowTop.toFloat(), (x + width).toFloat(), (rowTop + rowHeight).toFloat(),
                    if (isHover) ROW_HOVER else ROW_BG,
                )
                wrapped.forEachIndexed { i, line ->
                    context.drawStringScaledMaxWidth(
                        line, font, x + pad, rowTop + rowPad + i * (font.height + 1), true, width - pad * 2, TEXT_COLOR,
                    )
                }
                if (row.action != null) rowHits.add((rowTop until (rowTop + rowHeight)) to row)
                cursorY += rowHeight + 1
            }
            panelHeight = cursorY - panelTop
        } else {
            panelHeight = 0
        }
        contentY += panelHeight
        base.render(context, x, contentY, width)
    }

    private fun isRowHovered(x: Int, width: Int, rowTop: Int, rowHeight: Int): Boolean {
        val mx = IMinecraft.INSTANCE.mouseX
        val my = IMinecraft.INSTANCE.mouseY
        return mx in x..(x + width) && my in rowTop until (rowTop + rowHeight)
    }

    override fun mouseInput(
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        mouseEvent: MouseEvent?,
    ): Boolean {
        if (mouseEvent is MouseEvent.Click && mouseEvent.mouseState && mouseEvent.mouseButton == 0) {
            if (mouseY in y until (y + bannerHeight)) {
                expanded = !expanded
                return true
            }
            if (expanded) {
                rowHits.firstOrNull { mouseY in it.first }?.let { (_, row) ->
                    row.action?.invoke()
                    return true
                }
            }
        }
        return base.mouseInput(x, y + bannerHeight + panelHeight, width, mouseX, mouseY, mouseEvent)
    }

    override fun keyboardInput(event: KeyboardEvent?): Boolean = base.keyboardInput(event)

    override fun getHeight(): Int = base.height + bannerHeight + panelHeight

    override fun renderOverlay(context: RenderContext, x: Int, y: Int, width: Int) {
        val mx = IMinecraft.INSTANCE.mouseX
        val my = IMinecraft.INSTANCE.mouseY
        if (mx in x..(x + width) && my in y until (y + bannerHeight)) {
            hoverTooltip?.let { RenderableTooltips.setTooltipForRender(it.map(StringRenderable::from)) }
        } else if (expanded) {
            rowHits.firstOrNull { my in it.first }?.let { (_, row) ->
                row.tooltip?.let { RenderableTooltips.setTooltipForRender(listOf(StringRenderable.from(it))) }
            }
        }
        base.renderOverlay(context, x, y + bannerHeight + panelHeight, width)
    }

    companion object {
        private const val MIN_BANNER_HEIGHT = 18
        private const val BANNER_BG = 0x44E53935
        private const val BANNER_BG_ACTIVE = 0x66C62828
        private const val ROW_BG = 0x24151521
        private const val ROW_HOVER = 0x40373757
        private const val TEXT_COLOR = -0x1
    }
}
