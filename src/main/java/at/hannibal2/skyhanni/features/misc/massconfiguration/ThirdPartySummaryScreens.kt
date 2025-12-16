package at.hannibal2.skyhanni.features.misc.massconfiguration

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
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

    private val sections: List<CategorySection> = entries
        .groupBy { it.category }
        .map { (category, catEntries) ->
            val groups = catEntries.groupBy { it.thirdParty }
                .map { (thirdParty, tpEntries) ->
                    ThirdPartyGroup(thirdParty, tpEntries.map { it.option })
                }
                .sortedBy { it.thirdParty.displayName }
            CategorySection(category, groups)
        }
        .sortedBy { it.category.name }

    private var scrollRenderable: Renderable? = null
    private var lastScrollHeight = -1

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground(mouseX, mouseY, partialTicks)
        val contentWidth = (width * 0.75).toInt()
        val contentHeight = (height * 0.8).toInt()
        val listHeight = (contentHeight - 160).coerceAtLeast(80)
        ensureScrollRenderable(listHeight)
        val xOffset = (width - contentWidth) / 2
        val yOffset = (height - contentHeight) / 2

        Renderable.withMousePosition(mouseX - xOffset, mouseY - yOffset) {
            Renderable.drawInsideDarkRect(buildColumn(contentWidth, listHeight))
                .renderXYAligned(xOffset, yOffset, contentWidth, contentHeight)
        }
    }

    private fun buildColumn(contentWidth: Int, listHeight: Int): Renderable {
        val header = Renderable.text("§dThird-Party Options Summary")
        val description = Renderable.text(
            "§7These options depend on servers that are not run or controlled by the SkyHanni Team."
        )
        val reminder = Renderable.text("§7You can revisit this summary later under §f/skyhanni → Third-Party Consent§7.")
        val list = scrollRenderable ?: Renderable.text("§7No third-party options detected during this wizard.")
        val toggleLabel = if (SkyHanniMod.feature.thirdPartyConsent.showThirdPartySummary) "Don't show again" else "Show this summary again"
        val toggleButton = Renderable.darkRectButton(
            content = Renderable.text(toggleLabel),
            onClick = { toggleNeverShow() },
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )
        val closeButton = Renderable.darkRectButton(
            content = Renderable.text("Close"),
            onClick = { mc.displayGuiScreen(null) },
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )
        val buttonRow = Renderable.horizontal(
            listOf(toggleButton, closeButton),
            spacing = 12,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )

        return Renderable.vertical(
            listOf(
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
            horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
        )
    }

    private fun ensureScrollRenderable(height: Int) {
        if (height <= 0) return
        if (scrollRenderable != null && height == lastScrollHeight) return
        lastScrollHeight = height
        scrollRenderable = if (sections.isEmpty()) {
            Renderable.text("§7No third-party options detected during this wizard.")
        } else {
            Renderable.scrollList(buildCategoryRenderables(), height = height)
        }
    }

    private fun buildCategoryRenderables(): List<Renderable> = sections.map { section ->
        val header = Renderable.text("§e${section.category.name}")
        val description = section.category.description.takeIf { it.isNotBlank() }
            ?.let { Renderable.text("§7$it") }
        val groups = section.thirdParties.map { group ->
            val tpTitle = Renderable.text("§c${group.thirdParty.displayName}")
            val optionList = Renderable.vertical(
                group.options.map { option ->
                    val desc = option.description.takeIf { it.isNotBlank() }?.let { " §7– $it" } ?: ""
                    Renderable.text("§7• §f${option.name}$desc")
                },
                spacing = 2,
                horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
            )
            Renderable.vertical(
                listOf(tpTitle, optionList),
                spacing = 4,
                horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
            )
        }
        Renderable.vertical(
            buildList {
                add(header)
                description?.let { add(it) }
                addAll(groups)
                add(Renderable.placeholder(0, CONTENT_PADDING / 2))
            },
            spacing = 6,
            horizontalAlign = RenderUtils.HorizontalAlignment.LEFT,
        )
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

    private data class CategorySection(
        val category: Category,
        val thirdParties: List<ThirdPartyGroup>,
    )

    private data class ThirdPartyGroup(
        val thirdParty: at.hannibal2.skyhanni.config.ThirdParty,
        val options: List<FeatureToggleableOption>,
    )
}
