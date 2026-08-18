package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.misc.PartyCommandsConfig
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import net.minecraft.client.gui.screens.Screen
import java.awt.Color
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment as HA
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment as VA

/**
 * Constructs the full Renderable display for [PartyUsersScreen] and opens the per-user
 * MoulConfig editor for editing a single [PartyCommandsConfig.TrustUserConfig].
 */
object PartyUsersGui {

    private val COLOR_BG = ChromaColour.fromStaticRGB(18, 18, 28, 235)
    private val COLOR_OUTLINE_TOP = ChromaColour.fromStaticRGB(100, 100, 160, 255)
    private val COLOR_OUTLINE_BOT = ChromaColour.fromStaticRGB(55, 55, 100, 255)
    private val COLOR_ROW_NORMAL = ChromaColour.fromStaticRGB(30, 30, 50, 180)
    private val COLOR_ROW_HOVER = ChromaColour.fromStaticRGB(55, 55, 85, 220)
    private val COLOR_BTN_ADD = ChromaColour.fromStaticRGB(35, 90, 35, 215)
    private val COLOR_BTN_DELETE = ChromaColour.fromStaticRGB(110, 30, 30, 215)
    private val COLOR_BTN_BACK = ChromaColour.fromStaticRGB(75, 65, 25, 215)
    private val COLOR_BTN_NEUTRAL = ChromaColour.fromStaticRGB(45, 45, 68, 215)

    fun open() {
        SkyHanniMod.screenToOpen = PartyUsersScreen()
    }

    /**
     * Opens a MoulConfig editor bound to a single user's config. Closing it returns to
     * [previousScreen].
     */
    fun openEditor(userConfig: PartyCommandsConfig.TrustUserConfig, previousScreen: Screen) {
        val processor = MoulConfigProcessor(userConfig)
        BuiltinMoulConfigGuis.addProcessors(processor)
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.processConfig(userConfig)
        val editor = MoulConfigEditor(processor)
        SkyHanniMod.screenToOpen = ConfigUtils.createConfigScreen(editor, previousScreen)
    }

    fun buildDisplay(screen: PartyUsersScreen): Renderable {
        val header = Renderable.text("§bParty Command Users", scale = 1.2, horizontalAlign = HA.CENTER)
        val subtitle = Renderable.text(
            "§7Per-user permissions for party chat commands",
            scale = 0.85,
            horizontalAlign = HA.CENTER,
        )

        val listArea = if (screen.userNames.isEmpty()) {
            Renderable.text("§7No users configured yet — add one below.", horizontalAlign = HA.CENTER)
        } else {
            Renderable.scrollList(
                screen.userNames.map { buildUserRow(screen, it) },
                height = 150,
                scrollValue = screen.listScrollValue,
                velocity = 4.0,
                bypassChecks = true,
                showScrollableTipsInList = false,
                showScrollbar = true,
            )
        }

        val addRow = Renderable.horizontal(
            listOf(
                buildAddField(screen),
                buildButton("§a+ Add", COLOR_BTN_ADD.toColor()) { screen.addUser() },
            ),
            spacing = 8,
            verticalAlign = VA.CENTER,
        )
        val suggestionRow = buildSuggestionDropdown(screen)
        val bottomRow = Renderable.horizontal(
            listOf(buildButton("§e← Back", COLOR_BTN_BACK.toColor()) { screen.onClose() }),
            spacing = 8,
            verticalAlign = VA.CENTER,
        )

        return Renderable.drawInsideFloatingRectWithBorder(
            Renderable.vertical(
                listOfNotNull(header, subtitle, listArea, addRow, suggestionRow, bottomRow),
                spacing = 6,
                horizontalAlign = HA.CENTER,
            ),
            backgroundColor = COLOR_BG,
            lightColor = COLOR_OUTLINE_TOP,
            darkColor = COLOR_OUTLINE_BOT,
            padding = 14,
            radius = 12,
            smoothness = 2,
            borderThickness = 2,
        )
    }

