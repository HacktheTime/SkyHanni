package at.hannibal2.skyhanni.features.misc.massconfiguration

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.ThirdPartyPolicy
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.config.features.About
import at.hannibal2.skyhanni.events.hypixel.HypixelJoinEvent
import at.hannibal2.skyhanni.features.misc.update.ChangelogViewer
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver

@SkyHanniModule
object DefaultConfigFeatures {

    private var didNotifyOnce = false

    @HandleEvent
    fun onHypixelJoin(event: HypixelJoinEvent) {
        if (didNotifyOnce) return
        didNotifyOnce = true

        val knownToggles = SkyHanniMod.knownFeaturesData.knownFeatures
        val updated = SkyHanniMod.VERSION !in knownToggles
        val processor = FeatureToggleProcessor()
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.processConfig(SkyHanniMod.feature)
        knownToggles[SkyHanniMod.VERSION] = processor.allOptions.map { it.path }
        SkyHanniMod.configManager.saveConfig(ConfigFileType.KNOWN_FEATURES, "Updated known feature flags")
        if (!SkyHanniMod.feature.storage.hasPlayedBefore) {
            SkyHanniMod.feature.storage.hasPlayedBefore = true
            ChatUtils.clickableChat(
                "Looks like this is the first time you are using SkyHanni. " +
                    "Click here to configure default options, or run /shdefaultoptions.",
                onClick = { onCommand("null", "null") },
                "§eClick to run /shdefaultoptions!",
            )
        } else if (updated) {
            val lastVersion = knownToggles.keys.lastOrNull { it != SkyHanniMod.VERSION }
                ?: ErrorManager.skyHanniError(
                    "lastVersion is null, this should never happen",
                    "knownToggles" to knownToggles,
                    "version" to SkyHanniMod.VERSION,
                )
            val command = "/shdefaultoptions $lastVersion ${SkyHanniMod.VERSION}"
            ChatUtils.chat("Looks like you updated SkyHanni.")
            ChatUtils.clickableChat(
                "Click here to configure the newly introduced options, or run $command.",
                onClick = { onCommand(lastVersion, SkyHanniMod.VERSION) },
                "§eClick to run /shdefaultoptions $lastVersion ${SkyHanniMod.VERSION}!",
            )
            ChatUtils.clickableChat(
                "Click here to see the changelog.",
                onClick = {
                    ChangelogViewer.showChangelog(lastVersion, SkyHanniMod.VERSION)
                },
            )
        }
    }

    private fun onCommand(old: String, new: String) {
        // Default behavior: open selective default config GUI with only new options
        val newDefaultOptionsScreen = SkyHanniMod.feature.about.shDefaultOptionsScreen
        if (newDefaultOptionsScreen == About.ShDefaultOptionsScreen.GROUPING || (old == "null" && new == "null")) {
            val processor = FeatureToggleProcessor()
            val driver = ConfigProcessorDriver(processor)
            driver.warnForPrivateFields = false
            driver.processConfig(SkyHanniMod.feature)
            var optionList = processor.orderedOptions
            val knownToggles = SkyHanniMod.knownFeaturesData.knownFeatures
            val togglesInNewVersion = knownToggles[new]
            if (new != "null" && togglesInNewVersion == null) {
                ChatUtils.chat("Unknown version $new")
                return
            }
            val togglesInOldVersion = knownToggles[old]
            if (old != "null" && togglesInOldVersion == null) {
                ChatUtils.chat("Unknown version $old")
                return
            }
            optionList = optionList
                .mapValues { option ->
                    option.value.filter {
                        (togglesInNewVersion == null || it.path in togglesInNewVersion) &&
                            (togglesInOldVersion == null || it.path !in togglesInOldVersion)
                    }
                }
                .filter { (_, filteredOptions) -> filteredOptions.isNotEmpty() }
            if (optionList.isEmpty()) {
                ChatUtils.chat("There are no new options to configure between $old and $new")
                return
            }
            SkyHanniMod.screenToOpen = DefaultConfigOptionGui(optionList, old, new)
            return
        } else if (newDefaultOptionsScreen == null) {
            ChatUtils.clickableChat(
                "Do you want to use a grouped version to enable all features (click here)",
                onClick = {
                    SkyHanniMod.feature.about.shDefaultOptionsScreen = About.ShDefaultOptionsScreen.GROUPING
                    onCommand(old, new)
                },
            )
            ChatUtils.clickableChat(
                "or a config screen to select all individually but with only new options (click here)?",
                onClick = {
                    SkyHanniMod.feature.about.shDefaultOptionsScreen = About.ShDefaultOptionsScreen.ONLY_NEW_MOUL_CONFIG
                    onCommand(old, new)
                },
            )
        } else if (newDefaultOptionsScreen == About.ShDefaultOptionsScreen.ONLY_NEW_MOUL_CONFIG) {
            openNewOptionsOnly(old, new)
        }
    }

