package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DiscordBotConfig {


    @Expose
    @ConfigOption(name = "Main Toggle", desc = "Main Toggle for the Discord Bot.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enable: Boolean = false

    @Expose
    @ConfigOption(name = "Bot Token", desc = "Token of your Discord Bot.")
    @ConfigEditorText
    var botToken: String = ""

    @Expose
    @ConfigOption(name = "Client Secret", desc = "Client Secret of your Discord Application.")
    @ConfigEditorText
    var clientSecret: String = ""


    @Expose
    @Accordion
    @ConfigOption(name = "Ping Config", desc = "Configure in which channels you want to receive pings")
    val chatConfig: ChatConfig = ChatConfig()


    class ChatConfig {
        var useDiscordAnsi = true
        val nickNames = mutableSetOf<String>()
        val groupTimeSeconds = 30
        val coop = PingBehaviour.SILENT_APPEND
        val party = PingBehaviour.SILENT_APPEND
        val guild = PingBehaviour.SILENT_APPEND
        val msg = PingBehaviour.IMMEDIATE
        val allChat = PingBehaviour.SILENT_APPEND
        val server = PingBehaviour.SILENT_APPEND

        enum class PingBehaviour {
            NOTHING,
            SILENT_APPEND,
            IMMEDIATE
        }
    }
}
