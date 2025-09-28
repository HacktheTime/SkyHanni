package de.hype.bingonet.shared.tutorials.steps.requirement

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.features.skillprogress.SkillType.Companion.toSh
import de.hype.bingonet.shared.constants.Skills
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class SkillTutorialStep(
    val skill: Skills,
    val level: Int,
) : TutorialStep() {

    override fun getStepName(tutorial: Tutorial): String {
        return "Reach level $level in ${skill.displayName}."
    }

    override fun getStepDescription(tutorial: Tutorial): String? = null

    override fun getRequirements(): List<TutorialNode> = emptyList()

    override fun isComplete(tutorial: Tutorial): Boolean = matchingCondition()


    fun matchingCondition(): Boolean {
        return (ProfileStorageData.profileSpecific?.skillData?.get(skill.toSh())?.level ?: 0) >= level
    }
}
