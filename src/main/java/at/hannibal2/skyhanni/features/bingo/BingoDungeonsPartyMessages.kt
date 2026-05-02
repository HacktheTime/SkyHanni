package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object BingoDungeonsPartyMessages {
    val config = SkyHanniMod.feature.event.bingo
    val patternGroup = RepoPattern.group("feature.event.bingo.dungeons")
    val skillLVLUPPattern by patternGroup.pattern(
        "skill-level-up",
        "DUNGEON LEVEL UP The Catacombs (?<oldLevel>\\d+)➜(?<newLevel>\\d+)",
    )

    /**
     * REGEX-TEST Mage Milestone ❶: You have dealt 60,000 Total Damage so far! 02s
     */
    val milestoneReachedPattern by patternGroup.pattern(
        "milestone-reached",
        "(Mage|Tank|Archer|Healer|Berserker) Milestone (?<milestone>.).*",
    )

    @HandleEvent
    fun onMessage(event: SkyHanniChatEvent.Allow) {
        if (!SkyBlockUtils.isBingoProfile) return
        if (config.sendCataLevelUP) skillLVLUPPattern.matchMatcher(event.cleanMessage) {
            HypixelCommands.partyChat("Dungeon Skill Level Up: ${group("newLevel")}")
        }
        if (config.sendImportantCataMilestones) {
            milestoneReachedPattern.matchMatcher(event.cleanMessage) {
                val milestone = group("milestone")
                if (milestone == "❷" || milestone == "❸") {
                    HypixelCommands.partyChat("Dungeon Milestone $milestone reached!")
                }
            }
        }
    }
}
