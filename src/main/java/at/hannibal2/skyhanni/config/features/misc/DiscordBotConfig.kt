package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
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

        @Expose
        @ConfigOption(name = "Send received Chat Messages to Discord while afk", desc = "Sends you the chat messages you receive while " +
            "you are afk.")
        @ConfigEditorBoolean
        @FeatureToggle
        var sendMessages = true
        @Expose
        @ConfigOption(name = "Use Discord ANSI", desc = "Use Discord ANSI codes for colors and formatting.")
        @ConfigEditorBoolean
        var useDiscordAnsi = true
        @Expose
        @ConfigOption(name = "Nicknames to Ping", desc = "Nicknames of users to ping in the format. separate with ;")
        @ConfigEditorText
        val nickNames = ""
        @Expose
        @ConfigOption(name = "Group Time Seconds", desc = "The Bot will only send you messages every X seconds unless a important message" +
            " is detected.")
        @ConfigEditorSlider(minValue = 10f, maxValue = 300f, minStep = 5f)
        val groupTimeSeconds = 30
        @Expose
        @ConfigOption(name = "Coop Channel Ping Behaviour", desc = "Behaviour for Coop Chat Messages.")
        @ConfigEditorDropdown
        val coop = PingBehaviour.SILENT_APPEND
        @Expose
        @ConfigOption(name = "Party Channel Ping Behaviour", desc = "Behaviour for Party Chat Messages.")
        @ConfigEditorDropdown
        val party = PingBehaviour.SILENT_APPEND
        @Expose
        @ConfigOption(name = "Guild Channel Ping Behaviour", desc = "Behaviour for Guild Chat Messages.")
        @ConfigEditorDropdown
        val guild = PingBehaviour.SILENT_APPEND
        @Expose
        @ConfigOption(name = "Msg Channel Ping Behaviour", desc = "Behaviour for Msg Messages.")
        @ConfigEditorDropdown
        val msg = PingBehaviour.IMMEDIATE
        @Expose
        @ConfigOption(name = "All Chat Channel Ping Behaviour", desc = "Behaviour for All Chat Messages.")
        @ConfigEditorDropdown
        val allChat = PingBehaviour.SILENT_APPEND
        @Expose
        @ConfigOption(name = "Server Channel Ping Behaviour", desc = "Behaviour for Server/Game Messages.")
        @ConfigEditorDropdown
        val server = PingBehaviour.SILENT_APPEND
    }
}
enum class PingBehaviour {
    NOTHING,
    SILENT_APPEND,
    IMMEDIATE
}
