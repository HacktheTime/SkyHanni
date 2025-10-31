package at.hannibal2.skyhanni.features.tutorial.logic

import de.hype.bingonet.shared.tutorials.AllOfTutorialCondition
import de.hype.bingonet.shared.tutorials.AndTutorialCondition
import de.hype.bingonet.shared.tutorials.AnyOfTutorialCondition
import de.hype.bingonet.shared.tutorials.NotTutorialCondition
import de.hype.bingonet.shared.tutorials.OrTutorialCondition
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialCondition

/**
 * Logic handler for TutorialCondition operations.
 * Evaluates condition data classes against tutorial state.
 */
object TutorialConditionLogic {

    fun matches(condition: TutorialCondition, tutorial: Tutorial): Boolean {
        return when (condition) {
            is NotTutorialCondition -> !matches(condition.condition, tutorial)
            is AndTutorialCondition -> condition.conditions.all { matches(it, tutorial) }
            is OrTutorialCondition -> condition.conditions.any { matches(it, tutorial) }
            is AllOfTutorialCondition -> condition.conditions.all { TutorialNodeLogic.isComplete(it, tutorial) }
            is AnyOfTutorialCondition -> condition.conditions.any { TutorialNodeLogic.isComplete(it, tutorial) }
            else -> false
        }
    }
}
