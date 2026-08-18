package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.features.misc.massconfiguration.FeatureToggleProcessor
import at.hannibal2.skyhanni.features.misc.massconfiguration.FeatureToggleableOption
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.common.IMinecraft
import net.minecraft.client.gui.screens.Screen
import kotlin.math.max

class ThirdPartyFeaturesScreen(
    private val thirdParty: ThirdParty,
    private val parent: Screen? = null,
) : SkyHanniBaseScreen() {
    private var scrollRenderable: Renderable? = null
    private var backButtonRenderable: Renderable? = null
    private var contentWidth = 0
    private var contentHeight = 0

    // Back button area in absolute screen coordinates, recomputed on every draw
    private var backButtonX = 0
    private var backButtonY = 0
    private var backButtonW = 0
    private var backButtonH = 0

    private val colorBg = ChromaColour.fromStaticRGB(18, 18, 28, 235)
    private val colorOutlineTop = ChromaColour.fromStaticRGB(100, 100, 160, 255)
    private val colorOutlineBot = ChromaColour.fromStaticRGB(55, 55, 100, 255)
    private val colorRowHover = ChromaColour.fromStaticRGB(55, 55, 85, 220)
    private val colorButton = ChromaColour.fromStaticRGB(40, 40, 70, 200)

    override fun onInitGui() {
        contentWidth = max((width * 0.8).toInt(), 420)
        contentHeight = max((height * 0.8).toInt(), 260)
        rebuildScroll()
    }

    private fun rebuildScroll() {
        val pad = 16
        val listWidth = (contentWidth - pad * 2).coerceAtLeast(100)
        val entries = FeatureToggleProcessor.thirdPartyRegistry[thirdParty]
            ?.map { buildEntryRenderable(it, listWidth) }
            .orEmpty()
        scrollRenderable = Renderable.scrollList(
            entries.ifEmpty { listOf(Renderable.text("§7No features registered")) },
            height = (contentHeight * 0.6).toInt().coerceAtLeast(160),
            bypassChecks = true,
            showScrollbar = true,
        )
    }

    private fun buildEntryRenderable(option: FeatureToggleableOption, listWidth: Int): Renderable {
        val name = Renderable.text("§f• ${option.name}")
        // Descriptions may contain literal line feed characters (or the escaped "\n" sequence).
        // Normalize and split so every line renders as its own row instead of showing raw escapes.
        val descriptionLines = option.description
            .replace("\\n", "\n")
            .split("\n")
            .filter { it.isNotBlank() }
            .map { line -> Renderable.text("   §7${truncateToWidth(line, listWidth - 24)}") }
        return Renderable.vertical(listOf(name) + descriptionLines, spacing = 0)
    }

    private fun truncateToWidth(line: String, maxWidth: Int): String =
        IMinecraft.INSTANCE.defaultFontRenderer.trimStringToWidth(line, maxWidth)

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground(mouseX, mouseY, partialTicks)
        if (contentWidth == 0 || contentHeight == 0) {
            contentWidth = max((width * 0.8).toInt(), 420)
            contentHeight = max((height * 0.8).toInt(), 260)
            rebuildScroll()
        }
        val xOffset = (width - contentWidth) / 2
        val yOffset = (height - contentHeight) / 2

        Renderable.withMousePosition(mouseX - xOffset, mouseY - yOffset) {
            val column = buildColumn()
            // The back button is the last element of the column, drawn at the bottom-left of the panel.
            val back = backButtonRenderable
            if (back != null) {
                backButtonW = back.width
                backButtonH = back.height
                backButtonX = xOffset + 16
                backButtonY = yOffset + 16 + (column.height - back.height)
            }
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
        }
    }

    private fun buildColumn(): Renderable {
        val header = Renderable.text("§c${thirdParty.displayName} Features")
        val list = scrollRenderable ?: Renderable.text("§7No features registered")
        val backButton = Renderable.hoverable(
            Renderable.drawInsideRoundedRect(
                Renderable.text("§fBack", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER),
                colorRowHover.toColor(),
                padding = 4,
                radius = 5,
            ),
            Renderable.drawInsideRoundedRect(
                Renderable.text("§fBack", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER),
                colorButton.toColor(),
                padding = 4,
                radius = 5,
            ),
            bypassChecks = true,
        ).also { backButtonRenderable = it }

        return Renderable.vertical(
            listOf(
                header,
                Renderable.placeholder(0, 8),
                list,
                Renderable.placeholder(0, 8),
                backButton,
            ),
            spacing = 8,
            horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
        )
    }

    override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        // Explicit hitbox handling instead of relying on the global renderable click dispatch.
        if (mouseButton == 0 && backButtonW > 0 &&
            originalMouseX in backButtonX..(backButtonX + backButtonW) &&
            originalMouseY in backButtonY..(backButtonY + backButtonH)
        ) {
            MinecraftCompat.screen = parent
        }
    }
}