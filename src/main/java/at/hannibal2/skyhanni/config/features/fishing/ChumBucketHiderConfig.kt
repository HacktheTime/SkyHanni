package at.hannibal2.skyhanni.config.features.fishing

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.DependencyDelegate
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class ChumBucketHiderConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Hide the Chum/Chumcap Bucket name tags for other players.")
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @DependencyDelegate(field = "enabled")
    private var isEnabled: Boolean
        get() = enabled.get()
        set(value) { enabled.set(value) }

    @Expose
    @ConfigOption(name = "Hide Bucket", desc = "Hide the Chum/Chumcap Bucket.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#isEnabled")
    val hideBucket: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Hide Own", desc = "Hide your own Chum/Chumcap Bucket.")
    @ConfigEditorBoolean
    @FeatureDependencyRequirement("#isEnabled")
    val hideOwn: Property<Boolean> = Property.of(false)
}
