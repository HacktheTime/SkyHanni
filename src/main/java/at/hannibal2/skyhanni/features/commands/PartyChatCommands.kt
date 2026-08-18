package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.features.misc.PartyCommandsConfig
import at.hannibal2.skyhanni.config.features.misc.PermissionLevel
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.hypixel.chat.event.Direction
import at.hannibal2.skyhanni.data.hypixel.chat.event.PartyChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PrivateMessageChatEvent
import at.hannibal2.skyhanni.events.chat.TabCompletionEvent
import at.hannibal2.skyhanni.features.misc.CurrentPing
import at.hannibal2.skyhanni.features.misc.TpsCounter
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.DevApi
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PartyChatCommands {
    private val config get() = SkyHanniMod.feature.misc.partyCommands
    private val storage get() = SkyHanniMod.feature.storage

    data class PartyChatCommand(
        val names: List<String>,
        val permission: (PartyCommandsConfig.TrustUserConfig) -> PermissionLevel,
        val triggerableBySelf: Boolean = true,
        val requiresPartyLead: Boolean = true,
        val offCooldown: () -> Boolean = { true },
        val validateArgs: (List<String>) -> Boolean = { true },
        val execute: (event: PartyChatEvent.Allow, args: List<String>) -> Unit,
    ) {
        fun canUse(userConfig: PartyCommandsConfig.TrustUserConfig): PermissionLevel = permission(userConfig)
    }

    private val allPartyCommands = listOf(
        PartyChatCommand(
            listOf("pt", "ptme", "transfer"),
            { it.effectiveTransferLeader },
            triggerableBySelf = false,
            execute = { event, _ ->
                PartyApi.partyTransfer(event.authorName)
            },
        ),
        PartyChatCommand(
            listOf("pw", "warp", "warpus"),
            { it.effectiveWarp },
            execute = { _, _ ->
                lastWarp = SimpleTimeMark.now()
                PartyApi.warp()
            },
            offCooldown = { lastWarp.passedSince() > 5.seconds },
        ),
        PartyChatCommand(
            listOf("allinv", "allinvite"),
            { it.effectiveEnableAllInvite },
            execute = { event, _ ->
                if (PartyApi.partyLimit != null && !canEnableAllInviteWhileLimited(event.authorName)) {
                    ChatUtils.chat(
                        "§cA party limit is set, you need permission to bypass it to enable all invites.",
                    )
                    return@PartyChatCommand
                }
                lastAllInvite = SimpleTimeMark.now()
                PartyApi.allInvite()
            },
            offCooldown = { lastAllInvite.passedSince() > 2.seconds },
        ),
        PartyChatCommand(
            listOf("inv", "invite"),
            { it.effectiveInviteOthers },
            requiresPartyLead = false,
            execute = { event, args ->
                val name = event.authorName
                val invitees = if (args.isEmpty()) listOf(name) else args
                if (wouldExceedLimit(invitees.size) && !canBypassPartyLimit(name)) {
                    ChatUtils.chat("§cParty limit reached, cannot invite ${invitees.joinToString(", ")}.")
                    return@PartyChatCommand
                }
                PartyApi.invite(invitees)
                PartyApi.addPendingInvites(invitees)
            },
        ),
        PartyChatCommand(
            listOf("inviteme", "request"),
            { it.effectiveSelfInvite },
            requiresPartyLead = false,
            execute = { event, _ ->
                val name = event.authorName
                if (wouldExceedLimit(1) && !canBypassPartyLimit(name)) {
                    ChatUtils.chat("§cParty limit reached, cannot invite $name.")
                    return@PartyChatCommand
                }
                PartyApi.invite(name)
                PartyApi.addPendingInvites(listOf(name))
            },
        ),
        PartyChatCommand(
            listOf("kick"),
            { it.effectiveKick },
            validateArgs = { it.isNotEmpty() },
            execute = { _, args ->
                PartyApi.kick(args)
            },
        ),
        PartyChatCommand(
            listOf("kickoff", "kickoffline"),
            { it.effectiveKickOffline },
            execute = { _, _ ->
                PartyApi.kickOffline()
            },
        ),
        PartyChatCommand(
            listOf("stream", "streamopen"),
            { it.effectiveStreamOpen },
            validateArgs = { it.size <= 1 && (it.isEmpty() || (it.first().toIntOrNull() ?: 0) > 0) },
            execute = { _, args ->
                PartyApi.streamOpen(args.firstOrNull()?.toIntOrNull())
            },
        ),
        PartyChatCommand(
            listOf("poll"),
            { it.effectiveDoPolls },
            validateArgs = { args ->
                val splitIndex = args.indexOfFirst { it.contains('/') }
                splitIndex >= 1 &&
                    args.drop(splitIndex).flatMap { it.split('/') }.filter { it.isNotEmpty() }.size >= 2
            },
            execute = { _, args ->
                val splitIndex = args.indexOfFirst { it.contains('/') }
                val question = args.take(splitIndex).joinToString(" ")
                val options = args.drop(splitIndex).flatMap { it.split('/') }.filter { it.isNotEmpty() }
                PartyApi.poll(question, options)
            },
        ),
        PartyChatCommand(
            listOf("limit"),
            { it.effectiveStreamOpen },
            execute = { _, args ->
                val value = args.firstOrNull()?.toIntOrNull()
                if (args.isEmpty()) {
                    val limit = PartyApi.partyLimit
                    ChatUtils.chat(
                        if (limit == null) "§eNo party limit is set." else "§eParty limit is set to §b$limit§e.",
                    )
                    return@PartyChatCommand
                }
                if (value == null || value <= 0) {
                    PartyApi.partyLimit = null
                    ChatUtils.chat("§eParty limit removed.")
                } else {
                    PartyApi.partyLimit = value
                    ChatUtils.chat("§eParty limit set to §b$value§e.")
                }
                PartyApi.partyChat("Party limit is now set to: ${PartyApi.partyLimit ?: "no limit"}.", prefix = true)
            },
        ),
        PartyChatCommand(
            listOf("ping"),
            { if (config.pingCommand) PermissionLevel.INSTANT else PermissionLevel.NEVER },
            requiresPartyLead = false,
            execute = { _, _ ->
                if (!CurrentPing.isEnabled()) {
                    ChatUtils.notifyOrDisable(
                        "Ping API is disabled, the ping command won't work!",
                        DevApi.mainToggles::pingApi,
                    )
                    return@PartyChatCommand
                }
                HypixelCommands.partyChat(CurrentPing.getFormattedPing(), prefix = true)
            },
        ),
        PartyChatCommand(
            listOf("tps"),
            { if (config.tpsCommand) PermissionLevel.INSTANT else PermissionLevel.NEVER },
            requiresPartyLead = false,
            execute = { _, _ ->
                TpsCounter.tps?.let { tps ->
                    HypixelCommands.partyChat("Current TPS: %.2f".format(tps), prefix = true)
                } ?: run {
                    ChatUtils.chat("Command sent too early to calculate TPS")
                }
            },
        ),
    )

    private var lastWarp = SimpleTimeMark.farPast()
    private var lastAllInvite = SimpleTimeMark.farPast()

    private val indexedPartyChatCommands = buildMap {
        for (command in allPartyCommands) {
            for (name in command.names) {
                put(name.lowercase(), command)
            }
        }
    }

    // Names of commands that let a user request to join the party (self invite).
    private val selfInviteCommandNames = setOf("inviteme", "request")

    private fun getUserConfig(name: String): PartyCommandsConfig.TrustUserConfig {
        return config.users.get(name) ?: config.TrustUserConfig(name)
    }

    private val commandPrefixes = ".!?".toSet()

    private fun isBlockedUser(name: String): Boolean {
        return storage.blacklistedUsers.any { it.equals(name, ignoreCase = true) }
    }

    /**
     * The current amount of occupied slots, counting both joined members and outstanding invite requests.
     */
    private fun currentOccupancy(): Int {
        return PartyApi.partyMembers.size + PartyApi.pendingInviteCount()
    }

    private fun wouldExceedLimit(additional: Int): Boolean {
        val limit = PartyApi.partyLimit ?: return false
        return currentOccupancy() + additional > limit
    }

    /**
     * Whether the given player is allowed to bypass the party limit. The party leader (self) can always bypass,
     * as can party moderators or anyone with the "bypass party limit" permission.
     */
    private fun canBypassPartyLimit(name: String): Boolean {
        if (name == PlayerUtils.getName()) return true
        if (PartyApi.isModerator(name)) return true
        return getUserConfig(name).effectiveBypassPartyLimit == PermissionLevel.INSTANT
    }

    /**
     * Whether the given player may enable all invites while a party limit is active.
     */
    private fun canEnableAllInviteWhileLimited(name: String): Boolean {
        return canBypassPartyLimit(name)
    }

    private fun permissionLevelFor(name: String, command: PartyChatCommand): PermissionLevel {
        if (name == PlayerUtils.getName()) return PermissionLevel.INSTANT
        return command.canUse(getUserConfig(name))
    }

    private fun notifyPermissionDenied(name: String) {
        if (!config.showIgnoredReminder) return
        ChatUtils.chat(
            "§cIgnoring chat command from $name. " +
                "Change your party chat command settings or /friend (best) them.",
        )
        sendDeniedFeedback(name, "You do not have permission to use party commands.")
    }

    private val deniedNotificationTimes = mutableListOf<SimpleTimeMark>()

    /**
     * Rate limits feedback sent to other players: none within the last second, at most 2 in the last 10 seconds
     * and at most 3 in the last 30 seconds. Only outgoing messages need this to avoid server spam,
     * local chat messages are not rate limited.
     */
    private fun shouldNotifyDenied(): Boolean {
        deniedNotificationTimes.removeAll { it.passedSince() > 30.seconds }
        if (deniedNotificationTimes.isNotEmpty() && deniedNotificationTimes.last().passedSince() < 1.seconds) return false
        if (deniedNotificationTimes.count { it.passedSince() < 10.seconds } >= 2) return false
        if (deniedNotificationTimes.size >= 3) return false
        deniedNotificationTimes.add(SimpleTimeMark.now())
        return true
    }

    /**
     * Sends feedback about a denied command to the requesting player via private message.
     * The message goes to the server chat, so it must not contain any formatting codes (§) or the sender
     * would get kicked, and it is rate limited to avoid outgoing spam.
     */
    private fun sendDeniedFeedback(name: String, message: String) {
        if (!config.showIgnoredReminder) return
        if (!shouldNotifyDenied()) return
        sendPrivateMessage(name, message.removeColor())
    }

    private fun sendPrivateMessage(name: String, message: String) {
        ChatUtils.sendMessageToServer("/msg $name $message")
    }

    @HandleEvent
    fun onPartyCommand(event: PartyChatEvent.Allow) {
        if (event.cleanMessage.firstOrNull() !in commandPrefixes) return
        val commandLabel = event.cleanMessage.substring(1).substringBefore(' ')
        val command = indexedPartyChatCommands[commandLabel.lowercase()] ?: return
        val name = event.authorName
        if (name == PlayerUtils.getName() && (!command.triggerableBySelf)) return
        if (command.requiresPartyLead && !PartyApi.isPartyLeader()) return
        if (isBlockedUser(name)) {
            if (config.showIgnoredReminder) ChatUtils.clickableChat(
                "§cIgnoring chat command from ${event.author}. " +
                    "Stop ignoring them using /shignore remove <player> or click here!",
                onClick = { blacklistModify(event.author) },
                "§eClick to ignore ${event.author}!",
            )
            return
        }
        if (!command.offCooldown.invoke()) return
        val args = event.cleanMessage.substring(1).removePrefix(commandLabel).trim()
            .split(' ')
            .filter { it.isNotEmpty() }
        if (!command.validateArgs(args)) {
            ChatUtils.chat("§cInvalid arguments for !$commandLabel.")
            return
        }
        val level = permissionLevelFor(name, command)
        if (level == PermissionLevel.NEVER) {
            notifyPermissionDenied(name)
            return
        } else if (level == PermissionLevel.INSTANT) {
            command.execute.invoke(event, args)
        } else if (level == PermissionLevel.ASK) {
            ChatUtils.chatPrompt(
                "§e${event.author} wants to run §b!$commandLabel§e. Press §a%KEY%§e to allow it.",
                config.partyCommandPromptKey,
                code = {
                    command.execute.invoke(event, args)
                },
            )
        }
    }

    /**
     * Handles a private message command request. [validate] returns a feedback message for the requester
     * if the command can not run right now, null if it is fine.
     * Commands that leave or disband the party must not be triggerable by the player themselves.
     */
    private fun handlePmCommand(
        name: String,
        commandLabel: String,
        permission: (PartyCommandsConfig.TrustUserConfig) -> PermissionLevel,
        triggerableBySelf: Boolean = true,
        validate: () -> String?,
        action: () -> Unit,
    ) {
        if (name == PlayerUtils.getName() && !triggerableBySelf) return
        val level = if (name == PlayerUtils.getName()) PermissionLevel.INSTANT else permission(getUserConfig(name))
        when (level) {
            PermissionLevel.NEVER -> {
                if (config.showIgnoredReminder) {
                    ChatUtils.chat("§cIgnoring party command request from $name.")
                }
                sendDeniedFeedback(name, "You do not have permission to use !$commandLabel.")
            }
            PermissionLevel.INSTANT -> {
                val error = validate()
                if (error != null) sendDeniedFeedback(name, error) else action()
            }
            PermissionLevel.ASK -> {
                ChatUtils.chatPrompt(
                    "§e$name wants you to run §b!$commandLabel§e. Press §a%KEY%§e to allow it.",
                    config.partyCommandPromptKey,
                    code = {
                        val error = validate()
                        if (error != null) sendDeniedFeedback(name, error) else action()
                    },
                )
            }
        }
    }

    @HandleEvent
    fun onPrivateMessage(event: PrivateMessageChatEvent.Allow) {
        if (event.direction != Direction.INCOMING) return
        val message = event.cleanMessage
        if (message.firstOrNull() !in commandPrefixes) return
        val rest = message.substring(1)
        val commandLabel = rest.substringBefore(' ').lowercase()
        val args = rest.removePrefix(commandLabel).trim().split(' ').filter { it.isNotEmpty() }
        val name = event.author.cleanPlayerName()
        if (isBlockedUser(name)) return

        when (commandLabel) {
            in selfInviteCommandNames -> handlePmCommand(name, commandLabel, { it.effectiveSelfInvite }, validate = {
                if (wouldExceedLimit(1) && !canBypassPartyLimit(name)) {
                    "Party limit reached, cannot invite you."
                } else null
            }) {
                PartyApi.invite(name)
                PartyApi.addPendingInvites(listOf(name))
            }
            "accept" -> {
                val force = args.firstOrNull()?.lowercase() == "force"
                handlePmCommand(
                    name,
                    commandLabel,
                    // Force requires permission for both accepting and leaving the party.
                    { it.effectiveAcceptJoin },
                    triggerableBySelf = !force,
                    validate = {
                        if (PartyApi.isInParty() && !force) {
                            "You are currently in a party, use !accept force so I can leave it first."
                        } else null
                    },
                ) {
                    if (force) PartyApi.leaveParty()
                    PartyApi.acceptParty(name)
                }
            }
            "leave" -> {
                val force = args.firstOrNull()?.lowercase() == "force"
                handlePmCommand(
                    name,
                    commandLabel,
                    { it.effectiveAcceptJoin },
                    triggerableBySelf = !force,
                    validate = { null },
                ) {
                    PartyApi.leaveParty()
                }
            }
            "disband" -> handlePmCommand(
                name,
                commandLabel,
                { it.effectiveTransferLeader },
                triggerableBySelf = false,
                validate = {
                    if (!PartyApi.isPartyLeader()) {
                        "You are not the party leader, cannot disband the party."
                    } else null
                },
            ) {
                PartyApi.disband()
            }
        }
    }

    @HandleEvent
    fun onTabComplete(event: TabCompletionEvent) {
        if (PartyApi.partyLeader == null) return
        val prefix = event.fullText.firstOrNull() ?: return
        if (prefix !in commandPrefixes) return

        val commandText = event.fullText.substring(1)
        indexedPartyChatCommands.keys
            .filter { it.startsWith(commandText) }
            .map { "$prefix$it" }
            .forEach(event::addSuggestion)
    }

    @HandleEvent
    fun onCommandRegister(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtrustedpartyusers") {
            description = "Open the party command user permission editor"
            category = CommandCategory.USERS_ACTIVE
            simpleCallback { PartyUsersGui.open() }
        }

        event.registerBrigadier("shignore") {
            description = "Add/Remove a user from your blacklist"
            category = CommandCategory.USERS_ACTIVE

            literal("add") {
                arg("name", BrigadierArguments.string()) { nameArg ->
                    callback {
                        val name = getArg(nameArg)
                        if (isBlockedUser(name)) {
                            ChatUtils.userError("$name is already ignored!")
                        } else blacklistModify(name)
                    }
                }
            }

            literal("remove") {
                arg("name", BrigadierArguments.string()) { nameArg ->
                    callback {
                        val name = getArg(nameArg)
                        if (!isBlockedUser(name)) {
                            ChatUtils.userError("$name isn't ignored!")
                        } else blacklistModify(name)
                    }
                }
            }
            literal("list") {
                argCallback("name", BrigadierArguments.string()) { name ->
                    blacklistView(name)
                }
                callback {
                    blacklistView()
                }
            }
            literalCallback("clear") {
                ChatUtils.clickableChat(
                    "Are you sure you want to do this? Click here to confirm.",
                    onClick = {
                        storage.blacklistedUsers.clear()
                        ChatUtils.chat("Cleared your ignored players list!")
                    },
                    "§eClick to confirm.",
                    oneTimeClick = true,
                )
            }
            argCallback("name", BrigadierArguments.string()) { name ->
                blacklistModify(name)
            }
        }
    }

    private fun blacklistModify(player: String) {
        if (isBlockedUser(player)) {
            ChatUtils.chat("§aStopped ignoring §b$player§e!")
            storage.blacklistedUsers.removeIf { it.equals(player, ignoreCase = true) }
            return
        }
        ChatUtils.chat("§cNow ignoring §b$player§e!")
        storage.blacklistedUsers.add(player)
    }

    private fun blacklistView() {
        val blacklist = storage.blacklistedUsers
        if (blacklist.isEmpty()) {
            ChatUtils.chat("Your ignored players list is empty!")
            return
        }
        var message = "Ignored player list:"
        if (blacklist.size > 15) {
            message += "\n§e"
            blacklist.forEachIndexed { i, blacklistedMessage ->
                message += blacklistedMessage
                if (i < blacklist.size - 1) {
                    message += ", "
                }
            }
        } else {
            blacklist.forEach { message += "\n§e$it" }
        }
        ChatUtils.chat(message)
    }

    private fun blacklistView(player: String) {
        if (isBlockedUser(player)) {
            ChatUtils.chat("$player §ais §eignored.")
        } else {
            ChatUtils.chat("$player §cisn't §eignored.")
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(95, "misc.partyCommands.defaultRequiredTrustLevel", "misc.partyCommands.requiredTrustLevel")
    }
}
