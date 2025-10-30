package at.hannibal2.skyhanni.features.tutorial.handlers.steps.basic

import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.*

class CollectionHandler : TutorialStepHandler<CollectionCollection> {
    override fun getStepName(step: CollectionCollection, tutorial: Tutorial): String = "TODO"
    override fun getStepDescription(step: CollectionCollection, tutorial: Tutorial): String? = null
    override fun getRequirements(step: CollectionCollection): List<TutorialNode> = emptyList()
    
    companion object {
        private val activeSteps = mutableSetOf<CollectionCollection>()
    }
}
