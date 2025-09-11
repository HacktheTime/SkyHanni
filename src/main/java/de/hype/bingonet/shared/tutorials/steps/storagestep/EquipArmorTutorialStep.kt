package de.hype.bingonet.shared.tutorials.steps.storagestep

import de.hype.bingonet.shared.tutorials.TaggedItemCheck
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class EquipArmorTutorialStep(
    val helmet: TaggedItemCheck,
    val chestplate: TaggedItemCheck,
    val leggings: TaggedItemCheck,
    val boots: TaggedItemCheck,
    val necklace: TaggedItemCheck,
    val cloak: TaggedItemCheck,
    val belt : TaggedItemCheck,
    val gloves : TaggedItemCheck,
) : TutorialStep()
