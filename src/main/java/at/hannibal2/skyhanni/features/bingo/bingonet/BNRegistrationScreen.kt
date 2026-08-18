package at.hannibal2.skyhanni.features.bingo.bingonet

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.event.bingo.BingoNetSystem
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.features.misc.discordrpc.DiscordRPCManager
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.MojangUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import de.hype.bingonet.BNConnection
import de.hype.bingonet.BNConnection.reconnectToBNServer
import de.hype.bingonet.environment.packetconfig.InterceptPacketInfo
import de.hype.bingonet.shared.packets.network.RequestAuthentication
import de.hype.bingonet.shared.packets.network.RequestRegisterPacket
import io.github.notenoughupdates.moulconfig.ChromaColour
import java.awt.Color
import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.seconds

class BNRegistrationScreen(
    val discordUserId: String,
    val discordUserName: String,
) : SkyHanniBaseScreen() {

    private fun title(maxSize: Int) = centeredText("§b§lBingo Net Registration", maxSize)
    private fun description(maxSize: Int) = text(
        "§c⚠ Warning ⚠: The Bingo Net Server is a closed source Project by Hype_the_Time. " +
            "We as the Sky Hanni Team DO NOT HAVE ACCESS to the Server nor its Code.",
        maxSize,
    )

    private fun repeatLabel(maxSize: Int) =
        text("Please repeat the following Text in the Box below: \"${RequestRegisterPacket.PHRASE}\"", maxSize)

    val textInput = TextInput()
    private fun textBox(width: Int) = Renderable.textBox("", textInput, width, bypassChecks = true)
    private fun correctAccount(maxSize: Int) = Renderable.clickable(
        render = text(
            "We auto detected your running §bDiscord§f. Do you want to register with your §6$discordUserName§f Account? The Mc and DC connection can not be changed anymore afterwards! If this is not the desired Account swap over to it and follow the Bots DM instructions",
            maxSize,
        ),
        onLeftClick = {
            OSUtils.openBrowser("https://hackthetime.de/discord")
        },
    )

    private fun discordLabel(maxSize: Int) = text(
        "Due too how Bingo Net works you break parts of the Functionality for you, BUT ALSO FOR OTHERS if you are not on the Discord." +
            " During Registration you HAVE to be in the Discord!",
        maxSize,
    )

    private fun discordLink(maxSize: Int) = Renderable.link(
        text("§bBingo Net Discord: https://hackthetime.de/discord", maxSize), bypassChecks = true,
        onLeftClick = {
            OSUtils.openBrowser("https://hackthetime.de/discord")
        },
    )

    private fun openTermsRenderable(maxSize: Int) = Renderable.clickable(
        text("§a(Click to open Terms of Service, Privacy Policy and Rules)", maxSize),
        onLeftClick = {
            clickedTos = SimpleTimeMark.now()
            openTerms()
        },
        bypassChecks = true,
    )

    var clickedTos: SimpleTimeMark? = null
    private fun confirmButton(maxSize: Int) = buildButton(
        text(
            "§8I accept the Terms of Service, Privacy Policy and Rules (click to register)", maxSize,
        ),
        colorBtnConfirm.toColor(),
        onClick = {
            val clicked = clickedTos
            if (clicked == null) {
                openTerms()
                clickedTos = SimpleTimeMark.now().plus(30.seconds)
                feedbackMessage =
                    "§cYou need to read the Text above before you can continue."
            } else if (clicked.isInPast()) {
                registerNow()
            } else openTerms()
        },
    )

    private fun buildButton(
        content: Renderable,
        color: Color,
        onClick: () -> Unit,
    ): Renderable = Renderable.clickable(
        Renderable.hoverable(
            Renderable.drawInsideRoundedRect(content, color.brighter(), padding = 6, radius = 6),
            Renderable.drawInsideRoundedRect(content, color, padding = 6, radius = 6),
            bypassChecks = true,
        ),
        onLeftClick = onClick,
        bypassChecks = true,
    )

    // Feedback message to show to the user
    private var feedbackMessage: String? = null

    private val colorBg = ChromaColour.fromStaticRGB(18, 18, 28, 235)
    private val colorOutlineTop = ChromaColour.fromStaticRGB(100, 100, 160, 255)
    private val colorOutlineBot = ChromaColour.fromStaticRGB(55, 55, 100, 255)
    private val colorBtnConfirm = ChromaColour.fromStaticRGB(45, 45, 90, 215)

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val contentWidth = 4 * this.width / 5
        val contentHeight = 4 * this.height / 5
        val xTranslate = this.width / 10
        val yTranslate = this.height / 10
        val textWidth = contentWidth - 40
        // Calculate the main area similar to ChangeLogViewerScreen
        drawDefaultBackground(mouseX, mouseY, partialTicks)
        DrawContextUtils.translate(xTranslate.toFloat(), yTranslate.toFloat() + 5)
        Renderable.withMousePosition(mouseX - xTranslate, mouseY - yTranslate) {
            // Text width should be smaller than content width for proper wrapping

            // Create a list of all UI elements
            val elements = mutableListOf<Renderable>()
            elements.add(title(textWidth))
            elements.add(description(textWidth))
            elements.add(repeatLabel(textWidth))
            elements.add(textBox(textWidth))
            elements.add(correctAccount(textWidth))
            elements.add(discordLabel(textWidth))
            elements.add(discordLink(textWidth))
            elements.add(openTermsRenderable(textWidth))
            elements.add(confirmButton(textWidth))
            feedbackMessage?.let { msg ->
                elements.add(Renderable.wrappedText(msg, textWidth))
            }

            // Create a vertical list that centers itself
            Renderable.drawInsideFloatingRectWithBorder(
                Renderable.vertical(
                    elements,
                    spacing = 10,
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                    verticalAlign = RenderUtils.VerticalAlignment.CENTER,
                ),
                backgroundColor = colorBg,
                lightColor = colorOutlineTop,
                darkColor = colorOutlineBot,
                padding = 18,
                radius = 12,
                smoothness = 2,
                borderThickness = 2,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            ).renderXYAligned(0, 0, contentWidth, contentHeight)
        }
        DrawContextUtils.translate(-xTranslate.toFloat(), -yTranslate.toFloat() - 5)
    }

    fun openTerms() {
        val url = "https://hackthetime.de"
        OSUtils.openBrowser("$url/privacy")
        sleep(100)
        OSUtils.openBrowser("$url/tos")
        sleep(100)
        OSUtils.openBrowser("$url/rules")
        sleep(100)
    }

    fun registerNow() {
        if (!RequestRegisterPacket.passesAccuracyCheck(textInput.textBox)) {
            // Show error that they have to write the phrase.
            feedbackMessage = "§cYou must correctly repeat the phrase in the box above (95% accuracy, case-insensitive)."
            return
        }
        feedbackMessage = "§eWaiting for server response..."

        val intercept: InterceptPacketInfo<RequestAuthentication> =
            object : InterceptPacketInfo<RequestAuthentication>(
                RequestAuthentication::class.java, true, true,
                false, true,
            ) {
                override fun run(packet: RequestAuthentication) {
                    val clientRandom = MojangUtils.generateClientRandom()
                    val full = clientRandom + packet.serverIdSuffix
                    MojangUtils.joinServer(full)
                    val requestRegistration = RequestRegisterPacket(
                        PlayerUtils.getRawUuid(),
                        discordUserId,
                        clientRandom,
                    )
                    BNConnection.sendPacket(requestRegistration)
                }
            }
        val reponseIntercept = object : InterceptPacketInfo<RequestRegisterPacket.MCRegistrationResponsePacket>(
            RequestRegisterPacket.MCRegistrationResponsePacket::class.java, true, true,
            false, true,
        ) {
            override fun run(packet: RequestRegisterPacket.MCRegistrationResponsePacket) {
                val stringResponse = when (packet.response) {
                    RequestRegisterPacket.MCRegistrationResponsePacket.ResponseType.AWAITING_DC_USER_CONFIRMATION ->
                        "§aYou should have received a DM on Discord to confirm your Account."

                    RequestRegisterPacket.MCRegistrationResponsePacket.ResponseType.NOT_ON_DISCORD ->
                        "§cYou are not on the Bingo Net Discord Server. You need to join for this to work."

                    RequestRegisterPacket.MCRegistrationResponsePacket.ResponseType.ALREADY_REGISTERED ->
                        "§cEither your MC Account or Discord Account is already registered."

                    RequestRegisterPacket.MCRegistrationResponsePacket.ResponseType.BAD_REQUEST ->
                        "§cThe request was malformed. Please open a Bug Report on the Bingo Net Discord Server."

                    RequestRegisterPacket.MCRegistrationResponsePacket.ResponseType.ERROR ->
                        "§cThere was an error on our side (Bingo Net)."
                }
                feedbackMessage = stringResponse
            }
        }
        SkyHanniMod.launchCoroutine("BN Registration") {
            BNConnection.reconnectToBNServer(false, BingoNetSystem.MAIN, listOf(intercept, reponseIntercept))
        }
    }

    companion object {
        fun openHelper() {
            SkyHanniMod.launchCoroutine("Opening BN Registration Helper") {
                try {
                    val discordUser = DiscordRPCManager.getDiscordUser()
                    if (discordUser != null) {
                        ChatUtils.clickableChat(
                            "§cYou are not registered in the Bingo Net Network. Click here to open the Registration Screen",
                            {
                                SkyHanniMod.screenToOpen =
                                    BNRegistrationScreen(discordUser.userId, discordUser.username)
                            },
                        )
                        return@launchCoroutine
                    }
                } catch (t: Throwable) {
                    ErrorManager.logErrorWithData(
                        t,
                        "Failed to obtain Discord User ID and Username for prefill. Falling back to manual.",
                    )
                }

                ChatUtils.clickableChat(
                    "§cYou are not registered in the Bingo Net Network." +
                        " Click here to open the Discord Invite and follow the Instructions. Our Bot will send a DM with instructions, BUT" +
                        " the process is handled IN THE SERVER. The Bot will only inform you where to go and NOT handle it in the DMs. ",
                    {
                        OSUtils.openBrowser("https://hackthetime.de/discord")
                    },
                )
            }
        }
    }

    fun centeredText(text: String, maxSize: Int): Renderable =
        Renderable.wrappedText(
            text,
            maxSize,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )


    fun text(text: String, maxSize: Int): Renderable =
        Renderable.wrappedText(
            text,
            maxSize,
        )

}
