package at.hannibal2.skyhanni.features.tutorial.handlers.forks

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialForkHandler
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialConditionLogic
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialStepLogic
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.paths.WhileTutorialNode

class WhileTutorialNodeHandler : TutorialForkHandler<WhileTutorialNode> {
    
    override fun getNodes(fork: WhileTutorialNode, tutorial: Tutorial): List<TutorialNode> {
        return fork.nodes
    }
    
    override fun isAsync(fork: WhileTutorialNode): Boolean = false
    
    override fun getAllInternalNodes(fork: WhileTutorialNode): List<TutorialNode> {
        return fork.nodes
    }
    
    override fun getHeader(fork: WhileTutorialNode, tutorial: Tutorial): String {
        return "Do While ${fork.conditionExplenation}"
    }
    
    override fun isComplete(fork: WhileTutorialNode, tutorial: Tutorial): Boolean {
        val done = TutorialConditionLogic.matches(fork.condition, tutorial)
        if (done) {
            fork.nodes.forEach { TutorialStepLogic.complete(it) }
        } else {
            val lastDone = fork.nodes.last().completed
            if (lastDone) fork.nodes.forEach { TutorialStepLogic.reset(it, tutorial) }
        }
        return done
    }
}
