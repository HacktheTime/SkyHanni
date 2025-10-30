package at.hannibal2.skyhanni.features.tutorial.handlers.forks

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialForkHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.TutorialNodeLogic
import de.hype.bingonet.shared.tutorials.paths.AsyncTutorialFork

class AsyncTutorialForkHandler : TutorialForkHandler<AsyncTutorialFork> {
    
    override fun getNodes(fork: AsyncTutorialFork, tutorial: Tutorial): List<TutorialNode> {
        return fork.pathNodes
    }
    
    override fun isAsync(fork: AsyncTutorialFork): Boolean = true
    
    override fun getAllInternalNodes(fork: AsyncTutorialFork): List<TutorialNode> {
        return fork.pathNodes
    }
    
    override fun getHeader(fork: AsyncTutorialFork, tutorial: Tutorial): String {
        return "Required at some point"
    }
    
    override fun isComplete(fork: AsyncTutorialFork, tutorial: Tutorial): Boolean {
        return fork.pathNodes.all { TutorialNodeLogic.isComplete(it, tutorial) }
    }
}
