package at.hannibal2.skyhanni.skyhannimodule

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import java.io.File
import java.nio.file.Paths
import java.util.Locale
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import kotlin.io.path.absolutePathString
import kotlin.sequences.forEach
val PREDEFINED_APPLY_TYPES: Set<String> = setOf(
    "SWORD", "AXE", "BOW", "FISHING_ROD", "PICKAXE", "HOE"
)

@OptIn(DelicateKotlinPoetApi::class, KspExperimental::class)
@SupportedSourceVersion(SourceVersion.RELEASE_21)
class NeuConstantGeneratorPreprocessor(val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val startTime = System.currentTimeMillis()
        try {
            // --- Caching logic start ---
            val projectRoot = "./"
            val cacheFile = File(Paths.get(projectRoot, "build", ".skyblockitems_cache").toString())
            val backupCacheFile = File(Paths.get(projectRoot, ".skyblockitems_cache").toString())
            val metaCacheFile = File(Paths.get(projectRoot, "build", ".skyblockitems_meta").toString())
            val backupMetaCacheFile = File(Paths.get(projectRoot, ".skyblockitems_meta_backup").toString())
            val currentCommit = try { // changed var to val
                ProcessBuilder("git", "rev-parse", "HEAD")
                    .directory(File(projectRoot))
                    .redirectErrorStream(true)
                    .start()
                    .inputStream.bufferedReader().readText().trim()
            } catch (e: Throwable) {
                environment.logger.warn("Failed to get git commit: ${e.message}")
                null
            }
//        currentCommit=null

            // Restore cache/meta from backup if build cache is missing but backup exists
            if (!cacheFile.exists() && backupCacheFile.exists()) {
                cacheFile.parentFile.mkdirs()
                backupCacheFile.copyTo(cacheFile, overwrite = true)
            }
            if (!metaCacheFile.exists() && backupMetaCacheFile.exists()) {
                metaCacheFile.parentFile.mkdirs()
                backupMetaCacheFile.copyTo(metaCacheFile, overwrite = true)
            }

            val lastCommit = cacheFile.takeIf { it.exists() }?.readText()?.trim()
            val cacheHit = currentCommit != null && currentCommit == lastCommit

            // --- SkyblockItems splitting logic ---
            val itempkg = "de.hype.bingonet.generated.sbenums"
            val itemType = "SkyblockItems"
            val chunkSize = 200

            // --- Property/Chunk metadata ---
            data class ItemMeta(val name: String, val chunk: Int, val itemId: String)
            data class ItemProperty(val name: String, val property: PropertySpec, val itemId: String)

            val allProperties = mutableListOf<ItemProperty>()
            val metaList = mutableListOf<ItemMeta>()
            var chunkObjectNames: List<String>

            // --- Enchantment collection structures ---
            data class EnchantInfo(
                val itemId: String,
                val propertyName: String,
                val applyTypes: Set<String>,
                val fromTable: Boolean
            )

            val enchantInfos = mutableListOf<EnchantInfo>()
            // New collections for generated registries
            val mobIds = mutableListOf<String>()
            val seaCreatureIds = mutableListOf<String>()
            val npcIds = mutableListOf<String>()
            val petEntries = mutableListOf<NeuEntityConstantsGeneratorPreprocessor.PetEntry>()

            fun stripColorCodes(s: String): String = s.replace(Regex("§."), "")
            fun normalizeApplyType(raw: String): String = raw
                .trim()
                .replace(Regex("^[\\-•]+\\s*"), "") // remove leading dash or bullet
                .replace(Regex("[^A-Za-z0-9 ]"), "")
                .trim()
                .replace(Regex("\\s+"), "_")
                .uppercase(Locale.US)

            fun rarityFromNumber(n: Int): String = when (n) {
                0 -> "Common"
                1 -> "Uncommon"
                2 -> "Rare"
                3 -> "Epic"
                4 -> "Legendary"
                5 -> "Mythic"
                else -> "Unknown"
            }

            fun toTitleCaseWords(raw: String): String = raw
                .split("_", "-", " ")
                .filter { it.isNotBlank() }
                .joinToString("_") { part ->
                    val lower = part.lowercase(Locale.US)
                    lower.replaceFirstChar { c -> c.titlecase(Locale.US) }
                }

            fun parsePetRarity(raw: String?): String {
                val token = raw?.trim()?.removePrefix(" ")
                val numeric = token?.toIntOrNull()
                if (numeric != null) return rarityFromNumber(numeric)
                return when (token?.uppercase(Locale.US)) {
                    "COMMON" -> "Common"
                    "UNCOMMON" -> "Uncommon"
                    "RARE" -> "Rare"
                    "EPIC" -> "Epic"
                    "LEGENDARY" -> "Legendary"
                    "MYTHIC" -> "Mythic"
                    else -> "Unknown"
                }
            }

            fun stripPetDisplayName(displayName: String): String = displayName
                .replace("\\[Lvl (\\{LVL\\}|\\d+)]".toRegex(), "")
                .removeColors()
                .trim()

            fun tryBuildPetEntry(itemId: String, item: NEUItem): NeuEntityConstantsGeneratorPreprocessor.PetEntry? {
                val skyId = item.skyblockItemId
                val looksLikePet = skyId.contains("PET", true) || itemId.contains("PET", true) ||
                    item.displayName.contains("Lvl", true)
                if (!looksLikePet) return null

                val parts = skyId.split(";")
                val baseId = parts.firstOrNull()?.ifBlank { itemId.split(";").firstOrNull() ?: itemId } ?: itemId
                val baseNameRaw = baseId.removePrefix("PET_")
                val baseName = if (baseNameRaw.isNotBlank()) {
                    toTitleCaseWords(baseNameRaw)
                } else {
                    toTitleCaseWords(stripPetDisplayName(item.displayName))
                }
                val rarityToken = parts.getOrNull(1)
                    ?: itemId.split(";").getOrNull(1)
                val rarity = parsePetRarity(rarityToken)
                return NeuEntityConstantsGeneratorPreprocessor.PetEntry(itemId = itemId, baseName = baseName, rarity = rarity)
            }

            data class ItemCategoryRule(
                val key: String,
                val predicate: (String, NEUItem) -> Boolean
            )

            data class CategoryRuleSpec(val key: String, val matchType: SBMatchType, val value: String)

            fun readRuleSpec(annotation: KSAnnotation): CategoryRuleSpec? {
                val args = annotation.arguments.associateBy { it.name?.asString() }
                val key = args["key"]?.value as? String ?: return null
                val rawMatchType = args["matchType"]?.value
                val matchTypeName = rawMatchType?.toString()?.substringAfterLast('.')?.trim()
                val matchType = runCatching { SBMatchType.valueOf(matchTypeName ?: "") }.getOrNull() ?: return null
                val value = args["value"]?.value as? String ?: return null
                return CategoryRuleSpec(key = key, matchType = matchType, value = value)
            }

            fun loadCategoryRules(resolver: Resolver): List<ItemCategoryRule> {
                val annotationName = SBEntityCategoryRule::class.qualifiedName ?: return emptyList()
                val specs = mutableListOf<CategoryRuleSpec>()
                resolver.getSymbolsWithAnnotation(annotationName).forEach { symbol ->
                    symbol.annotations
                        .filter { it.shortName.asString() == "SBEntityCategoryRule" }
                        .forEach { ann ->
                            val spec = readRuleSpec(ann)
                            if (spec != null) specs.add(spec)
                        }
                }
                if (specs.isEmpty()) return emptyList()

                return specs.map { spec ->
                    ItemCategoryRule(spec.key) { itemId, item ->
                        when (spec.matchType) {
                            SBMatchType.SKYBLOCK_ID_SUFFIX -> item.skyblockItemId.endsWith(spec.value, true)
                            SBMatchType.SKYBLOCK_ID_CONTAINS -> item.skyblockItemId.contains(spec.value, true)
                            SBMatchType.ITEM_ID_CONTAINS -> itemId.contains(spec.value, true)
                            SBMatchType.DISPLAY_NAME_CONTAINS -> stripColorCodes(item.displayName)
                                .contains(spec.value, true)
                        }
                    }
                }
            }

            val defaultCategoryRules = listOf(
                ItemCategoryRule("mobs") { _, item ->
                    item.skyblockItemId.endsWith("_MONSTER") ||
                        item.skyblockItemId.endsWith("_MINIBOSS") ||
                        item.skyblockItemId.endsWith("_BOSS")
                },
                ItemCategoryRule("seaCreatures") { _, item -> item.skyblockItemId.endsWith("_SC") },
                ItemCategoryRule("npcs") { _, item -> item.skyblockItemId.endsWith("_NPC") },
                ItemCategoryRule("accessoryReforges") { string, item ->
                    return@ItemCategoryRule item.lore.first().removeColors() == "Power Stone"
                }
            )

            val categoryRules = loadCategoryRules(resolver).ifEmpty { defaultCategoryRules }

            fun collectCategories(items: Map<String, NEUItem>): Map<String, List<String>> {
                val buckets = mutableMapOf<String, MutableList<String>>()
                items.forEach { (itemId, item) ->
                    categoryRules.forEach { rule ->
                        if (rule.predicate(itemId, item)) {
                            buckets.getOrPut(rule.key) { mutableListOf() }.add(itemId)
                        }
                    }
                }
                return buckets
            }

            if (!cacheHit) {
                // Build property list and meta
                val nameUsage = mutableMapOf<String, MutableList<Pair<String, NEUItem>>>()
                try {
                    NeuRepoManager.items.forEach { (itemId, item) ->
                        var displayName = item.displayName
                            .replace("\\[Lvl (\\{LVL\\}|100)]".toRegex(), "")
                            .removeColors()
                            .trim()
                            .replace("[\\s.:/\\[\\]\"`$]".toRegex(), "_")

                        // Detect enchant books before mutating property name further
                        val rawDisplayNameForEnchant = stripColorCodes(item.displayName).trim()
                        val isEnchantBook = rawDisplayNameForEnchant.equals("Enchanted Book", true)

                        if (displayName.equals("Enchanted_Book", true)) {
                            displayName =
                                "Enchanted_Book_${item.skyblockItemId.split(";").first().lowercase(Locale.US)}"
                        }
                        if (displayName.equals("Attribute_Shard", true)) {
                            displayName = item.skyblockItemId.split(";").first().lowercase(Locale.US)
                        }

                        // Categorize items in a later, rule-driven pass

                        nameUsage.computeIfAbsent(displayName) { mutableListOf() }
                            .add(itemId to item)

                        // Collect enchant details
                        if (isEnchantBook) {
                            // Parse lore
                            val loreLines: List<String> = try {
                                item.lore ?: emptyList()
                            } catch (_: Throwable) {
                                emptyList()
                            }
                            // Extract apply types
                            val applyIndex =
                                loreLines.indexOfFirst { stripColorCodes(it).trim().equals("Applied To:", true) }
                            val sourceIndex =
                                loreLines.indexOfFirst { stripColorCodes(it).trim().equals("Source:", true) }
                            val applyTypes = mutableSetOf<String>()
                            if (applyIndex != -1) {
                                val endIdx =
                                    if (sourceIndex != -1 && sourceIndex > applyIndex) sourceIndex else loreLines.size
                                for (i in applyIndex + 1 until endIdx) {
                                    val line = stripColorCodes(loreLines[i])
                                    if (line.isBlank()) break
                                    if (line.trim().startsWith("- ") || line.contains("-")) {
                                        val cleaned = line.replaceFirst(Regex(".*?-\\s*"), "").trim()
                                        if (cleaned.isNotBlank()) applyTypes.add(normalizeApplyType(cleaned))
                                    } else {
                                        // Lines like "Fishing Rod" without dash
                                        val cleaned = line.trim()
                                        if (cleaned.isNotBlank()) applyTypes.add(normalizeApplyType(cleaned))
                                    }
                                }
                            }
                            val fromTable = if (sourceIndex != -1) {
                                val endIdx = loreLines.indexOfLast { it.isNotBlank() }
                                val segment = loreLines.subList(sourceIndex + 1, endIdx + 1)
                                segment.any { stripColorCodes(it).contains("Enchantment Table", true) }
                            } else false
                            // Property name will have been computed above as displayName
                            val escapedName =
                                if (displayName.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) displayName else "`$displayName`"
                            enchantInfos.add(EnchantInfo(itemId, escapedName, applyTypes, fromTable))
                        }
                    }
                } catch (e: Throwable) {
                    environment.logger.error("Failed to process NEU items: ${e.message}", null)
                    throw e
                }

                try {
                    nameUsage.forEach { (name, entries) ->
                        entries.forEach { (itemId, _) ->
                            val propertyName = if (entries.size > 1) {
                                itemId
                                    .split("_", "-", ";", ".", " ")
                                    .joinToString("_") { it.lowercase().replaceFirstChar(Char::uppercaseChar) }
                            } else {
                                name
                            }
                            val escapedName = if (propertyName.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
                                propertyName
                            } else {
                                "`$propertyName`"
                            }
                            val propertySpec = run {
                                // Build KDoc with item details when possible
                                val kdocText = runCatching {
                                    val neu = NeuRepoManager.items[itemId]
                                    if (neu != null) {
                                        val sb = StringBuilder()
                                        sb.append("NEU item: ${neu.skyblockItemId}\n")
                                        sb.append("Display: ${stripColorCodes(neu.displayName)}\n")
                                        neu.lore?.forEach { line -> sb.append("- ${stripColorCodes(line)}\n") }
                                        sb.toString().trim()
                                    } else null
                                }.getOrNull()

                                val builder = PropertySpec.builder(
                                    escapedName,
                                    NEUItem::class.asClassName()
                                )
                                    .addModifiers(KModifier.PUBLIC)
                                    .delegate("lazy { %T.items[%S]!! }", NeuRepoManager::class.asClassName(), itemId)

                                if (!kdocText.isNullOrBlank()) {
                                    builder.addKdoc("%L", CodeBlock.of("%S", kdocText))
                                }
                                builder.build()
                            }
                            allProperties.add(ItemProperty(escapedName, propertySpec, itemId))
                        }
                    }
                } catch (e: Throwable) {
                    environment.logger.error("Failed to process name usage: ${e.message}", null)
                    throw e
                }

                chunkObjectNames = allProperties.chunked(chunkSize).mapIndexed { idx, _ -> "SkyblockItemsChunk$idx" }
                allProperties.forEachIndexed { idx, prop ->
                    metaList.add(ItemMeta(prop.name, idx / chunkSize, prop.itemId))
                }
                // Save meta for cache hit usage - now include itemId mapping
                try {
                    val metaWithItemIds = mutableListOf<String>()
                    nameUsage.forEach { (_, entries) ->
                        entries.forEach { (itemId, _) ->
                            val propertyName = if (entries.size > 1) {
                                itemId
                                    .split("_", "-", ";", ".", " ")
                                    .joinToString("_") { it.lowercase().replaceFirstChar(Char::uppercaseChar) }
                            } else {
                                // Find original key containing this itemId
                                nameUsage.keys.first { key -> nameUsage[key]?.any { it.first == itemId } == true }
                            }
                            val escapedName = if (propertyName.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
                                propertyName
                            } else {
                                "`$propertyName`"
                            }
                            val chunk = metaList.find { it.name == escapedName }?.chunk ?: 0
                            metaWithItemIds.add("$escapedName\t$chunk\t$itemId")
                        }
                    }
                    metaCacheFile.writeText(metaWithItemIds.joinToString("\n"))
                } catch (e: Throwable) {
                    environment.logger.warn("Failed to write meta cache: ${e.message}")
                }
            } else {
                // On cache hit, restore meta and build functional properties using stored itemIds
                val metaLines = metaCacheFile.takeIf { it.exists() }?.readLines() ?: emptyList()
                metaLines.forEach { line ->
                    val parts = line.split('\t')
                    if (parts.size >= 3) {
                        val name = parts[0]
                        val chunk = parts[1].toInt()
                        val itemId = parts[2] // raw itemId (should not contain backticks)
                        metaList.add(ItemMeta(name, chunk, itemId))
                        val propertySpec = PropertySpec.builder(
                            name,
                            NEUItem::class.asClassName()
                        )
                            .addModifiers(KModifier.PUBLIC)
                            .delegate(
                                "lazy { %T.items[%S] ?: error(\"Item not found: %L\") }",
                                NeuRepoManager::class.asClassName(), itemId, itemId
                            )
                            .build()
                        allProperties.add(ItemProperty(name, propertySpec, itemId))
                    } else if (parts.size == 2) {
                        // Fallback for old cache format - sanitize backticks
                        val name = parts[0]
                        val chunk = parts[1].toInt()
                        val sanitized = name.removePrefix("`").removeSuffix("`")
                        val itemId = if (sanitized.contains("_") && sanitized.matches(Regex("^[A-Z][A-Z0-9_()]*$"))) {
                            sanitized
                        } else {
                            sanitized.replace("_", "_").uppercase(Locale.US)
                        }
                        metaList.add(ItemMeta(name, chunk, itemId))
                        val propertySpec = PropertySpec.builder(
                            name,
                            NEUItem::class.asClassName()
                        )
                            .addModifiers(KModifier.PUBLIC)
                            .delegate(
                                "lazy { %T.items[%S] ?: error(\"Item not found: %L\") }",
                                NeuRepoManager::class.asClassName(), itemId, itemId
                            )
                            .build()
                        allProperties.add(ItemProperty(name, propertySpec, itemId))
                    }
                }
                val maxChunk = metaList.maxOfOrNull { it.chunk } ?: 0
                chunkObjectNames = (0..maxChunk).map { "SkyblockItemsChunk$it" }
            }

            val metaByChunk = metaList.groupBy { it.chunk }

            // Generate chunked objects
            val chunked = Array(chunkObjectNames.size) { mutableListOf<PropertySpec>() }
            metaList.forEachIndexed { idx, meta ->
                chunked[meta.chunk].add(allProperties[idx].property)
            }
            chunked.forEachIndexed { idx, props ->
                val chunkObj = TypeSpec.objectBuilder(chunkObjectNames[idx])
                    .addModifiers(KModifier.INTERNAL)
                props.forEach { prop -> chunkObj.addProperty(prop) }
                val chunkByIdInit = CodeBlock.builder()
                    .add("lazy {\n")
                    .add("  mapOf(\n")
                metaByChunk[idx].orEmpty().forEach { meta ->
                    chunkByIdInit.add("    %S to %L,\n", meta.itemId, meta.name)
                }
                chunkByIdInit.add("  )\n")
                chunkByIdInit.add("}\n")
                chunkObj.addProperty(
                    PropertySpec.builder(
                        "byItemIdChunk",
                        Map::class.asClassName()
                            .parameterizedBy(String::class.asClassName(), NEUItem::class.asClassName())
                    )
                        .addModifiers(KModifier.INTERNAL)
                        .delegate(chunkByIdInit.build())
                        .build()
                )
                try {
                    environment.codeGenerator.createNewFile(
                        Dependencies.ALL_FILES,
                        itempkg,
                        chunkObjectNames[idx],
                    ).writer().use {
                        FileSpec.builder(itempkg, chunkObjectNames[idx])
                            .addType(chunkObj.build())
                            .build()
                            .writeTo(it)
                    }
                } catch (_: FileAlreadyExistsException) { // unused var replaced
                } catch (e: Throwable) {
                    environment.logger.error("Failed to generate chunk ${chunkObjectNames[idx]}: ${e.message}", null)
                    throw e
                }
            }

            // Generate central SkyblockItems object delegating to chunks
            val itemClassBuilder = TypeSpec.objectBuilder(itemType)
                .addModifiers(KModifier.PUBLIC)
            metaList.forEach {
                val chunkName = chunkObjectNames[it.chunk]
                itemClassBuilder.addProperty(
                    PropertySpec.builder(it.name, NEUItem::class.asClassName())
                        .getter(
                            FunSpec.getterBuilder()
                                .addStatement("return %L.%L", chunkName, it.name)
                                .build()
                        )
                        .build()
                )
            }

            val byItemIdInit = CodeBlock.builder()
                .add("lazy {\n")
                .add("  val acc = mutableMapOf<String, %T>()\n", NEUItem::class.asClassName())
            chunkObjectNames.forEach { chunkName ->
                byItemIdInit.add("  acc.putAll(%L.byItemIdChunk)\n", chunkName)
            }
            byItemIdInit.add("  acc\n")
            byItemIdInit.add("}\n")

            itemClassBuilder.addProperty(
                PropertySpec.builder(
                    "byItemId",
                    Map::class.asClassName()
                        .parameterizedBy(String::class.asClassName(), NEUItem::class.asClassName())
                )
                    .addModifiers(KModifier.PUBLIC)
                    .delegate(byItemIdInit.build())
                    .build()
            )

            itemClassBuilder.addFunction(
                FunSpec.builder("itemById")
                    .addParameter("itemId", String::class.asClassName())
                    .returns(NEUItem::class.asClassName().copy(nullable = true))
                    .addStatement("return byItemId[itemId]")
                    .build()
            )

            try {
                environment.codeGenerator.createNewFile(
                    Dependencies.ALL_FILES,
                    itempkg,
                    itemType,
                ).writer().use {
                    FileSpec.builder(itempkg, itemType)
                        .addType(itemClassBuilder.build())
                        .build()
                        .writeTo(it)
                }
            } catch (_: FileAlreadyExistsException) { // unused var replaced
            } catch (e: Throwable) {
                environment.logger.error("Failed to generate main SkyblockItems class: ${e.message}", null)
                throw e
            }

            // Build categories and pet entries from rules to avoid hard-coded paths
            try {
                val categories = collectCategories(NeuRepoManager.items)
                mobIds.addAll(categories["mobs"].orEmpty())
                seaCreatureIds.addAll(categories["seaCreatures"].orEmpty())
                npcIds.addAll(categories["npcs"].orEmpty())
                NeuRepoManager.items.forEach { (itemId, item) ->
                    val petEntry = tryBuildPetEntry(itemId, item)
                    if (petEntry != null) petEntries.add(petEntry)
                }
            } catch (e: Throwable) {
                environment.logger.warn("Entity categorization failed: ${e.message}")
            }

            // Delegate entity registries generation to NeuEntityConstantsGeneratorPreprocessor (split file)
            try {
                NeuEntityConstantsGeneratorPreprocessor.generate(
                    environment = environment,
                    projectRoot = projectRoot,
                    itempkg = itempkg,
                    mobIds = mobIds.distinct(),
                    seaCreatureIds = seaCreatureIds.distinct(),
                    npcIds = npcIds.distinct(),
                    petEntries = petEntries.distinctBy { it.itemId }
                )
            } catch (e: Throwable) {
                environment.logger.error("Failed to generate entities: ${e.message}", null)
                throw e
            }

            // --- Generate enchant apply type enum and registry ---
            val enchantPkg = "de.hype.bingonet.generated.sbenums.enchants"
            val enchantRegistryName = "EnchantApplyTypes"
            val enchantSpec = TypeSpec.enumBuilder(enchantRegistryName)

            // Collect all apply types from enchantInfos
            val allApplyTypes = enchantInfos.flatMap { it.applyTypes }.distinct()

            // Sort apply types: first by predefined order, then alphabetically
            val sortedApplyTypes = allApplyTypes.sortedWith(
                compareBy(
                    { if (it in de.hype.bingonet.sharedcompilation.processors.PREDEFINED_APPLY_TYPES) 0 else 1 }, // predefined types first
                    { it } // then alphabetically
                )
            )

            // Add enum constants for each apply type
            sortedApplyTypes.forEach {
                enchantSpec.addEnumConstant(it)
            }

            try {
                environment.codeGenerator.createNewFile(
                    Dependencies.ALL_FILES,
                    enchantPkg,
                    enchantRegistryName,
                ).writer().use {
                    FileSpec.builder(enchantPkg, enchantRegistryName)
                        .addType(enchantSpec.build())
                        .build()
                        .writeTo(it)
                }
            } catch (_: FileAlreadyExistsException) { // unused var replaced
            } catch (e: Throwable) {
                environment.logger.error("Failed to generate enchant apply type registry: ${e.message}", null)
                throw e
            }

            // --- Generate item apply type registry ---
            val itemApplyTypePkg = "de.hype.bingonet.generated.sbenums.items"
            val itemApplyTypeRegistryName = "ItemApplyTypes"
            val itemApplyTypeSpec = TypeSpec.objectBuilder(itemApplyTypeRegistryName)

            // Group enchantInfos by itemId for per-item registries
            val enchantInfoGroups = enchantInfos.groupBy { it.itemId }

            // Generate a registry for each itemId with enchantments
            enchantInfoGroups.forEach { (itemId, infos) ->
                // Skip items without apply types
                val allTypes = infos.flatMap { it.applyTypes }.distinct()
                if (allTypes.isEmpty()) return@forEach

                // Sort types for consistent ordering
                val sortedTypes = allTypes.sorted()

                // Create a safe class name for the registry type (PascalCase, letters/digits only)
                val className = itemId
                    .split("_", "-", ";", ".", " ")
                    .map { it.filter { ch -> ch.isLetterOrDigit() } }
                    .filter { it.isNotEmpty() }
                    .joinToString("") { it.lowercase(Locale.US).replaceFirstChar { c -> c.uppercase(Locale.US) } }

                val typeName = if (className.isNotEmpty() && className.first().isLetter()) {
                    className
                } else {
                    // Fallback: prefix with Item to ensure it starts with a letter
                    "Item${className.ifEmpty { itemId.filter { it.isLetterOrDigit() } }}"
                }

                // Generate the registry enum type (enum, not object)
                val registryEnum = TypeSpec.enumBuilder(typeName)
                    .addKdoc("Apply type registry for $itemId\n")

                sortedTypes.forEach { type ->
                    registryEnum.addEnumConstant(type)
                }
                itemApplyTypeSpec.addType(registryEnum.build())
            }

            try {
                environment.codeGenerator.createNewFile(
                    Dependencies.ALL_FILES,
                    itemApplyTypePkg,
                    itemApplyTypeRegistryName,
                ).writer().use {
                    FileSpec.builder(itemApplyTypePkg, itemApplyTypeRegistryName)
                        .addType(itemApplyTypeSpec.build())
                        .build()
                        .writeTo(it)
                }
            } catch (_: FileAlreadyExistsException) { // unused var replaced
            } catch (e: Throwable) {
                environment.logger.error("Failed to generate item apply type registry: ${e.message}", null)
                throw e
            }

            // --- Generate Accessory Powers enum (dynamic backing data) ---
            try {
                val accessoryPkg = "de.hype.bingonet.generated.sbenums.accessories"
                val accessoryEnumName = "AccessoryPower"

                fun toEnumConstantName(raw: String): String {
                    val clean = raw.trim().replace(Regex("[^A-Za-z0-9]+"), "_")
                    val collapsed = clean.replace(Regex("_+"), "_").trim('_')
                    val upper = collapsed.uppercase(Locale.US)
                    return if (upper.firstOrNull()?.isLetter() == true) upper else "POWER_$upper"
                }

                val powers = NeuRepoManager.accessoryPowers.values.toList()
                val enumSpec = TypeSpec.enumBuilder(accessoryEnumName)
                    .addKdoc(
                        """Generated from `neu-repo/constants/accessorypowers.json`.
                        |
                        |This enum is stable for code-completion, while the underlying stats remain dynamic:
                        |`base`, `unique` and `stone` are resolved through `NeuRepoManager.accessoryPowers`.
                        |""".trimMargin()
                    )
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter("key", String::class)
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder("key", String::class)
                            .initializer("key")
                            .build()
                    )

                val managerClass = ClassName("de.hype.bingonet.sharedcompilation.sbenumcode", "NeuRepoManager")
                val defClass = ClassName("de.hype.bingonet.sharedcompilation.sbenumcode", "AccessoryPowerDefinition")

                enumSpec.addProperty(
                    PropertySpec.builder("definition", defClass)
                        .getter(
                            FunSpec.getterBuilder()
                                .addStatement(
                                    "return %T.accessoryPowers[key] ?: error(%S + key)",
                                    managerClass,
                                    "Accessory power not found in repo: "
                                )
                                .build()
                        )
                        .build()
                )

                val mapType = MAP.parameterizedBy(String::class.asTypeName(), Double::class.asTypeName())

                enumSpec.addProperty(
                    PropertySpec.builder("base", mapType)
                        .getter(FunSpec.getterBuilder().addStatement("return definition.base").build())
                        .build()
                )
                enumSpec.addProperty(
                    PropertySpec.builder("unique", mapType)
                        .getter(FunSpec.getterBuilder().addStatement("return definition.unique").build())
                        .build()
                )
                enumSpec.addProperty(
                    PropertySpec.builder("stone", String::class.asTypeName().copy(nullable = true))
                        .getter(FunSpec.getterBuilder().addStatement("return definition.stone").build())
                        .build()
                )

                powers.forEach { power ->
                    enumSpec.addEnumConstant(
                        toEnumConstantName(power.key),
                        TypeSpec.anonymousClassBuilder()
                            .addSuperclassConstructorParameter("%S", power.key)
                            .build()
                    )
                }

                environment.codeGenerator.createNewFile(
                    Dependencies.ALL_FILES,
                    accessoryPkg,
                    accessoryEnumName,
                ).writer().use {
                    FileSpec.builder(accessoryPkg, accessoryEnumName)
                        .addType(enumSpec.build())
                        .build()
                        .writeTo(it)
                }
            } catch (_: FileAlreadyExistsException) {
                // ignore incremental re-generation collisions
            } catch (e: Throwable) {
                environment.logger.error("Failed to generate accessory power enum: ${e.message}", null)
                throw e
            }

            val endTime = System.currentTimeMillis()
            environment.logger.info("SBEnumGenerators processed in ${endTime - startTime} ms")
        } catch (e: Throwable) {
            environment.logger.error("Unexpected error in SBEnumGenerators: ${e.message}", null)
            throw e
        }

        return emptyList()
    }
}
