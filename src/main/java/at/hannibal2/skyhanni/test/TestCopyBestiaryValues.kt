package at.hannibal2.skyhanni.test
import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullOwner
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import net.minecraft.world.item.ItemStack
import java.io.File
import java.util.Locale

@SkyHanniModule
object TestCopyBestiaryValues {
    class MobEntry {
        @Expose var name: String = ""
        @Expose var skullOwner: String = ""
        @Expose var texture: String = ""
        @Expose var cap: Int = 0
        @Expose var mobs: List<String> = emptyList()
        @Expose var bracket: Int = 0
    }

    class BestiaryObject {
        @Expose var name: String = ""
        @Expose var skullOwner: String = ""
        @Expose var texture: String = ""
        @Expose var cap: Int = 0
        @Expose var mobs: List<String> = emptyList()
        @Expose var bracket: Int = 0
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
        var skillXps: MutableMap<String, Int> = mutableMapOf(),
    )

    class MobDrop(
        @Expose val id: String,
        @Expose val chance: String,
    )

    private val bestiaryTypePattern by RepoPattern.pattern(
        "test.bestiary.type",
        "(?:§[0-9a-fk-or])*\\[(?:§[0-9a-fk-or])*Lv(?<lvl>\\d+)(?:§[0-9a-fk-or])*] (?:§r)?(?<text>§.[a-zA-Z ]*) ?.*",
    )

    private val romanNumeralRegex = Regex("\\s+[IVXLCDM]+$")

