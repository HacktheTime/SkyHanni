package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName

@SkyHanniModule
object CompactSplashPotionMessage {

    private val config get() = SkyHanniMod.feature.chat.compactPotionMessages

    val selfSplashPattern =
        "BUFF! You splashed yourself with (?<effectName>.*)! Press TAB or type /effects to view your active effects!".toPattern()

    @Suppress("MaxLineLength")
    private val potionEffectPatternList = listOf(
        "BUFF! You were splashed by (?<playerName>.*) with (?<effectName>.*)! Press TAB or type /effects to view your active effects!".toPattern(),
        "BUFF! You have gained (?<effectName>.*)! Press TAB or type /effects to view your active effects!".toPattern(),
        selfSplashPattern,

        // Fix for Hypixel having a different message for Poisoned Candy.
        // Did not make the first pattern optional to prevent conflicts with Dungeon Buffs/other things
        "BUFF! You have gained (?<effectName>Poisoned Candy I)!".toPattern(),
        "BUFF! You splashed yourself with (?<effectName>Poisoned Candy I)!".toPattern(),
        "BUFF! You were splashed by (?<playerName>.*) with (?<effectName>Poisoned Candy I)!".toPattern(),
    )

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        if (!event.cleanMessage.isPotionMessage()) return
        event.blockedReason = "compact_potion_effect"
    }

    private fun sendMessage(message: String) {
        if (config.clickableChatMessage) {
            ChatUtils.hoverableChat(
                message,
                listOf("Click to view your potion effects."),
                "/effects",
                prefix = false,
            )
        } else {
            ChatUtils.chat(message, prefix = false)
        }
    }

    private fun String.isPotionMessage(): Boolean {
        return potionEffectPatternList.any {
            it.matchMatcher(this) {
                val effectName = group("effectName")
                // If splashed by a player, append their name.
                val byPlayer = groupOrNull("playerName")?.let { player ->
                    val displayName = player.cleanPlayerName(displayName = true)
                    " by $displayName"
                }.orEmpty()
                sendMessage("Potion Effect! $effectName$byPlayer")
            } != null
        }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}
