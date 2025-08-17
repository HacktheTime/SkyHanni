package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.misc.AwaitPotionEffectTutorialStep

class AsyncTutorialFork(
    val pathNodes : List<TutorialNode>
)  : TutorialFork() {
    override fun getNodes(tutorial: Tutorial): List<TutorialNode> {
        return pathNodes
    }

    override fun isAsync(): Boolean = true

    override fun isComplete(): Boolean {
        return pathNodes.all { it.isComplete() }
    }

    companion object{
        /**
         * Helper method to create an fork that requires a god splash effect.
         */
        fun awaitingSplash(nodes: List<TutorialNode>){
            AsyncTutorialFork(AwaitPotionEffectTutorialStep()+nodes)
        }
    }
}