    private fun buildUserRow(screen: PartyUsersScreen, name: String): Renderable {
        val nameFixed = Renderable.fixedSizeLine(Renderable.text("§f$name"), width = 230)
        val editBtn = buildButton("§bEdit", COLOR_BTN_NEUTRAL.toColor()) { screen.openUserEditor(name) }
        val removeBtn = buildButton("§c✗ Remove", COLOR_BTN_DELETE.toColor()) { screen.removeUser(name) }
        val rowContent = Renderable.horizontal(
            listOf(nameFixed, editBtn, removeBtn),
            spacing = 4,
            verticalAlign = VA.CENTER,
        )
        return Renderable.hoverable(
            Renderable.drawInsideRoundedRect(rowContent, COLOR_ROW_HOVER.toColor(), padding = 3, radius = 5),
            Renderable.drawInsideRoundedRect(rowContent, COLOR_ROW_NORMAL.toColor(), padding = 3, radius = 5),
            bypassChecks = true,
        )
    }

    /**
     * An editable text-field Renderable for the player name to add. Reads live from
     * [PartyUsersScreen.addNameInput] every frame; click to focus.
     */
    private fun buildAddField(screen: PartyUsersScreen): Renderable = Renderable.clickable(
        object : Renderable {
            override val width = 200
            override val height = 16
            override val horizontalAlign = HA.LEFT
            override val verticalAlign = VA.TOP

            override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
                val isActive = screen.activeInput === screen.addNameInput
                if (isActive) {
                    GuiRenderUtils.drawFloatingRectLight(0, 0, width, height, false)
                    screen.addNameInput.makeActive()
                    screen.addNameInput.handle()
                } else {
                    GuiRenderUtils.drawFloatingRectDark(0, 0, width, height, false)
                }
                val displayText = if (isActive) screen.addNameInput.editText() else screen.addNameInput.textBox
                DrawContextUtils.pushPop {
                    DrawContextUtils.translate(3f, ((height - 8) / 2).toFloat())
                    RenderableUtils.renderString(displayText, scale = 1.0, color = Color.WHITE)
                }
            }
        },
        onLeftClick = { screen.activeInput = screen.addNameInput },
        bypassChecks = true,
    )

    /**
     * A scrollable dropdown of matching friend/party names. Uses a fixed-height scrolled list so
     * accounts with thousands of friends never expand the GUI off screen.
     */
    private fun buildSuggestionDropdown(screen: PartyUsersScreen): Renderable {
        val toggle = buildButton(
            if (screen.suggestionsOpen) "§bFriend suggestions ▴" else "§bFriend suggestions ▾",
            COLOR_BTN_NEUTRAL.toColor(),
        ) {
            screen.suggestionsOpen = !screen.suggestionsOpen
            screen.suggestionsScroll.setValue(0.0)
            screen.rebuildDisplay()
        }
        if (!screen.suggestionsOpen) return toggle

        val candidates = screen.dropdownCandidates()
        val list = if (candidates.isEmpty()) {
            Renderable.text("§7No friends match — type to filter.", horizontalAlign = HA.CENTER)
        } else {
            Renderable.scrollList(
                candidates.map { name ->
                    buildButton("§f$name", COLOR_ROW_NORMAL.toColor()) { screen.selectSuggestion(name) }
                },
                height = 150,
                scrollValue = screen.suggestionsScroll,
                velocity = 4.0,
                bypassChecks = true,
                showScrollableTipsInList = false,
                showScrollbar = true,
            )
        }
        return Renderable.vertical(
            listOf(toggle, list),
            spacing = 4,
            horizontalAlign = HA.CENTER,
        )
    }

    private fun buildButton(label: String, color: Color, onClick: () -> Unit): Renderable {
        val text = Renderable.text(label)
        return Renderable.clickable(
            Renderable.hoverable(
                Renderable.drawInsideRoundedRect(text, color.brighter(), padding = 5, radius = 6),
                Renderable.drawInsideRoundedRect(text, color, padding = 5, radius = 6),
                bypassChecks = true,
            ),
            onLeftClick = onClick,
            bypassChecks = true,
        )
    }
}