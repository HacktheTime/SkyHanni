package at.hannibal2.skyhanni.features.bingo.bingonet

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.storage.StorageApi
import at.hannibal2.skyhanni.api.storage.StorageCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemNameCompact
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import de.hype.bingonet.BNConnection
import de.hype.bingonet.shared.packets.function.SelfOfferPotionDonationsPacket
import kotlin.math.roundToInt

@SkyHanniModule
object SplashHelperUtils {
    val repoPattern = RepoPattern.group("bingo.splashing")
    val donatablePotsPattern by repoPattern.pattern(
        "donatablepots",
        "(.* XP Boost I+ Potion)|(Wisp's Ice-Flavored Water.*)",
    )
    val noTierPattern by repoPattern.pattern(
        "notierpots",
        "\\sI+\\s",
    )
    val requiredPortions: List<NeuInternalName> by lazy {
        ChatUtils.chat("The Neu Repo has no recipies for Potions yet.")
        val allpotions = NeuItems.allItemsCache.filter { it.key.startsWith("POTION") }.values
        val bestPotions = allpotions.groupBy {
            return@groupBy it.internalName.split(";")
        }.map {
            it.value.maxBy { it.internalName.split(";").last().toIntOrNull()?:1 }
        }
        return@lazy bestPotions
    }
    val requiredMaterials: Map<NeuInternalName, Int> by lazy {
        // Recursively flattens all potion ingredients until only non-potion materials remain.
        fun flatPotionMats(internalName: NeuInternalName): Map<NeuInternalName, Double> {
            val recipe = EnoughUpdatesManager.getRecipesFor(internalName).firstOrNull() ?: return emptyMap()

            val flattened: List<Pair<NeuInternalName, Double>> = recipe.ingredients.flatMap { ingredient ->
                val ingName = ingredient.internalName
                val countMultiplier = ingredient.count.toDouble()
                if (ingName.internalName.contains("POTION", ignoreCase = true)) {
                    val nested = flatPotionMats(ingName)
                    if (nested.isEmpty()) emptyList() else nested.map { (k, v) -> k to (v * countMultiplier) }
                } else {
                    listOf(ingName to countMultiplier)
                }
            }

            // Merge duplicates inside this recipe expansion
            return flattened.groupBy { it.first }.mapValues { (_, list) -> list.sumOf { it.second } }
        }

        // Aggregate across all required potion entries
        val aggregated: Map<NeuInternalName, Double> = requiredPortions
            .map { flatPotionMats(it) }
            .flatMap { it.entries }
            .groupBy { it.key }
            .mapValues { (_, list) -> list.sumOf { it.value } }

        // Convert to Int counts (rounding). Adjust if floor/ceil behavior is desired instead.
        return@lazy aggregated.mapValues { (_, v) -> v.roundToInt() }.also {
            if (it.isEmpty()) ChatUtils.chat("The Neu Repo has no recipies for Potions yet.")
        }
    }

