package at.hannibal2.skyhanni.features.tutorial.gui.editor

import at.hannibal2.skyhanni.utils.renderables.Renderable
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode

/**
 * Pluggable editor for a TutorialNode. Implementations keep their own transient UI state
 * (TextInputs, selection caches, etc.) and render the edit form with suggestions.
 */
interface TutorialNodeEditor {
    /** True if this editor can edit the given node type. */
    fun supports(node: TutorialNode): Boolean

    /** Optional header to show in the editor form. */
    fun title(node: TutorialNode): String = "Edit Node"

    /**
     * Build the editor UI. Call [onSave] with a fully constructed replacement node to persist,
     * or [onCancel] to abort.
     */
    fun buildEditor(
        tutorial: Tutorial,
        node: TutorialNode,
        onSave: (TutorialNode) -> Unit,
        onCancel: () -> Unit,
    ): List<Renderable>
}
