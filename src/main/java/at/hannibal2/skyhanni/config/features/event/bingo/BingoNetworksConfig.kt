package at.hannibal2.skyhanni.config.features.event.bingo

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.ThirdParty
import at.hannibal2.skyhanni.config.ThirdPartyDependency
import at.hannibal2.skyhanni.config.core.config.KeyBind
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import kotlin.reflect.KMutableProperty1

class BingoNetworksConfig {
    @Expose
    @ConfigOption(
        name = "Bingo Net",
        desc = "Bingo Net connection Settings",
    )
    @ConfigEditorBoolean
    @Accordion
    val bingoNet: BingoNetConfig = BingoNetConfig()

    @Expose
    @ConfigOption(
        name = "Use Bingo Brewers",
        desc = "Connects SkyHanni to the Bingo Brewers Network.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    @ThirdPartyDependency(ThirdParty.BINGO_BREWERS)
    var useBB: Boolean = false

    @Expose
    @ConfigOption(
        name = "Use Bingo Splash Community",
        desc = "Connects SkyHanni to the Bingo Splash Community Splash announcement server",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    @ThirdPartyDependency(ThirdParty.BINGO_SPLASH_COMMUNITY)
    var useBSC : Boolean = false


    // TODO requires restart rn still so fix somehow?

    @Expose
    @ConfigOption(name = "Show Splashes", desc = "Show Splashes announcements")
    @ConfigEditorBoolean
    @FeatureToggle
    @FeatureDependencyRequirement("BingoNetConfig#useBN")
    @FeatureDependencyRequirement("#useBB")
    @FeatureDependencyRequirement("#useBSC")
    var showSplashes: Boolean = true

    @Expose
    @ConfigOption(name = "Highlight Splash Hub", desc = "Highlight the Splash Hubs in the Hub Selector.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("BingoNetConfig#useBN")
    @FeatureDependencyRequirement("#useBB")
    @FeatureDependencyRequirement("#useBSC")
    @FeatureToggle
    var highlightSplashHub: Boolean = true

    @Expose
    @ConfigOption(name = "Show ChChests", desc = "Subscribe to ChChests.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("BingoNetConfig#useBN")
    @FeatureDependencyRequirement("#useBB")
    @FeatureToggle
    var chestWaypoints: Boolean = true

    @Expose
    @ConfigOption(
        name = "Allow Server Invite",
        desc = "Allows the BingoNet Server to Manage your parties. This is required for some Features.",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("BingoNetConfig#useBN")
    @FeatureToggle
    var allowBNServerPartyManagement: Boolean = true

    @Expose
    @ConfigOption(name = "Show Bingo Chat", desc = "Bingo Chat is a Chat for every Bingo Net participant.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("BingoNetConfig#useBN")
    @FeatureToggle
    var showBingoChat: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show Goal Completions",
        desc = "Shows a Message in the Chat when someone completes a Goal. (and Bingo Net knows)",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("BingoNetConfig#useBN")
    var showGoalCompletions: Boolean = false

    @Expose
    @ConfigOption(
        name = "Show Card Completions",
        desc = "Shows a Message in the Chat when someone completes a Bingo Card. (and Bingo Net knows)",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("BingoNetConfig#useBN")
    var showCardCompletions: Boolean = false

    @Expose
    @ConfigOption(name = "Show Packet Traffic (Debug)", desc = "Show incoming and outgoing Packets in the Chat.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("BingoNetConfig#useBN")
    @FeatureDependencyRequirement("#useBB")
    var showPacketTraffic = false


    // TODO hide unless you have splasher perm?
    @Expose
    @ConfigOption(name = "§dSplasher Config", desc = "Only Important if you are a Splasher.")
    @Accordion
    val splasherConfig: SplasherConfig = SplasherConfig()

    @FeatureToggle
    @Expose
    @ConfigOption(
        name = "Show Splash Status Updates",
        desc = "Will inform you about Splash Status Updates in the Chat.",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("BingoNetConfig#useBN")
    @FeatureDependencyRequirement("#showSplashes")
    var showSplashStatusUpdates: Boolean = true

    @Expose
    @ConfigOption(
        name = "Splash Multipurpose Keybind",
        desc = "Used to trigger Server Warp and if in Hub Selector to warp to the right splash automatically.",
    )
    @Accordion
    @FeatureDependencyRequirement("#showSplashes")
    var splashHubWarp: KeyBind = KeyBind()

    @Expose
    @FeatureToggle
    @ConfigOption(
        name = "Show Private Splashes",
        desc = "Show Splashes that require you to join a party to be warped in.",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#showSplashes")
    var showPrivateSplashes: Boolean = true

    @Expose
    @ConfigOption(
        name = "Server Action Chat Prompt Key",
        desc = "Shown when a Bingo Network server wants to receive an acknowledgement. NOT USED FOR PARTY COMMANDS",
    )
    @Accordion
    @FeatureDependencyRequirement("BingoNetConfig#useBN")
    val serverActionChatPrompt = KeyBind()


    @Expose
    @ConfigOption(name = "Ch Chest Items Config", desc = "Configure the Chat Prompt Key and which items your are interested in.")
    @Accordion
    @FeatureDependencyRequirement("BingoNetConfig#useBN")
    @FeatureDependencyRequirement("#useBB")
    val chChestConfig: ChChestConfig = ChChestConfig()

    @Expose
    @ConfigOption(name = "Ch Chest Overlay", desc = "Show an Overlay with the Ch Chest Items in the Lobby.")
    @ConfigEditorBoolean
    @FeatureToggle
    @FeatureDependencyRequirement("BingoNetConfig#useBN")
    @FeatureDependencyRequirement("#useBB")
    var chChestOverlay: Boolean = true

    @Expose
    @ConfigOption(name = "Show Splash Location Waypoint", desc = "Show Path to go to and Location of the Splash.")
    @FeatureToggle
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#showSplashes")
    var renderSplashLocationWaypoint = false
}
