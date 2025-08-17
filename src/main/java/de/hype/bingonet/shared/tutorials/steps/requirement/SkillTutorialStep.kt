package de.hype.bingonet.shared.tutorials.steps.requirement

import at.hannibal2.skyhanni.data.ProfileStorageData
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class SkillTutorialStep(
    val skill: Skill,
    val level: Int
) : TutorialStep(){

    override fun onActivate() {
        super.onActivate()
    }

    fun matchingCondition(){
        ProfileStorageData.profileSpecific.skillData.get()
    }

}
