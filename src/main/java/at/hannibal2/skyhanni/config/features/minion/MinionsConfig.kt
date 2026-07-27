package at.hannibal2.skyhanni.config.features.minion

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

class MinionsConfig {
    @Expose
    @ConfigOption(name = "Name Display", desc = "Show the minion name and tier over the minion.")
    @ConfigEditorBoolean
    @FeatureToggle
    var nameDisplay: Boolean = true

    @Expose
    @ConfigOption(name = "Only Tier", desc = "Show only the tier number over the minion. (Useful for Bingo)")
    @ConfigEditorBoolean
    var nameOnlyTier: Boolean = false

    // TODO rename minionConfigHelper to minionUpgradeHelper
    @Expose
    @ConfigOption(
        name = "Minion Upgrade Helper",
        desc = "Add a button in the Minion menu to obtain required items for the next upgrade from Sacks or Bazaar.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var minionConfigHelper: Boolean = true

    @Expose
    @ConfigOption(name = "Last Clicked", desc = "")
    @Accordion
    val lastClickedMinion: LastClickedMinionConfig = LastClickedMinionConfig()

    @Expose
    @ConfigOption(name = "Emptied Time", desc = "")
    @Accordion
    val emptiedTime: EmptiedTimeConfig = EmptiedTimeConfig()

    @Expose
    @ConfigOption(
        name = "Hopper Profit Display",
        desc = "Use the hopper's held coins and the last empty time to calculate the coins per day.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hopperProfitDisplay: Boolean = true

    @Expose
    @ConfigOption(name = "Show XP", desc = "Show how much skill experience you will get when picking up items from the minion storage.")
    @ConfigEditorBoolean
    @FeatureToggle
    var xpDisplay: Boolean = true

    @Expose
    @ConfigLink(owner = MinionsConfig::class, field = "hopperProfitDisplay")
    val hopperProfitPos: Position = Position(360, 90)

    @Expose
    @ConfigOption(name = "Hide Mob Nametag", desc = "Hide the nametags of mobs close to minions.")
    @ConfigEditorBoolean
    @FeatureToggle
    var hideMobsNametagNearby: Boolean = false

    @Expose
    @ConfigOption(name = "Inferno Fuel Blocker", desc = "Prevent picking up the fuel or minion while there is active fuel.")
    @ConfigEditorBoolean
    @FeatureToggle
    var infernoFuelBlocker: Boolean = false

    @Expose
    @ConfigOption(
        name = "Open Minion Recipe Hotkey",
        desc = "If you are holding a resource that can be used to craft a minion," +
            " It will open the recipe for you. It is planned for it to work on hover but as of now it is only" +
            " working for the currently held item (selected hotbar slot)",
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_R)
    var openMinionRecipeForHeldResource: Int = GLFW.GLFW_KEY_R

    @Expose
    @ConfigOption(
        name = "Open Minion Recipe Tier",
        desc = "The Tier of the minion that gets opened. 1 for example for Tier 1.",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 11f, minStep = 1f)
    var openMinionRecipeForHeldResourceTier: Int = 5
    @Expose
    @ConfigOption(
        name = "Search Minion Recipe Hotkey",
        desc = "Searches the recipies for this minion by name to easily find the overview.",
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_Z)
    var searchGenericMinionRecipesForHeldResource: Int =GLFW.GLFW_KEY_Z
    @Expose
    @ConfigOption(name = "Inferno Minion Profit Tracker", desc = "")
    @Accordion
    val infernoProfitTracker: InfernoProfitTrackerConfig = InfernoProfitTrackerConfig()
}
