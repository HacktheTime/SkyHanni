package de.hype.bingonet.shared.tutorials.steps

import at.hannibal2.skyhanni.utils.InventoryDetector
import java.util.regex.Pattern

abstract class GUIBasedTutorialStep(val guiName: Pattern) : TutorialStep() {
    val inventory = InventoryDetector(
        pattern = guiName,
        onOpenInventory = {},
        onCloseInventory = {},
    )

    override fun ignoreEvent(): Boolean {
        return super.ignoreEvent() || !inventory.isInside()
    }
}
