package at.hannibal2.skyhanni.features.misc.discordbot

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.data.model.TextInput
import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.seconds

class DiscordBotSetupScreen : SkyHanniBaseScreen() {

    private fun title(maxSize: Int) = Renderable.wrappedText("§b§lDiscord Bot Setup", maxSize, horizontalAlign = RenderUtils.HorizontalAlignment.CENTER)

    private var feedbackMessage: String? = null

    // Step state
    private var step = 1

    private val botTokenInput = TextInput()
    private val clientSecretInput = TextInput()

    private fun stepOne(maxSize: Int) = Renderable.vertical(listOf(
        Renderable.wrappedText("Step 1: Create an application and bot in the Discord Developer Portal:\nhttps://discord.com/developers/applications", maxSize),
        Renderable.link(Renderable.wrappedText("Open Developer Portal", maxSize), bypassChecks = true, onLeftClick = { OSUtils.openBrowser("https://discord.com/developers/applications") })
    ), spacing = 6)

    private fun stepTwo(maxSize: Int) = Renderable.vertical(listOf(
        Renderable.wrappedText("Step 2: Add a Bot user to your application and copy the Bot Token. If you don't see the token click 'Reset Token'. Also enable Presence, Server Members and Message Content intents (Message Content may be required for some features).", maxSize),
        Renderable.textBox("Bot Token", botTokenInput, maxSize, bypassChecks = true),
        Renderable.darkRectButton(Renderable.wrappedText("Start JDA (init bot)", maxSize), onClick = {
            val tok = botTokenInput.textBox.trim()
            if (tok.isEmpty()) {
                feedbackMessage = "§cPlease enter the bot token first."
                return@darkRectButton
            }
            step = 2
            feedbackMessage = "§eStarting JDA..."
            SkyHanniMod.launchCoroutine("Start JDA From GUI") {
                try {
                    DiscordBotManager.startJdaWithToken(tok)
                    // wait a bit for application info
                    sleep(1500)
                    feedbackMessage = "§aJDA started. Application id: ${DiscordBotManager.applicationInfo.id}"
                    step = 3
                } catch (t: Throwable) {
                    feedbackMessage = "§cFailed to start JDA: ${t.message}"
                }
            }
        }, horizontalAlign = RenderUtils.HorizontalAlignment.CENTER, bypassChecks = true)
    ), spacing = 10)

    private fun stepThree(maxSize: Int) = Renderable.vertical(listOf(
        Renderable.wrappedText("Step 3: Get your Client Secret. Open the OAuth2 page for your application to copy the client secret.", maxSize),
        Renderable.link(Renderable.wrappedText("Open OAuth2 page for application", maxSize), bypassChecks = true, onLeftClick = {
            try {
                val id = DiscordBotManager.applicationInfo.id
                OSUtils.openBrowser("https://discord.com/developers/applications/$id/oauth2")
            } catch (e: Exception) {
                OSUtils.openBrowser("https://discord.com/developers/applications")
            }
        }),
        Renderable.textBox("Client Secret", clientSecretInput, maxSize, bypassChecks = true),
        Renderable.darkRectButton(Renderable.wrappedText("Save Client Secret", maxSize), onClick = {
            val secret = clientSecretInput.textBox.trim()
            if (secret.isEmpty()) {
                feedbackMessage = "§cPlease enter the client secret."
                return@darkRectButton
            }
            // persist to config
            SkyHanniMod.feature.discordBot.clientSecret = secret
            // clear cached developer token so it will be fetched next time
            // (we access the manager's private fields by calling a refresh helper if needed; for now rely on next request)
            feedbackMessage = "§aClient secret saved. Will fetch developer token on next API call."
        }, horizontalAlign = RenderUtils.HorizontalAlignment.CENTER, bypassChecks = true)
    ), spacing = 10)

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val contentWidth = 4 * this.width / 5
        val contentHeight = 4 * this.height / 5
        val xTranslate = this.width / 10
        val yTranslate = this.height / 10
        val textWidth = contentWidth - 40
        // Calculate the main area similar to ChangeLogViewerScreen
        drawDefaultBackground(mouseX, mouseY, partialTicks)
        DrawContextUtils.translate(xTranslate - 2.0, yTranslate - 2.0, 0.0)
        GuiRenderUtils.drawFloatingRectDark(0, 0, contentWidth, contentHeight)
        DrawContextUtils.translate(-(xTranslate - 2.0), -(yTranslate - 2.0), 0.0)

        DrawContextUtils.translate(xTranslate.toFloat(), yTranslate.toFloat() + 5, 0f)
        Renderable.withMousePosition(mouseX - xTranslate, mouseY - yTranslate) {
            // Text width should be smaller than content width for proper wrapping

            val elements = mutableListOf<Renderable>()
            elements.add(title(textWidth))

            when (step) {
                1 -> elements.add(stepOne(textWidth))
                2 -> elements.add(stepTwo(textWidth))
                3 -> elements.add(stepThree(textWidth))
            }

            feedbackMessage?.let { elements.add(Renderable.wrappedText(it, textWidth)) }

            Renderable.vertical(elements, spacing = 10,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            ).renderXYAligned(0, 0, contentWidth, contentHeight)
        }
        DrawContextUtils.translate(-xTranslate.toFloat(), -yTranslate.toFloat() - 5, 0f)
    }

    fun centeredText(text: String, maxSize: Int) = Renderable.wrappedText(text, maxSize, horizontalAlign = RenderUtils.HorizontalAlignment.CENTER)

}
