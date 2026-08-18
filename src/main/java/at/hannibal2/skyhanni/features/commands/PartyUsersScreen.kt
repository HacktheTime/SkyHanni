package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.data.FriendApi
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import org.lwjgl.glfw.GLFW

/**
 * Standalone screen for editing per-user party command permissions.
 *
 * All mutable UI state lives here.
 * All Renderable construction is delegated to [PartyUsersGui].
 */
class PartyUsersScreen : SkyHanniBaseScreen() {
    private val config get() = SkyHanniMod.feature.misc.partyCommands

    val addNameInput = TextInput()
    val listScrollValue = ScrollValue()

    var activeInput: TextInput? = null
    private var display: Renderable? = null
    val userNames = config.users.keys.sorted().toMutableList()
    private var suggestionIndex = 0

    /** Whether the friend-suggestion dropdown panel is currently open. */
    var suggestionsOpen = false
    val suggestionsScroll = ScrollValue()

    init {
        addNameInput.registerToEvent(ADD_NAME_FIELD) {
            suggestionIndex = 0
            rebuildDisplay()
        }
    }

    /**
     * Names matching the currently typed text, from friends and current party members,
     * excluding users that are already configured.
     */
    fun suggestedNames(): List<String> {
        val typed = addNameInput.textBox.trim()
        if (typed.isEmpty()) return emptyList()
        return allCandidates()
            .filter { it.startsWith(typed, ignoreCase = true) && !config.users.containsKey(it) }
            .take(MAX_SUGGESTIONS)
    }

    /**
     * All candidate friend/party names for the dropdown, filtered by the typed text.
     * Unlike [suggestedNames] this is not capped so it can be scrolled through.
     */
    fun dropdownCandidates(): List<String> {
        val typed = addNameInput.textBox.trim()
        return allCandidates()
            .filter { typed.isEmpty() || it.startsWith(typed, ignoreCase = true) }
            .filterNot { config.users.containsKey(it) }
    }

    private fun allCandidates(): List<String> = buildList {
        addAll(FriendApi.getAllFriends().map { it.name })
        addAll(PartyApi.partyMembers)
    }.map { it.cleanPlayerName() }.distinct().sorted()

    /** TAB cycles through the suggested names, filling the add field. */
    fun completeName() {
        val names = suggestedNames()
        if (names.isEmpty()) return
        if (suggestionIndex >= names.size) suggestionIndex = 0
        addNameInput.textBox = names[suggestionIndex]
        suggestionIndex++
        rebuildDisplay()
    }

    fun selectSuggestion(name: String) {
        addNameInput.textBox = name
        suggestionIndex = 0
        suggestionsOpen = false
        rebuildDisplay()
    }

    /**
     * Rebuilds the Renderable tree. Call on structural changes such as adding or removing users.
     * Do not call while the user is actively typing.
     */
    fun rebuildDisplay() {
        display = PartyUsersGui.buildDisplay(this)
    }

    override fun onInitGui() = rebuildDisplay()

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground(mouseX, mouseY, partialTicks)
        val renderable = display ?: return
        val startX = (width - renderable.width) / 2
        val startY = (height - renderable.height) / 2

        DrawContextUtils.pushPop {
            DrawContextUtils.translate(startX.toFloat(), startY.toFloat())
            Renderable.withMousePosition(mouseX - startX, mouseY - startY) {
                renderable.render(0, 0)
            }
        }
    }

    override fun onKeyTyped(typedChar: Char?, keyCode: Int?) = keyCode?.let {
        when {
            keyCode == GLFW.GLFW_KEY_ESCAPE || KeyboardManager.checkIsInventoryClosure(keyCode) -> onClose()
            keyCode == GLFW.GLFW_KEY_TAB && activeInput === addNameInput -> completeName()
            keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER -> addUser()
        }
    } ?: Unit

    override fun isPauseScreen() = false

    fun addUser() {
        val name = addNameInput.textBox.trim().cleanPlayerName()
        if (name.isEmpty()) return
        if (config.users.containsKey(name)) {
            ChatUtils.chat("§c$name is already configured.")
            addNameInput.clear()
            return
        }
        config.users[name] = config.TrustUserConfig(name)
        userNames.add(name)
        userNames.sort()
        addNameInput.clear()
        save()
        rebuildDisplay()
    }

    fun removeUser(name: String) {
        config.users.remove(name)
        userNames.remove(name)
        save()
        rebuildDisplay()
    }

    fun openUserEditor(name: String) {
        val userConfig = config.users[name] ?: return
        userConfig.ensurePermissions()
        PartyUsersGui.openEditor(userConfig, this)
    }

    private fun save() {
        SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "Updated party command users")
    }

    private companion object {
        const val ADD_NAME_FIELD = 0x50A2
        const val MAX_SUGGESTIONS = 8
    }
}