package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.convertToUnformatted
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternGroup
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@SkyHanniModule
object CenturyCakeAPI {
    val lastEaten = ProfileStorageData.profileSpecific?.centuryCakes ?: HashMap()
    private val patternGroup = RepoPatternGroup("misc")
    /**
     * REGEX-TEST: Yum! You gain +5☘ Farming Fortune for 48 hours!
     * REGEX-TEST: Big Yum! You refresh +5☘ Farming Fortune for 48 hours!
     */
    private val cakePattern by patternGroup.pattern(
        "cakes",
        "(?:Big )?Yum! You (?:gain|refresh) (?<stat>.*) for 48 hours!",
    )

    enum class CenturyCakeType {
        PET_LUCK,
        HEALTH,
        STRENGTH,
        FEROCITY,
        SPEED,
        DEFENSE,
        INTELLIGENCE,
        SEA_CREATURE_CHANCE,
        MAGIC_FIND,
        FARMING_FORTUNE,
        FORAGING_FORTUNE,
        MINING_FORTUNE,
        VITALITY,
        TRUE_DEFENSE,
        COLD_RESISTANCE,
        RIFT_TIME,
    }

    fun getCakeExpiration(cake: CenturyCakeType): SimpleTimeMark {
        return lastEaten[cake] ?: SimpleTimeMark.farPast()
    }

    fun ateAllCakes(minimumDuration: Duration): Boolean {
        CenturyCakeType.entries.forEach {
            val eatenTime = getCakeExpiration(it)
            if (eatenTime + minimumDuration < SimpleTimeMark.now()) return false
        }
        return true
    }
    @HandleEvent(onlyOnIslands = [IslandType.PRIVATE_ISLAND, IslandType.PRIVATE_ISLAND_GUEST] )
    fun onChat(event: at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent) {
        cakePattern.matchMatcher(event.message.convertToUnformatted()){
            val stat = group("stat") ?: return@matchMatcher
            val cakeType = when {
                stat.contains("Farming Fortune") -> CenturyCakeType.FARMING_FORTUNE
                stat.contains("Foraging Fortune") -> CenturyCakeType.FORAGING_FORTUNE
                stat.contains("Mining Fortune") -> CenturyCakeType.MINING_FORTUNE
                stat.contains("Health") -> CenturyCakeType.HEALTH
                stat.contains("Strength") -> CenturyCakeType.STRENGTH
                stat.contains("Ferocity") -> CenturyCakeType.FEROCITY
                stat.contains("Speed") -> CenturyCakeType.SPEED
                stat.contains("Defense") -> CenturyCakeType.DEFENSE
                stat.contains("Intelligence") -> CenturyCakeType.INTELLIGENCE
                stat.contains("Sea Creature Chance") -> CenturyCakeType.SEA_CREATURE_CHANCE
                stat.contains("Magic Find") -> CenturyCakeType.MAGIC_FIND
                stat.contains("Vitality") -> CenturyCakeType.VITALITY
                stat.contains("True Defense") -> CenturyCakeType.TRUE_DEFENSE
                stat.contains("Cold Resistance") -> CenturyCakeType.COLD_RESISTANCE
                stat.contains("Rift Time") -> CenturyCakeType.RIFT_TIME
                stat.contains("Pet Luck") -> CenturyCakeType.PET_LUCK
                else -> return@matchMatcher
            }
            lastEaten[cakeType] = (SimpleTimeMark.now()+48.hours)
        }
    }
}


fun CenturyCakeAPI.CenturyCakeType.expirationTime(): SimpleTimeMark {
    return CenturyCakeAPI.getCakeExpiration(this)
}
