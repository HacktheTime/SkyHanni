package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PartyChatEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import de.hype.bingonet.BNConnection
import de.hype.bingonet.shared.packets.function.RequestPartyStatePacket
import de.hype.bingonet.shared.packets.function.RequestPartyStatePacket.PartyStatePacket
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
@Suppress("LongMethod", "ComplexMethod")
object PartyApi {
    val hideConfig = SkyHanniMod.feature.misc.hidePartyMessagesConfig
    private val patternGroup = RepoPattern.group("data.party")

    /**
     * REGEX-TEST: -----------------------------------------------------
     */
    private val wrapper by patternGroup.pattern("wrapper", "9m-----------------------------------------------------")

    /**
     * REGEX-TEST: You have joined [MVP+] Throwpo's party!
     */
    private val youJoinedPartyPattern by patternGroup.pattern(
        "you.joined",
        "You have joined (?<name>.*)'s? party!",
    )

    // TODO
    private val otherDisconnect5MinTimePattern by patternGroup.pattern(
        "others.disconnect.5min",
        ".*has disconnected, they have 5 minutes to rejoin before they are removed from the party.",
    )

    /**
     * REGEX-TEST: [MVP+] Throwpo joined the party.
     */
    private val othersJoinedPartyPattern by patternGroup.pattern(
        "others.joined",
        "(?<name>.*) joined the party\\.",
    )

    // TODO the invited you to their party was created based on what i had in mind. has to be tested still!
    /**
     * REGEX-TEST: [MVP+] Throwpo invited you to join their Party.
     */
    val receivedInvitePattern by patternGroup.pattern(
        "others.joined",
        "(?<name>.*) invited you to join their Party\\.",
    )

    /**
     * REGEX-TEST: You'll be partying with: [VIP] FungalBeatle550
     */
    private val othersInThePartyPattern by patternGroup.pattern(
        "others.inparty",
        "You'll be partying with: (?<names>.*)",
    )

    /**
     * REGEX-TEST: 246sweets has left the party.
     */
    private val otherLeftPattern by patternGroup.pattern(
        "others.left",
        "(?<name>.*) has left the party\\.",
    )

    /**
     * REGEX-TEST: riblets has been removed from the party.
     */
    private val otherKickedPattern by patternGroup.pattern(
        "others.kicked",
        "(?<name>.*) has been removed from the party\\.",
    )

    /**
     * REGEX-TEST: Kicked [MVP+] Throwpo because they were offline.
     */
    private val otherOfflineKickedPattern by patternGroup.pattern(
        "others.offline",
        "Kicked (?<name>.*) because they were offline\\.",
    )

    /**
     * REGEX-TEST: [MVP+] Throwpo was removed from your party because they disconnected.
     */
    private val otherDisconnectedPattern by patternGroup.pattern(
        "others.disconnect",
        "(?<name>.*) was removed from your party because they disconnected\\.",
    )

    /**
     * REGEX-TEST: The party was transferred to [MVP+] CalMWolfs because [MVP+] Throwpo left
     */
    private val transferOnLeavePattern by patternGroup.pattern(
        "others.transfer.leave",
        "The party was transferred to (?<newowner>.*) because (?<name>.*) left",
    )

    /**
     * REGEX-TEST: The party was transferred to [MVP+] Throwpo by [MVP+] CalMWolfs
     */
    val transferVoluntaryPattern by patternGroup.pattern(
        "others.transfer.voluntary",
        "The party was transferred to (?<newowner>.*) by (?<name>.*)",
    )

    /**
     * REGEX-TEST: [MVP+] Throwpo has disbanded the party!
     */
    private val disbandedPattern by patternGroup.pattern(
        "others.disband",
        ".* has disbanded the party!",
    )

    /**
     * REGEX-TEST: You have been kicked from the party by [MVP+] Throwpo 
     */
    private val kickedPattern by patternGroup.pattern(
        "you.kicked",
        "You have been kicked from the party by .* ",
    )

    /**
     * REGEX-TEST: Party Members (2)
     */
    private val partyMembersStartPattern by patternGroup.pattern(
        "members.start",
        "Party Members \\(\\d+\\)",
    )

    /**
     * REGEX-TEST: The party invite to [MVP++] Mininoob46 has expired.
     */
    private val inviteExpiredPattern by patternGroup.pattern(
        "invite.expired",
        "The party invite to (?<name>.*) has expired\\.",
    )

    /**
     * REGEX-TEST: [MVP+] Hype_the_Time has promoted [VIP] NPCforCommands to Party Moderator
     */
    private val promoteModeratorPattern by patternGroup.pattern(
        "moderator.promoted",
        "(?<promoter>.*) has promoted (?<name>.*) to Party Moderator",
    )

