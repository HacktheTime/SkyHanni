package de.hype.bingonet.shared.tutorials.steps

import at.hannibal2.skyhanni.data.ProfileStorageData

class MinionTutorialStep(val slots: Int) : TutorialStep(

) {
    //TODO following things
    // Add automatic tips for minion slots if not enough yet
    // Add detection for which minions need to be replaced etc.
    // like essentially apply a exact minion configuration with mismatch detection
    override fun getStepName(): String {
        TODO("Not yet implemented")
    }

    override fun getStepDescription(): String? {
        TODO("Not yet implemented")
    }

    fun check() {
        val count = ProfileStorageData.profileSpecific?.minions?.size ?: 0

    }
}
