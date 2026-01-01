package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.inventory.chocolatefactory.CFConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SuperCraftingCoinWasteConfig {
    @Expose
    @ConfigOption(name = "Warn about Super Crafting Coin Waste", desc = "Warns you when you can save more than Xm coins by insta buying the item and instant selling the materials.")
    @ConfigEditorBoolean
    @FeatureToggle
    val warnCoinWasteEnabled: Boolean = true

    @Expose
    @ConfigOption(name = "Amount", desc = "The minimum amount of coins (in millions) you must save (instant sell and insta buy for wanted) to get a warning.")
    @ConfigEditorSlider(minValue = 1.0f, maxValue = 50.0f, minStep = 0.1f)
    val warnCoinWaste: Double = 10.0

    @Expose
    @ConfigLink(owner = SuperCraftingCoinWasteConfig::class, field = "warnCoinWaste")
    val warnCoinWastePosition: Position = Position(300, 300)
}
