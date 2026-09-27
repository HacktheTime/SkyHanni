package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonCompleteEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object BossDialogueDetector {

    private val config get() = SkyHanniMod.feature.dungeon.bossDialogueDetector

    private val patternGroup = RepoPattern.group("dungeon.boss.dialogue")

    /**
     * Matches the [BOSS] prefix in chat messages with color codes.
     * Captures the boss name and dialogue text.
     *
     * REGEX-TEST: §c[BOSS] Sadan§r§f: So you made it all the way here...
     * REGEX-TEST: §c[BOSS] Bonzo§r§f: Gratz for making it this far
     * REGEX-TEST: §4[BOSS] Necron§r§c: You went further than any human before
     */
    private val bossMessagePattern by patternGroup.pattern(
        "bossmessage",
        "§[c4]\\[BOSS] (?<boss>[^§]+)§r§[fc]: (?<dialogue>.+)",
    )

    private var dialogueCount = 0
    private var warpQueued = false
    private var phaseWarned = false
    private var lastBoss: DungeonFloor? = null
    private var finalKillTotalLines = 0
    private var finalKillSeenLines = 0

    // region Dialogue line lists

    private val watcherIntroLines = listOf(
        "Ah, you've finally arrived.",
        "Ah, we meet again...",
        "So you made it this far...",
        "You've managed to scratch and claw your way here",
        "I'm starting to get tired of seeing you around here",
        "Oh... hello? You've arrived too early",
        "Things feel a little more roomy now",
        "Congratulations, you made it through the Entrance",
    )

    private const val watcherWinLine = "You have proven yourself. You may pass."

    private val watcherSummonLines = listOf(
        "You'll do",
        "This guy looks like a fighter",
        "Let's see how you can handle this",
        "Go, fight!",
        "Hmmm... This one!",
        "Go and live again",
    )

    private const val watcherStopLine = "That will be enough for now"

    private val bonzoIntroLines = listOf(
        "Gratz for making it this far, but I'm basically unbeatable",
        "I don't even need to fight, this is the life",
        "I can summon lots of undead",
    )

    private val bonzoSecondPhaseLines = listOf(
        "Hoho, looks like you killed me",
        "You fools!",
        "You fell victim to one of the classic blunders",
        "Check mate",
        "I can revive myself and become much stronger",
    )

    private val bonzoFinalKillLines = listOf(
        "Alright, maybe I'm just weak after all",
        "But my masters are a lot stronger",
        "Just you wait",
    )

    private val scarfIntroLines = listOf(
        "This is where the journey ends for you, Adventurers",
        "The last few who tried fighting me are now in those Crypts",
        "If you can beat my Undeads, I'll personally grant you the privilege to replace them",
        "ARISE, MY CREATIONS",
        "This should be interesting",
    )

    private val scarfPhase2Lines = listOf(
        "Those toys are not strong enough I see",
        "Don't get too excited though",
        "Did you forget? I was taught by the best",
    )

    private val scarfFinalKillLines = listOf(
        "Whatever...",
        "You'll never beat my teacher",
        "His technique.. is too advanced",
    )

    private val professorIntroLines = listOf(
        "I was burdened with terrible news recently",
        "My most talented student",
        "I'll show you real power",
    )

    private val professorPhase2Lines = listOf(
        "Oh? You found my Guardians' one weakness",
        "Even if you took my barrier down, I can still fight",
        "This time I'll be your opponent",
    )

    private val professorPhase3Lines = listOf(
        "I see. You have forced me to use my ultimate technique",
        "These Guardians aren't just for show",
        "The process is irreversible, but I'll be stronger than a Wither now",
    )

    private val professorFinalKillLines = listOf(
        "What?! My Guardian power is unbeatable",
        "How could you... I needed more power",
        "I can't let my Master hear about this",
    )

    private val thornIntroLines = listOf(
        "Welcome Adventurers! I am Thorn, the Spirit! And host of the Vegan Trials",
        "My Arena is the most entertaining place in all the Catacombs",
        "Today you'll be our spectacle",
        "Dance! Dance with my Spirit animals",
        "And may you perish in a delightful way",
    )

    private val thornFinalKillLines = listOf(
        "This is it...where shall I go now",
        "All those memories will be lost in time, like tears in rain",
        "Congratulations humans, you may pass",
    )

    private val lividWelcomeLines = listOf(
        "Welcome, you arrived right on time",
        "I am Livid, the Master of Shadows",
        "This Orb you see, is Thorn, or what is left of him",
        "In a way, I have to thank you for getting rid of him",
        "I can now turn those Spirits into shadows of myself",
        "I respect you for making it to here, but I'll be your undoing",
    )

    private val lividFinalKillLines = listOf(
        "Impossible! How did you figure out which one I was",
        "If you think this is the end, you are so wrong",
        "My shadows are everywhere, THEY WILL FIND YOU",
    )

    private val sadanIntroLines = listOf(
        "So you made it all the way here... Now you wish to defy me? Sadan?!",
        "The audacity! I have been the ruler of these floors for a hundred years",
        "I am the bridge between this realm and the world below! You shall not pass",
    )

    private val sadanPhase2Lines = listOf(
        "ENOUGH!",
        "My giants! Unleashed!",
    )

    private val sadanPhase3Lines = listOf(
        "You did it. I understand now, you have earned my respect",
        "If only you had become my disciples instead of this incompetent bunch",
        "Maybe in another life. Until then, meet my ultimate corpse",
        "I'm sorry but I need to concentrate. I wish it didn't have to come to this",
    )

    private val sadanFinalKillLines = listOf(
        "NOOOOOOOOO",
        "THIS IS IMPOSSIBLE",
        "FATHER, FORGIVE ME",
    )

    private val necronIntroLines = listOf(
        "Finally, I heard so much about you. The Eye likes you very much",
        "You went further than any human before, congratulations",
        "I'm afraid, your journey ends now",
        "Goodbye",
    )

    private val necronFinalKillLines = listOf(
        "All this, for nothing",
        "I understand your words now, my master",
        "The Catacombs... are no more",
    )

    // endregion

    private fun matchesAny(dialogue: String, lines: List<String>): Boolean =
        lines.any { dialogue.contains(it) }

    private fun resetState() {
        dialogueCount = 0
        warpQueued = false
        phaseWarned = false
        lastBoss = null
        finalKillTotalLines = 0
        finalKillSeenLines = 0
    }

    private fun sendPartyChat(message: String) {
        HypixelCommands.partyChat(message)
    }

    private fun performWarp() {
        HypixelCommands.warp("hub")
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.enabled) return
        if (!DungeonApi.inDungeon()) return

        val message = event.message

        val (bossName, dialogue) = parseBossMessage(message) ?: return

        val expectedBoss = DungeonFloor.byBossName(bossName)

        if (bossName == "The Watcher") {
            handleWatcherDialogue(dialogue)
            return
        }

        if (expectedBoss == null || expectedBoss != DungeonApi.getCurrentBoss()) return

        if (expectedBoss != lastBoss) {
            resetState()
            lastBoss = expectedBoss
        }

        dialogueCount++
        handleFloorDialogue(expectedBoss, dialogue)
    }

    private fun parseBossMessage(message: String): Pair<String, String>? {
        var bossName: String? = null
        var dialogue: String? = null
        bossMessagePattern.matchMatcher(message) {
            bossName = group("boss")
            dialogue = group("dialogue")
        }
        val name = bossName ?: return null
        val text = dialogue ?: return null
        return Pair(name.trim(), text.trim())
    }

    private fun handleWatcherDialogue(dialogue: String) {
        if (DungeonApi.getCurrentBoss() == DungeonFloor.E) {
            handleWatcherAsBoss(dialogue)
        } else {
            handleWatcherAsMiniBoss(dialogue)
        }
    }

    private fun handleWatcherAsBoss(dialogue: String) {
        when {
            matchesAny(dialogue, watcherIntroLines) -> {
                if (config.warnPhases || config.warnWatcherStart) {
                    if (!phaseWarned) {
                        sendPartyChat("Watcher fight started!")
                        phaseWarned = true
                    }
                }
                dialogueCount = 0
                warpQueued = false
            }
            dialogue.contains(watcherWinLine) -> {
                if (config.autoWarp) {
                    sendPartyChat("Watcher defeated! Warping...")
                    performWarp()
                }
            }
        }
    }

    private fun handleWatcherAsMiniBoss(dialogue: String) {
        when {
            matchesAny(dialogue, watcherIntroLines) || matchesAny(dialogue, watcherSummonLines) -> {
                if (config.warnWatcherStart) {
                    if (!phaseWarned) {
                        sendPartyChat("Watcher mini-boss active!")
                        phaseWarned = true
                    }
                }
            }
            dialogue.contains(watcherStopLine) || dialogue.contains(watcherWinLine) -> {
                phaseWarned = false
            }
        }
    }

    private fun handleFloorDialogue(floor: DungeonFloor, dialogue: String) {
        when (floor) {
            DungeonFloor.F1 -> handleBonzo(dialogue)
            DungeonFloor.F2 -> handleScarf(dialogue)
            DungeonFloor.F3 -> handleProfessor(dialogue)
            DungeonFloor.F4 -> handleThorn(dialogue)
            DungeonFloor.F5 -> handleLivid(dialogue)
            DungeonFloor.F6 -> handleSadan(dialogue)
            DungeonFloor.F7 -> handleNecron(dialogue)
            DungeonFloor.E -> {} // handled separately
        }
    }

    private fun handleBonzo(dialogue: String) {
        when {
            matchesAny(dialogue, bonzoIntroLines) -> {
                if (config.warnPhases) sendPartyChat("Bonzo fight started!")
            }
            matchesAny(dialogue, bonzoSecondPhaseLines) -> {
                if (config.warnPhases) sendPartyChat("Bonzo phase 2!")
            }
            matchesAny(dialogue, bonzoFinalKillLines) -> handleFinalKill()
        }
    }

    private fun handleScarf(dialogue: String) {
        when {
            matchesAny(dialogue, scarfIntroLines) -> {
                if (config.warnPhases) sendPartyChat("Scarf fight started!")
            }
            matchesAny(dialogue, scarfPhase2Lines) -> {
                if (config.warnPhases) sendPartyChat("Scarf phase 2!")
            }
            matchesAny(dialogue, scarfFinalKillLines) -> handleFinalKill()
        }
    }

    private fun handleProfessor(dialogue: String) {
        when {
            matchesAny(dialogue, professorIntroLines) -> {
                if (config.warnPhases) sendPartyChat("Professor fight started!")
            }
            matchesAny(dialogue, professorPhase2Lines) -> {
                if (config.warnPhases) sendPartyChat("Professor phase 2!")
            }
            matchesAny(dialogue, professorPhase3Lines) -> {
                if (config.warnPhases) sendPartyChat("Professor phase 3 (Guardian form)!")
            }
            matchesAny(dialogue, professorFinalKillLines) -> handleFinalKill()
        }
    }

    private fun handleThorn(dialogue: String) {
        when {
            matchesAny(dialogue, thornIntroLines) -> {
                if (config.warnPhases) sendPartyChat("Thorn fight started!")
            }
            matchesAny(dialogue, thornFinalKillLines) -> handleFinalKill()
        }
    }

    private fun handleLivid(dialogue: String) {
        when {
            matchesAny(dialogue, lividWelcomeLines) -> {
                if (config.warnPhases) sendPartyChat("Livid fight started!")
            }
            matchesAny(dialogue, lividFinalKillLines) -> handleFinalKill()
        }
    }

    private fun handleSadan(dialogue: String) {
        when {
            matchesAny(dialogue, sadanIntroLines) -> {
                if (config.warnPhases) sendPartyChat("Sadan fight started! Terracotta phase.")
            }
            matchesAny(dialogue, sadanPhase2Lines) -> {
                if (config.warnPhases) sendPartyChat("Sadan phase 2 - Giants!")
            }
            matchesAny(dialogue, sadanPhase3Lines) -> {
                if (config.warnPhases) sendPartyChat("Sadan phase 3 - Giant One!")
            }
            matchesAny(dialogue, sadanFinalKillLines) -> handleFinalKill()
        }
    }

    private fun handleNecron(dialogue: String) {
        when {
            matchesAny(dialogue, necronIntroLines) -> {
                if (config.warnPhases) sendPartyChat("Necron fight started!")
            }
            matchesAny(dialogue, necronFinalKillLines) -> handleFinalKill()
        }
    }

    private fun handleFinalKill() {
        if (!config.autoWarp) return

        if (finalKillTotalLines == 0) {
            finalKillTotalLines = countFinalKillLinesForFloor(lastBoss ?: return)
            finalKillSeenLines = 1

            if (finalKillTotalLines <= 5) {
                sendPartyChat("Boss defeated! Warping...")
                performWarp()
            } else {
                sendPartyChat("Boss defeated! Warping soon...")
                warpQueued = true
            }
        } else {
            finalKillSeenLines++

            if (warpQueued && finalKillSeenLines >= finalKillTotalLines - 2) {
                sendPartyChat("Warping now!")
                performWarp()
                warpQueued = false
            }
        }
    }

    private fun countFinalKillLinesForFloor(floor: DungeonFloor): Int = when (floor) {
        F1 -> bonzoFinalKillLines.size
        F2 -> scarfFinalKillLines.size
        F3 -> professorFinalKillLines.size
        F4 -> thornFinalKillLines.size
        F5 -> lividFinalKillLines.size
        F6 -> sadanFinalKillLines.size
        F7 -> necronFinalKillLines.size
        E -> 0
    }

    @HandleEvent
    private fun onWorldChange() {
        resetState()
    }

    @HandleEvent
    private fun onDungeonEnd(event: DungeonCompleteEvent) {
        resetState()
    }
}
