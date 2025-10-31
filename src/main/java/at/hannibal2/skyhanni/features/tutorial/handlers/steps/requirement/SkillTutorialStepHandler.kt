package at.hannibal2.skyhanni.features.tutorial.handlers.steps.requirement

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialStepLogic
import de.hype.bingonet.shared.tutorials.steps.requirement.SkillTutorialStep

class SkillTutorialStepHandler : TutorialStepHandler<SkillTutorialStep> {
    
    override fun getStepName(step: SkillTutorialStep, tutorial: Tutorial): String {
        return "Reach ${step.skill.displayName} level ${step.level}"
    }
    
    override fun getStepDescription(step: SkillTutorialStep, tutorial: Tutorial): String? {
        return null
    }
    
    override fun getRequirements(step: SkillTutorialStep): List<TutorialNode> {
        return emptyList()
    }
    
    override fun check(step: SkillTutorialStep, tutorial: Tutorial): Boolean {
        val currentLevel = ProfileStorageData.profileSpecific?.skillData?.get(step.skill) ?: 0
        return currentLevel >= step.level
    }
    
    override fun onActivate(step: SkillTutorialStep, tutorial: Tutorial) {
        if (check(step, tutorial)) {
            TutorialStepLogic.complete(step)
        }
        activeSteps.add(step)
    }
    
    override fun onDeactivate(step: SkillTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    override fun onReset(step: SkillTutorialStep, tutorial: Tutorial) {
        activeSteps.remove(step)
    }
    
    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        activeSteps.forEach { step ->
            if (!ignoreEvent(step) && check(step, Tutorial.DUMMY)) {
                TutorialStepLogic.complete(step)
            }
        }
    }
    
    companion object {
        private val activeSteps = mutableSetOf<SkillTutorialStep>()
    }
}
