package de.hype.bingonet.shared.tutorials.paths

import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.misc.AwaitGodSplashTutorialStep
import kotlin.time.Duration

class AsyncTutorialFork(
    val pathNodes : List<TutorialNode>
)  : TutorialFork() {
    override fun getNodes(tutorial: Tutorial): List<TutorialNode> {
        return pathNodes
    }

    override fun isAsync(): Boolean = true

    override fun isComplete(tutorial: Tutorial): Boolean {
        return pathNodes.all { it.isComplete() }
    }

    companion object{
        /**
         * Helper method to create an fork that requires a god splash effect.
         */
        fun awaitingSplash(minimumDuration: Duration,nodes: List<TutorialNode>){
            AsyncTutorialFork(listOf(AwaitGodSplashTutorialStep(minimumDuration))+nodes)
        }
    }

    override fun reset() {
        pathNodes.forEach { it.reset() }
    }
}
