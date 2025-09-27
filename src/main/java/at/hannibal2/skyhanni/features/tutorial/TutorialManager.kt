package at.hannibal2.skyhanni.features.tutorial

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import de.hype.bingonet.shared.tutorials.Tutorial
import de.hype.bingonet.shared.tutorials.steps.TutorialStep
import at.hannibal2.skyhanni.utils.json.BaseGsonBuilder
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.SkyHanniMod

/**
 * TutorialManager: holds the in-memory active tutorial and provides JSON import/export
 * and persistence helpers to save/load into profile storage.
 */

class TutorialManager {
    var activeTutorial: Tutorial? = null

    fun exportActiveTutorialJson(): String? {
        val t = activeTutorial ?: return null
        return try {
            BaseGsonBuilder.gson().create().toJson(t)
        } catch (e: Throwable) {
            ChatUtils.chat("§cCould not export tutorial: ${e.message}")
            null
        }
    }

    fun importTutorialFromJson(json: String): Boolean {
        return try {
            val gson = BaseGsonBuilder.gson().create()
            val parsed = gson.fromJson(json, Tutorial::class.java)
            activeTutorial = parsed
            parsed.onLoad()
            // persist into profile storage
            ProfileStorageData.profileSpecific?.tutorialManager?.activeTutorial = parsed
            SkyHanniMod.launchCoroutine("Save Tutorial") { SkyHanniMod.configManager.saveConfig(ConfigFileType.STORAGE, "import-tutorial") }
            ChatUtils.chat("§aImported tutorial successfully")
            true
        } catch (e: Throwable) {
            ChatUtils.chat("§cFailed to parse tutorial JSON: ${e.message}")
            false
        }
    }


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
