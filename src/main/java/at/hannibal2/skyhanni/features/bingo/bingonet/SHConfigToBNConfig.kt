package at.hannibal2.skyhanni.features.bingo.bingonet

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesRepoManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils

@SkyHanniModule
object SHConfigToBNConfig {
    val bingoNetConfig get() = SkyHanniMod.feature.event.bingo.bingoNetworks.bingoNet

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        if (bingoNetConfig.firstSetup) {
            bingoNetConfig.firstSetup = false
            val wasUsingShBefore = SkyHanniMod.feature.dev.neuRepo.location.user == "NotEnoughUpdates"
            if (wasUsingShBefore) {
                ChatUtils.chat("§cIt seems like you have been using SH Previously. Due to some incompatibilities some of your config values were updated to match our defaults.")
                SkyHanniMod.feature.dev.neuRepo.location.user = "HacktheTime"
                ChatUtils.chat("§cUpdating NEU Repo due to change in repository user.")
                SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "Bingo Net Default values set")
                EnoughUpdatesRepoManager.updateRepo("SH to BN config migration", true)
            }

            BNFirstSetupConsentScreen.openScreen()
        }
    }
}
