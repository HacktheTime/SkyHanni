package at.hannibal2.skyhanni.features.tutorial.handlers.forks

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialForkHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import at.hannibal2.skyhanni.features.tutorial.logic.TutorialNodeLogic
import de.hype.bingonet.shared.tutorials.paths.SelectPathTutorialFork

class SelectPathTutorialForkHandler : TutorialForkHandler<SelectPathTutorialFork> {
    
    override fun getNodes(fork: SelectPathTutorialFork, tutorial: Tutorial): List<TutorialNode> {
        val selectedOption = tutorial.getSelectedOption(fork.paths) ?: return emptyList()
        return selectedOption.pathNodes
    }
    
    override fun isAsync(fork: SelectPathTutorialFork): Boolean = false
    
    override fun getAllInternalNodes(fork: SelectPathTutorialFork): List<TutorialNode> {
        return fork.paths.flatMap { it.pathNodes }
    }
    
    override fun getHeader(fork: SelectPathTutorialFork, tutorial: Tutorial): String {
        return "Select a Path"
    }
    
    override fun isComplete(fork: SelectPathTutorialFork, tutorial: Tutorial): Boolean {
        return fork.paths.any { it.pathNodes.all { node -> TutorialNodeLogic.isComplete(node, tutorial) } }
    }
}