    @HandleEvent(priority = HandleEvent.LOW)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
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
        // Only strip trailing zeros if the number is not extremely small (avoid 0.0000% issue)
        return if (this < 0.0001) formatted else formatted.replace("[^0]0+$".toRegex(), "")
    }

    private fun process(titleItem: ItemStack, guiScreenName: String, inventoryItems: Map<Int, ItemStack>) {
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
                            if (subCatName.contains(categorySearch, ignoreCase = true) || categorySearch.contains(subCatName, ignoreCase = true)) {
                                panorama = key
                                targetCategoryKey = "$key.$subKey"
                            }
                        }
                    }
                } else {
                    if (catName.contains(categorySearch, ignoreCase = true) || categorySearch.contains(catName, ignoreCase = true)) {
                        panorama = key
                        targetCategoryKey = key
                    }
                }
            }
        }

        val mobsIds = mutableListOf<String>()
        val mobEntries = mutableListOf<MobEntry>()
        val recipes = mutableListOf<MobRecipe>()

        for (i in 10..43) {
            val stack = inventoryItems[i] ?: continue
            val loreLines = stack.getLore()

            bestiaryTypePattern.matchMatcher(stack.hoverName.formattedTextCompat()) {
                val lvl = group("lvl").toInt()
                val textGroup = group("text")
                val master = textGroup.lowercase().contains("(master)")
                val cleanBaseName = if (master) textGroup.substringBeforeLast(" (").trim() else textGroup.trim()
                val apiId = generateApiId(cleanBaseName, lvl, master)
                mobsIds.add(apiId)

                val entry = mobEntries.find { it.name == cleanBaseName } ?: MobEntry().apply {
                    name = cleanBaseName
                    texture = stack.getSkullTexture() ?: ""
                    skullOwner = stack.getSkullOwner() ?: ""
                    mobEntries.add(this)
                }
                entry.mobs = entry.mobs + apiId

                val render = stack.getSkullTexture() ?: "minecraft:player_head"
                val coins = loreLines.find { it.contains("Coins per Kill:") }?.substringAfter("Coins per Kill:")?.removeColor()?.replace(" ", "")?.trim('§', '6', ' ')?.toIntOrNull() ?: 0
                val xpLine = loreLines.find { it.contains(" Exp:") }
                val xpTypeName = xpLine?.substringBefore(" Exp:")?.removeColor()?.trim() ?: "Combat"
                val skillXp = xpLine?.substringAfter(" Exp:")?.removeColor()?.replace(" ", "")?.toIntOrNull() ?: 0
                val orbXp = loreLines.find { it.contains("XP Orbs:") }?.removeColor()?.substringAfter("XP Orbs:")?.replace(",", "")?.replace(" ", "")?.toIntOrNull() ?: 0
                val mobTypes = loreLines.find { it.contains("Mob Types?:".toRegex()) }?.substringAfter(":")?.split(",")?.map { it.removeColor().trim().uppercase().replace(Regex("[^A-Z]"), "") } ?: emptyList()

                val drops = mutableListOf<MobDrop>()
                var inDrops = false
                for (line in loreLines) {
                    if (line.contains("Loot") && !line.contains("■")) { inDrops = true; continue }
                    if (inDrops && line.contains("■")) {
                        val dropLineRaw = line.substringAfter("■").trim()
                        val dropLine = dropLineRaw.removeColor()
                        val nameWithQty = dropLine.substringBefore("(").trim()
                        val qtyMatch = Regex("x(\\d+)(?:-(\\d+))?").find(nameWithQty)
                        val cleanDropName = nameWithQty.replace(Regex("x\\d+(?:-\\d+)?"), "").trim()
                        val chanceRaw = dropLine.substringAfter("(").substringBefore(")").trim()

                        var id = resolveIdByDisplayName(cleanDropName) ?: "${cleanDropName.uppercase().replace(" ", "_")} TODO"
                        var finalChance = parseChance(chanceRaw)

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
                        // FIX: Guaranteed drops should be 100%, not 0%
                        if (finalChance == cleanDropName) finalChance = "100%"
                        drops.add(MobDrop(id, finalChance))
                    }
                }

                val recipe = MobRecipe(
                    coins = coins, orbXp = orbXp, level = lvl,
                    name = textGroup, panorama = panorama, render = render, mobTypes = mobTypes, apiId = apiId, drops = drops,
                )
                recipe.skillXps["${xpTypeName.lowercase()}_xp"] = skillXp
                recipes.add(recipe)
            }
        }
        obj.mobs = mobsIds
        if (DevApi.config.debug.copyBestiaryData) OSUtils.copyToClipboard(gson.toJson(obj))
        if (DevApi.config.debug.inlineReplaceBestiaryData) {
            if (targetCategoryKey != null) {
                updateBestiaryJson(targetCategoryKey, mobEntries, obj)
            }
            updateRepoFile(cleanTitle, recipes, titleName)
        }
    }

    private fun resolveIdByDisplayName(displayName: String): String? {
        val clean = displayName.removeColor().trim().lowercase()
        return EnoughUpdatesManager.getItemInformation().entries.find { it.value.displayName?.removeColor()?.trim()?.lowercase() == clean }?.key?.internalName
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
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun updateMobArray(mobsArray: JsonArray, newMobs: List<MobEntry>, groupData: BestiaryObject) {
        for (newMob in newMobs) {
            var existingMob = mobsArray.find {
                it.isJsonObject && it.asJsonObject.get("name")?.asString?.removeColor()?.trim() == newMob.name.removeColor().trim()
            } as? JsonObject

            if (existingMob == null) {
                existingMob = JsonObject().apply {
                    addProperty("name", newMob.name)
                    addProperty("skullOwner", newMob.skullOwner)
                    addProperty("texture", newMob.texture)
                    addProperty("cap", groupData.cap) // Use Group Cap
                    addProperty("bracket", groupData.bracket) // Use Group Bracket
                    add("mobs", JsonArray())
                }
                mobsArray.add(existingMob)
            } else {
                // Always update group data if present in the title item
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
            .map { File(EnoughUpdatesManager.repoDirectory, "items/${it.key}.json") }.toMutableSet()

        var finalFiles = existingFiles
        if (finalFiles.isEmpty()) {
            val isFishing = newRecipes.any { it.skillXps.containsKey("fishing_xp") }
            val isBoss = newRecipes.all { it.level > 200 && !it.name.lowercase().contains("master") }
            val suffix = when {
                isFishing -> "_SC"; isBoss -> "_BOSS"; else -> "_MONSTER"
            }
            finalFiles = mutableSetOf(File(EnoughUpdatesManager.repoDirectory, "items/${generateRepoId(cleanTitle, suffix)}.json"))
        }

        val suffixText = if (newRecipes.any { it.skillXps.containsKey("fishing_xp") }) " (Sea Creature)"
        else if (newRecipes.all { it.level > 200 && !it.name.lowercase().contains("master") }) " (Boss)"
        else " (Monster)"

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

                if (newRecipes.isNotEmpty()) {
                    val recipesArray = JsonArray()
                    for (recipe in newRecipes) {
                        val rObj = gson.toJsonTree(recipe).asJsonObject
                        rObj.remove("skillXps")
                        recipe.skillXps.forEach { (key, value) -> rObj.addProperty(key, value) }
                        recipesArray.add(rObj)
                    }
                    val oldRecipes = json.getAsJsonArray("recipes")
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
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dev.copyBestiaryData", "dev.debug.copyBestiaryData")
    }
}
