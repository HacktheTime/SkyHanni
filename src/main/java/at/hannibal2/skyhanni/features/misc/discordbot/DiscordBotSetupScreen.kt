package at.hannibal2.skyhanni.features.misc.discordbot

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.features.misc.discordbot.DiscordBotSHEvents.jda
import java.lang.Thread.sleep

class DiscordBotSetupScreen : SkyHanniBaseScreen() {

    private fun title(maxSize: Int) =
        Renderable.wrappedText("§b§lDiscord Bot Setup", maxSize, horizontalAlign = RenderUtils.HorizontalAlignment.CENTER)

    private var feedbackMessage: String? = null

    // Step state
    private var step = 1

    private val botTokenInput = TextInput()
    private val clientSecretInput = TextInput()

    private fun stepOne(maxSize: Int) = Renderable.vertical(
        listOf(
            Renderable.wrappedText(
                "Step 1: Create an application and bot in the Discord Developer Portal:\nhttps://discord.com/developers/applications",
                maxSize,
            ),
            // When the user clicks the link to open the portal, advance to step 2 so the UI is not stuck on step 1
            Renderable.link(
                Renderable.wrappedText("Open Developer Portal", maxSize), bypassChecks = true,
                onLeftClick = {
                    OSUtils.openBrowser("https://discord.com/developers/applications")
                    step = 2
                    feedbackMessage = "§eOpened Developer Portal. Proceed to Step 2: Add a Bot user and copy the Bot Token."
                },
            ),
        ),
        spacing = 6,
    )

    private fun stepTwo(maxSize: Int) = Renderable.vertical(
        listOf(
            Renderable.wrappedText(
                "Step 2: Add a Bot user to your application and copy the Bot Token. If you don't see the token click 'Reset Token'. Also enable Presence, Server Members and Message Content intents (Message Content may be required for some features).",
                maxSize,
            ),
            Renderable.textBox("Bot Token", botTokenInput, maxSize, bypassChecks = true),
            Renderable.darkRectButton(
                Renderable.wrappedText("Start JDA (init bot)", maxSize),
                onClick = {
                    val tok = botTokenInput.textBox.trim()
                    if (tok.isEmpty()) {
                        feedbackMessage = "§cPlease enter the bot token first."
                        return@darkRectButton
                    }
                    feedbackMessage = "§eStarting JDA..."
                    SkyHanniMod.launchCoroutine("Start JDA From GUI") {
                        SkyHanniMod.feature.discordBot.botToken = tok
                        SkyHanniMod.feature.discordBot.enable = true
                        val message: String =
                            try {
                                DiscordBotManager.getJdaOrNull().let {
                                    if (it == null) {
                                        "Bad Token"
                                    } else {
                                        "Success"
                                    }
                                }
                            } catch (t: Throwable) {
                                t.message ?: "Unknown error"
                            }
                        if (jda == null) {
                            feedbackMessage = "§cFailed to start JDA. $message"
                        } else {
                            SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "bot-setup")
                            feedbackMessage = "§aJDA started. Application id: ${DiscordBotManager.applicationInfo.id}"
                            step = 3
                        }
                    }
                },
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER, bypassChecks = true,
            ),
        ),
        spacing = 10,
    )

    private fun stepThree(maxSize: Int) = Renderable.vertical(
        listOf(
            Renderable.wrappedText(
                "Step 3: Get your Client Secret. Open the OAuth2 page for your application to copy the client secret.",
                maxSize,
            ),
            Renderable.link(
                Renderable.wrappedText("Open OAuth2 page for application", maxSize), bypassChecks = true,
                onLeftClick = {
                    try {
                        val id = DiscordBotManager.applicationInfo.id
                        OSUtils.openBrowser("https://discord.com/developers/applications/$id/oauth2")
                    } catch (_: Exception) {
                        OSUtils.openBrowser("https://discord.com/developers/applications")
                    }
                },
            ),
            Renderable.textBox("Client Secret", clientSecretInput, maxSize, bypassChecks = true),
            Renderable.darkRectButton(
                Renderable.wrappedText("Save Client Secret", maxSize),
                onClick = {
                    val secret = clientSecretInput.textBox.trim()
                    if (secret.isEmpty()) {
                        feedbackMessage = "§cPlease enter the client secret."
                        return@darkRectButton
                    }
                    // persist to config
                    val devOauthToken = try {
                        DiscordBotManager.getDeveloperOauthToken()
                    } catch (_: Throwable) {
                        null
                    }
                    SkyHanniMod.feature.discordBot.clientSecret = secret
                    if (devOauthToken != null) {
                        feedbackMessage = "§aClient secret saved. You can now close this screen!"
                        sleep(1000)
                    } else {
                        feedbackMessage = "§cFailed to get developer token with the provided client secret. Please double-check the value."
                    }
                },
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER, bypassChecks = true,
            ),
        ),
        spacing = 10,
    )

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

            Renderable.vertical(
                elements, spacing = 10,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            ).renderXYAligned(0, 0, contentWidth, contentHeight)
        }
        DrawContextUtils.translate(-xTranslate.toFloat(), -yTranslate.toFloat() - 5, 0f)
    }

}
