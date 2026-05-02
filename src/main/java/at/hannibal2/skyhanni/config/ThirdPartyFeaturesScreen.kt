package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.features.misc.massconfiguration.FeatureToggleProcessor
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.client.gui.screens.Screen

class ThirdPartyFeaturesScreen(
    private val thirdParty: ThirdParty,
    private val parent: Screen? = null,
) : SkyHanniBaseScreen() {
    private var scrollRenderable: Renderable? = null

    override fun onInitGui() {
        val entries = FeatureToggleProcessor.thirdPartyRegistry[thirdParty]
            ?.map { Renderable.text("§c• §f${it.name}§7 – ${it.description}") }
            .orEmpty()
        scrollRenderable = Renderable.scrollList(entries, height = height - 160)
    }

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground(mouseX, mouseY, partialTicks)
        val contentWidth = (width * 0.75).toInt()
        val contentHeight = (height * 0.75).toInt()
        val xOffset = (width - contentWidth) / 2
        val yOffset = (height - contentHeight) / 2

        Renderable.withMousePosition(mouseX - xOffset, mouseY - yOffset) {
            val column = buildColumn(contentWidth)
            Renderable.drawInsideDarkRect(column)
                .renderXYAligned(xOffset, yOffset, contentWidth, contentHeight)
        }
    }

    private fun buildColumn(contentWidth: Int): Renderable {
        val padding = 10
        val header = Renderable.text("§c${thirdParty.displayName} Features")
        val list = scrollRenderable ?: Renderable.text("§7No features registered")
        val backButton = Renderable.darkRectButton(
            content = Renderable.text("Back"),
            onClick = { mc.setScreen(parent) },
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )

        return Renderable.vertical(
            listOf(
                header,
                Renderable.placeholder(0, padding),
                list,
                Renderable.placeholder(0, padding),
                backButton,
            ),
            spacing = padding,
            horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
        )
    }
}
