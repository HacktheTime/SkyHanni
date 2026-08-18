package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.chat.TabCompletionEvent;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Mixin(value = CommandDispatcher.class, remap = false)
public abstract class MixinCommandDispatcher<S> {

    /**
     * Merges the TabCompletionEvent suggestions with the original Brigadier suggestions,
     * so both sources are shown regardless of whether the user is completing the first
     * word or a sub-argument of the command.
     */
    @ModifyReturnValue(method = "getCompletionSuggestions(Lcom/mojang/brigadier/ParseResults;I)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "RETURN"))
    public CompletableFuture<Suggestions> getCompletionSuggestionsWithEvent(CompletableFuture<Suggestions> original, @Local(ordinal = 1) int start, @Local(ordinal = 0) String fullInput, @Local(ordinal = 1) String beforeCursor) {
        return original.thenApply(suggestions -> {
            ArrayList<String> suggestionList = new ArrayList<>(suggestions.getList().stream()
                .map(Suggestion::getText)
                .toList());
            SuggestionsBuilder newSuggestions = buildFromEvent(start, fullInput, beforeCursor, suggestionList);
            if (newSuggestions == null) {
                return suggestions;
            }
            return newSuggestions.build();
        });
    }

    @Unique
    private SuggestionsBuilder buildFromEvent(int start, String fullInput, String beforeCursor, ArrayList<String> existing) {
        TabCompletionEvent tabCompletionEvent = new TabCompletionEvent(beforeCursor, fullInput, existing);
        tabCompletionEvent.post();
        String[] additional = tabCompletionEvent.intoSuggestionArray();
        if (additional == null) return null;

        SuggestionsBuilder suggestionsBuilder = new SuggestionsBuilder(beforeCursor, start);

        for (String s : additional) {
            suggestionsBuilder.suggest(s);
        }
        return suggestionsBuilder;
    }
}