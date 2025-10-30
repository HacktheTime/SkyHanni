package at.hannibal2.skyhanni.features.tutorial.handlers.steps

import at.hannibal2.skyhanni.api.CollectionApi
import at.hannibal2.skyhanni.features.tutorial.handlers.TutorialStepHandler
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.CollectionTutorialStep

class CollectionTutorialStepHandler : TutorialStepHandler<CollectionTutorialStep> {
    
    override fun getStepName(step: CollectionTutorialStep, tutorial: Tutorial): String {
        return "Obtain ${step.amount} ${step.collection.displayName}"
    }
    
    override fun getStepDescription(step: CollectionTutorialStep, tutorial: Tutorial): String? {
        return null
    }
    
    override fun getRequirements(step: CollectionTutorialStep): List<TutorialNode> {
        return emptyList()
    }
    
    override fun onActivate(step: CollectionTutorialStep, tutorial: Tutorial) {
        CollectionApi.startTracking(step.collection, step.amount.toLong())
    }
    
    override fun onDeactivate(step: CollectionTutorialStep, tutorial: Tutorial) {
        CollectionApi.stopTracking()
    }
    
    override fun onReset(step: CollectionTutorialStep, tutorial: Tutorial) {
        CollectionApi.stopTracking()
    }
    
    override fun check(step: CollectionTutorialStep, tutorial: Tutorial): Boolean {
        val current = CollectionApi.getCollectionCounter(step.collection) ?: 0
        return current >= step.amount
    }
}
