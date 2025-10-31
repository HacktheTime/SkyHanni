package at.hannibal2.skyhanni.features.tutorial.handlers.forks

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialForkHandler
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialConditionLogic
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialNodeLogic
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.paths.HiddenOptionalImprovementFork

class HiddenOptionalImprovementForkHandler : TutorialForkHandler<HiddenOptionalImprovementFork> {
    
    override fun getNodes(fork: HiddenOptionalImprovementFork, tutorial: Tutorial): List<TutorialNode> {
        return if (TutorialConditionLogic.matches(fork.condition, tutorial)) {
            fork.improved
        } else {
            fork.default
        }
    }
    
    override fun isAsync(fork: HiddenOptionalImprovementFork): Boolean = false
    
    override fun getAllInternalNodes(fork: HiddenOptionalImprovementFork): List<TutorialNode> {
        return fork.default + fork.improved
    }
    
    override fun getHeader(fork: HiddenOptionalImprovementFork, tutorial: Tutorial): String {
        return "Optional Improvement"
    }
    
    override fun isComplete(fork: HiddenOptionalImprovementFork, tutorial: Tutorial): Boolean {
        return getNodes(fork, tutorial).all { TutorialNodeLogic.isComplete(it, tutorial) }
    }
}
