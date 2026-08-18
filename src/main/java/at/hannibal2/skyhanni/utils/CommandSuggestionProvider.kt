package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.config.commands.CommandsRegistry
import at.hannibal2.skyhanni.events.chat.TabCompletionEvent
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.ParseResults
import com.mojang.brigadier.suggestion.Suggestions
import net.minecraft.client.Minecraft
import java.util.concurrent.CompletableFuture

/**
 * Provides merged Brigadier command suggestions from both the client-side (SkyHanni / SHS) dispatcher
 * and the current server dispatcher (if available). Accepts a raw input line and a cursor position inside it.
 *
 * Behaviors:
 *  - Leading slash preserved only for first token suggestions.
 *  - Cursor-aware: parses only substring up to cursor first; falls back to full line parse if needed.
 *  - Merges client + server suggestions in insertion order, removing duplicates.
 *  - Fires [TabCompletionEvent] directly for the input as typed, and additionally for a slash-prefixed
 *    variant when the user typed without a leading slash, so command listeners that require a "/"
 *    prefix (TabComplete, WarpTabComplete, PartyChatCommands, ...) also contribute suggestions.
 */
object CommandSuggestionProvider {

    fun suggest(input: String, cursor: Int = input.length): List<String> {
        if (input.isBlank()) return emptyList()
        val mcPlayer = try { Minecraft.getInstance().player } catch (_: Throwable) { null } ?: return emptyList()
        val client: CommandDispatcher<Any> = try { CommandsRegistry.getDispatcher() as CommandDispatcher<Any> } catch (_: Throwable) { return emptyList() }
        val server: CommandDispatcher<Any>? = try { CommandsRegistry.mcServerDispatcher() } catch (_: Throwable) { null }

        val effectiveCursor = cursor.coerceIn(0, input.length)
        val leadingSlash = input.startsWith('/')

        // Fire the tab-completion event first (synchronous dispatch). Fire it for the input as typed
        // and, when there is no leading slash, also for a slash-prefixed variant so slash-requiring
        // listeners still see it. Each event is created with an empty existing list so the merged
        // event suggestions are exactly what the listeners contributed.
        val eventSuggestions = LinkedHashSet<String>()
        fun fireEvent(text: String, fullText: String) {
            val event = TabCompletionEvent(text, fullText, emptyList())
            event.post()
            event.intoSuggestionArray()?.let { eventSuggestions.addAll(it) }
        }
        fireEvent(input.take(effectiveCursor), input)
        if (!leadingSlash) {
            fireEvent("/" + input.take(effectiveCursor), "/$input")
        }

        val rawFull = if (leadingSlash) input.drop(1) else input
        val rawCursor = rawFull.take((if (leadingSlash) effectiveCursor - 1 else effectiveCursor).coerceAtLeast(0).coerceAtMost(rawFull.length))

        fun collect(dispatcher: CommandDispatcher<Any>, partial: String): CompletableFuture<Suggestions> = try {
            val parse: ParseResults<Any> = dispatcher.parse(partial, mcPlayer)
            dispatcher.getCompletionSuggestions(parse)
        } catch (_: Throwable) { Suggestions.empty() }

        fun applyLeadingSlash(list: Collection<String>): List<String> {
            if (!leadingSlash) return list.toList()
            val firstTokenPhase = !rawCursor.contains(' ')
            return list.map { if (firstTokenPhase && !it.startsWith('/')) "/$it" else it }
        }

        val clientFuture = collect(client, rawCursor)
        val serverFuture = server?.let { collect(it, rawCursor) }
        val brigadierMerged = LinkedHashSet<String>().apply {
            runCatching { clientFuture.get().list }.getOrElse { emptyList() }.forEach { add(it.text) }
            runCatching { serverFuture?.get()?.list ?: emptyList() }.getOrElse { emptyList() }.forEach { add(it.text) }
        }
        if (brigadierMerged.isEmpty()) {
            val clientFull = collect(client, rawFull)
            val serverFull = server?.let { collect(it, rawFull) }
            runCatching { clientFull.get().list }.getOrElse { emptyList() }.forEach { brigadierMerged.add(it.text) }
            runCatching { serverFull?.get()?.list ?: emptyList() }.getOrElse { emptyList() }.forEach { brigadierMerged.add(it.text) }
        }

        // Merge event suggestions first (they are the direct answer to what the user typed),
        // then the brigadier completions as a fallback/supplement.
        val merged = LinkedHashSet<String>(eventSuggestions)
        brigadierMerged.forEach { merged.add(it) }
        return applyLeadingSlash(merged)
    }
}