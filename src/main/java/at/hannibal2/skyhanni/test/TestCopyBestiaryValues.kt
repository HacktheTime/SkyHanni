package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullOwner
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.File
import java.util.Locale

@SkyHanniModule
object TestCopyBestiaryValues {
// <editor-fold desc="Brackets">

    private val brackets = mapOf(
        1 to intArrayOf(
            20,
            40,
            60,
            100,
            200,
            400,
            800,
            1400,
            2000,
            3000,
            6000,
            12000,
            20000,
            30000,
            40000,
            50000,
            60000,
            72000,
            86000,
            100000,
            200000,
            400000,
            600000,
            800000,
            1000000,
        ),
        2 to intArrayOf(
            5,
            10,
            15,
            25,
            50,
            100,
            200,
            350,
            500,
            750,
            1500,
            3000,
            5000,
            7500,
            10000,
            12500,
            15000,
            18000,
            21500,
            25000,
            50000,
            100000,
            150000,
            200000,
            250000,
        ),
        3 to intArrayOf(
            4,
            8,
            12,
            16,
            20,
            40,
            80,
            140,
            200,
            300,
            600,
            1200,
            2000,
            3000,
            4000,
            5000,
            6000,
            7200,
            8600,
            10000,
            20000,
            40000,
            60000,
            80000,
            100000,
        ),
        4 to intArrayOf(
            2,
            4,
            6,
            10,
            15,
            20,
            25,
            35,
            50,
            75,
            150,
            300,
            500,
            750,
            1000,
            1350,
            1650,
            2000,
            2500,
            3000,
            5000,
            10000,
            15000,
            20000,
            25000,
        ),
        5 to intArrayOf(1, 2, 3, 5, 7, 10, 15, 20, 25, 30, 60, 120, 200, 300, 400, 500, 600, 720, 860, 1000, 2000, 4000, 6000, 8000, 10000),
        6 to intArrayOf(1, 2, 3, 5, 7, 9, 14, 17, 21, 25, 50, 80, 125, 175, 250, 325, 425, 525, 625, 750, 1500, 3000, 4500, 6000, 7500),
        7 to intArrayOf(1, 2, 3, 5, 7, 9, 11, 14, 17, 20, 30, 40, 55, 75, 100, 150, 200, 275, 375, 500, 1000, 1500, 2000, 2500, 3000),
        8 to intArrayOf(1, 2, 3, 4, 5, 8, 11, 14, 17, 20, 25, 30, 35, 40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 180, 200),
    )

    private val critterBrackets = mapOf(
        1 to intArrayOf(1, 5, 10, 20, 35, 50, 65, 85, 105, 125),
        2 to intArrayOf(1, 5, 10, 15, 25, 40, 55, 70, 85, 100),
        3 to intArrayOf(1, 5, 10, 15, 20, 25, 35, 45, 60, 75),
        4 to intArrayOf(1, 3, 6, 10, 15, 20, 25, 30, 40, 50),
        5 to intArrayOf(1, 2, 3, 4, 6, 8, 10, 15, 20, 25),
    )

    // </editor-fold>

    class MobEntry {
        @Expose var name: String = ""
        @Expose var skullOwner: String = ""
        @Expose var texture: String = ""
        @Expose var cap: Int = 0
        @Expose var mobs: List<String> = emptyList()
        @Expose var bracket: Int = 0
        var panorama: String = ""
    }

    class BestiaryObject {
        @Expose var name: String = ""
        @Expose var skullOwner: String = ""
        @Expose var texture: String = ""
        @Expose var cap: Int = 0
        @Expose var mobs: List<String> = emptyList()
        @Expose var bracket: Int = 0
        @Expose var bracketType: String? = null
    }

    class MobRecipe(
        @Expose val coins: Int,
        @Expose @SerializedName("xp") val orbXp: Int,
        @Expose val level: Int,
        @Expose val name: String,
        @Expose val panorama: String,
        @Expose val render: String,
        @Expose val type: String = "drops",
        @Expose val extra: List<String> = emptyList(),
        @Expose @SerializedName("mob_types") val mobTypes: List<String>,
        @Expose @SerializedName("api_id") val apiId: String,
        @Expose val drops: List<MobDrop>,
        @Expose val health: Int = 0,
        @Expose val damage: Int = 0,
        @Expose @SerializedName("magic_resistance") val magicResistance: Int = 0,
        var skillXps: MutableMap<String, Int> = mutableMapOf(),
    )

