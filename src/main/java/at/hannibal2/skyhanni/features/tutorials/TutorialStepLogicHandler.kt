package at.hannibal2.skyhanni.features.tutorials

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi.completed
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.paths.TutorialFork
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

abstract class TutorialNodeLogicHandler<Step : TutorialNode> {

    abstract fun refresh(step: Step)

    fun complete(step: Step) {
        //TODO complete logic
        onComplete(step)
    }

    fun reset(step: Step) {
        //TODO reset logic
        onReset(step)
    }

    protected abstract fun onRefresh(step: Step, completed: Boolean)

    fun activate(step: Step) {
        if (activeCheck(step)) {
            complete(step)
            return
        }
        onActivate(step)
    }

    protected open fun onActivate(step: Step) {

    }

    protected open fun onComplete(step: Step) {}

    protected open fun onReset(step: Step) {}
}

abstract class TutorialStepLogicHandler<Step : TutorialStep, Event : SkyHanniEvent> : TutorialNodeLogicHandler<Step>() {
    abstract fun activeCheck(step: Step): Boolean

    abstract fun passiveCheck(step: Step, event: Event): Boolean

    final fun refresh(step: Step) {
        val completed = activeCheck(step)
        if (completed) complete(step)
        onRefresh(step, completed)
    }

    final fun complete(step: Step) {
        //TODO complete logic
        onComplete(step)
    }

    final fun reset(step: Step) {
        //TODO reset logic
        onReset(step)
    }

    abstract fun onRefresh(step: Step, completed: Boolean)

    final fun activate(step: Step) {
        if (activeCheck(step)) {
            complete(step)
            return
        }
        onActivate(step)
    }

    fun onActivate(step: Step) {

    }

    fun onComplete(step: Step) {}

    fun onReset(step: Step) {}
}

abstract class TutorialForkLogicHandler<Fork : TutorialFork> {
    abstract fun getNodes(fork: Fork): List<TutorialNode>
    abstract fun getAllInternalNodes(fork: Fork): List<TutorialNode>
    abstract fun isBlocking(fork: Fork): Boolean

    fun refresh(fork: Fork) {
        getNodes(fork).forEach {
            it.refresh()
        }
                if (completed) complete(fork)
        onRefresh(fork, completed)
    }

    fun complete(fork: Fork) {
        //TODO complete logic
        onComplete(fork)
    }

    fun reset(fork: Fork) {
        //TODO reset logic
        onReset(fork)
    }

    protected abstract fun onRefresh(fork: Fork, completed: Boolean)

    fun activate(fork: Fork) {
        if (activeCheck(fork)) {
            complete(fork)
            return
        }
        onActivate(fork)
    }

    protected open fun onActivate(fork: Fork) {}

    protected open fun onComplete(fork: Fork) {}

    protected open fun onReset(fork: Fork) {}
}
