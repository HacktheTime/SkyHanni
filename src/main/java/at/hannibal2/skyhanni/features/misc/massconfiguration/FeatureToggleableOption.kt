package at.hannibal2.skyhanni.features.misc.massconfiguration

import java.lang.reflect.Field

data class FeatureToggleableOption(
    val name: String,
    val description: String,
    val previouslyEnabled: Boolean,
    val isTrueEnabled: Boolean,
    val category: Category,
    val setter: (Boolean) -> Unit,
    val path: String,
    val field: Field,
    // Third-party metadata (null when not third-party dependent)
    val thirdParty: at.hannibal2.skyhanni.config.ThirdParty? = null,
    val tpRequiresMainToggle: Boolean = true,
    var toggleOverride: ResetSuggestionState? = null,
)