    class MobDrop(
        @Expose val id: String,
        @Expose val chance: String,
    )

    // TODO add regex test
    @Suppress("RepoPatternRegexTestMissing")
    private val bestiaryTypePattern by RepoPattern.pattern(
        "test.bestiary.type",
        "(?:§[0-9a-fk-or])*\\[(?:§[0-9a-fk-or])*Lv(?<lvl>\\d+)(?:§[0-9a-fk-or])*] (?:§r)?(?<text>§.[a-zA-Z ]*) ?.*",
    )

    private val romanNumeralRegex = Regex("\\s+[IVXLCDM]+$")
    private val romanToIntMap = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)

    private fun romanToInt(roman: String): Int {
        var result = 0
        var prev = 0
        for (c in roman.reversed()) {
            val curr = romanToIntMap[c] ?: 0
            if (curr < prev) result -= curr else result += curr
            prev = curr
        }
        return result
    }

    private fun resolveEnchantedBookId(enchantInfo: String): String? {
        val match = romanNumeralRegex.find(enchantInfo) ?: return null
        val enchantType = enchantInfo.replace(romanNumeralRegex, "").trim().uppercase().replace(" ", "_")
        val levelInt = romanToInt(match.value.trim())
        val possibleId = "$enchantType;$levelInt"
        return if (EnoughUpdatesManager.getItemById(possibleId) != null) possibleId else null
    }

    private const val PAGE_SIZE = 28

    private fun findBracket(rawCap: Int, capTier: Int, isCritter: Boolean): Int {
        val map = if (isCritter) critterBrackets else brackets
        val index = capTier - 1
        for ((bracketNum, arr) in map) {
            if (index in arr.indices && arr[index] == rawCap) {
                return bracketNum
            }
        }
        ChatUtils.chat("no bracket found for rawCap=$rawCap, capTier=$capTier, isCritter=$isCritter")
        return 0
    }

    @HandleEvent(priority = HandleEvent.LOW)
    private fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!DevApi.config.debug.copyBestiaryData && !DevApi.config.debug.inlineReplaceBestiaryData) return
        val backItem = event.inventoryItems[3 + 9 * 5 + 3] ?: return
        if (backItem.getLore().none { it.contains("Bestiary Milestone") }) return
        val rankingItem = event.inventoryItems[3 + 9 * 5 + 2] ?: return
        if (rankingItem.getLore().none { it.contains("Ranking") }) return
        val titleItem = event.inventoryItems[4] ?: return
        process(titleItem, event.inventoryName, event.inventoryItems)
    }

    private fun parseCap(value: String): Int {
        val clean = value.removeColor().trim().lowercase()
        return when {
            clean.endsWith("k") -> ((clean.removeSuffix("k").toDoubleOrNull() ?: 0.0) * 1000).toInt()
            clean.endsWith("m") -> ((clean.removeSuffix("m").toDoubleOrNull() ?: 0.0) * 1000000).toInt()
            else -> clean.toIntOrNull() ?: 0
        }
    }

    private fun generateApiId(name: String, lvl: Int, master: Boolean): String {
        val cleanName = name.removeColor().lowercase().replace(" ", "_")
        val masterPrefix = if (master) "master_" else ""
        return "$masterPrefix${cleanName}_$lvl"
    }

    private fun generateRepoId(name: String, suffix: String): String {
        val cleanName = name.replace(romanNumeralRegex, "")
        return "${cleanName.uppercase().replace(" ", "_")}$suffix"
    }

    private fun parseChance(chance: String): String {
        val cleanChance = chance.removeColor().lowercase().trim()
        if (!cleanChance.contains("/")) return chance
        val parts = cleanChance.split("/")
        val numerator = parts[0].toDoubleOrNull() ?: 1.0
        val denominatorStr = parts[1]
        val denominator = when {
            denominatorStr.endsWith("m") -> denominatorStr.removeSuffix("m").toDoubleOrNull()?.let { it * 1_000_000 }
            denominatorStr.endsWith("k") -> denominatorStr.removeSuffix("k").toDoubleOrNull()?.let { it * 1_000 }
            else -> denominatorStr.toDoubleOrNull()
        } ?: return chance
        return if (denominator != 0.0) "${(numerator / denominator * 100).format(6)}%" else chance
    }

    private fun Double.format(digits: Int): String {
        val formatted = String.format(Locale.US, "%.${digits}f", this)
        if (this == 0.0) return "0"
        return formatted.trimEnd('0').trimEnd('.')
    }

    private val accumulatedRecipes = mutableMapOf<String, MutableList<MobRecipe>>()
    private val accumulatedMobEntries = mutableMapOf<String, MutableList<MobEntry>>()
    private val accumulatedObj = mutableMapOf<String, BestiaryObject>()
    private val processedPages = mutableMapOf<String, MutableSet<Int>>()

    private fun process(titleItem: SafeItemStack, guiScreenName: String, inventoryItems: Map<Int, SafeItemStack>) {
        val fullTitle = titleItem.hoverName.formattedTextCompatLeadingWhiteLessResets()
        val titleName = fullTitle.substringBefore(" ➜").trim()
        val cleanTitle = titleName.removeColor().trim()

        val obj = BestiaryObject().apply {
            name = titleName
            texture = titleItem.getSkullTexture() ?: "no texture found"
            skullOwner = titleItem.getSkullOwner() ?: "no skullOwner found"
        }

        val lore = titleItem.getLore()
        val overallProgress = lore.find { it.contains("Overall Progress") }
        if (overallProgress != null) {
            val capLine = lore.nextAfter(overallProgress)
            if (capLine != null) {
                obj.cap = parseCap(capLine.substringAfter("/"))
            }
        }
        lore.find { it.contains("Capped at Tier") }?.let {
            obj.bracket = it.substringAfter("Capped at Tier").removeColor().trim().toIntOrNull() ?: 0
        }

        val bestiaryJsonFile = File(EnoughUpdatesManager.repoDirectory, "constants/bestiary.json")
        val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
        val root = if (bestiaryJsonFile.exists()) gson.fromJson(bestiaryJsonFile.readText(), JsonObject::class.java) else null

        var panorama = "hub"
        var targetCategoryKey: String? = null

        if (guiScreenName.contains("➜")) {
            val categorySearch = guiScreenName.substringBefore("➜").trim()
            val toCheck = root?.entrySet()?.filter { it.key != "brackets" }
            toCheck?.forEach { (key, value) ->
                val catObj = value.asJsonObject
                val catName = (catObj.get("name")?.asString ?: "").removeColor()
                val hasSubcategories = catObj.get("hasSubcategories")?.asBoolean ?: false
                if (hasSubcategories) {
                    catObj.entrySet().forEach { (subKey, subValue) ->
                        if (subKey != "name" && subKey != "icon" && subKey != "hasSubcategories") {
                            val subCatObj = subValue.asJsonObject
                            val subCatName = (subCatObj.get("name")?.asString ?: "").removeColor()
                            if (subCatName.equals(categorySearch, ignoreCase = true)) {
                                panorama = key
                                targetCategoryKey = "$key.$subKey"
                            }
                        }
                    }
                } else {
                    if (catName.equals(categorySearch, ignoreCase = true)) {
                        panorama = key
                        targetCategoryKey = key
                    }
                }
            }
        }

        val bottomSlots = (45..53).mapNotNull { inventoryItems[it] }
        fun hasPageText(text: String) = bottomSlots.any {
            it.cleanName.contains(text) || it.getLore().any { l -> l.contains(text) }
        }

        val hasPrevPage = hasPageText("Previous Page")
        val hasNextPage = hasPageText("Next Page")
        val currentPage =
            bottomSlots.mapNotNull {
                it.getLore().firstNotNullOfOrNull { l -> Regex("Page (\\d+)").find(l)?.groupValues?.get(1)?.toIntOrNull() }
            }.firstOrNull() ?: 1
        val hasPagination = hasPrevPage || hasNextPage

        val mobsIds = mutableListOf<String>()
        val mobEntries = mutableListOf<MobEntry>()
        val recipes = mutableListOf<MobRecipe>()

        for (i in 10..43) {
            if (mobsIds.size >= PAGE_SIZE) break
            val stack = inventoryItems[i] ?: continue
            bestiaryTypePattern.matchMatcher(stack.cleanName) {
                val lvl = group("lvl").formatInt()
                val textGroup = group("text")
                val master = textGroup.lowercase().contains("(master)")
                val cleanBaseName = if (master) textGroup.substringBeforeLast(" (").trim() else textGroup.trim()
                val apiId = generateApiId(cleanBaseName, lvl, master)
                mobsIds.add(apiId)

                val entry = mobEntries.find { it.name == cleanBaseName && it.panorama == panorama } ?: MobEntry().apply {
                    name = cleanBaseName
                    texture = stack.getSkullTexture() ?: ""
                    skullOwner = stack.getSkullOwner() ?: ""
                    mobEntries.add(this)
                }
                entry.mobs = entry.mobs + apiId
                val loreLines = stack.getLore()
                val render = stack.getSkullTexture() ?: "minecraft:player_head"
                val coins =
                    loreLines.find { it.contains("Coins per Kill:") }?.removeColor()?.replace(Regex("[^\\d]"), "")?.toIntOrNull() ?: 0
                val health = loreLines.find { it.contains("Health:") }?.removeColor()?.replace(Regex("[^\\d]"), "")?.toIntOrNull() ?: 0
                val damage = loreLines.find { it.contains("Damage:") }?.removeColor()?.replace(Regex("[^\\d]"), "")?.toIntOrNull() ?: 0
                val magicResistance =
                    loreLines.find { it.contains("Magic Resistance:") }?.removeColor()?.replace(Regex("[^\\d]"), "")?.toIntOrNull() ?: 0
                val xpLine = loreLines.find { it.contains(" Exp:") }
                val xpTypeName = xpLine?.substringBefore(" Exp:")?.removeColor()?.trim() ?: "Combat"
                val skillXp = xpLine?.substringAfter(" Exp:")?.removeColor()?.replace(Regex("[^\\d]"), "")?.toIntOrNull() ?: 0
                val orbXp =
                    loreLines.find { it.contains("XP Orbs:") }?.removeColor()?.substringAfter("XP Orbs:")?.replace(",", "")
                        ?.replace(" ", "")?.toIntOrNull() ?: 0
                val mobTypes =
                    loreLines.find { it.contains("Mob Types?:".toRegex()) }?.substringAfter(":")?.split(",")
                        ?.map { it.removeColor().trim().uppercase().replace(Regex("[^A-Z]"), "") } ?: emptyList()

                val drops = mutableListOf<MobDrop>()
                var inDrops = false
                for (line in loreLines) {
                    if (line.contains("Loot") && !line.contains("■")) {
                        inDrops = true; continue
                    }
                    if (inDrops && line.contains("■")) {
                        val dropLineRaw = line.substringAfter("■").trim()
                        val dropLine = dropLineRaw.removeColor()
                        val nameWithQty = dropLine.substringBeforeLast("(").trim()
                        val qtyMatch = Regex("x(\\d+)(?:-(\\d+))?").find(nameWithQty)
                        val cleanDropName = nameWithQty.replace(Regex("x\\d+(?:-\\d+)?"), "").trim()
                        val chanceRaw = dropLine.substringAfterLast("(").substringBeforeLast(")").trim()

                        var id: String? = null
                        val parenIdx = cleanDropName.indexOf("(")
                        if (parenIdx >= 0) {
                            val enchantInfo = cleanDropName.substring(parenIdx + 1, cleanDropName.length - 1).trim()
                            id = resolveEnchantedBookId(enchantInfo)
                        }
                        if (id == null) id = resolveIdByDisplayName(cleanDropName)
                        if (id == null) {
                            val baseName = cleanDropName.substringBefore("(").trim()
                            id = resolveIdByDisplayName(baseName) ?: "${cleanDropName.uppercase().replace(" ", "_")} TODO"
                        }
                        var finalChance = parseChance(chanceRaw)

                        if (lore.any { it.contains("Critter") }) {
                            obj.bracketType = "CRITTERS"
                        }

        val capTier = if (overallProgress.contains("100%")) {
            titleItem.hoverName.string.substringAfterLast(" ").romanToDecimalIfNecessary()
        } else {
            lore.firstOrNull { it.contains("Capped at Tier") }
                ?.substringAfter("Capped at Tier ")
                ?.formatIntOrNull() ?: 0
        }

        if (capTier == 0) {
            ChatUtils.chat("§cNo capTier found for $titleName, bracket will not be set!")
        } else {
            obj.bracket = findBracket(rawCap, capTier, obj.bracketType == "CRITTERS")
        }

                        if (qtyMatch != null) {
                            val min = qtyMatch.groupValues[1]
                            val max = qtyMatch.groupValues.getOrNull(2)
                            if (max != null && max.isNotEmpty()) {
                                finalChance = "x$min-$max"
                            } else if (min != "1") {
                                id = "$id:$min"
                                finalChance = "100%"
                            }
                        }
                        if (finalChance == cleanDropName) finalChance = "100%"
                        drops.add(MobDrop(id, finalChance))
                    }
                }

                val recipe = MobRecipe(
                    coins = coins, orbXp = orbXp, level = lvl,
                    name = textGroup, panorama = panorama, render = render, mobTypes = mobTypes, apiId = apiId, drops = drops,
                    health = health, damage = damage, magicResistance = magicResistance,
                )
                recipe.skillXps["${xpTypeName.lowercase()}_xp"] = skillXp
                recipes.add(recipe)
            }
        }
        obj.mobs = mobsIds

        if (hasPagination) {
            val cacheKey = "$cleanTitle|$targetCategoryKey"
            val seen = processedPages.getOrPut(cacheKey) { mutableSetOf() }
            if (currentPage in seen) return
            seen.add(currentPage)
            if (!hasPrevPage) {
                processedPages[cacheKey] = mutableSetOf(currentPage)
                accumulatedRecipes[cacheKey] = recipes.toMutableList()
                accumulatedMobEntries[cacheKey] = mobEntries.toMutableList()
                accumulatedObj[cacheKey] = obj
            } else {
                accumulatedRecipes.getOrPut(cacheKey) { mutableListOf() }.addAll(recipes)
                val existing = accumulatedMobEntries.getOrPut(cacheKey) { mutableListOf() }
                for (entry in mobEntries) {
                    (existing.find { it.name == entry.name && it.panorama == entry.panorama })?.let {
                        it.mobs = (it.mobs + entry.mobs).distinct()
                    } ?: existing.add(entry)
                }
                val existingObj = accumulatedObj.getOrPut(cacheKey) { obj }
                existingObj.mobs = (existingObj.mobs + obj.mobs).distinct()
            }
            if (hasNextPage) return
            val allRecipes = accumulatedRecipes.remove(cacheKey) ?: recipes
            val allMobEntries = accumulatedMobEntries.remove(cacheKey) ?: mobEntries
            val allObj = accumulatedObj.remove(cacheKey) ?: obj
            if (DevApi.config.debug.copyBestiaryData) OSUtils.copyToClipboard(gson.toJson(allObj))
            if (DevApi.config.debug.inlineReplaceBestiaryData) {
                if (targetCategoryKey != null) {
                    updateBestiaryJson(targetCategoryKey, allMobEntries, allObj)
                }
                updateRepoFile(cleanTitle, allRecipes, titleName)
            }
        } else {
            if (DevApi.config.debug.copyBestiaryData) OSUtils.copyToClipboard(gson.toJson(obj))
            if (DevApi.config.debug.inlineReplaceBestiaryData) {
                if (targetCategoryKey != null) {
                    updateBestiaryJson(targetCategoryKey, mobEntries, obj)
                }
                updateRepoFile(cleanTitle, recipes, titleName)
            }
        }
    }

    private fun resolveIdByDisplayName(displayName: String): String? {
        val clean = displayName.removeColor().trim().lowercase()
        val itemInfo = EnoughUpdatesManager.getItemInformation()
        val formattedMatch = itemInfo.entries.find { it.value.displayName?.trim() == displayName.trim() }
        if (formattedMatch != null) return formattedMatch.key.internalName
        return itemInfo.entries.find { it.value.displayName?.removeColor()?.trim()?.lowercase() == clean }?.key?.internalName
    }

    private fun updateBestiaryJson(categoryPath: String, newMobs: List<MobEntry>, groupData: BestiaryObject) {
        val file = File(EnoughUpdatesManager.repoDirectory, "constants/bestiary.json")
        if (!file.exists()) return
        try {
            val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
            val root = gson.fromJson(file.readText(), JsonObject::class.java)
            val keys = categoryPath.split(".")
            var currentObj = root
            for (i in 0 until keys.size - 1) {
                currentObj = currentObj.getAsJsonObject(keys[i]) ?: return
            }
            val category = currentObj.getAsJsonObject(keys.last()) ?: return
            val mobsArray = category.getAsJsonArray("mobs") ?: JsonArray().also { category.add("mobs", it) }
            updateMobArray(mobsArray, newMobs, groupData)
            file.writeText(gson.toJson(root))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateMobArray(mobsArray: JsonArray, newMobs: List<MobEntry>, groupData: BestiaryObject) {
        for (newMob in newMobs) {
            var existingMob: JsonObject? = null
            val newName = newMob.name.removeColor().trim()
            val newPanorama = newMob.panorama

            for (entry in mobsArray) {
                if (!entry.isJsonObject) continue
                val obj = entry.asJsonObject
                val entryName = obj.get("name")?.asString?.removeColor()?.trim()
                val entryPanorama = obj.get("panorama")?.asString ?: ""
                if (entryName == newName && entryPanorama == newPanorama) {
                    existingMob = obj
                    break
                }
            }

            if (existingMob == null) {
                existingMob = JsonObject().apply {
                    addProperty("name", newMob.name)
                    addProperty("skullOwner", newMob.skullOwner)
                    addProperty("texture", newMob.texture)
                    addProperty("cap", groupData.cap)
                    addProperty("bracket", groupData.bracket)
                    if (newPanorama.isNotEmpty()) addProperty("panorama", newPanorama)
                    add("mobs", JsonArray())
                }
                mobsArray.add(existingMob)
            } else {
                if (groupData.cap > 0) existingMob.addProperty("cap", groupData.cap)
                if (groupData.bracket > 0) existingMob.addProperty("bracket", groupData.bracket)
            }

            val currentIds = existingMob.getAsJsonArray("mobs") ?: JsonArray().also { existingMob.add("mobs", it) }
            newMob.mobs.forEach { id ->
                if (currentIds.none { it.asString == id }) currentIds.add(id)
            }

            val seenIndices = currentIds.mapIndexedNotNull { index, element ->
                if (newMob.mobs.contains(element.asString)) index else null
            }
            if (seenIndices.isNotEmpty()) {
                val minIdx = seenIndices.minOrNull() ?: 0
                val maxIdx = seenIndices.maxOrNull() ?: 0
                val idsToRemove = mutableListOf<Int>()
                for (i in 0 until currentIds.size()) {
                    if (i < minIdx - 1 || i > maxIdx + 1) idsToRemove.add(i)
                }
                idsToRemove.sortedDescending().forEach { currentIds.remove(it) }
            }
        }
    }

    private fun updateRepoFile(cleanTitle: String, newRecipes: List<MobRecipe>, categoryName: String) {
        val repoItems = EnoughUpdatesManager.getItemInformation()
        val existingFiles = repoItems.filter { it.value.displayName?.removeColor()?.trim() == categoryName.removeColor().trim() }
            .map { File(EnoughUpdatesManager.repoDirectory, "items/${it.key.internalName}.json") }.toMutableSet()

        var finalFiles = existingFiles
        if (finalFiles.isEmpty()) {
            val nameUpper = categoryName.removeColor().trim().uppercase().replace(" ", "_")
            val match = repoItems.entries.find { (key, value) ->
                key.internalName.uppercase().contains(nameUpper) && value.recipes.isNotEmpty()
            }
            if (match != null) finalFiles = mutableSetOf(File(EnoughUpdatesManager.repoDirectory, "items/${match.key.internalName}.json"))
        }
        if (finalFiles.isEmpty()) {
            val allTags = newRecipes.flatMap { it.mobTypes }.toSet()
            val suffix = when {
                "AQUATIC" in allTags -> "_SC"
                allTags.all { it == "ANIMAL" } && allTags.isNotEmpty() -> "_ANIMAL"
                else -> "_MONSTER"
            }
            val repoId = if ("PEST" in allTags) {
                "PEST_${generateRepoId(cleanTitle, "_MONSTER")}"
            } else {
                generateRepoId(cleanTitle, suffix)
            }
            finalFiles = mutableSetOf(File(EnoughUpdatesManager.repoDirectory, "items/$repoId.json"))
        }

        val allTags = newRecipes.flatMap { it.mobTypes }.toSet()
        val suffixText = when {
            "AQUATIC" in allTags -> " (Sea Creature)"
            "PEST" in allTags -> " (Pest)"
            allTags.all { it == "ANIMAL" } && allTags.isNotEmpty() -> " (Animal)"
            newRecipes.all { it.level > 200 && !it.name.lowercase().contains("master") } -> " (Boss)"
            else -> " (Monster)"
        }

        val cleanCatName = categoryName.removeColor().trim().replace(romanNumeralRegex, "")
        val fullDisplayName = "§9${cleanCatName}$suffixText"
        val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()

        for (file in finalFiles) {
            try {
                val json = if (file.exists()) gson.fromJson(file.readText(), JsonObject::class.java) else JsonObject().apply {
                    addProperty("itemid", "minecraft:skull")
                    addProperty("displayname", fullDisplayName)
                    addProperty("internalname", file.nameWithoutExtension)
                }

                val oldExtras = mutableMapOf<String, List<String>>()
                val oldRecipes = json.getAsJsonArray("recipes")
                if (oldRecipes != null) {
                    for (i in 0 until oldRecipes.size()) {
                        val oldR = oldRecipes.get(i).asJsonObject
                        val apiId = oldR.get("api_id")?.asString
                        if (apiId != null) {
                            val oldExtra = oldR.get("extra")
                            if (oldExtra != null && oldExtra.isJsonArray) {
                                oldExtras[apiId] = oldExtra.asJsonArray.map { it.asString }
                            }
                        }
                    }
                }

                if (newRecipes.isNotEmpty()) {
                    val recipesArray = JsonArray()
                    for (recipe in newRecipes) {
                        val rObj = gson.toJsonTree(recipe).asJsonObject
                        rObj.remove("skillXps")
                        recipe.skillXps.forEach { (key, value) -> rObj.addProperty(key, value) }

                        val oldExtra = oldExtras[recipe.apiId]
                        if (oldExtra != null && oldExtra.isNotEmpty()) {
                            val extraArray = JsonArray()
                            oldExtra.forEach { extraArray.add(it) }
                            rObj.add("extra", extraArray)
                        }

                        if (recipe.magicResistance == 0) {
                            rObj.remove("magic_resistance")
                        }

                        recipesArray.add(rObj)
                    }
                    if (oldRecipes != null && oldRecipes.size() > 0) {
                        val oldRender = oldRecipes.get(0).asJsonObject.get("render")?.asString
                        if (oldRender != null && oldRender != "minecraft:player_head" && !oldRender.startsWith("eyJ")) {
                            for (nr in recipesArray) nr.asJsonObject.addProperty("render", oldRender)
                        }
                    }
                    json.add("recipes", recipesArray)
                }
                json.addProperty("displayname", fullDisplayName)
                file.writeText(gson.toJson(json))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dev.copyBestiaryData", "dev.debug.copyBestiaryData")
    }
}
