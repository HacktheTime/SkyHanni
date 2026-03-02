package at.hannibal2.skyhanni.config.features.event.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.darkRectButton
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.utils.RenderUtils

/**
 * Simple screen to configure the Bingo Brewers splash message stored in SplasherConfig.bbSplashMessage
 * Requires the message to contain the placeholders: SERVER_ID, HUB, ROLE_MENTIONS, SPLASHER
 * User can add optional extra placeholders as text (they will be replaced later at post time).
 */
class BBSplashMessageConfigureScreen : SkyHanniBaseScreen() {

    private val textInput = TextInput()

    private var feedback: String? = null

    private val requiredPlaceholders = listOf(SERVER_ID, HUB, ROLE_MENTIONS, SPLASHER)

    override fun onInitGui() {
        // ensure TextInput is not active by default
        TextInput.disable()
        // initialize from config
        textInput.textBox = SkyHanniMod.feature.event.bingo.bingoNetworks.splasherConfig.bbSplashMessage
        feedback = null
    }

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground(mouseX, mouseY, partialTicks)

        val contentW = (width * 0.8).toInt()
        val contentH = (height * 0.6).toInt()
        val left = (width - contentW) / 2
        val top = (height - contentH) / 2

        DrawContextUtils.translate(left.toFloat(), top.toFloat())
        // drawFloatingRectDark is on GuiRenderUtils; replicate small container without importing GuiRenderUtils to keep imports minimal
        // however other screens usually call GuiRenderUtils.drawFloatingRectDark before using Renderable.withMousePosition; we'll skip and rely on container position

        DrawContextUtils.translate(16f, 12f)
        val textWidth = contentW - 32

        val elems = mutableListOf<Renderable>()
        elems.add(
            Renderable.wrappedText(
                "§b§lConfigure Bingo Brewers Splash Message", textWidth,
                horizontalAlign =
                    RenderUtils
                        .HorizontalAlignment.CENTER,
            ),
        )
        elems.add(
            Renderable.wrappedText(
                "\nThe message must contain the placeholders: ${requiredPlaceholders.joinToString(", ")}",
                textWidth,
            ),
        )
        elems.add(Renderable.wrappedText("You can also add the optional placeholder: $EXTRA_MESSAGE", textWidth))
        //FIXME the user must be forced to make it so the BB system detects them as the splasher since otherwise the dual send causes
        // issues.
        elems.add(Renderable.textBox("Message", textInput, textWidth))
        feedback?.let { elems.add(Renderable.wrappedText(it, textWidth)) }

        // buttons
        val buttons = Renderable.vertical(
            listOf(
                darkRectButton(
                    Renderable.wrappedText("Save Message", textWidth),
                    onClick = {
                        val msg = textInput.textBox.trim()
                        val missing = requiredPlaceholders.filter { !msg.contains(it) }
                        if (missing.isNotEmpty()) {
                            feedback = "§cMissing placeholders: ${missing.joinToString(", ")}. They are required."
                            return@darkRectButton
                        }
                        // persist to config
                        SkyHanniMod.feature.event.bingo.bingoNetworks.splasherConfig.bbSplashMessage = msg
                        SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "save-bb-splash-message")
                        feedback = "§aSaved."
                    },
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                ),
                darkRectButton(
                    Renderable.wrappedText("Cancel", textWidth),
                    onClick = {
                        mc.setScreen(null)
                    },
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                ),
            ),
            6, RenderUtils.HorizontalAlignment.CENTER,
            RenderUtils.VerticalAlignment.CENTER,
        )

        elems.add(buttons)

        // Render within the current mouse position context so child renderables can access mouse coords correctly
        Renderable.withMousePosition(mouseX - left - 16, mouseY - top - 12) {
            Renderable.vertical(elems, spacing = 8, horizontalAlign = RenderUtils.HorizontalAlignment.LEFT)
                .renderXYAligned(0, 0, textWidth, contentH)
        }

        DrawContextUtils.translate(-16f, -12f)
        DrawContextUtils.translate(-left.toFloat(), -top.toFloat())
    }

    companion object {
        @JvmStatic
        fun openScreen() {
            SkyHanniMod.shouldCloseScreen = false
            SkyHanniMod.screenToOpen = BBSplashMessageConfigureScreen()
        }

        const val SERVER_ID = "{serverID}"
        const val HUB = "{hub}"
        const val ROLE_MENTIONS = "{role_mention}"
        const val SPLASHER = "{IGN}" //must contain a line Splasher: <name>!
        const val EXTRA_MESSAGE = "{extra_message}" //Optional
    }
}
