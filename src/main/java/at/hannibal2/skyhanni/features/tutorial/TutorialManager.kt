package at.hannibal2.skyhanni.features.tutorial

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.steps.TutorialStep


class TutorialManager {
    var activeTutorial: Tutorial? = null


    @SkyHanniModule
    companion object{

        val activeTutorial: Tutorial? get() {
            return ProfileStorageData.profileSpecific?.tutorialManager?.activeTutorial
        }

        fun getActiveSteps(): List<TutorialStep>? {
            return activeTutorial?.getActiveSteps()
        }

        @HandleEvent
        fun onProfileJoin(event: ProfileJoinEvent) {

        }
    }

    //TODO make store and retrieve step
    //TODO add a tag item step to allow for reference later

    //TODO ask user for a routing option for example maybe ask do you want to go mage or melee etc.
}
