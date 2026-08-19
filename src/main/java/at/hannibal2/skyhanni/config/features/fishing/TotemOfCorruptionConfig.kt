package at.hannibal2.skyhanni.config.features.fishing

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.DependencyDelegate
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class TotemOfCorruptionConfig {
    @Expose
    @ConfigOption(
        name = "Show Overlay",
        desc = "Show the Totem of Corruption overlay.\n" +
            "Shows the totem, in which effective area you are in, with the longest time left.\n" +
            "§eThis needs to be enabled for the other options to work.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val showOverlay: Property<Boolean> = Property.of(true)

    @DependencyDelegate(field = "showOverlay")
    private var isOverlayShown: Boolean
        get() = showOverlay.get()
        set(value) { showOverlay.set(value) }

    @Expose
    @ConfigOption(
        name = "Own Totem Only",
        desc = "Ignore totems from other players, since they won't affect your sea creatures.\n" +
            "§eMay break if you are nicked!"
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#isOverlayShown")
    var ownTotemOnly: Boolean = true

    @Expose
    @ConfigOption(
        name = "Distance Threshold",
        desc = "The minimum distance to the Totem of Corruption for the overlay.\n" +
            "The effective distance of the totem is 16.\n" +
            "§cLimited by how far you can see the nametags.",
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
    @FeatureDependencyRequirement("#isOverlayShown")
    var distanceThreshold: Int = 16

    @Expose
    @ConfigOption(
        name = "Hide Particles",
        desc = "Hide the particles of the Totem of Corruption.\n" +
            "§eRequires the Overlay to be active.",
    )
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#isOverlayShown")
    var hideParticles: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show Effective Area",
        desc = "Show the effective area (16 blocks) of the Totem of Corruption.",
    )
    @ConfigEditorDropdown
    @FeatureDependencyRequirement("#isOverlayShown")
    var outlineType: OutlineType = OutlineType.FILLED

    enum class OutlineType(private val displayName: String) {
        NONE("No Outline"),
        FILLED("Filled"),
        WIREFRAME("Wireframe"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Color of the area", desc = "The color of the area of the Totem of Corruption.")
    @ConfigEditorColour
    @FeatureDependencyRequirement("#isOverlayShown")
    var color: ChromaColour = ChromaColour.fromStaticRGB(18, 159, 85, 153)

    @Expose
    @ConfigOption(
        name = "Warn when about to expire",
        desc = "Select the time in seconds when the totem is about to expire to warn you.\n" +
            "Select 0 to disable.",
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 60f, minStep = 1f)
    @FeatureDependencyRequirement("#isOverlayShown")
    var warnWhenAboutToExpire: Int = 5

    @Expose
    @ConfigLink(owner = TotemOfCorruptionConfig::class, field = "showOverlay")
    @FeatureDependencyRequirement("#isOverlayShown")
    val position: Position = Position(50, 20)
}