    /**
     * REGEX-TEST: [MVP+] Hype_the_Time has promoted [VIP] NPCforCommands to Party Leader
     */
    private val promoteLeaderPattern by patternGroup.pattern(
        "moderator.promoted.leader",
        "(?<promoter>.*) has promoted (?<name>.*) to Party Leader",
    )

    /**
     * REGEX-TEST: [MVP+] Hype_the_Time has demoted [VIP] NPCforCommands to Party Member
     */
    private val demoteModeratorPattern by patternGroup.pattern(
        "moderator.demoted",
        "(?<demoter>.*) has demoted (?<name>.*) to Party Member",
    )

    /**
     * REGEX-TEST: [MVP+] Hype_the_Time is now a Party Moderator
     */
    private val nowModeratorPattern by patternGroup.pattern(
        "moderator.now",
        "(?<name>.*) is now a Party Moderator",
    )

    /**
     * REGEX-TEST: Party Members: [MVP+] Throwpo ●
     * REGEX-TEST: Party Leader: [MVP+] CalMWolfs ●
     */
    private val partyMemberListPattern by patternGroup.pattern(
        "members.list.withkind",
        "Party (?<kind>Leader|Moderators|Members): (?<names>.*)",
    )
    private val kuudraFinderJoinPattern by patternGroup.pattern(
        "kuudrafinder.join",
        "Party Finder > (?<name>.*?) joined the group! \\(a-fA-F0-9]+Combat Level \\d+\\)",
    )

    /**
     * REGEX-TEST: Party Finder > GhostsTM joined the dungeon group! (Archer Level 9)
     */
    private val dungeonFinderJoinPattern by patternGroup.pattern(
        "dungeonfinder.join",
        "Party Finder > (?<name>.*?) joined the dungeon group! \\(a-fA-F0-9].* Level \\d+a-fA-F0-9]\\)",
    )

    val partyMembers = mutableListOf<String>()

    val partyModerators = mutableListOf<String>()

    var partyLeader: String? = null
    var prevPartyLeader: String? = null
    var allInvite: Boolean = false

    // The maximum amount of players that are allowed to join the party. null means no limit is set.
    var partyLimit: Int? = null

    // The names of players we sent an invite to (or accepted a join request for) that have not yet joined the party.
    // A player may also join without an invite while the party is open (e.g. stream open), those are not tracked here.
    // Entries are removed when the player joins the party or when their invite expires.
    val pendingInvites: MutableMap<String, SimpleTimeMark> = mutableMapOf()

    fun addPendingInvites(names: List<String>) {
        val expiry = SimpleTimeMark.now() + 60.seconds
        names.forEach { pendingInvites[it] = expiry }
    }

    fun removePendingInvite(name: String) {
        pendingInvites.remove(name)
    }

    /**
     * The amount of outstanding invites, removing entries whose invite already expired.
     */
    fun pendingInviteCount(): Int {
        pendingInvites.entries.removeIf { it.value.passedSince() > 60.seconds }
        return pendingInvites.size
    }

    fun poll(question: String, options: List<String>){
        val message = question+"/"+options.joinToString("/")
        send("party poll $message")
    }

    fun isInParty() = partyMembers.isNotEmpty()

    private fun listMembers() {
        val size = partyMembers.size
        if (size == 0) {
            ChatUtils.chat("No tracked party members!")
            return
        }
        ChatUtils.chat("Tracked party members ($size) :", prefixColor = "")
        for (member in partyMembers) {
            ChatUtils.chat(" - $member" + if (partyLeader == member) " (Leader)" else "", false)
        }
        if (partyModerators.isNotEmpty()) {
            ChatUtils.chat("Moderators: ${partyModerators.joinToString(", ")}", false)
        }

        if (partyLeader == PlayerUtils.getName()) {
            ChatUtils.chat("You are leader")
        }

        if (Random.nextDouble() < 0.1) {
            OSUtils.openBrowser("https://www.youtube.com/watch?v=iANP7ib7CPA")
            ChatUtils.hoverableChat("Are You Ready To Party?", listOf("~Spongebob"), prefix = false)
        }
    }

