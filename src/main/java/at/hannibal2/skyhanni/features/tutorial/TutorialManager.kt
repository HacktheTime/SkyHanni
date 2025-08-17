package at.hannibal2.skyhanni.features.tutorial

import de.hype.bingonet.shared.tutorials.paths.SelectPathTutorialFork
import de.hype.bingonet.shared.tutorials.steps.TutorialStep

object TutorialManager {
    fun getActiveStep() : TutorialStep{}
    //TODO add asynchronous support like waiting for mining events, chchest findings etc.
    //TODO add alternative paths

    //TODO make store and retrieve step
    //TODO add a tag item step to allow for reference later

    //TODO ask user for a routing option for example maybe ask do you want to go mage or melee etc.
}
