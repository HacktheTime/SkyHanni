package at.hannibal2.skyhanni.features.misc.massconfiguration

import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import io.github.notenoughupdates.moulconfig.ChromaColour
import kotlin.math.max
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment as HA

/** Simple viewer that lists options (usually the new ones) grouped under headers. */
class NewOptionsListScreen(
    private val titleText: String,
    private val groups: List<OptionGroup>,
) : SkyHanniBaseScreen() {

    data class OptionGroup(val name: String, val options: List<FeatureToggleableOption>)

    private var scrollRenderable: Renderable? = null
    private var contentWidth = 0
    private var contentHeight = 0

    private val colorBg = ChromaColour.fromStaticRGB(18, 18, 28, 235)
    private val colorOutlineTop = ChromaColour.fromStaticRGB(100, 100, 160, 255)
    private val colorOutlineBot = ChromaColour.fromStaticRGB(55, 55, 100, 255)
    private val colorRowNormal = ChromaColour.fromStaticRGB(30, 30, 50, 180)
    private val colorRowHover = ChromaColour.fromStaticRGB(55, 55, 85, 220)

    override fun onInitGui() {
        contentWidth = max((width * 0.85).toInt(), 420)
        contentHeight = max((height * 0.85).toInt(), 260)
        rebuildScroll()
    }

    private fun rebuildScroll() {
        val rows = mutableListOf<Renderable>()
        val availableWidth = (contentWidth - 64).coerceAtLeast(260)
        for (group in groups) {
            rows += Renderable.text("§b§l${group.name}", scale = 1.1)
            val entries = group.options.ifEmpty { emptyList() }
            for (opt in entries) {
                rows += buildOptionRow(opt, availableWidth)
            }
            rows += Renderable.placeholder(0, 8)
        }
        scrollRenderable = Renderable.scrollList(
            rows,
            height = (contentHeight * 0.72).toInt().coerceAtLeast(160),
            bypassChecks = true,
            showScrollbar = true,
        )
    }

    private fun buildOptionRow(opt: FeatureToggleableOption, availableWidth: Int): Renderable {
        val content = Renderable.vertical(
            listOfNotNull(
                Renderable.text("§f${opt.name}"),
                opt.description.takeIf { it.isNotBlank() }?.let { massConfigMultilineText("§7$it", availableWidth - 20) },
            ),
            spacing = 2,
            horizontalAlign = HA.LEFT,
        )
        return Renderable.clickable(
            Renderable.hoverable(
                Renderable.drawInsideRoundedRect(content, colorRowHover.toColor(), padding = 6, radius = 6),
                Renderable.drawInsideRoundedRect(content, colorRowNormal.toColor(), padding = 6, radius = 6),
                bypassChecks = true,
            ),
            onLeftClick = { ConfigUtils.jumpToEditor(opt.field) },
            bypassChecks = true,
            tips = listOf("§7Click to open in the config"),
        )
    }

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground(mouseX, mouseY, partialTicks)
        if (contentWidth == 0 || contentHeight == 0) {
            contentWidth = max((width * 0.85).toInt(), 420)
            contentHeight = max((height * 0.85).toInt(), 260)
            rebuildScroll()
        }
        val xOffset = (width - contentWidth) / 2
        val yOffset = (height - contentHeight) / 2

        val list = scrollRenderable ?: Renderable.text("§7No options to display")
        Renderable.withMousePosition(mouseX - xOffset, mouseY - yOffset) {
            val column = Renderable.vertical(
                listOf(
                    Renderable.text(titleText, scale = 1.2),
                    Renderable.placeholder(0, 8),
                    list,
                ),
                spacing = 8,
                horizontalAlign = HA.LEFT,
            )
            DrawContextUtils.pushMatrix()
            DrawContextUtils.translate(xOffset.toFloat(), yOffset.toFloat())
            Renderable.drawInsideFloatingRectWithBorder(
                column,
                backgroundColor = colorBg,
                lightColor = colorOutlineTop,
                darkColor = colorOutlineBot,
                padding = 16,
                radius = 12,
                smoothness = 2,
                borderThickness = 2,
            ).renderXYAligned(0, 0, contentWidth, contentHeight)
            DrawContextUtils.popMatrix()
        }
    }
}