    fun priceForSplashes(
        sets: Int = 1,
    ) {
        val instantBuy = requiredMaterials.map { it.key.getPrice(ItemPriceSource.BAZAAR_INSTANT_BUY) * it.value }.sum()
        val buyOrder = requiredMaterials.map { it.key.getPrice(ItemPriceSource.BAZAAR_INSTANT_SELL) * it.value }.sum()
        ChatUtils.chat(
            "Cost for 3 Splashes: \n§aInstant Buy: §6${
                String.format(
                    "%,.1f",
                    instantBuy,
                )
            } coins\n§aBuy Order: §6${String.format("%,.1f", buyOrder)} coins",
        )
    }

    @HandleEvent
    fun registerCommand(event: CommandRegistrationEvent) {
        event.registerBrigadier("splashutils") {
            description = "Utilities for Bingo Splashing."
            literal("cost") {
                argCallback("sets", BrigadierArguments.integer(min = 1)) {
                    priceForSplashes(it)
                }
                simpleCallback {
                    priceForSplashes()
                }
            }

            literal("required-materials") {
                argCallback("potions", BrigadierArguments.integer(min = 1)) {
                    val builder = StringBuilder()
                    val sets: Int
                    if (it % 3 == 0) {
                        sets = it / 3
                        builder.append("Materials for $sets sets of 3 Potions → ${it} Potions:\n")
                    } else {
                        sets = (it / 3) + 1
                        builder.append("Materials for $sets sets of 3 Potions → ${sets * 3} Potions:\n")
                        builder.append(
                            "§c- Note your count is not devisable by 3. For efficiency purposes we upped it to next 3 " +
                                "devisable number.\n§e",
                        )
                    }
                    requiredMaterials.forEach {
                        builder.append("§6${it.key.repoItemName}§e: §a${it.value * sets}\n")
                    }
                }
            }

            literal("donate") {
                simpleCallback {
                    run {
                        ChatUtils.chat("Scanning inventories...")
                        val results = StorageApi.search().filter { item, _, _ ->
                            return@filter donatablePotsPattern.matches(item.displayName.removeColor())
                        }.getResults()
                        val builder = StringBuilder()
                        builder.append("Found §6${results.size}§e donatable potions:\n§r")
                        results.groupBy { it.item.displayName.replace(Regex(" I+ "), "").removeColor() }.forEach { group, values ->
                            if (values.isEmpty()) return@forEach
                            val name = values.first().item.displayName.replace(noTierPattern.pattern(), "")
                            builder.append(
                                " §7-§r ${name}§r §7x§a${values.count()}§r\n",
                            )
                        }
                        if (results.isNotEmpty()) {
                            builder.append("----------------------------------\n")
                            builder.append("Inventory locations:\n")
                            ChatUtils.chat(builder.toString())
                            results.groupBy {
                                if (it.category == StorageCategory.PRIVATE_ISLAND_CHEST)
                                    return@groupBy "Private Island Chest(s)"
                                else return@groupBy it.storageName
                            }.forEach {
                                ChatUtils.clickableChat(
                                    it.key,
                                    onClick = {
                                        it.value.firstOrNull()?.runAccess(true)
                                    },
                                    hover = it.value.joinToString("\n") {
                                        it.item.displayName + "§r x§a" + it.item.stackSize
                                    },
                                )
                            }
                            ChatUtils.clickableChat(
                                "§6To clear the results run /searchitem or click here",
                                {
                                    StorageApi.toHighlightResults.clear()
                                },
                            )
                            if (SkyHanniMod.feature.event.bingo.bingoNetworks.bingoNet.useBN && results.size>30) ChatUtils.clickableChat(
                                "§cClick here to send a offer Broadcast for these Potions to Splashers over Bingo Net. " +
                                    "(THIS IS A COMMITMENT!)",
                                {
                                    ChatUtils.clickableChat(
                                        "§cAre you sure? Click again to confirm.",
                                        {
                                            BNConnection.sendPacket(
                                                SelfOfferPotionDonationsPacket(
                                                    results.groupBy { it.item.getInternalName() }
                                                        .mapValues { it.value.sumOf { it.item.stackSize } },
                                                ),
                                            )
                                            ChatUtils.chat(
                                                "§aSent offer broadcast to Splashers via Bingo Net. If there is interest from a " +
                                                    "Splasher we will let you know. May come from either the System or the Splasher itself directly. " +
                                                    "If there is no immediate contact (Like 1 Minute) do something else in the mean time.",
                                            )
                                        },
                                    )
                                },
                            )
                        } else ChatUtils.chat(builder.toString())
                    }
                }
            }

            literal("offer-donation") {
                argCallback("extramessage", BrigadierArguments.greedyString()) {
                    //If user has not enabled BN they will be prompted from the connect method automatically.
                    run {
                        val results = StorageApi.search().filter { item, _, _ ->
                            return@filter donatablePotsPattern.matches(item.displayName.removeColor())
                        }.getResults().groupBy {
                            it.item.getInternalName()
                        }.mapValues { it.value.sumOf { it.item.stackSize } }
                        if (results.values.sum()<=30){
                            ChatUtils.chat("§cYou should only offer your potions if you have more than 30 potions to donate. " +
                                "Otherwise it is not worth the effort.")
                            return@run
                        }
                        BNConnection.sendPacket(
                            SelfOfferPotionDonationsPacket(
                                results,
                                it,
                            ),
                        )
                    }
                }
                simpleCallback {
                    ChatUtils.clickableChat(
                        "§cAre you sure? Click this to confirm. (THIS IS A COMMITMENT!)",
                        {
                            run {
                                val results = StorageApi.search().filter { item, _, _ ->
                                    return@filter donatablePotsPattern.matches(item.displayName.removeColor())
                                }.getResults().groupBy {
                                    it.item.getInternalName()
                                }.mapValues { it.value.sumOf { it.item.stackSize } }
                                if (results.values.sum() <= 30) {
                                    ChatUtils.chat(
                                        "§cYou should only offer your potions if you have more than 30 potions to donate. " +
                                            "Otherwise it is not worth the effort.",
                                    )
                                    return@run
                                }
                                BNConnection.sendPacket(
                                    SelfOfferPotionDonationsPacket(
                                        results,
                                    ),
                                )
                            }
                        })
                }
            }

            literal("current-materials") {
                simpleCallback {
                    materials()
                }
            }
            literal("missing-materials"){
                argCallback("sets", BrigadierArguments.integer(min = 1)) {
                    materials(it)
                }
            }
        }
    }

    fun materials(sets: Int? = null) {
        run {
            ChatUtils.chat("§eAnalyzing... This may take a few seconds.")
            val neededForOneSet = requiredMaterials
            val itemSet = neededForOneSet.keys.toHashSet()
            val results = StorageApi.search().filter { item, _, _ ->
                val itemKey = item.getInternalName()
                return@filter itemSet.contains(itemKey)
            }.getResultsNoHighlight()
            val inStorage = results.groupBy { it.item.getInternalName() }.mapValues {
                it.value.sumOf { it.item.stackSize }
            }
            val builder = StringBuilder()
            var minimum = Int.MAX_VALUE
            val missingMaterials: MutableMap<NeuInternalName, Int> = mutableMapOf()
            neededForOneSet.entries.forEach { (key, value) ->
                val have = inStorage[key] ?: 0
                if (sets == null) {
                    val canMake = have / value
                    if (canMake < minimum) minimum = canMake
                    builder.append(" §7-§r ${key.repoItemName} §7x§a$have → $canMake Sets → ${canMake * 3} Potions\n")
                } else {
                    val need = value * sets
                    val missing = if (have >= need) 0 else need - have
                    if (missing != 0) missingMaterials[key] = missing
                    //Output later via clickable link
                    //TODO modify the garden visitor shopping list to do same for materials
                }
            }
            builder.append("----------------------------------\n")
            builder.append("§aYou can make §6$minimum§a sets (→ ${minimum * 3} potions) with your current materials.\n")
            if (sets == null) {
                val instaSellCost = inStorage.map {
                    (it.key.getPriceOrNull(ItemPriceSource.BAZAAR_INSTANT_SELL) ?: 0.toDouble()) * it.value.toDouble()
                }
                val sellOfferCost = inStorage.map {
                    (it.key.getPriceOrNull(ItemPriceSource.BAZAAR_INSTANT_BUY) ?: 0.toDouble()) * it.value.toDouble()
                }
                builder.append("These ingredients come to a Market Value of roughly (only bazaar listed items): \n")
                builder.append("§6${String.format("%,.1f", instaSellCost.sum())} coins§a if sold instantly via Bazaar.\n")
                builder.append("§6${String.format("%,.1f", sellOfferCost.sum())} coins§a if sold via Sell Offer in Bazaar.\n")
            } else {
                missingMaterials.map {
                    (it.key.getPriceOrNull(ItemPriceSource.BAZAAR_INSTANT_SELL) ?: 0.toDouble()) * it.value.toDouble()
                }
                missingMaterials.map {
                    (it.key.getPriceOrNull(ItemPriceSource.BAZAAR_INSTANT_BUY) ?: 0.toDouble()) * it.value.toDouble()
                }
                if (missingMaterials.isEmpty()){
                    ChatUtils.chat("§aYou have all materials needed to make §6$sets§a sets (→ ${sets*3} Potions) potions.")
                }else{
                    ChatUtils.chat("§cTo make §6$sets§c sets (→ ${sets*3} Potions) you are missing the following materials:")
                    missingMaterials.forEach {
                        ChatUtils.clickableChat(
                            "§7-§r ${it.key.repoItemName} §7x§c${it.value}",
                            {
                                HypixelCommands.bazaar(it.key.repoItemNameCompact.removeColor())
                            },
                            "Click to open chests containing this item.",
                        )
                    }
                }
                ChatUtils.chat("§eYou ")
            }
            ChatUtils.chat(builder.toString())
        }
    }
}

