package at.hannibal2.skyhanni.features.bingo.bingonet

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.repo.ChatProgressUpdates
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.hypixel.HypixelJoinEvent
import at.hannibal2.skyhanni.events.minecraft.ClientShutdownEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.constants.StatusConstants
import de.hype.bingonet.shared.objects.SplashData
import kotlinx.coroutines.Job
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.INFINITE

@SkyHanniModule
object DiscordSplashNotificationHook {
    private val config get() = SkyHanniMod.feature.event.bingo.bingoNetworks
    private val dbusStringPattern = Regex(
        pattern = """string\s+\"((?:\\.|[^\"\\])*)\"""",
        options = setOf(RegexOption.DOT_MATCHES_ALL),
    )

    private val progressCategory = ChatProgressUpdates.category("Discord Splash Notification Hook")
    private val ioConfig = CoroutineSettings("discord splash notification hook", timeout = INFINITE).withIOContext()

    private val splashIdCounter = AtomicInteger(-1)

    @Volatile private var workerJob: Job? = null
    @Volatile private var monitorProcess: Process? = null
    @Volatile private var enabledLastTick = false
    @Volatile private var unsupportedMessageSent = false

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        syncLifecycle(force = true)
    }

    @HandleEvent
    fun onConnect(
        event: HypixelJoinEvent
    ){
        syncLifecycle(force = true)
    }

    @HandleEvent
    fun onClientShutdown(event: ClientShutdownEvent) {
        stopHook()
    }

    private fun syncLifecycle(force: Boolean = false) {
        val shouldRun = config.showSplashes && config.useDiscordSplashNotificationHook
        if (!force && shouldRun == enabledLastTick) return
        enabledLastTick = shouldRun
        if (shouldRun) startHook() else stopHook()
    }

    private fun startHook() {
        if (workerJob?.isActive == true) return
        unsupportedMessageSent = false
        val progress = progressCategory.start("start")
        workerJob = with(SkyHanniMod) {
            ioConfig.launchUnScopedCoroutine {
                runHook(progress)
            }
        }
    }

    private suspend fun runHook(progress: ChatProgressUpdates) {
        when (OSUtils.getOperatingSystem()) {
            OSUtils.OperatingSystem.LINUX -> runLinuxMonitor(progress)
            else -> {
                progress.end("unsupported operating system")
                sendUnsupportedMessageOnce()
            }
        }
    }

    private suspend fun runLinuxMonitor(progress: ChatProgressUpdates) {
        try {
            val process = ProcessBuilder(
                "dbus-monitor",
                "--session",
                "type='method_call',interface='org.freedesktop.Notifications',member='Notify'",
            )
                .redirectErrorStream(true)
                .start()
            monitorProcess = process
            progress.end("running")
            ChatUtils.chat("§aDiscord splash notification hook enabled on Linux.")

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            readLinuxNotifyStream(reader)
        } catch (_: Throwable) {
            progress.end("failed to start dbus-monitor")
            ChatUtils.userError(
                "Could not start Discord splash notification hook on Linux. " +
                    "Install dbus-monitor (dbus package) and try again.",
            )
        } finally {
            monitorProcess = null
        }
    }

    private suspend fun readLinuxNotifyStream(reader: BufferedReader) {
        var currentBlock: MutableList<String>? = null
        while (true) {
            val line = reader.readLine() ?: break
            val trimmed = line.trim()
            val isNotifyHeader =
                (trimmed.startsWith("method call ") || trimmed.startsWith("signal ")) &&
                    trimmed.contains("interface=org.freedesktop.Notifications") &&
                    trimmed.contains("member=Notify")
            if (trimmed.startsWith("method call ") || trimmed.startsWith("signal ")) {
                currentBlock?.let { consumeLinuxNotifyBlockSafely(it) }
                currentBlock = if (isNotifyHeader) mutableListOf(trimmed) else null
                continue
            }

            if (trimmed.isBlank()) {
                currentBlock?.let { consumeLinuxNotifyBlockSafely(it) }
                currentBlock = null
                continue
            }

            val block = currentBlock ?: continue
            block.add(line)
        }
        currentBlock?.let { consumeLinuxNotifyBlockSafely(it) }
    }

    private fun consumeLinuxNotifyBlockSafely(lines: List<String>) {
        runCatching { consumeLinuxNotifyBlock(lines) }
    }

    private fun consumeLinuxNotifyBlock(lines: List<String>) {
        val strings = extractDbusStrings(lines)
        if (strings.size < 4) return

        val appName = strings[0]
        if (!appName.contains("discord", ignoreCase = true)) return

        onDiscordNotification(
            DiscordNotification(
                title = strings[2],
                body = strings[3],
            ),
        )
    }

    private fun extractDbusStrings(lines: List<String>): List<String> {
        val block = lines.joinToString("\n")
        return dbusStringPattern.findAll(block)
            .map { it.groupValues[1] }
            .map(::decodeDbusString)
            .toList()
    }

    private fun decodeDbusString(raw: String): String {
        return raw
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
    }

    private fun onDiscordNotification(notification: DiscordNotification) {
        val parsed = DiscordSplashParser.parse(notification) ?: return
        if (isBlacklisted(parsed)) return
        if (isDuplicate(parsed)) return

        val hubSelectorData = SplashData.HubSelectorData(parsed.hubNumber, parsed.island)
        val splashData = SplashData(
            announcer = parsed.announcer,
            locationInHub = null,
            extraMessage = parsed.extraMessage,
            lessWaste = false,
            serverID = parsed.serverId,
            hubSelectorData = hubSelectorData,
            status = StatusConstants.WAITING,
            funder = null,
        ).also {
            it.splashId = -splashIdCounter.decrementAndGet()
        }

        SplashManager.addSplashAndDisplay(splashData, SplashManager.SplashSource.DISCORD_OS)
    }

    private fun isDuplicate(parsed: ParsedDiscordSplash): Boolean {
        return SplashManager.splashPool.any {
            val existing = it.value
            if (existing.receivedTime.isBefore(Instant.now().minus(2, ChronoUnit.MINUTES))) {
                return false
            }
            if (parsed.serverId != null) return parsed.serverId == existing.serverID
            return parsed.hubNumber == existing.hubSelectorData?.hubNumber
        }
    }

    val discordSplashNotificationBlacklist: Set<String> = setOf(
        "BingoNet",
//         "BingoBrewer",//even if its Bingo Brewers using BingoBrewer means its contained anyway.
        //bingo brewers integration is broken rn so allow.
    )

    private fun isBlacklisted(parsed: ParsedDiscordSplash): Boolean {
        val blacklist = discordSplashNotificationBlacklist
            .map { it.lowercase().replace(" ","") }
            .filter { it.isNotEmpty() }
            .toSet()
        if (blacklist.isEmpty()) return false

        val name = parsed.discordServerName?.replace(" ", "")
        return blacklist.any { blocked ->
            name?.contains(blocked) != false
        }
    }

    private fun sendUnsupportedMessageOnce() {
        if (unsupportedMessageSent) return
        unsupportedMessageSent = true
        ChatUtils.userError("Discord splash notification hook is currently only available on Linux in this build.")
    }

    fun stopHook() {
        workerJob?.cancel()
        workerJob = null
        monitorProcess?.destroyForcibly()
        monitorProcess = null
    }
}