    /** Opens the default options GUI but scoped to only new options. */
    private fun openNewOptionsOnly(old: String, new: String) {
        val processor = FeatureToggleProcessor()
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.processConfig(SkyHanniMod.feature)
        val knownToggles = SkyHanniMod.knownFeaturesData.knownFeatures
        val togglesInNewVersion = knownToggles[new] ?: emptyList()
        val togglesInOldVersion = knownToggles[old] ?: emptyList()
        val allowed = processor.allOptions
            .filter { opt ->
                (new == "null" || opt.path in togglesInNewVersion) &&
                    (old == "null" || opt.path !in togglesInOldVersion)
            }
            .map { it.path }
            .toSet()
        if (allowed.isEmpty()) {
            ChatUtils.chat("No new options available between $old and $new")
            return
        }
        FilteredConfigGui.open(allowed)
    }

    private fun openNewOptionsList(old: String, new: String) {
        val processor = FeatureToggleProcessor()
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.processConfig(SkyHanniMod.feature)
        val knownToggles = SkyHanniMod.knownFeaturesData.knownFeatures
        val togglesInNewVersion = knownToggles[new] ?: emptyList()
        val togglesInOldVersion = knownToggles[old] ?: emptyList()
        val groups = processor.orderedOptions
            .mapNotNull { (cat, opts) ->
                val newOpts = opts.filter { opt ->
                    (new == "null" || opt.path in togglesInNewVersion) &&
                        (old == "null" || opt.path !in togglesInOldVersion)
                }
                if (newOpts.isEmpty()) null else NewOptionsListScreen.OptionGroup(cat.name, newOpts)
            }
        if (groups.isEmpty()) {
            ChatUtils.chat("No new options to list between $old and $new")
            return
        }
        SkyHanniMod.screenToOpen = NewOptionsListScreen("§dNew Options", groups)
    }

    fun applyCategorySelections(
        resetSuggestionState: MutableMap<Category, ResetSuggestionState>,
        orderedOptions: Map<Category, List<FeatureToggleableOption>>,
    ) {
        for ((cat, options) in orderedOptions) {
            for (option in options) {
                val resetState = option.toggleOverride ?: resetSuggestionState[cat]!!
                if (resetState == ResetSuggestionState.LEAVE_DEFAULTS) continue
                val onState = option.isTrueEnabled
                val setTo = if (resetState == ResetSuggestionState.TURN_ALL_ON) {
                    onState
                } else {
                    !onState
                }
                // Do not auto-enable third-party dependent features unless consent allows it.
                val tp = option.thirdParty
                if (tp != null) {
                    // Only skip when attempting to enable; disabling is always allowed.
                    val isEnabling = setTo == option.isTrueEnabled
                    if (isEnabling && ThirdPartyPolicy.shouldSkipAutoEnable(tp)) {
                        continue
                    }
                }
                option.setter(setTo)
            }
        }
    }

    private val autocomplete get() = SkyHanniMod.knownFeaturesData.knownFeatures.keys + listOf("null")

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shdefaultoptions") {
            description = "Select default options"
            arg("oldVersion", BrigadierArguments.string(), BrigadierUtils.dynamicSuggestionProvider { autocomplete }) { oldVersion ->
                arg("newVersion", BrigadierArguments.string(), BrigadierUtils.dynamicSuggestionProvider { autocomplete }) { newVersion ->
                    callback {
                        onCommand(getArg(oldVersion), getArg(newVersion))
                    }
                }
                callback {
                    onCommand(getArg(oldVersion), "null")
                }
            }
            simpleCallback {
                onCommand("null", "null")
            }
        }
        event.registerBrigadier("shnewoptions") {
            description = "Open MoulConfig with only new options"
            arg("oldVersion", BrigadierArguments.string(), BrigadierUtils.dynamicSuggestionProvider { autocomplete }) { oldVersion ->
                arg("newVersion", BrigadierArguments.string(), BrigadierUtils.dynamicSuggestionProvider { autocomplete }) { newVersion ->
                    callback {
                        openNewOptionsOnly(getArg(oldVersion), getArg(newVersion))
                    }
                }
                callback {
                    openNewOptionsOnly(getArg(oldVersion), "null")
                }
            }
            simpleCallback {
                openNewOptionsOnly("null", "null")
            }
        }
        event.registerBrigadier("shnewoptionslist") {
            description = "List only new options (simple viewer)"
            arg("oldVersion", BrigadierArguments.string(), BrigadierUtils.dynamicSuggestionProvider { autocomplete }) { oldVersion ->
                arg("newVersion", BrigadierArguments.string(), BrigadierUtils.dynamicSuggestionProvider { autocomplete }) { newVersion ->
                    callback {
                        openNewOptionsList(getArg(oldVersion), getArg(newVersion))
                    }
                }
                callback {
                    openNewOptionsList(getArg(oldVersion), "null")
                }
            }
            simpleCallback {
                openNewOptionsList("null", "null")
            }
        }
    }
}
