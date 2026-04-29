package at.hannibal2.skyhanni.skyhannimodule

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.gson.JsonParser
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import de.hype.bingonet.sharedcompilation.extensionutils.removeColors
import de.hype.bingonet.sharedcompilation.sbenumcode.NeuRepoManager
import io.github.moulberry.repo.data.NEUItem
import java.io.File
import java.nio.file.Paths
import java.util.*
import kotlin.text.get

class NeuEntityConstantsGeneratorPreprocessor {
    data class PetEntry(
        val itemId: String,
        val baseName: String,
        val rarity: String
    )

    fun generate(
        environment: SymbolProcessorEnvironment,
        projectRoot: String,
        itempkg: String,
        mobIds: List<String>,
        seaCreatureIds: List<String>,
        npcIds: List<String>,
        petEntries: List<PetEntry>
    ) {
        try {
            fun toTitleCaseWords(raw: String): String = raw
                .split("_", "-", " ")
                .filter { it.isNotBlank() }
                .joinToString("_") { part ->
                    val lower = part.lowercase(Locale.US)
                    lower.replaceFirstChar { c -> c.titlecase(Locale.US) }
                }

            fun formatPetEnumName(baseName: String, rarity: String): String {
                val base = toTitleCaseWords(baseName).ifBlank { "Pet" }
                val rarityPart = toTitleCaseWords(rarity).ifBlank { "Unknown" }
                return "${base}_${rarityPart}"
            }

            fun sanitizeEnumName(raw: String): String {
                var s = raw.replace(Regex("§."), "").replace(Regex("[^A-Za-z0-9_]"), "_")
                s = s.replace(Regex("_+"), "_")
                s = s.trim('_')
                if (s.isEmpty()) s = "ITEM"
                if (s[0].isDigit()) s = "I$s"
                s = s.uppercase(Locale.US)
                if (!s.matches(Regex("^[A-Z_][A-Z0-9_]*$"))) {
                    s = "ITEM_${s.hashCode().toUInt()}"
                }
                return s
            }

            fun readNeuJson(itemId: String): com.google.gson.JsonObject? {
                val basePaths = listOf(
                    NeuRepoManager.LOCAL_PATH.resolve("items").toFile(),
                    File(Paths.get(projectRoot, "neu-repo", "items").toString())
                )
                val candidates = mutableListOf<File>()
                candidates += basePaths.map { File(it, "${itemId}.json") }
                for (f in candidates) {
                    if (!f.exists()) continue
                    try {
                        val txt = f.readText()
                        return JsonParser.parseString(txt).asJsonObject
                    } catch (_: Throwable) {
                    }
                }
                return null
            }

            // helper: check several plausible KSP output locations for an already-generated file
            fun alreadyGeneratedInAnyKspOutput(pkg: String, fileName: String): Boolean {
                val candidateModules = listOf("annotation-proccessor", "main", "shared-annotation-proccessor", "")
                for (mod in candidateModules) {
                    val parts = if (mod.isBlank()) listOf(projectRoot) else listOf(projectRoot, mod)
                    val pathParts = parts + listOf(
                        "build",
                        "generated",
                        "ksp",
                        "main",
                        "kotlin",
                        pkg.replace('.', File.separatorChar),
                        fileName
                    )
                    val first = pathParts.first()
                    val rest = if (pathParts.size > 1) pathParts.drop(1).toTypedArray() else emptyArray<String>()
                    val outPath = Paths.get(first, *rest)
                    val f = File(outPath.toString())
                    if (f.exists()) return true
                }
                return false
            }

            // Build a global map itemId -> source file name
            // no local file lookup; NEU repo is source of truth

            // --- Mobs: per-recipe generation ---
            if (mobIds.isNotEmpty()) {
                data class MobEntry(
                    val enumName: String,
                    val neuItemId: String,
                    val level: Int,
                    val groupKey: String
                )

                data class RawMobEntry(
                    val baseName: String,
                    val neuItemId: String,
                    val level: Int,
                    val groupKey: String
                )

                val rawMobEntries = mutableListOf<RawMobEntry>()
                val mobDropClass = ClassName("io.github.moulberry.repo.data", "NEUMobDropRecipe")

                val mobInterfaceName = "SkyblockMob"
                val mobEnumName = mobInterfaceName + "s"
                val mobObjectName = "SkyblockMobs"

                mobIds.sorted().forEach { id ->
                    try {
                        val neuItem = NeuRepoManager.items[id] ?: return@forEach
                        val mobRecipes =
                            neuItem.recipes?.filterIsInstance<io.github.moulberry.repo.data.NEUMobDropRecipe>()
                                ?: emptyList()
                        if (mobRecipes.isEmpty()) return@forEach
                        val groupKey = neuItem.skyblockItemId ?: id
                        val baseName =
                            formatMobEnumName(neuItem.displayName, null).ifEmpty { formatMobEnumName(id, null) }
                        if (mobRecipes.isEmpty()) {
                            rawMobEntries.add(RawMobEntry(baseName, id, -1, groupKey))
                        } else if (mobRecipes.size == 1) {
                            val recipe = mobRecipes.first()
                            val baseName = formatMobEnumName(recipe.name, null)
                            rawMobEntries.add(
                                RawMobEntry(
                                    baseName.ifEmpty { formatMobEnumName(id, recipe.level) },
                                    id,
                                    recipe.level,
                                    groupKey
                                )
                            )
                        } else {
                            mobRecipes.forEach { recipe ->
                                val baseName = if (baseName == formatMobEnumName(recipe.name, null)) formatMobEnumName(
                                    recipe.name,
                                    recipe.level
                                )
                                else formatMobEnumName(baseName + " " + recipe.name, recipe.level)

                                rawMobEntries.add(
                                    RawMobEntry(
                                        baseName.ifEmpty { formatMobEnumName(id, recipe.level) },
                                        id,
                                        recipe.level,
                                        groupKey
                                    )
                                )
                            }
                        }
                    } catch (_: Throwable) {
                    }
                }

                val levelsByName =
                    rawMobEntries.groupBy { it.baseName }.mapValues { entry -> entry.value.map { it.level } }

                val mobEntries = rawMobEntries.map { raw ->
                    val levels = levelsByName[raw.baseName] ?: emptyList()
                    val multipleVariants = levels.size > 1
                    val shouldAppendLevel = raw.level !in listOf(0, -1) && multipleVariants
                    val finalName = if (shouldAppendLevel) formatMobEnumName(raw.baseName, raw.level) else raw.baseName
                    MobEntry(finalName, raw.neuItemId, raw.level, raw.groupKey)
                }

                val groupedMobEntries = mobEntries.groupBy { it.groupKey }

                fun normalizeGroupName(raw: String): String {
                    val stripped = raw.replace("_(?i)(MONSTER|MINIBOSS|BOSS|SC|SEA_CREATURE|NPC)$".toRegex(), "")
                    return formatMobEnumName(stripped, null)
                }

                val mobPkg = itempkg

                val islandsClass = ClassName("de.hype.bingonet.shared.constants", "Islands")
                val mobInterface = TypeSpec.interfaceBuilder("SkyblockMob")

                    .addProperty(PropertySpec.builder("itemId", String::class).addModifiers(KModifier.ABSTRACT).build())
                    .addProperty(PropertySpec.builder("level", INT).addModifiers(KModifier.ABSTRACT).build())
                    .addProperty(
                        PropertySpec.builder("skyblockId", String::class).addModifiers(KModifier.ABSTRACT).build()
                    )
                    .addProperty(
                        PropertySpec.builder("displayName", String::class).addModifiers(KModifier.ABSTRACT).build()
                    )
                    .addProperty(PropertySpec.builder("render", String::class).addModifiers(KModifier.ABSTRACT).build())
                    .addProperty(
                        PropertySpec.builder("island", islandsClass.copy(nullable = true))
                            .addModifiers(KModifier.ABSTRACT).build()
                    )
                    .addProperty(
                        PropertySpec.builder(
                            "drops",
                            List::class.asClassName().parameterizedBy(mobDropClass.nestedClass("Drop"))
                        ).addModifiers(KModifier.ABSTRACT).build()
                    )
                    .addProperty(
                        PropertySpec.builder("formattedDisplayName", String::class).addModifiers(KModifier.ABSTRACT)
                            .build()
                    )
                    .build()

                val detailedMobInterface = TypeSpec.interfaceBuilder("DetailedSkyblockMob")

                    .addSuperinterface(ClassName(mobPkg, "SkyblockMob"))
                    .addProperty(PropertySpec.builder("coins", INT).addModifiers(KModifier.ABSTRACT).build())
                    .addProperty(PropertySpec.builder("combatXp", INT).addModifiers(KModifier.ABSTRACT).build())
                    .addProperty(PropertySpec.builder("vanillaXp", INT).addModifiers(KModifier.ABSTRACT).build())
                    .build()

                val removeColorMember = MemberName("de.hype.bingonet.server.extensionutils", "removeColorCodes")

                val mobEnumBuilder = TypeSpec.enumBuilder("SkyblockMobs")
                    .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "Unused").build())
                    .addSuperinterface(ClassName(mobPkg, "DetailedSkyblockMob"))
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter("itemId", String::class)
                            .addParameter("level", INT)
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder("itemId", String::class)
                            .addModifiers(KModifier.OVERRIDE)
                            .initializer("itemId")
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder("level", INT)
                            .addModifiers(KModifier.OVERRIDE)
                            .initializer("level")
                            .build()
                    )

