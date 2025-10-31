package at.hannibal2.skyhanni.features.tutorial.handlers.forks

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialForkHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialNodeLogic
import de.hype.bingonet.shared.tutorials.paths.OptionalTutorialFork

class OptionalTutorialForkHandler : TutorialForkHandler<OptionalTutorialFork> {
    
    override fun getNodes(fork: OptionalTutorialFork, tutorial: Tutorial): List<TutorialNode> {
        return fork.paths.flatMap { it.first }
    }
    
    override fun isAsync(fork: OptionalTutorialFork): Boolean = true
    
    override fun getAllInternalNodes(fork: OptionalTutorialFork): List<TutorialNode> {
        return fork.paths.flatMap { it.first }
    }
    
    override fun getHeader(fork: OptionalTutorialFork, tutorial: Tutorial): String {
        return "Optional Paths"
    }
    
    override fun isComplete(fork: OptionalTutorialFork, tutorial: Tutorial): Boolean {
        return fork.paths.any { (nodes, _) ->
            nodes.all { TutorialNodeLogic.isComplete(it, tutorial) }
        }
    }
}