    @HandleEvent
    fun onPartyChat(event: PartyChatEvent.Allow) {
        val name = event.author.cleanPlayerName()
        addPlayer(name)
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.cleanMessage.trimWhiteSpace()

        wrapper.matchMatcher(message) {
            if (hideConfig.hideWrapper) {
                event.blockedReason = "Hide Party Messages: Hide Wrapper"
            }
        }

        // new member joined
        youJoinedPartyPattern.matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            partyLeader = name
            addPlayer(name)
        }
        othersJoinedPartyPattern.matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            if (partyMembers.isEmpty()) {
                partyLeader = PlayerUtils.getName()
            }
            addPlayer(name)
            if (partyMembers.size >= hideConfig.hideJoinAndLeave && hideConfig.hideJoinAndLeave!=0) {
                event.blockedReason = "Hide Party Messages: Hide Join/Leave"
            }
        }
        othersInThePartyPattern.matchMatcher(message) {
            for (name in group("names").split(", ")) {
                addPlayer(name.cleanPlayerName())
            }
        }
        kuudraFinderJoinPattern.matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            addPlayer(name)
        }
        dungeonFinderJoinPattern.matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            addPlayer(name)
        }

        // one member got removed
        otherLeftPattern.matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            removeWithLeader(name)
            if (partyMembers.size >= hideConfig.hideJoinAndLeave && hideConfig.hideJoinAndLeave!=0) {
                event.blockedReason = "Hide Party Messages: Hide Join/Leave"
            }
        }
        otherKickedPattern.matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            removeWithLeader(name)
            if (partyMembers.size >= hideConfig.hideKicks && hideConfig.hideKicks!=0) {
                event.blockedReason = "Hide Party Messages: Hide Kicks"
            }
        }
        otherOfflineKickedPattern.matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            removeWithLeader(name)
            if (partyMembers.size >= hideConfig.hideKicks&& hideConfig.hideKicks!=0) {
                event.blockedReason = "Hide Party Messages: Hide Kicks"
            }
        }
        otherDisconnectedPattern.matchMatcher(message) {
            val name = group("name").cleanPlayerName()
            if (partyMembers.size >= hideConfig.hideDisconnects && hideConfig.hideDisconnects!=0) {
                event.blockedReason = "Hide Party Messages: Hide Disconnects"
            }
            partyMembers.remove(name)
        }
        otherDisconnect5MinTimePattern.matchMatcher(message) {
            if (partyMembers.size >= hideConfig.hideDisconnects && hideConfig.hideDisconnects!=0) {
                event.blockedReason = "Hide Party Messages: Hide Disconnects"
            }
        }
        inviteExpiredPattern.matchMatcher(message) {
            removePendingInvite(group("name").cleanPlayerName())
        }
        promoteModeratorPattern.matchMatcher(message.removeColor()) {
            val name = group("name").cleanPlayerName()
            if (!partyModerators.contains(name)) {
                partyModerators.add(name)
            }
        }
        promoteLeaderPattern.matchMatcher(message.removeColor()) {
            val promoted = group("name").cleanPlayerName()
            partyModerators.remove(promoted)
            partyLeader = promoted
            prevPartyLeader = group("promoter").cleanPlayerName()
            // The previous leader becomes a Party Moderator, covered by the "is now a Party Moderator" message.
        }
        demoteModeratorPattern.matchMatcher(message.removeColor()) {
            partyModerators.remove(group("name").cleanPlayerName())
        }
        nowModeratorPattern.matchMatcher(message.removeColor()) {
            val name = group("name").cleanPlayerName()
            if (!partyModerators.contains(name)) {
                partyModerators.add(name)
            }
        }
        transferOnLeavePattern.matchMatcher(message.removeColor()) {
            val name = group("name").cleanPlayerName()
            partyLeader = group("newowner").cleanPlayerName()
            partyMembers.remove(name)
            partyModerators.remove(name)
        }
        transferVoluntaryPattern.matchMatcher(message.removeColor()) {
            partyLeader = group("newowner").cleanPlayerName()
            prevPartyLeader = group("name").cleanPlayerName()
            // Transferring the party lets the previous leader keep moderator status.
            val oldLeader = group("name").cleanPlayerName()
            if (!partyModerators.contains(oldLeader)) {
                partyModerators.add(oldLeader)
            }
        }

        // party disbanded
        disbandedPattern.matchMatcher(message) {
            partyLeft()
        }
        kickedPattern.matchMatcher(message) {
            partyLeft()
        }
        if (message == "You left the party." ||
            message == "The party was disbanded because all invites expired and the party was empty." ||
            message == "You are not currently in a party." ||
            message == "You are not in a party." ||
            message == "The party was disbanded because the party leader disconnected."
        ) {
            partyLeft()
        }

        // party list
        partyMembersStartPattern.matchMatcher(message.removeResets()) {
            partyMembers.clear()
            partyModerators.clear()
        }

        partyMemberListPattern.matchMatcher(message.removeColor()) {
            val kind = group("kind")
            val isPartyLeader = kind == "Leader"
            val isModerators = kind == "Moderators"
            for (name in group("names").split(" ● ")) {
                val playerName = name.replace(" ●", "").cleanPlayerName()
                addPlayer(playerName)
                if (isPartyLeader) {
                    partyLeader = playerName
                } else if (isModerators && !partyModerators.contains(playerName)) {
                    partyModerators.add(playerName)
                }
            }
        }
    }

    private fun removeWithLeader(name: String) {
        partyMembers.remove(name)
        partyModerators.remove(name)
        if (name == prevPartyLeader) {
            prevPartyLeader = null
        }
    }

    private fun addPlayer(playerName: String) {
        if (partyMembers.contains(playerName)) return
        if (playerName == PlayerUtils.getName()) return
        partyMembers.add(playerName)
        // The player joined, so any outstanding invite to them is consumed.
        pendingInvites.remove(playerName)
    }

    private fun partyLeft() {
        partyMembers.clear()
        partyModerators.clear()
        partyLeader = null
        prevPartyLeader = null
        allInvite = false
        partyLimit = null
        pendingInvites.clear()
    }

    fun isPartyLeader() = partyLeader == PlayerUtils.getName()

    fun canInvite(): Boolean {
        return !isInParty() || isPartyLeader() || isModerator() || allInvite
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shpartydebug") {
            description = "List persons into the chat SkyHanni thinks are in your party."
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback { listMembers() }
        }
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Party")
        event.addIrrelevant {
            val size = partyMembers.size
            if (size == 0) {
                add("No tracked party members!")
            } else {
                add("Tracked party members ($size)")
                for (member in partyMembers) {
                    add(" - $member" + if (partyLeader == member) " (Leader)" else "")
                }
                if (partyModerators.isNotEmpty()) {
                    add("Moderators: ${partyModerators.joinToString(", ")}")
                }
            }

            if (partyLeader == PlayerUtils.getName()) {
                add("")
                add("You are leader")
            }
        }
    }


    fun warp(): Boolean {
        if (!isPartyLeader()) return false
        send("party warp")
        return true
    }

    fun partyTransfer(player: String): Boolean {
        if (!isPartyLeader()) return false
        send("party transfer $player")
        return true
    }

    fun promote(player: String): Boolean {
        if (!isPartyLeader()) return false
        send("party promote $player")
        return true
    }

    fun disband(): Boolean {
        if (!isPartyLeader()) return false
        send("party disband")
        return true
    }

    fun kick(player: String): Boolean {
        if (!isPartyLeader()) return false
        send("party kick $player")
        return true
    }

    fun kick(player: List<String>): Boolean {
        if (!isPartyLeader()) return false
        player.forEach {
            send("party kick $it")
        }
        return true
    }

    fun kickOffline(): Boolean {
        if (!isPartyLeader()) return false
        send("party kickoffline")
        return true
    }

    fun allInvite(): Boolean {
        if (!isPartyLeader()) return false
        send("party settings allinvite")
        return true
    }

    fun streamOpen(limit: Int? = null): Boolean {
        if (!isPartyLeader()) return false
        if (limit != null && limit > 0) {
            // Opening the party with a limit also sets the tracked limit, so normal invite
            // requests (e.g. via party chat commands) can not bypass it.
            partyLimit = limit
            send("stream open $limit")
        } else {
            send("stream")
        }
        return true
    }


    fun invite(username: String): Boolean {
        if (!canInvite()) return false
        send("party invite $username")
        return true
    }
    // TODO add something that slows down invites if a lot people are supposed to be invited.

    fun invite(usernames: List<String>): Boolean {
        if (!canInvite()) return false
        for (chunked in usernames.chunked(5)) {
            send("party invite ${chunked.joinToString(" ")}")
        }
        return true
    }

    private fun send(message: String) {
        ChatUtils.sendMessageToServer("/$message")
    }
    fun isModerator(name: String = PlayerUtils.getName()): Boolean {
        return partyModerators.contains(name)
    }

    fun leaveParty() {
        if (!isInParty()) return
        send("party leave")
    }

    fun joinParty(user: String): Boolean {
        if (isInParty()) return false
        send("party join $user")
        return true
    }

    fun acceptParty(user: String): Boolean {
        if (isInParty()) return false
        send("party accept $user")
        return true
    }

    fun allPartyPlayersInLobby(): Boolean {
        val playerList: Set<String> = EntityUtils.getPlayerList()
        return partyMembers.all { playerList.contains(it) }
    }

    fun onRequestPartyStatePacket(requestPartyStatePacket: RequestPartyStatePacket) {
        val general = SkyHanniMod.feature.event.bingo.bingoNetworks.allowBNServerPartyManagement
        val count = if (general) partyMembers.size else 0
        val response = PartyStatePacket(
            general,
            general && isInParty(),
            !general || allPartyPlayersInLobby(),
            count,
            general && isPartyLeader(),
            general && canInvite(),
        )
        BNConnection.sendPacket(requestPartyStatePacket.preparePacketToReplyToThis(response))
    }

    /**
     * PC does not need to be refactored in my opinion so take this helper if your looking here for it.
     */
    fun partyChat(message: String, prefix: Boolean = false) {
        HypixelCommands.partyChat(
            message,
            prefix,
        )
    }
}
