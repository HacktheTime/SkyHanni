package at.hannibal2.skyhanni.features.tutorial.gui.suggest

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text

/**
 * Minimal suggestion dropdown implementation used by tutorial editors.
 */
object SuggestionDropdown {
    fun below(textInput: TextInput, optionsProvider: () -> List<String> = { emptyList() }): Renderable {
        val opts = optionsProvider().take(10)
        if (opts.isEmpty()) return Renderable.text("")
        val items = opts.map { opt ->
            Renderable.clickable(Renderable.text("§7- §f$opt"), onLeftClick = { textInput.textBox = opt }, bypassChecks = true)
        }
        return Renderable.vertical(items, spacing = 2)
    }

    // Convenience overload when callers omit the optionsProvider named parameter
    fun below(textInput: TextInput): Renderable = below(textInput, { emptyList() })
}
