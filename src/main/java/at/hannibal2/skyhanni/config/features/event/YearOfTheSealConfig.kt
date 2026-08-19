package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.DependencyDelegate
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import io.github.notenoughupdates.moulconfig.observer.Property

class YearOfTheSealConfig {
    @Expose
    @ConfigOption(
        name = "Fishy Treat Profit",
        desc = "Shows what items to buy with your hard earned Fishy Treat.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var fishyTreatProfit: Boolean = true

    @Expose
    @ConfigLink(owner = YearOfTheSealConfig::class, field = "fishyTreatProfit")
    @FeatureDependencyRequirement("#fishyTreatProfit")
    val fishyTreatProfitPosition: Position = Position(170, 150)

    @Expose
    @ConfigOption(name = "Bouncy Ball Line", desc = "Shows a line for your bouncy balls thrown (Only works on normal ones, not giant).")
    @ConfigEditorBoolean
    @SearchTag("beach")
    @FeatureToggle
    val bouncyBallLine: Property<Boolean> = Property.of(true)

    @DependencyDelegate(field = "bouncyBallLine")
    private var hasBouncyBallLine: Boolean
        get() = bouncyBallLine.get()
        set(value) { bouncyBallLine.set(value) }

    @Expose
    @ConfigOption(name = "Bouncy Ball Line Color", desc = "Color of the Bouncy Ball Line.")
    @ConfigEditorColour
    @SearchTag("beach")
    @FeatureDependencyRequirement("#hasBouncyBallLine")
    var bouncyBallLineColor: ChromaColour = ChromaColour.fromStaticRGB(255, 0, 196, 245)

    @Expose
    @ConfigOption(name = "Bouncy Ball Landing Spot", desc = "Show the spot where the Bouncy Ball will land, and add a counter.")
    @ConfigEditorBoolean
    @SearchTag("beach")
    val bouncyBallLandingSpot: Property<Boolean> = Property.of(true)
}
