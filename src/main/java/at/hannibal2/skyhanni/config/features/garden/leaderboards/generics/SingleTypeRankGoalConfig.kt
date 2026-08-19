package at.hannibal2.skyhanni.config.features.garden.leaderboards.generics

import at.hannibal2.skyhanni.config.FeatureDependencyRequirement
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

@FeatureDependencyRequirement(
    "at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteFarmersLeaderboardsConfig#enabled",
)
class SingleTypeRankGoalConfig : RankGoalGenericConfig() {

    @Expose
    @ConfigOption(
        name = "All-Time Rank Goal",
        desc = "Set a rank goal for the All-Time Leaderboard.",
    )
    @ConfigEditorText
    val rankGoal: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(
        name = "Monthly Rank Goal",
        desc = "Set a rank goal for the Monthly Leaderboard.",
    )
    @ConfigEditorText
    val monthlyRankGoal: Property<String> = Property.of("10000")
}
