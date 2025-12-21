package at.hannibal2.skyhanni.features.misc.massconfiguration

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text

private const val CONTENT_PADDING = 12

class ThirdPartySummaryIntroScreen(
    private val entries: List<ThirdPartySummaryEntry>,
) : SkyHanniBaseScreen() {

    private var hasOpenedSummary = false

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground(mouseX, mouseY, partialTicks)
        val contentWidth = (width * 0.65).toInt()
        val contentHeight = (height * 0.45).toInt()
        val xOffset = (width - contentWidth) / 2
        val yOffset = (height - contentHeight) / 2

        Renderable.withMousePosition(mouseX - xOffset, mouseY - yOffset) {
            Renderable.drawInsideDarkRect(buildColumn(contentWidth))
                .renderXYAligned(xOffset, yOffset, contentWidth, contentHeight)
        }
    }

    private fun buildColumn(contentWidth: Int): Renderable {
        val title = Renderable.text("§dThird-Party Services Notice")
        val lines = listOf(
            "§7Some of the configurable options rely on external services.",
            "§7Those services are run by their own teams, so the SkyHanni Team cannot control data handling or availability.",
            "§7We'll highlight these options so you can decide yourself whether you want to enable them or not.",
        ).map(Renderable::text)
        val continueButton = Renderable.darkRectButton(
            content = Renderable.text("Continue"),
            onClick = { proceed() },
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )
        val escHint = Renderable.text("§8(Press ESC to continue as well)")

        return Renderable.vertical(
            buildList {
                add(title)
                add(Renderable.placeholder(0, CONTENT_PADDING))
                addAll(lines)
                add(Renderable.placeholder(0, CONTENT_PADDING))
                add(continueButton)
                add(escHint)
            },
            spacing = 6,
            horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
        )
    }

    private fun proceed() {
        if (hasOpenedSummary) return
        hasOpenedSummary = true
        ThirdPartySummaryFlow.markIntroSeen()
        ThirdPartySummaryFlow.openSummary(entries)
    }

    override fun guiClosed() {
        super.guiClosed()
        if (!hasOpenedSummary) {
            hasOpenedSummary = true
            ThirdPartySummaryFlow.markIntroSeen()
            ThirdPartySummaryFlow.openSummary(entries)
        }
    }
}