private data class DiscordNotification(
    val title: String,
    val body: String,
)

private data class ParsedDiscordSplash(
    val announcer: String,
    val extraMessage: String,
    val serverId: String?,
    val hubNumber: Int,
    val island: Islands,
    val discordServerName: String?,
)

private object DiscordSplashParser {
    private val serverIdPattern = Regex("(?i)\\b(?:mini|mega)\\d+[a-z]\\b")
    private val hubPattern = Regex("(?i)\\b(?:hub|dhub|dungeon\\s*hub)\\s*#?\\s*(\\d{1,2})\\b")
    private val notificationTitleRegex = Regex("(?<author>.+)\u2069 \\(\u2068(?<channel>#[^ ]+)\u2069, (?<guildname>.+)\u2069\\)")

    fun parse(notification: DiscordNotification): ParsedDiscordSplash? {
        val combined = "${notification.title}\n${notification.body}"
        if (!combined.contains("splash")) return null

        val hubNumber = parseHubNumber(combined) ?: return null
        if (hubNumber !in 1..28) return null

        val island = if (
            combined.contains("dungeon hub", ignoreCase = true) ||
            combined.contains("dhub", ignoreCase = true)
        ) {
            Islands.DUNGEON_HUB
        } else {
            Islands.HUB
        }

        val serverId = serverIdPattern.find(combined)?.value?.lowercase()
        val guildName = parseGuildName(notification.title)

        val announcer = parseAnnouncer(notification.title)

        return ParsedDiscordSplash(
            announcer = announcer,
            extraMessage = notification.body,
            serverId = serverId,
            hubNumber = hubNumber,
            island = island,
            discordServerName = guildName,
        )
    }

    private fun parseHubNumber(text: String): Int? {
        return hubPattern.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun parseGuildName(title: String): String? {
        if (title.isBlank()) return null
        val match = notificationTitleRegex.find(title) ?: return null
        return match.groups["guildname"]?.value
    }

    private fun parseAnnouncer(title: String): String {
        if (title.isBlank()) return "Unknown"
        val match = notificationTitleRegex.find(title) ?: return title
        return match.groups["author"]?.value ?: title
    }
}
