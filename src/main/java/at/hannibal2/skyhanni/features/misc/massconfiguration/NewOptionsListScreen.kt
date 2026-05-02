package at.hannibal2.skyhanni.features.misc.massconfiguration

import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.math.max

/** Simple viewer that lists options (usually the new ones) grouped under headers. */
class NewOptionsListScreen(
    private val titleText: String,
    private val groups: List<OptionGroup>,
) : SkyHanniBaseScreen() {

    data class OptionGroup(val name: String, val options: List<FeatureToggleableOption>)

    private var scrollRenderable: Renderable? = null
    private var contentWidth = 0
    private var contentHeight = 0

    override fun onInitGui() {
        contentWidth = max((width * 0.85).toInt(), 420)
        contentHeight = max((height * 0.85).toInt(), 260)
        rebuildScroll()
    }

    private fun rebuildScroll() {
        val rows = mutableListOf<Renderable>()
        val availableWidth = (contentWidth - 32).coerceAtLeast(260)
        for (group in groups) {
            rows += Renderable.text("§e${group.name}")
            val entries = group.options.ifEmpty { emptyList() }
            for (opt in entries) {
                val content = Renderable.vertical(
                    listOfNotNull(
                        Renderable.text("§f${opt.name}"),
                        opt.description.takeIf { it.isNotBlank() }?.let { massConfigMultilineText("§7$it", availableWidth - 20) },
                    ),
                    spacing = 2,
                    horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
                )
                rows += Renderable.horizontal(
                    listOf(Renderable.text("§7•"), content),
                    spacing = 6,
                    horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
                )
            }
            rows += Renderable.placeholder(0, 6)
        }
        scrollRenderable = Renderable.scrollList(rows, height = (contentHeight * 0.7).toInt().coerceAtLeast(160), bypassChecks = true)
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
                    Renderable.text(titleText),
                    Renderable.placeholder(0, 8),
                    list,
                ),
                spacing = 8,
                horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
            )
            DrawContextUtils.pushMatrix()
            DrawContextUtils.translate(xOffset.toFloat(), yOffset.toFloat())
            GuiRenderUtils.drawFloatingRectDark(0, 0, contentWidth, contentHeight)
            Renderable.drawInsideDarkRect(column).renderXYAligned(0, 0, contentWidth, contentHeight)
            DrawContextUtils.popMatrix()
        }
    }
}
