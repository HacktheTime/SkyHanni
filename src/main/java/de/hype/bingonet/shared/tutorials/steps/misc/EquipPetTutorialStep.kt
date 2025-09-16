package de.hype.bingonet.shared.tutorials.steps.misc

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.PetChangeEvent
import at.hannibal2.skyhanni.utils.toSh
import de.hype.bingonet.environment.toInternalName
import de.hype.bingonet.shared.constants.Rarity
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import de.hype.bingonet.sharedcompilation.sbenums.BNNEUItem

class EquipPetTutorialStep(
    val petType: BNNEUItem,
    val minimumPetRarity: Rarity,
) : TutorialStep() {
    override fun getStepName(tutorial: Tutorial): String {
        val pet = fetchPet() ?: return ("No pet of type ${petType.internalName} and rarity ${minimumPetRarity.name} found!")
        return "Equip your ${pet.coloredName} Pet."
    }

    override fun getStepDescription(tutorial: Tutorial): String? {
        return null
    }

    override fun getRequirements(): List<TutorialNode> = emptyList()

    fun fetchPet(): PetData? {
        val validPets = ProfileStorageData.petProfiles?.pets?.filter {
            it.rarity == minimumPetRarity.toSh() && it.fauxInternalName == petType.toInternalName()
        }
        val best = validPets?.maxBy { it.level }
        return best
    }

    @HandleEvent
    fun onPetChange(event: PetChangeEvent) {
        val desired = fetchPet()?.uuid ?: return
        if (desired == event.newPet?.uuid) complete()
    }
}
