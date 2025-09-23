package de.hype.bingonet.shared.tutorials.steps.location
import de.hype.bingonet.shared.constants.SkyblockInstance
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.TutorialNode
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

class JoinInstanceTutorialStep(
    val instance: SkyblockInstance
) : TutorialStep() {
    override fun getStepName(tutorial: Tutorial): String {
        return "Join a ${instance.displayName}"
    }

    override fun getStepDescription(tutorial: Tutorial): String? {
        return null
    }

    override fun getRequirements(): List<TutorialNode> {
        //TODO adjust to include former catacombs
        return emptyList()
    }
}