                mobEntries.forEach { me ->
                    val constName = me.enumName
                    val anonBuilder = TypeSpec.anonymousClassBuilder()
                        .addSuperclassConstructorParameter("%S", me.neuItemId)
                        .addSuperclassConstructorParameter("%L", me.level)
                    mobEnumBuilder.addEnumConstant(constName, anonBuilder.build())
                }

                val neuItemLazy = PropertySpec.builder("neuCategoryItem", NEUItem::class)

                    .delegate(
                        CodeBlock.of(
                            "lazy { %T.items[itemId] ?: error(%S + itemId + %S) }",
                            NeuRepoManager::class,
                            "Neu Item ",
                            " no longer exists"
                        )
                    ).build()

                val mobLazy = PropertySpec.builder("mob", mobDropClass)

                    .delegate(
                        CodeBlock.of(
                            "lazy {\n  val categoryMobs = neuCategoryItem.recipes?.filterIsInstance<%T>() ?: error(%S)\n  categoryMobs.find { it.level == level } ?: error(%S + itemId + %S + level + %S)\n }",
                            mobDropClass,
                            "Neu Mob no longer exists",
                            "Neu Mob ",
                            " level ",
                            " no longer exists"
                        )
                    ).build()

                val dropsProp = PropertySpec.builder(
                    "drops",
                    List::class.asClassName().parameterizedBy(mobDropClass.nestedClass("Drop"))
                )
                    .addModifiers(KModifier.OVERRIDE)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return mob.drops")
                            .build()
                    ).build()

                val islandProp = PropertySpec.builder("island", islandsClass.copy(nullable = true))
                    .addModifiers(KModifier.OVERRIDE)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return %T.fromInternalName(mob.panorama)", islandsClass)
                            .build()
                    ).build()

                val renderProp = PropertySpec.builder("render", String::class)
                    .addModifiers(KModifier.OVERRIDE)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return mob.render")
                            .build()
                    ).build()

                val coinsProp = PropertySpec.builder("coins", INT)
                    .addModifiers(KModifier.OVERRIDE)
                    .getter(FunSpec.getterBuilder().addStatement("return mob.coins").build())
                    .build()
                val combatXpProp = PropertySpec.builder("combatXp", INT)
                    .addModifiers(KModifier.OVERRIDE)
                    .getter(FunSpec.getterBuilder().addStatement("return mob.combatExperience").build())
                    .build()
                val vanillaXpProp = PropertySpec.builder("vanillaXp", INT)
                    .addModifiers(KModifier.OVERRIDE)
                    .getter(FunSpec.getterBuilder().addStatement("return mob.enchantingExperience").build())
                    .build()

                val formattedDisplayProp = PropertySpec.builder("formattedDisplayName", String::class)
                    .addModifiers(KModifier.OVERRIDE)
                    .getter(FunSpec.getterBuilder().addStatement("return mob.name").build())
                    .build()

                val displayNameProp = PropertySpec.builder("displayName", String::class)
                    .addModifiers(KModifier.OVERRIDE)
                    .delegate(CodeBlock.of("lazy { mob.name.%M() }", removeColorMember))
                    .build()

                val skyblockIdProp = PropertySpec.builder("skyblockId", String::class)
                    .addModifiers(KModifier.OVERRIDE)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return TODO(%S)", "missing skyblockId since missing in repo rn.")
                            .build()
                    ).build()

                mobEnumBuilder.addProperty(neuItemLazy)
                mobEnumBuilder.addProperty(mobLazy)
                mobEnumBuilder.addProperty(dropsProp)
                mobEnumBuilder.addProperty(islandProp)
                mobEnumBuilder.addProperty(renderProp)
                mobEnumBuilder.addProperty(coinsProp)
                mobEnumBuilder.addProperty(combatXpProp)
                mobEnumBuilder.addProperty(vanillaXpProp)
                mobEnumBuilder.addProperty(formattedDisplayProp)
                mobEnumBuilder.addProperty(displayNameProp)
                mobEnumBuilder.addProperty(skyblockIdProp)

                mobEnumBuilder.addType(
                    TypeSpec.companionObjectBuilder()
                        .addFunction(
                            FunSpec.builder("extractNeuMobs")
                                .returns(List::class.asClassName().parameterizedBy(mobDropClass))
                                .addCode(
                                    "return %T.items.values.flatMap { it.recipes?.filterIsInstance<%T>() ?: emptyList() }\n",
                                    NeuRepoManager::class,
                                    mobDropClass
                                )
                                .build()
                        )
                        .addProperty(
                            PropertySpec.builder(
                                "groupedNeuMobsByRender",
                                Map::class.asClassName().parameterizedBy(
                                    String::class.asClassName(),
                                    List::class.asClassName().parameterizedBy(mobDropClass)
                                )
                            ).delegate(
                                CodeBlock.of(
                                    "lazy { extractNeuMobs().groupBy { it.render.lowercase(%T.US) } }",
                                    Locale::class
                                )
                            ).build()
                        )
                        .addProperty(
                            PropertySpec.builder(
                                "groupedByRender",
                                Map::class.asClassName().parameterizedBy(
                                    String::class.asClassName(),
                                    List::class.asClassName().parameterizedBy(ClassName(mobPkg, "SkyblockMobs"))
                                )
                            ).delegate(
                                CodeBlock.of(
                                    "lazy { entries.groupBy { it.render.lowercase(%T.US) } }",
                                    Locale::class
                                )
                            ).build()
                        )
                        .build()
                )

                val mobGroupInterface = TypeSpec.interfaceBuilder("SkyblockMobGroup")
                    .addProperty(
                        PropertySpec.builder(
                            "mobs",
                            Set::class.asClassName().parameterizedBy(ClassName(mobPkg, mobInterfaceName))
                        ).addModifiers(KModifier.ABSTRACT).build()
                    )
                    .build()

                val mobGroupEnumBuilder = TypeSpec.enumBuilder("SkyblockMobGroups")
                    .addSuperinterface(ClassName(mobPkg, "SkyblockMobGroup"))
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter(
                                "mobs",
                                Set::class.asClassName().parameterizedBy(ClassName(mobPkg, mobEnumName))
                            )
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder(
                            "mobs",
                            Set::class.asClassName().parameterizedBy(ClassName(mobPkg, mobEnumName))
                        )
                            .addModifiers(KModifier.OVERRIDE)
                            .initializer("mobs")
                            .build()
                    )

                groupedMobEntries.forEach { (groupKey, entries) ->
                    if (entries.size < 2) return@forEach
                    val groupName = normalizeGroupName(groupKey)
                    val setLiteral = entries.joinToString(", ") { "${mobEnumName}.${it.enumName}" }
                    val enumAnon = TypeSpec.anonymousClassBuilder()
                        .addSuperclassConstructorParameter("setOf(%L)", setLiteral)
                        .build()
                    mobGroupEnumBuilder.addEnumConstant(groupName, enumAnon)
                }

                val mobFileBuilder = FileSpec.builder(mobPkg, mobObjectName)
                    .addType(mobInterface)
                    .addType(detailedMobInterface)
                    .addType(mobEnumBuilder.build())
                    .addType(mobGroupInterface)
                    .addType(mobGroupEnumBuilder.build())
                    .build()

                try {
                    val fileName = "$mobObjectName.kt"
                    if (alreadyGeneratedInAnyKspOutput(mobPkg, fileName)) {
                        environment.logger.info("Skipping $mobObjectName generation; file already exists in another module's KSP output")
                    } else {
                        try {
                            environment.codeGenerator.createNewFile(
                                Dependencies.ALL_FILES,
                                mobPkg,
                                mobObjectName
                            )
                                .writer()
                                .use { w -> mobFileBuilder.writeTo(w) }
                        } catch (e: Throwable) {
                            if (e is java.nio.file.FileAlreadyExistsException || e is FileAlreadyExistsException) {
                                environment.logger.info("Skipping $mobObjectName generation; file already created by KSP: ${e.message}")
                            } else {
                                throw e
                            }
                        }
                    }
                } catch (e: Throwable) {
                    environment.logger.error("Failed to write SkyblockMobs: ${e.message}", null)
                    throw e
                }
            }

            // --- Sea Creatures ---
            if (seaCreatureIds.isNotEmpty()) {
                val seaPkg = itempkg
                val seaEnumName = "SkyblockSeaCreature"
                val seaObjectName = "SkyblockSeaCreatures"

                val seaEnumBuilder = TypeSpec.enumBuilder(seaEnumName)
                    .primaryConstructor(FunSpec.constructorBuilder().addParameter("itemId", String::class).build())
                    .addProperty(PropertySpec.builder("itemId", String::class).initializer("itemId").build())

                seaCreatureIds.forEach { id ->
                    val constName = sanitizeEnumName(id).uppercase(Locale.US)
                    seaEnumBuilder.addEnumConstant(
                        constName,
                        TypeSpec.anonymousClassBuilder().addSuperclassConstructorParameter("%S", id).build()
                    )
                }

                val seaObjectBuilder = TypeSpec.objectBuilder(seaObjectName)


                val seaFileBuilder = FileSpec.builder(seaPkg, seaObjectName)
                seaFileBuilder.addType(seaEnumBuilder.build())
                seaFileBuilder.addType(seaObjectBuilder.build())
                try {
                    val fileName = "$seaObjectName.kt"
                    if (alreadyGeneratedInAnyKspOutput(seaPkg, fileName)) {
                        environment.logger.info("Skipping $seaObjectName generation; file already exists in another module's KSP output")
                    } else {
                        try {
                            environment.codeGenerator.createNewFile(Dependencies.ALL_FILES, seaPkg, seaObjectName)
                                .writer()
                                .use { w -> seaFileBuilder.build().writeTo(w) }
                        } catch (e: Throwable) {
                            if (e is java.nio.file.FileAlreadyExistsException || e is FileAlreadyExistsException) {
                                environment.logger.info("Skipping $seaObjectName generation; file already created by KSP: ${e.message}")
                            } else {
                                throw e
                            }
                        }
                    }
                } catch (e: Throwable) {
                    environment.logger.error("Failed to write SkyblockSeaCreatures: ${e.message}", null)
                    throw e
                }
            }

            // --- NPCs ---
            if (npcIds.isNotEmpty()) {
                val npcPkg = itempkg
                val npcEnumName = "SkyblockNPCs"
                val npcInterfaceName = "SkyblockNPC"

                val npcInterface = TypeSpec.interfaceBuilder(npcInterfaceName)

                    .addProperty(
                        PropertySpec.builder("itemId", String::class)
                            .addModifiers(KModifier.ABSTRACT)
                            .build()
                    ).addProperty(
                        PropertySpec.builder("displayName", String::class)
                            .addModifiers(KModifier.ABSTRACT)
                            .build()
                    ).addProperty("island", String::class)
                    .addProperty("position", ClassName("de.hype.bingonet.shared.objects", "Position"))
                    .build()

                val npcEnumBuilder =
                    TypeSpec.enumBuilder(npcEnumName)
                        .primaryConstructor(
                            FunSpec.constructorBuilder().addParameter("itemId", String::class).build()
                        )
                        .addProperty(
                            PropertySpec.builder("itemId", String::class).initializer("itemId")
                                .addModifiers(KModifier.OVERRIDE).build()
                        )
                        .addProperty(
                            PropertySpec.builder("item", NEUItem::class).addModifiers(KModifier.PRIVATE).getter(
                                FunSpec.getterBuilder()
                                    .addStatement("return %T.items[itemId]!!", NeuRepoManager::class)
                                    .build()
                            ).build()
                        )
                        .addProperty(
                            PropertySpec.builder(
                                "position",
                                ClassName("de.hype.bingonet.shared.objects", "Position")
                            )
                                .addModifiers(KModifier.OVERRIDE).getter(
                                    FunSpec.getterBuilder()
                                        .addStatement(
                                            "return %T(item.x, item.x, item.z)",
                                            ClassName("de.hype.bingonet.shared.objects", "Position")
                                        )
                                        .build()
                                ).build()
                        )
                        .addProperty(
                            PropertySpec.builder("island", String::class).addModifiers(KModifier.OVERRIDE).getter(
                                FunSpec.getterBuilder()
                                    .addStatement("return item.island")
                                    .build()
                            ).build()
                        )
                        .addProperty(
                            PropertySpec.builder("displayName", String::class).addModifiers(KModifier.OVERRIDE)
                                .getter(
                                    FunSpec.getterBuilder()
                                        .addStatement("return item.displayName")
                                        .build()
                                ).build()
                        ).addSuperinterface(ClassName(npcPkg, npcInterfaceName))

                npcIds.forEach { id ->
                    val constName = sanitizeEnumName(id).uppercase(Locale.US)
                    npcEnumBuilder.addEnumConstant(
                        constName,
                        TypeSpec.anonymousClassBuilder().addSuperclassConstructorParameter("%S", id).build()
                    )
                }

                val npcFileBuilder = FileSpec.builder(npcPkg, npcEnumName)
                npcFileBuilder.addType(npcInterface)
                npcFileBuilder.addType(npcEnumBuilder.build())
                try {
                    val fileName = "$npcEnumName.kt"
                    if (alreadyGeneratedInAnyKspOutput(npcPkg, fileName)) {
                        environment.logger.info("Skipping $npcEnumName generation; file already exists in another module's KSP output")
                    } else {
                        try {
                            environment.codeGenerator.createNewFile(Dependencies.ALL_FILES, npcPkg, npcEnumName)
                                .writer()
                                .use { w -> npcFileBuilder.build().writeTo(w) }
                        } catch (e: Throwable) {
                            if (e is java.nio.file.FileAlreadyExistsException || e is FileAlreadyExistsException) {
                                environment.logger.info("Skipping $npcEnumName generation; file already created by KSP: ${e.message}")
                            } else {
                                throw e
                            }
                        }
                    }
                } catch (e: Throwable) {
                    environment.logger.error("Failed to write SkyblockNPCs: ${e.message}", null)
                    throw e
                }
            }

            // --- Pets ---
            if (petEntries.isNotEmpty()) {
                val petPkg = itempkg
                val petEnumName = "SkyblockPet"
                val petObjectName = "SkyblockPets"
                val skyblockItemsClass = ClassName(itempkg, "SkyblockItems")
                val petInterfaceName = "SkyblockPetEntry"

                val petInterface = TypeSpec.interfaceBuilder(petInterfaceName)
                    .addProperty(
                        PropertySpec.builder("itemId", String::class)
                            .addModifiers(KModifier.ABSTRACT)
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder("rarity", String::class)
                            .addModifiers(KModifier.ABSTRACT)
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder("item", NEUItem::class)
                            .addModifiers(KModifier.ABSTRACT)
                            .build()
                    )
                    .build()

                val petEnumBuilder = TypeSpec.enumBuilder(petEnumName)
                    .addSuperinterface(ClassName(petPkg, petInterfaceName))
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter("itemId", String::class)
                            .addParameter("rarity", String::class)
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder("itemId", String::class)
                            .addModifiers(KModifier.OVERRIDE)
                            .initializer("itemId")
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder("rarity", String::class)
                            .addModifiers(KModifier.OVERRIDE)
                            .initializer("rarity")
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder("item", NEUItem::class)
                            .addModifiers(KModifier.OVERRIDE)
                            .getter(
                                FunSpec.getterBuilder()
                                    .addStatement(
                                        "return %T.itemById(itemId) ?: error(%S + itemId)",
                                        skyblockItemsClass,
                                        "SkyblockItems missing itemId: "
                                    )
                                    .build()
                            )
                            .build()
                    )

                petEntries.forEach { entry ->
                    val constName = formatPetEnumName(entry.baseName, entry.rarity)
                    petEnumBuilder.addEnumConstant(
                        constName,
                        TypeSpec.anonymousClassBuilder()
                            .addSuperclassConstructorParameter("%S", entry.itemId)
                            .addSuperclassConstructorParameter("%S", entry.rarity)
                            .build()
                    )
                }

                val petObjectBuilder = TypeSpec.objectBuilder(petObjectName)


                val petFileBuilder = FileSpec.builder(petPkg, petObjectName)
                petFileBuilder.addType(petInterface)
                petFileBuilder.addType(petEnumBuilder.build())
                petFileBuilder.addType(petObjectBuilder.build())
                try {
                    val fileName = "$petObjectName.kt"
                    if (alreadyGeneratedInAnyKspOutput(petPkg, fileName)) {
                        environment.logger.info("Skipping $petObjectName generation; file already exists in another module's KSP output")
                    } else {
                        try {
                            environment.codeGenerator.createNewFile(Dependencies.ALL_FILES, petPkg, petObjectName)
                                .writer()
                                .use { w -> petFileBuilder.build().writeTo(w) }
                        } catch (e: Throwable) {
                            if (e is java.nio.file.FileAlreadyExistsException || e is FileAlreadyExistsException) {
                                environment.logger.info("Skipping $petObjectName generation; file already created by KSP: ${e.message}")
                            } else {
                                throw e
                            }
                        }
                    }
                } catch (e: Throwable) {
                    environment.logger.error("Failed to write SkyblockPets: ${e.message}", null)
                    throw e
                }
            }

        } catch (e: Throwable) {
            environment.logger.error("SBEntityGenerators failed: ${e.message}", null)
            throw e
        }
    }

    fun formatMobEnumName(name: String, level: Int? = null): String {
        val stripped = name
            .replace(Regex("§k(§.)*[^§]+((§.)|$)"),"")// needed for mage outlaw
            .replace("\\((Monster|Sea Creature|Miniboss|Boss)\\)$".toRegex(), "")
            .removeColors()
        val cleaned = stripped
            .replace("[^A-Za-z0-9]+".toRegex(), "_")
            .replace("_+".toRegex(), "_")
            .trim('_')
        val titleCased = cleaned
            .split('_')
            .filter { it.isNotBlank() }
            .joinToString("_") { part ->
                val lower = part.lowercase(Locale.US)
                lower.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            }
        val base = if (titleCased.isBlank()) "Item" else titleCased
        val safeBase = if (base.firstOrNull()?.isDigit() == true) "Mob_$base" else base
        val alreadyHasLevel = safeBase.matches(".*_\\d+$".toRegex())
        val appendLevel = level != null && level !in listOf(0, -1) && !alreadyHasLevel
        return if (appendLevel) "${safeBase}_$level" else safeBase
    }
}
