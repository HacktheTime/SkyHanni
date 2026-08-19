package at.hannibal2.skyhanni.config.features.fishing.trophyfishing

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.DependencyDelegate
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import org.lwjgl.glfw.GLFW

class TrophyFishDisplayConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a display of all trophy fishes ever caught.")
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @DependencyDelegate(field = "enabled")
    private var isEnabled: Boolean
        get() = enabled.get()
        set(value) { enabled.set(value) }

    @Expose
    @ConfigOption(name = "When Show", desc = "Change when the trophy fish display should be visible in Crimson Isle.")
    @ConfigEditorDropdown
    @FeatureDependencyRequirement("#isEnabled")
    val whenToShow: Property<WhenToShow> = Property.of(WhenToShow.ALWAYS)

    enum class WhenToShow(private val displayName: String) {
        ALWAYS("Always"),
        ONLY_IN_INVENTORY("In inventory"),
        ONLY_WITH_ROD_IN_HAND("Rod in hand"),
        ONLY_WITH_KEYBIND("On keybind"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Keybind", desc = "")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    @FeatureDependencyRequirement("#isEnabled")
    var keybind: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(
        name = "Trophy Fishing Gear",
        desc = "Only show when: wearing 2+ Hunter Armor pieces, full Ember Armor, trophy line on your rod, using hot bait, " +
            "or you're too low level to fish for sea creatures."
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#isEnabled")
    val requireArmor: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Highlight New", desc = "Highlight new trophies green for couple seconds.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#isEnabled")
    val highlightNew: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Extra space", desc = "Space between each line of text.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 10f, minStep = 1f)
    @FeatureDependencyRequirement("#isEnabled")
    val extraSpace: Property<Int> = Property.of(1)

    @Expose
    @ConfigOption(name = "Sorted By", desc = "Sorting type of items in sack.")
    @ConfigEditorDropdown
    @FeatureDependencyRequirement("#isEnabled")
    val sortingType: Property<TrophySorting> = Property.of(TrophySorting.ITEM_RARITY)

    enum class TrophySorting(private val displayName: String) {
        ITEM_RARITY("Item Rarity"),
        TOTAL_AMOUNT("Total Amount"),
        BRONZE_AMOUNT("Bronze Amount"),
        SILVER_AMOUNT("Silver Amount"),
        GOLD_AMOUNT("Gold Amount"),
        DIAMOND_AMOUNT("Diamond Amount"),
        HIGHEST_RARITY("Highest Rarity"),
        NAME("Name Alphabetical"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Reverse Order", desc = "Reverse the sorting order.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#isEnabled")
    val reverseOrder: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Text Order", desc = "Drag text to change the line format.")
    @ConfigEditorDraggableList
    @FeatureDependencyRequirement("#isEnabled")
    val textOrder: Property<MutableList<TextPart>> = Property.of(
        mutableListOf(
            TextPart.NAME,
            TextPart.ICON,
            TextPart.TOTAL,
            TextPart.BRONZE,
            TextPart.SILVER,
            TextPart.GOLD,
            TextPart.DIAMOND,
        ),
    )

    enum class TextPart(private val displayName: String) {
        ICON("Item Icon"),
        NAME("Item Name"),
        BRONZE("Amount Bronze"),
        SILVER("Amount Silver"),
        GOLD("Amount Gold"),
        DIAMOND("Amount Diamond"),
        TOTAL("Amount Total"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Show ✖", desc = "Instead of the number 0, show §c✖ §7if not found.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#isEnabled")
    val showCross: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Show ✔", desc = "Instead of the exact numbers, show §e§l✔ §7if found.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#isEnabled")
    val showCheckmark: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Only Show Missing", desc = "Only show Trophy Fish that are still missing at this rarity.")
    @ConfigEditorDropdown
    @FeatureDependencyRequirement("#isEnabled")
    val onlyShowMissing: Property<HideCaught> = Property.of(HideCaught.NONE)

    enum class HideCaught(private val displayName: String) {
        NONE("Show All"),
        BRONZE("Bronze"),
        SILVER("Silver"),
        GOLD("Gold"),
        DIAMOND("Diamond"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Show If Caught Higher Tier",
        desc = "Show Trophy Fish missing at the chosen tier even if a higher tier has already been caught.",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#isEnabled")
    val showCaughtHigher: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigLink(owner = TrophyFishDisplayConfig::class, field = "enabled")
    @FeatureDependencyRequirement("#isEnabled")
    val position: Position = Position(144, 139)
}
