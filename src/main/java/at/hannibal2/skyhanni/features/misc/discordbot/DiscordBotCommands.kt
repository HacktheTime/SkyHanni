package at.hannibal2.skyhanni.features.misc.discordbot

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object DiscordBotCommands {

    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shdiscordbotsetup") {
            description = "Open Discord Bot setup GUI"
            category = CommandCategory.MAIN
            simpleCallback {
                SkyHanniMod.screenToOpen = DiscordBotSetupScreen()
            }
        }
    }
}