class ThirdPartySummaryScreen(
    private val entries: List<ThirdPartySummaryEntry>,
) : SkyHanniBaseScreen() {

    // Group by thirdParty -> category
    private val byThirdParty: Map<at.hannibal2.skyhanni.config.ThirdParty, Map<Category, List<FeatureToggleableOption>>> =
        entries.groupBy { it.thirdParty }
            .mapValues { (_, list) -> list.groupBy { it.category }.mapValues { it.value.map { e -> e.option } } }

    // Accordion open state per third-party
    private val openState = byThirdParty.keys.associateWith { true }.toMutableMap()
    private var showOnlyNew = true // self-select: show only options included that are new

    private var scrollRenderable: Renderable? = null
    private var lastScrollHeight = -1

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        // draw a solid black background behind the centered dialog
        drawDefaultBackground(mouseX, mouseY, partialTicks)
        val contentWidth = (width * 0.75).toInt()
        val contentHeight = (height * 0.8).toInt()
        val listHeight = (contentHeight - 160).coerceAtLeast(80)
        ensureScrollRenderable(listHeight)
        val xOffset = (width - contentWidth) / 2
        val yOffset = (height - contentHeight) / 2

        // Translate to dialog origin and draw background rect (so mouse coords relative to origin)
        DrawContextUtils.pushMatrix()
        DrawContextUtils.translate(xOffset.toFloat(), yOffset.toFloat(), 0f)
        GuiRenderUtils.drawFloatingRectDark(0, 0, contentWidth, contentHeight)

        // Render inner content using local coordinates so Renderable click handling receives correct mouse coords
        Renderable.withMousePosition(mouseX - xOffset, mouseY - yOffset) {
            // build column with center alignment so content centers inside the dialog
            val col = buildColumn(contentWidth, listHeight)
            col.renderXYAligned(0, 0, contentWidth, contentHeight)
        }

        DrawContextUtils.popMatrix()
    }

    private fun buildColumn(contentWidth: Int, listHeight: Int): Renderable {
        val header = Renderable.text("§dThird-Party Options Summary")
        val debugPlaceholder = Renderable.placeholder(0, 4)
        val description = Renderable.text(
            "§7These options depend on servers that are not run or controlled by the SkyHanni Team."
        )
        val reminder = Renderable.text("§7You can revisit this summary later under §f/skyhanni → Third-Party Consent§7.")

        val toggleSelfSelect = Renderable.darkRectButton(
            content = Renderable.text(if (showOnlyNew) "Show: Only new options" else "Show: All third-party options"),
            onClick = {
                println("[TP Summary] toggleSelfSelect clicked")
                showOnlyNew = !showOnlyNew; scrollRenderable = null
            },
            bypassChecks = true,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )

        val toggleLabel = if (SkyHanniMod.feature.thirdPartyConsent.showThirdPartySummary) "Don't show again" else "Show this summary again"
        val toggleButton = Renderable.darkRectButton(
            content = Renderable.text(toggleLabel),
            onClick = {
                println("[TP Summary] toggleButton clicked")
                toggleNeverShow()
            },
            bypassChecks = true,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )
        val closeButton = Renderable.darkRectButton(
            content = Renderable.text("Close"),
            onClick = {
                println("[TP Summary] close clicked")
                mc.setScreen(null)
            },
            bypassChecks = true,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )
        val buttonRow = Renderable.horizontal(
            listOf(toggleSelfSelect, toggleButton, closeButton),
            spacing = 12,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )

        val list = scrollRenderable ?: Renderable.text("§7No third-party options detected during this wizard.")

        return Renderable.vertical(
            listOf(
                debugPlaceholder,
                header,
                Renderable.placeholder(0, CONTENT_PADDING / 2),
                description,
                reminder,
                Renderable.placeholder(0, CONTENT_PADDING),
                list,
                Renderable.placeholder(0, CONTENT_PADDING),
                buttonRow,
            ),
            spacing = 8,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            verticalAlign = RenderUtils.VerticalAlignment.CENTER,
        )
    }

    private fun ensureScrollRenderable(height: Int) {
        if (height <= 0) return
        if (scrollRenderable != null && height == lastScrollHeight) return
        lastScrollHeight = height
        scrollRenderable = if (byThirdParty.isEmpty()) {
            Renderable.text("§7No third-party options detected during this wizard.")
        } else {
            Renderable.scrollList(buildThirdPartyRenderables(), height = height, bypassChecks = true)
        }
    }

    private fun buildThirdPartyRenderables(): List<Renderable> {
        return byThirdParty.entries.sortedBy { it.key.displayName }.map { (tp, catMap) ->
            buildThirdPartySection(tp, catMap)
        }
    }

    private fun buildThirdPartySection(tp: at.hannibal2.skyhanni.config.ThirdParty, catMap: Map<Category, List<FeatureToggleableOption>>): Renderable {
        Renderable.text("§c${tp.displayName}")
        // Main toggle row if available
        val mainToggle = tp.mainToggleField?.let { field ->
            val enabled = try { tp.isEnabled() } catch (_: Throwable) { false }
            val label = Renderable.text("§7Main toggle: ${field.name} - " + if (enabled) "§aEnabled" else "§cDisabled")
            val toggleBtn = Renderable.darkRectButton(
                content = Renderable.text(if (enabled) "Disable" else "Enable"),
                onClick = {
                    try {
                        tp.setEnabled(!tp.isEnabled())
                        SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "third-party-toggle")
                        // reset scrollable so label updates
                        scrollRenderable = null
                    } catch (_: Throwable) {
                    }
                },
                bypassChecks = true,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            )
            Renderable.horizontal(listOf(label, toggleBtn), spacing = 12, horizontalAlign = RenderUtils.HorizontalAlignment.LEFT)
        }

        // Accordion header: clickable to toggle open state
        val open = openState.getOrDefault(tp, true)
        val accordionHeader = Renderable.horizontal(
            listOf(
                Renderable.text(if (open) "§a▾ §c${tp.displayName}" else "§a▸ §c${tp.displayName}"),
                Renderable.text("§7${tp.description}"),
            ),
            spacing = 8,
            horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
        )

        val clickableHeader = Renderable.clickable(
            accordionHeader,
            onAnyClick = mapOf(at.hannibal2.skyhanni.utils.KeyboardManager.LEFT_MOUSE to {
                println("[TP Summary] header clicked: ${tp.displayName}")
                openState[tp] = !openState.getOrDefault(tp, true)
                scrollRenderable = null
            }),
            bypassChecks = true,
        )

        if (!open) {
            // collapsed: only show header (and main toggle if present)
            val collapsedParts = buildList<Renderable> {
                add(clickableHeader)
                mainToggle?.let { add(it) }
                add(Renderable.placeholder(0, CONTENT_PADDING / 2))
            }
            return Renderable.vertical(collapsedParts, spacing = 6, horizontalAlign = RenderUtils.HorizontalAlignment.LEFT)
        }

        // Build category groups as accordion entries (expanded)
        val categoryRenderables = catMap.entries.sortedBy { it.key.name }.map { (category, options) ->
            val header = Renderable.text("§e${category.name}")
            val desc = category.description.takeIf { it.isNotBlank() }?.let { Renderable.text("§7$it") }
            val optsToShow = if (showOnlyNew) options else options // entries already only includes new options
            val optionList = Renderable.vertical(
                optsToShow.map { opt ->
                    val descText = opt.description.takeIf { it.isNotBlank() }?.let { " §7– $it" } ?: ""
                    val item = Renderable.text("§7• §f${opt.name}$descText")
                    Renderable.clickable(item, onAnyClick = mapOf(at.hannibal2.skyhanni.utils.KeyboardManager.LEFT_MOUSE to {
                        println("[TP Summary] option clicked: ${opt.name}")
                        // best-effort: jump to option editor by field path
                    }), bypassChecks = true)
                },
                spacing = 2,
                horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
            )
            // Combine header/desc/options
            val comp = buildList<Renderable> {
                add(header)
                desc?.let { add(it) }
                add(optionList)
            }
            Renderable.vertical(comp, spacing = 4, horizontalAlign = RenderUtils.HorizontalAlignment.LEFT)
        }

        val bodyParts = buildList<Renderable> {
            add(clickableHeader)
            mainToggle?.let { add(it) }
            addAll(categoryRenderables)
            add(Renderable.placeholder(0, CONTENT_PADDING / 2))
        }

        return Renderable.vertical(bodyParts, spacing = 6, horizontalAlign = RenderUtils.HorizontalAlignment.LEFT)
    }

    private fun toggleNeverShow() {
        val consent = SkyHanniMod.feature.thirdPartyConsent
        consent.showThirdPartySummary = !consent.showThirdPartySummary
        try {
            SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "third-party-summary-toggle")
        } catch (_: Throwable) {
            // Ignore persistence problems; the new state will take effect for this session regardless.
        }
    }

}
