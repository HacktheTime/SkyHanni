package at.hannibal2.skyhanni.features.tutorial.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.tutorial.TutorialOverlayDisplay
import at.hannibal2.skyhanni.features.tutorial.TutorialManager
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyhanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.primitives.text

/**
 * Simple manager GUI for Tutorials: shows tree and a few toggles.
 */
class TutorialManagerGui : SkyhanniBaseScreen() {

    private val config get() = SkyHanniMod.feature.tutorials

    private val sizeX = 320
    private val sizeY = 220

    override fun onDrawScreen(originalMouseX: Int, originalMouseY: Int, partialTicks: Float) {
        val guiLeft = (width - sizeX) / 2
        val guiTop = (height - sizeY) / 2
        val mx = originalMouseX - guiLeft
        val my = originalMouseY - guiTop

        DrawContextUtils.pushMatrix()
        try {
            DrawContextUtils.translate(guiLeft.toFloat(), guiTop.toFloat(), 0f)
            GuiRenderUtils.drawRect(0, 0, sizeX, sizeY, 0x80000000.toInt())

            val tutorial = TutorialManager.activeTutorial

            val list = mutableListOf<Renderable>()
            list.add(Renderable.text("§b§lTutorial Manager"))
            list.add(
                Renderable.clickable(
                    "§eShow Descriptions: §f${if (config.showStepDescriptions) "§aON" else "§cOFF"}",
                    onLeftClick = {
                        config.showStepDescriptions = !config.showStepDescriptions
                        TutorialOverlayDisplay.markDirty()
                    },
                ),
            )
            list.add(
                Renderable.clickable(
                    "§eShow Hidden Optional Paths: §f${if (config.showHiddenOptionalPaths) "§aON" else "§cOFF"}",
                    onLeftClick = {
                        config.showHiddenOptionalPaths = !config.showHiddenOptionalPaths
                        TutorialOverlayDisplay.markDirty()
                    },
                ),
            )
            list.add(
                Renderable.clickable(
                    "§cClose",
                    tips = listOf("§7Click to close"),
                    onLeftClick = { mc.displayGuiScreen(null) },
                ),
            )
            list.add(text(" "))

            if (tutorial != null) {
                list.add(tutorial.getRenderable(config.showStepDescriptions))
            } else {
                list.add(text("§7No active tutorial."))
            }

            val content = vertical(list)

            Renderable.withMousePosition(mx, my) {
                content.renderXYAligned(8, 8, sizeX - 16, sizeY - 16)
            }
        } finally {
            DrawContextUtils.popMatrix()
        }
    }
}
