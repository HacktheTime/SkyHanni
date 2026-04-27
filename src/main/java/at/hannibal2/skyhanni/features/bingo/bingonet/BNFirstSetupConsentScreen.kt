package at.hannibal2.skyhanni.features.bingo.bingonet

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import de.hype.bingonet.BNConnection
import de.hype.bingonet.BNConnection.reconnectToBNServer

class BNFirstSetupConsentScreen : SkyHanniBaseScreen() {

    private val bingoNetworksConfig get() = SkyHanniMod.feature.event.bingo.bingoNetworks

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val contentWidth = 4 * this.width / 5
        val contentHeight = 4 * this.height / 5
        val xTranslate = this.width / 10
        val yTranslate = this.height / 10
        val textWidth = contentWidth - 40

        drawDefaultBackground(mouseX, mouseY, partialTicks)
        DrawContextUtils.translate(xTranslate - 2.0, yTranslate - 2.0)
        GuiRenderUtils.drawFloatingRectDark(0, 0, contentWidth, contentHeight)
        DrawContextUtils.translate(-(xTranslate - 2.0), -(yTranslate - 2.0))

        DrawContextUtils.translate(xTranslate.toFloat(), yTranslate.toFloat() + 5)
        Renderable.withMousePosition(mouseX - xTranslate, mouseY - yTranslate) {
            Renderable.vertical(
                listOf(
                    buildTitle(textWidth),
                    buildDescription(textWidth),
                    buildPrivacyLink(textWidth),
                    buildDiscordInfo(textWidth),
                    buildDiscordLink(textWidth),
                    Renderable.placeholder(0, 4),
                    buildEnableButton(textWidth),
                    buildDisableButton(textWidth),
                ),
                spacing = 10,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            ).renderXYAligned(0, 0, contentWidth, contentHeight)
        }
        DrawContextUtils.translate(-xTranslate.toFloat(), -yTranslate.toFloat() - 5)
    }

    private fun buildTitle(textWidth: Int) = Renderable.wrappedText(
        "§b§lEnable Bingo Net Server?",
        textWidth,
        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
    )

    private fun buildDescription(textWidth: Int) = Renderable.wrappedText(
        "Bingo Net is more of a Network than a Mod. " +
            "You are able to use some Features locally BUT the Point behind this Mod comes from connecting to the Server. " +
            "We think its important to inform users of our Privacy Policy before we open a connection for the first time.",
        textWidth,
    )

    private fun buildPrivacyLink(textWidth: Int) = Renderable.link(
        Renderable.wrappedText("§aOpen Privacy Policy: https://hackthetime.de/privacy", textWidth),
        bypassChecks = true,
        onLeftClick = {
            OSUtils.openBrowser("https://hackthetime.de/privacy")
        },
    )

    private fun buildDiscordInfo(textWidth: Int) = Renderable.wrappedText(
        "§eImportant: Note that the network needs a \"registration\" which consists out of linking a discord account to your mc account, accepting our TOS and Privacy Policy etc.",
        textWidth,
    )

    private fun buildDiscordLink(textWidth: Int) = Renderable.link(
        Renderable.wrappedText("§bBingo Net Discord: https://hackthetime.de/discord", textWidth),
        bypassChecks = true,
        onLeftClick = {
            OSUtils.openBrowser("https://hackthetime.de/discord")
        },
    )

    private fun buildEnableButton(textWidth: Int) = Renderable.darkRectButton(
        Renderable.wrappedText("§aEnable Bingo Net and Connect", textWidth),
        onClick = {
            bingoNetworksConfig.bingoNet.useBN = true
            SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "BN first-setup consent accepted")
            BNConnection.reconnectToBNServer()
            mc.setScreen(null)
        },
        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
    )

    private fun buildDisableButton(textWidth: Int) = Renderable.darkRectButton(
        Renderable.wrappedText("§7Keep Bingo Net Disabled", textWidth),
        onClick = {
            bingoNetworksConfig.bingoNet.useBN = false
            SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "BN first-setup consent declined")
            mc.setScreen(null)
        },
        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
    )

    companion object {
        fun openScreen() {
            SkyHanniMod.shouldCloseScreen = false
            SkyHanniMod.screenToOpen = BNFirstSetupConsentScreen()
        }
    }
}
