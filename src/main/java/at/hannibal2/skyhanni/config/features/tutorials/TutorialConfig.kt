package at.hannibal2.skyhanni.config.features.tutorials

import at.hannibal2.skyhanni.config.core.config.KeyBind
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard


class TutorialConfig {

    enum class AutoUseMode {
        ALWAYS,
        NEVER,
        AUTO
    }

    @Expose
    @ConfigOption(
        name = "Assume Grand Carrier",
        desc = "Whether you have a friend that carries you the Grands. Means you do not need to obtain them yourself.",
    )
    @ConfigEditorBoolean
    var assumeGrandCarrier = false

    @Expose
    @ConfigOption(
        name = "Assume Max EChest",
        desc = "Max EChest means you have a lot of accessible Space in your EChest. " +
            "This can be used with Tutorials to route you better but require more Storage usage.",
    )
    @ConfigEditorBoolean
    var assumeMaxEChest = false

    @Expose
    @ConfigOption(
        name = "Protect Resources Across Paths",
        desc = "Mark Resources to keep across paths EVEN if you selected the other path in an optional tutorial step.",
    )
    @ConfigEditorBoolean
    var protectResourcesAcrossPaths = true

    @Expose
    @ConfigOption(
        name = "Protect Required Items From Selling",
        desc = "Prevents selling items required by the tutorial unless holding the modifier key.",
    )
    @ConfigEditorBoolean
    var tutorialProtectRequiredItems: Boolean = true

    @Expose
    @ConfigOption(name = "Group Purchase Automatically", desc = "When buying from NPC, purchase for the whole tutorial if possible.")
    @ConfigEditorBoolean
    var tutorialGroupPurchaseAutomatic: Boolean = true

    @Expose
    @ConfigOption(name = "NPC Usage Automatic", desc = "Automatically prefer NPC/trade sources when available.")
    @ConfigEditorBoolean
    var tutorialNpcUseAutomatic: Boolean = true

    @Expose
    @ConfigOption(name = "NPC Usage Mode", desc = "Always, Never, or Automatic usage of NPC/trade sources when planning obtain steps.")
    @ConfigEditorDropdown
    var tutorialNpcUseMode: AutoUseMode = AutoUseMode.AUTO

    @Expose
    @ConfigOption(
        name = "Grind Tolerance Percent",
        desc = "If grinding extra is less than this percent, allow it instead of waiting for NPC limits.",
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
    var tutorialGrindTolerancePercent: Int = 5

    @Expose
    @ConfigOption(
        name = "Planned Progress Percent",
        desc = "Plan purchases for only this percent of the full tutorial (e.g., 50 to plan for half).",
    )
    @ConfigEditorSlider(minValue = 1f, maxValue = 100f, minStep = 1f)
    var tutorialPlannedProgressPercent: Int = 100

    @Expose
    @ConfigOption(name = "Auto-Add Required Items", desc = "Automatically compute and protect required items across the tutorial.")
    @ConfigEditorBoolean
    var tutorialAutoAddRequiredItems: Boolean = true

    @Expose
    @ConfigOption(
        name = "Auto-Add Required Items Mode",
        desc = "Always, Never, or Automatic add required items to protections and planning.",
    )
    @ConfigEditorDropdown
    var tutorialAutoAddRequiredItemsMode: AutoUseMode = AutoUseMode.AUTO

    @Expose
    @ConfigOption(name = "Show Compaction Hints", desc = "Show hints like 'you can compact lapis now' when conditions are met.")
    @ConfigEditorBoolean
    var showCompactionHints: Boolean = true

    @Expose
    @ConfigOption(
        name = "NPC Buy Hints Only When Way Null",
        desc = "Only show NPC buy hints when the obtain way is not specified (null) in the recipe plan.",
    )
    @ConfigEditorBoolean
    var npcBuyHintsOnlyWhenObtainWayNull: Boolean = true

    @Expose
    @ConfigOption(name = "Tutorial Overlay", desc = "Shows the current tutorial steps in a draggable overlay.")
    @ConfigEditorBoolean
    var overlayEnabled: Boolean = true

    @Expose
    @ConfigLink(owner = TutorialConfig::class, field = "overlayEnabled")
    val overlayPos: Position = Position(250, 120)

    @Expose
    @ConfigOption(name = "Show Step Descriptions", desc = "Show descriptions under each step in the overlay.")
    @ConfigEditorBoolean
    var showStepDescriptions: Boolean = true

    @Expose
    @ConfigOption(name = "Show Hidden Optional Paths", desc = "Also show optional paths marked as hidden.")
    @ConfigEditorBoolean
    var showHiddenOptionalPaths: Boolean = false

    @Expose
    @ConfigOption(name = "Tutorials Chat Prompt Key", desc = "Chat Prompt key used for Tutorials.")
    @Accordion
    val chatPromptKey: KeyBind = KeyBind()

    @Expose
    @ConfigOption(name = "Open Tutorial GUI Key", desc = "Keybind to open the Tutorial Manager GUI.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    val openGuiKey: Int = Keyboard.KEY_NONE

}
