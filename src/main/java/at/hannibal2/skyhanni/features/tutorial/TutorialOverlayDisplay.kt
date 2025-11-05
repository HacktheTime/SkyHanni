package at.hannibal2.skyhanni.features.tutorial

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.tutorial.gui.TutorialManagerGui
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.features.tutorial.gui.TutorialRenderableBuilder

/**
 * Displays the active tutorial in an SH overlay and wires up a GUI to manage it.
 */
@SkyHanniModule
object TutorialOverlayDisplay {

    private val config get() = SkyHanniMod.feature.tutorials

    private var cache: Renderable? = null
    private var dirty: Boolean = true

    fun markDirty() { dirty = true }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtutorial") {
            description = "Open the Tutorial Manager GUI"
            category = CommandCategory.USERS_ACTIVE
            callback { openGui() }
        }
    }

    private fun openGui() {
        SkyHanniMod.screenToOpen = TutorialManagerGui()
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!config.overlayEnabled) return
        val tutorial = TutorialManager.activeTutorial ?: return

        // Sync builder flags with config
        TutorialRenderableBuilder.showHiddenOptionalPaths = config.showHiddenOptionalPaths

        if (dirty || cache == null) {
            cache = tutorial.getRenderable(config.showStepDescriptions)
            dirty = false
        }

        val renderable = cache ?: return
        config.overlayPos.renderRenderable(renderable, posLabel = "Tutorial")
    }
}
