package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.FeatureDependencyResolver.Requirements
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.common.IMinecraft
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import java.lang.reflect.Field
import kotlin.math.max
import kotlin.reflect.jvm.javaField

/**
 * Renders dependency requirements on top of an option row.
 */
class GuiOptionEditorDependencies(
    private val base: GuiOptionEditor,
    private val initialRequirements: Requirements,
    private val dependencyField: Field? = null,
) : GuiOptionEditor(base.getOption()) {
    private data class ButtonHitbox(
        val sourceLabel: String,
        var x1: Int,
        var y1: Int,
        var x2: Int,
        var y2: Int,
        val labelText: String,
    )

    private val buttons = mutableListOf<ButtonHitbox>()
    // private val bannerButtons = mutableListOf<ButtonHitbox>() // bannerButtons removed; we only use rowButtons now
    private val rowButtons = mutableListOf<ButtonHitbox>()
    private var bannerHeight = MIN_BANNER_HEIGHT
    private var hoverTooltip: List<String>? = null
    private var blocked = true
    private var currentRequirements = initialRequirements
    private var dependencyListHeight = 0
    // caches to avoid expensive reflection/string work each frame
    private val ownerInstanceCache = mutableMapOf<Class<*>, Any?>()
    // per-frame satisfied cache to avoid stale values across renders; cleared at start of render
    private val frameSatisfied = mutableMapOf<FeatureDependencyResolver.Dependency, Boolean>()
    private var cachedBannerText: String? = null
    // dependencyField passed in constructor; if null, we'll rely on initialRequirements

    // Async loading state: avoid blocking UI while doing heavier reflection/resolution
    @Volatile
    private var requirementsResolved = false
    @Volatile
    private var loadingRequirements = false

    init {
        // estimate dependency list height before first render to avoid overlaying UI
        val estimatedRow = 14 // reasonable default for a font row
        val padPerRow = 4
        dependencyListHeight = initialRequirements.groups.sumOf { group ->
            // one line for group label
            val groupLines = 1
            val depLines = group.dependencies.size
            (groupLines + depLines) * (estimatedRow + padPerRow)
        }.coerceAtLeast(0)
        // schedule an async expansion later when first shown (lazy start)
    }

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        // Start async resolution on first render if not yet done
        if (!requirementsResolved && !loadingRequirements) {
            loadingRequirements = true
            Thread {
                try {
                    val baseReqs = dependencyField?.let { FeatureDependencyResolver.resolve(it) } ?: initialRequirements
                    val expanded = expandRequirementsRecursive(baseReqs)
                    synchronized(this) {
                        currentRequirements = expanded
                        cachedBannerText = null
                        // reset caches that may have been invalidated by new resolution
                        ownerInstanceCache.clear()
                        frameSatisfied.clear()
                        requirementsResolved = true
                        loadingRequirements = false
                    }
                } catch (_: Throwable) {
                    synchronized(this) {
                        // on any failure, fall back to initialRequirements and mark resolved so we don't retry frequently
                        currentRequirements = initialRequirements
                        requirementsResolved = true
                        loadingRequirements = false
                    }
                }
            }.start()
        }

        // If we're still resolving, show a small non-blocking banner and render base immediately
        if (!requirementsResolved) {
            // Show loading banner but do not block interaction with base content
            val font = IMinecraft.INSTANCE.defaultFontRenderer
            val pad = (base.height * 0.08f).toInt().coerceAtLeast(3)
            val loadingText = "§eLoading requirements..."
            val loadingH = max(font.height + pad * 2, MIN_BANNER_HEIGHT)
            val bannerBottom = y + loadingH
            context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), bannerBottom.toFloat(), 0x33333300)
            val ty = y + (loadingH - font.height) / 2
            context.drawStringScaledMaxWidth(loadingText.asStructuredText(), font, x + pad, ty, true, width - pad * 2, TEXT_COLOR)
            // render base content immediately under the loading banner
            base.render(context, x, y + loadingH, width)
            return
        }

        // Normal (resolved) rendering path
        dependencyField?.let {
            // Prefer to re-resolve live but expand recursively so dependencies-of-dependencies are visible.
            // Only do this when we already performed the initial async resolution to avoid blocking UI on first open.
            if (requirementsResolved) {
                try {
                    currentRequirements = expandRequirementsRecursive(FeatureDependencyResolver.resolve(it))
                } catch (_: Throwable) {
                    // fallback: keep currentRequirements
                }
            }
        } ?: run {
            // keep the async-expanded currentRequirements when possible
            // but if dependencyField exists, prefer to re-resolve live so we reflect runtime changes
            if (dependencyField == null)
        }
        // Do not eagerly clear caches here. Keep satisfiedCache/ownerInstanceCache across frames where possible
        // to reduce flicker and expensive reflection. We still clear them when a dependency is enabled.
        // start of render: clear per-frame satisfied cache so we recompute current states and avoid stale values
        frameSatisfied.clear()
         // compute buttons for rendering so they look correct; still compute on-demand in mouseInput
         buttons.clear()
        val font = IMinecraft.INSTANCE.defaultFontRenderer
        val pad = (base.height * 0.08f).toInt().coerceAtLeast(3)
        val text = cachedBannerText ?: buildBannerText().also { cachedBannerText = it }
        val satisfiedAnyGroup = currentRequirements.groups.any { group ->
            val satisfied = if (group.requireAll) {
                group.dependencies.all { isSatisfied(it) }
            } else {
                group.dependencies.any { isSatisfied(it) }
            }
            satisfied
        }
        blocked = !satisfiedAnyGroup
        // Only reserve banner/dependency list space when blocked (i.e. requirements not satisfied).
        if (blocked) {
            val bannerH = max(font.height + pad * 2, MIN_BANNER_HEIGHT)
            bannerHeight = bannerH
            val bannerBottom = y + bannerH
            val bgColor = BLOCKED_BG
            context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), bannerBottom.toFloat(), bgColor)
            val ty = y + (bannerH - font.height) / 2
            context.drawStringScaledMaxWidth("§c⚠ ${text}".asStructuredText(), font, x + pad, ty, true, width - pad * 2, TEXT_COLOR)

            // render dependency rows and then the base content
            val bodyY = y + bannerH
            // recompute layout each render so positions exactly match; avoids stale offsets
            layoutRowButtons(x, bodyY, width)
            renderDependencyList(context, x, bodyY, width)
            val contentY = bodyY + dependencyListHeight
            base.render(context, x, contentY, width)
            // draw overlay to visually block interaction with base content
            context.drawColoredRect(x.toFloat(), contentY.toFloat(), (x + width).toFloat(), (contentY + base.height).toFloat(), OVERLAY_BG)
        } else {
            // not blocked: clear banner/dependency heights so no reserved space
            bannerHeight = 0
            dependencyListHeight = 0
            base.render(context, x, y, width)
        }
    }

    private fun renderDependencyList(context: RenderContext, x: Int, y: Int, width: Int) {
        // layoutRowButtons should already have been called by the caller (render); avoid recomputing here to keep positions stable
        val font = IMinecraft.INSTANCE.defaultFontRenderer
        val pad = 4
        var cursorY = y
        val mx = IMinecraft.INSTANCE.mouseX
        val my = IMinecraft.INSTANCE.mouseY
        // Flatten dependencies under a single header; keep group messages only if non-blank and render them as small separators
        currentRequirements.groups.forEach { group ->
            if (group.message.isNotBlank()) {
                val gm = group.message.asStructuredText()
                context.drawStringScaledMaxWidth(gm, font, x + pad, cursorY, true, width - pad * 2, TEXT_COLOR)
                cursorY += font.height + pad
            }
            group.dependencies.forEach { dep ->
                val rowHeight = font.height + pad * 2
                val rowTop = cursorY
                val rowBottom = rowTop + rowHeight
                context.drawColoredRect(x.toFloat(), rowTop.toFloat(), (x + width).toFloat(), rowBottom.toFloat(), ROW_BG)
                // Append a short third-party marker if the overall annotated field declares ThirdPartyDependency
                var lblStr = buildDependencyLabel(dep)
                if (dep.source is FeatureDependencyResolver.DependencySource.ThirdParty && dependencyField?.getAnnotation(ThirdPartyDependency::class.java) != null) {
                    val tp = dep.source.value
                    lblStr += " §e(Third-party)"
                    // include main toggle name when present so user understands what's unlockable
                    tp.mainToggleField?.javaField?.let { f ->
                        lblStr += " §7[main: ${f.name}]"
                    }
                }
                val label = lblStr.asStructuredText()
                context.drawStringScaledMaxWidth(label, font, x + pad, rowTop + pad, true, width - pad * 3, TEXT_COLOR)
                val satisfied = frameSatisfied[dep] ?: isSatisfied(dep).also { frameSatisfied[dep] = it }
                if (!satisfied) {
                    // draw the enable button using the layout computed earlier
                    val hb = rowButtons.firstOrNull { it.sourceLabel == dep.label }
                    if (hb != null) drawEnableButton(context, hb.x1, hb.y1, hb.x2 - hb.x1, hb.y2 - hb.y1, hb.labelText.asStructuredText(), mx, my)
                }
                // debug: draw hitbox rectangles/lines if enabled
                if (DEBUG_HITBOX) {
                    rowButtons.firstOrNull { it.sourceLabel == dep.label }?.let { hb ->
                        // semi-transparent red overlay for hitbox
                        context.drawColoredRect(hb.x1.toFloat(), (rowTop).toFloat(), hb.x2.toFloat(), (rowBottom).toFloat(), 0x44FF0000)
                        // green line at rowTop
                        context.drawColoredRect(x.toFloat(), rowTop.toFloat(), (x + width).toFloat(), (rowTop + 1).toFloat(), 0xFF00FF00.toInt())
                        // blue line at button top (hb.y1)
                        context.drawColoredRect(x.toFloat(), hb.y1.toFloat(), (x + width).toFloat(), (hb.y1 + 1).toFloat(), 0xFF0000FF.toInt())
                    }
                }
                cursorY += rowHeight + pad / 2
            }
            cursorY += pad
        }
        // dependencyListHeight already set by layoutRowButtons
    }

    private fun layoutRowButtons(x: Int, y: Int, width: Int) {
         val font = IMinecraft.INSTANCE.defaultFontRenderer
         val padList = 4
         rowButtons.clear()
         var cursorY = y
         // cache annotation presence once
         val hasTpAnn = dependencyField?.getAnnotation(ThirdPartyDependency::class.java) != null
         currentRequirements.groups.forEach { group ->
             // only reserve space for a group header when a message is present (matches renderDependencyList)
             if (group.message.isNotBlank()) cursorY += font.height + padList
             group.dependencies.forEach { dep ->
                 val rowHeight = font.height + padList * 2
                 val rowTop = cursorY
                 val satisfied = frameSatisfied[dep] ?: isSatisfied(dep).also { frameSatisfied[dep] = it }
                 if (!satisfied) {
                     // Determine label: if dependency is a third-party and the annotation requests main-toggle behavior,
                     // show a more descriptive label like "Enable <ThirdParty.DisplayName>"; otherwise use generic "Enable".
                     val label = when (val s = dep.source) {
                         is FeatureDependencyResolver.DependencySource.ThirdParty -> {
                             if (hasTpAnn) {
                                 // descriptive button for annotated third-party dependency
                                 "Enable ${s.value.displayName}"
                             } else {
                                 // fallback to default short label
                                 "Enable"
                             }
                         }
                         else -> "Enable"
                     }
                     val btnW = font.getStringWidth(label) + 8
                     val buttonWidth = btnW.coerceAtLeast(font.height + 8)
                     val btnX = x + width - buttonWidth - padList
                     val btnH = font.height + 4
                     val btnY = rowTop + (rowHeight - btnH) / 2
                     val hb = ButtonHitbox(dep.label, btnX, btnY, btnX + buttonWidth, btnY + btnH, label)
                     rowButtons.add(hb)
                 }
                 cursorY += rowHeight + padList / 2
             }
             cursorY += padList
         }
         dependencyListHeight = (cursorY - y).coerceAtLeast(0)
    }

    private fun buildDependencyLabel(dep: FeatureDependencyResolver.Dependency): String {
        val state = if (isSatisfied(dep)) "§aEnabled" else "§cDisabled"
        return when (val source = dep.source) {
            is FeatureDependencyResolver.DependencySource.ThirdParty -> "${source.value.displayName} - $state"
            is FeatureDependencyResolver.DependencySource.BooleanField -> "${dep.label} - $state"
        }
    }

    private fun drawEnableButton(
        context: RenderContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        label: StructuredText,
        mouseX: Int = -1,
        mouseY: Int = -1,
    ) {
        // improved visuals: subtle rounded-ish look via 1px darker border and lighter top
        val bg = BUTTON_BG
        val border = BUTTON_BORDER
        val topHighlight = BUTTON_HIGHLIGHT
        // fill
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), bg)
        // top highlight
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + 1).toFloat(), topHighlight)
        // border
        context.drawColoredRect(x.toFloat(), (y + height - 1).toFloat(), (x + width).toFloat(), (y + height).toFloat(), border)
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + 1).toFloat(), (y + height).toFloat(), border)
        context.drawColoredRect((x + width - 1).toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), border)

        val font = IMinecraft.INSTANCE.defaultFontRenderer
        // slightly more padding for nicer look; use contrasting text color
        // use smaller left/right padding so button is not larger than needed
        context.drawStringScaledMaxWidth(label, font, x + 4, y + (height - font.height) / 2, true, width - 8, TEXT_COLOR)

        // hover effect (subtle overlay)
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), BUTTON_HOVER_OVERLAY)
        }

        // note: we don't mutate the shared `buttons` list here; hitboxes are recomputed centrally when needed
    }

    private fun isSatisfied(dep: FeatureDependencyResolver.Dependency): Boolean {
        // use satisfied cache if available (avoid recomputing during same frame)
        frameSatisfied[dep]?.let { return it }
        val result = when (val source = dep.source) {
            is FeatureDependencyResolver.DependencySource.ThirdParty -> source.value.isEnabled()
            is FeatureDependencyResolver.DependencySource.BooleanField -> inspectBooleanField(source)
        }
        // store in per-frame cache
        frameSatisfied[dep] = result
        return result
    }

    private fun inspectBooleanField(fieldSource: FeatureDependencyResolver.DependencySource.BooleanField): Boolean {
        return try {
            // prefer object singleton if available (cheap). Only deep-search when necessary.
            val obj = fieldSource.owner.kotlin.objectInstance
            val inst = if (obj != null) obj else runCatching { ownerInstanceCache[fieldSource.owner] ?: findExistingInstance(fieldSource.owner)?.also { ownerInstanceCache[fieldSource.owner] = it } }.getOrNull()
            val instance = inst ?: runCatching { fieldSource.owner.newInstance() }.getOrNull()
            if (instance == null) return false
            fieldSource.getter(instance)
        } catch (_: Throwable) {
            false
        }
    }

    override fun mouseInput(
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        mouseEvent: MouseEvent?,
    ): Boolean {
        val isClick = mouseEvent is MouseEvent.Click && mouseEvent.mouseState
        // compute hitboxes on-demand (font available via IMinecraft.INSTANCE) so mouse events are reliable even before render
        // compute hitboxes once and populate caches (computeHitboxes will populate ownerInstanceCache and satisfiedCache)
        val computed = computeHitboxes(x, y, width)
        // compute the full height reserved for this editor (banner + dependency list + base)
        val totalBannerAndList = bannerHeight + dependencyListHeight
        val dependencyZoneBottom = y + totalBannerAndList
        val insideDependencyZone = mouseX in x..(x + width) && mouseY in y..dependencyZoneBottom
        if (isClick && insideDependencyZone) {
            computed.firstOrNull { mouseX in it.x1..it.x2 && mouseY in it.y1..it.y2 }?.let { hit ->
                // resolve the live Dependency object by label before action
                findCurrentDependencyByLabel(hit.sourceLabel)?.let { dep ->
                    // Shift-click: jump to editor instead of enabling
                    if (at.hannibal2.skyhanni.utils.KeyboardManager.isShiftKeyDown()) {
                        when (val s = dep.source) {
                            is FeatureDependencyResolver.DependencySource.BooleanField -> ConfigUtils.openEditorForField(s.owner, s.fieldName)
                            is FeatureDependencyResolver.DependencySource.ThirdParty -> {
                                // try to jump to the third party's main toggle field if exists
                                val tp = dep.source.value
                                tp.mainToggleField?.javaField?.let { field ->
                                    ConfigUtils.openEditorForField(field.declaringClass, field.name)
                                }
                            }
                        }
                        return true
                    }
                    // normal click: enable
                    enableDependency(dep)
                    return true
                }
            }
        }
        if (blocked) {
            return insideDependencyZone && isClick
        }
        // forward remaining mouse events to base editor, adjusting y to the base content top
        val contentTop = y + totalBannerAndList
        return base.mouseInput(x, contentTop, width, mouseX, mouseY, mouseEvent)
    }

    // Ensure parent layout reserves space for our banner + dependency list
    override fun getHeight(): Int {
        // compute whether we'd be blocked to determine reserved height; keep same resolution logic as render
        // Avoid heavy synchronous resolution on initial load: if we haven't resolved async yet, rely on cached currentRequirements
        val reqs = if (!requirementsResolved) currentRequirements else (dependencyField?.let {
            try { expandRequirementsRecursive(FeatureDependencyResolver.resolve(it)) } catch (_: Throwable) { currentRequirements }
        } ?: currentRequirements)
        val satisfiedAnyGroup = reqs.groups.any { group ->
            if (group.requireAll) group.dependencies.all { isSatisfied(it) } else group.dependencies.any { isSatisfied(it) }
        }
        return if (satisfiedAnyGroup) base.height else base.height + bannerHeight + dependencyListHeight
    }

    private fun enableDependency(dep: FeatureDependencyResolver.Dependency) {
        val summary = buildEnableSummary(dep)
        // show persistent summary in our own hoverTooltip and also through RenderableTooltips for compatibility
        if (summary.isNotEmpty()) {
            hoverTooltip = summary
            RenderableTooltips.setTooltipForRender(summary.map(StringRenderable::from))
        } else {
            hoverTooltip = null
        }
        when (val source = dep.source) {
            is FeatureDependencyResolver.DependencySource.ThirdParty -> {
                source.value.setEnabled(true)
                SkyHanniMod.configManager.recreateConfig()
                // re-resolve requirements immediately and refresh state so UI updates
                dependencyField?.let { currentRequirements = expandRequirementsRecursive(FeatureDependencyResolver.resolve(it)) }
            }
            is FeatureDependencyResolver.DependencySource.BooleanField -> {
                // prefer to enable on existing singleton instance; avoid creating a new instance when owner isn't an object.
                val ownerClass = source.owner
                val singleton = ownerClass.kotlin.objectInstance
                val prop = source.property
                if (prop != null && singleton != null) {
                    try {
                        prop.set(singleton, true)
                        SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "dependency-unlock")
                        // rebuild config processor and editor so the option state refreshes everywhere
                        SkyHanniMod.configManager.recreateConfig()
                        // clear caches so subsequent render/input recomputes fresh state
                        ownerInstanceCache.clear(); cachedBannerText = null
                        dependencyField?.let { currentRequirements = expandRequirementsRecursive(FeatureDependencyResolver.resolve(it)) }
                        return
                    } catch (_: Throwable) {
                        // fallthrough to editor navigation
                    }
                }
                // prefer to find any existing instance of the owner class in known roots and set it there
                val existing = findExistingInstance(ownerClass)
                if (prop != null && existing != null) {
                    try {
                        prop.set(existing, true)
                        SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "dependency-unlock")
                        SkyHanniMod.configManager.recreateConfig()
                        ownerInstanceCache.clear(); cachedBannerText = null
                        dependencyField?.let { currentRequirements = expandRequirementsRecursive(FeatureDependencyResolver.resolve(it)) }
                        return
                    } catch (_: Throwable) {
                        // fallthrough
                    }
                }
                // if no Kotlin mutable property or it failed, try to set the java boolean field directly
                try {
                    val javaField = ownerClass.declaredFields.firstOrNull { it.name == source.fieldName }
                    if (javaField != null) {
                        val instanceForField = existing ?: findExistingInstance(ownerClass) ?: ownerClass.kotlin.objectInstance
                        if (instanceForField != null) {
                            javaField.isAccessible = true
                            if (javaField.type == java.lang.Boolean.TYPE || javaField.type == java.lang.Boolean::class.java) {
                                javaField.setBoolean(instanceForField, true)
                                SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "dependency-unlock")
                                SkyHanniMod.configManager.recreateConfig()
                                ownerInstanceCache.clear(); cachedBannerText = null
                                dependencyField?.let { currentRequirements = expandRequirementsRecursive(FeatureDependencyResolver.resolve(it)) }
                                return
                            }
                        }
                    }
                } catch (_: Throwable) {
                    // ignore and fall through to editor view
                }

                 // if we can't set the field programmatically, open the editor to the option so user can enable it
                 prop?.let { p ->
                     // try to jump to editor bound to an object instance if possible
                     singleton?.let { s -> p.jumpToEditor(s); return }
                 }
                 // fallback: open editor by field name on the owner class
                 ConfigUtils.openEditorForField(ownerClass, source.fieldName)
             }
         }
     }

    /**
     * Try to find an existing live instance of the given type in known roots (SkyHanniMod fields).
     */
    private fun deepFindInstance(root: Any, target: Class<*>, visited: MutableSet<Any>, depth: Int = 0): Any? {
        if (depth > 8) return null
        if (target.isInstance(root)) return root
        if (root in visited) return null
        visited.add(root)
        for (f in root.javaClass.declaredFields) {
            try {
                f.isAccessible = true
                val v = f.get(root) ?: continue
                if (target.isInstance(v)) return v
                val found = deepFindInstance(v, target, visited, depth + 1) ?: continue
                return found
            } catch (_: Throwable) {
                // ignore
            }
        }
        return null
    }

    private fun findExistingInstance(owner: Class<*>): Any? {
        // check primary features object
        val features = SkyHanniMod.feature
        deepFindInstance(features, owner, mutableSetOf())?.let { return it }
        // fallback: check top-level SkyHanniMod static fields
        if (owner.isInstance(features)) return features
        for (f in SkyHanniMod::class.java.declaredFields) {
            try {
                f.isAccessible = true
                val v = f.get(null) ?: continue
                if (owner.isInstance(v)) return v
            } catch (_: Throwable) {
                // ignore
            }
        }
        return null
    }

    private fun buildEnableSummary(dep: FeatureDependencyResolver.Dependency): List<String> {
        // Build summary listing what enabling this dependency will unlock. Use the expanded currentRequirements so
        // dependencies of dependencies are included.
        val dependents = currentRequirements.groups
            .filter { group -> group.dependencies.contains(dep) }
            .flatMap { group -> group.dependencies.map { it.label } }
            .distinct()
        return if (dependents.isEmpty()) emptyList() else buildList {
            add("§7Enabling dependency unlocks:")
            dependents.forEach { add(" §8- §f$it") }
            // if this dep is a third-party, surface main toggle info as well
            if (dep.source is FeatureDependencyResolver.DependencySource.ThirdParty) {
                val tp = dep.source.value
                tp.mainToggleField?.javaField?.let { f -> add("\n§7Main toggle: §f${f.name}") }
            }
        }
    }

    private fun buildBannerText(): String {
        // Simplified header: indicate whether requirements are 'all' (every dependency in single group) or 'any'
        if (currentRequirements.groups.isEmpty()) return "Requires"
        val needsAll = currentRequirements.groups.size == 1 && currentRequirements.groups[0].requireAll
        return if (needsAll) "§8Requires: §f(all)" else "§8Requires: §f(any)"
    }

    override fun mouseInputOverlay(
        x: Int,
        y: Int,
        width: Int,
        mouseX: Int,
        mouseY: Int,
        mouseEvent: MouseEvent?,
    ): Boolean {
        // forward overlay mouse events to base with Y offset of banner
        val bannerAndList = bannerHeight + dependencyListHeight
        return base.mouseInputOverlay(x, y + bannerAndList, width, mouseX, mouseY, mouseEvent)
    }

    override fun renderOverlay(context: RenderContext, x: Int, y: Int, width: Int) {
        // show tooltip for banner when hovering; if hoverTooltip set (e.g. after enabling) show it, otherwise compute from requirements
        val tips = hoverTooltip ?: buildList {
            add("§7Dependencies")
            currentRequirements.groups.forEach { group ->
                add((group.message.ifBlank { "Requires" }))
                group.dependencies.forEach { add(" - ${when (val s = it.source) {
                    is FeatureDependencyResolver.DependencySource.ThirdParty -> s.value.displayName
                    is FeatureDependencyResolver.DependencySource.BooleanField -> it.label
                }}") }
            }
            // include third-party annotation message if set on the original dependencyField
            dependencyField?.getAnnotation(ThirdPartyDependency::class.java)?.let { tpAnn ->
                if (tpAnn.message.isNotBlank()) add(" ") /* separator */
                add("§6${tpAnn.value.displayName}: ${tpAnn.message}")
            }
         }
        val mx = IMinecraft.INSTANCE.mouseX
        val my = IMinecraft.INSTANCE.mouseY
        val bannerBottom = y + bannerHeight
        if (mx in x..(x + width) && my in y..bannerBottom) {
            RenderableTooltips.setTooltipForRender(tips.map(StringRenderable::from))
        }
        // forward overlay rendering for base (content) offset by bannerHeight
        base.renderOverlay(context, x, y + bannerHeight + dependencyListHeight, width)
    }

    /**
     * Compute button hitboxes and update banner/dependency list heights using font metrics.
     * This is safe to call before render().
     */
    private fun computeHitboxes(x: Int, y: Int, width: Int): List<ButtonHitbox> {
        // Recompute currentRequirements and per-frame satisfied cache similar to render so hitboxes line up
        // Avoid blocking synchronous resolution on first load: only re-resolve when async resolution has completed.
        if (requirementsResolved) {
            dependencyField?.let { currentRequirements = try { expandRequirementsRecursive(FeatureDependencyResolver.resolve(it)) } catch (_: Throwable) { currentRequirements } }
        } // else: keep currentRequirements (initial/previous) so UI is immediate
        frameSatisfied.clear()
        // decide whether blocked
        val font = IMinecraft.INSTANCE.defaultFontRenderer
        val pad = (base.height * 0.08f).toInt().coerceAtLeast(3)
        val satisfiedAnyGroup = currentRequirements.groups.any { group ->
            if (group.requireAll) group.dependencies.all { isSatisfied(it) } else group.dependencies.any { isSatisfied(it) }
        }
        val localBlocked = !satisfiedAnyGroup
        if (localBlocked) {
            val bannerH = max(font.height + pad * 2, MIN_BANNER_HEIGHT)
            bannerHeight = bannerH
            // populate layout hitboxes for rows using the same logic as render so positions match
            layoutRowButtons(x, y + bannerH, width)
        } else {
            // not blocked: no banner or dependency rows reserved
            bannerHeight = 0
            dependencyListHeight = 0
            rowButtons.clear()
        }
        buttons.clear(); buttons.addAll(rowButtons)
        return buttons
     }

     private fun findCurrentDependencyByLabel(label: String): FeatureDependencyResolver.Dependency? {
        currentRequirements.groups.forEach { g ->
            g.dependencies.forEach { d -> if (d.label == label) return d }
        }
        return null
    }

    /**
     * Expand given requirements recursively by resolving any dependencies-of-dependencies (both boolean fields and
     * third-party main toggles). Prevent cycles by tracking visited owners/thirdparties.
     */
    private fun expandRequirementsRecursive(root: Requirements): Requirements {
        val outGroups = mutableListOf<FeatureDependencyResolver.RequirementGroup>()
        val visitedFields = mutableSetOf<Pair<String, String>>() // ownerName#fieldName
        val visitedThird = mutableSetOf<String>()

        fun resolveFieldIfPresent(owner: Class<*>, fieldName: String) {
            val key = Pair(owner.name, fieldName)
            if (key in visitedFields) return
            visitedFields.add(key)
            val javaField = runCatching { owner.getDeclaredField(fieldName) }.getOrNull() ?: return
            val sub = FeatureDependencyResolver.resolve(javaField)
            if (sub.groups.isNotEmpty()) {
                outGroups.addAll(sub.groups)
                // recurse into sub dependencies
                sub.groups.forEach { g ->
                    g.dependencies.forEach { sd ->
                        when (val s = sd.source) {
                            is FeatureDependencyResolver.DependencySource.BooleanField -> resolveFieldIfPresent(s.owner, s.fieldName)
                            is FeatureDependencyResolver.DependencySource.ThirdParty -> s.value.mainToggleField?.javaField?.let { f ->
                                if (s.value.id !in visitedThird) {
                                    visitedThird.add(s.value.id)
                                    val sub2 = FeatureDependencyResolver.resolve(f)
                                    outGroups.addAll(sub2.groups)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Start by adding original groups, then expand
        outGroups.addAll(root.groups)
        root.groups.forEach { g ->
            g.dependencies.forEach { dep ->
                when (val s = dep.source) {
                    is FeatureDependencyResolver.DependencySource.BooleanField -> resolveFieldIfPresent(s.owner, s.fieldName)
                    is FeatureDependencyResolver.DependencySource.ThirdParty -> {
                        // resolve third-party's main toggle field if present
                        s.value.mainToggleField?.javaField?.let { f ->
                            val tpId = s.value.id
                            if (tpId !in visitedThird) {
                                visitedThird.add(tpId)
                                val sub = FeatureDependencyResolver.resolve(f)
                                if (sub.groups.isNotEmpty()) outGroups.addAll(sub.groups)
                                // also try to recurse into those groups
                                sub.groups.forEach { sg -> sg.dependencies.forEach { sd ->
                                    if (sd.source is FeatureDependencyResolver.DependencySource.BooleanField) {
                                        val bf = sd.source
                                        resolveFieldIfPresent(bf.owner, bf.fieldName)
                                    }
                                }}
                            }
                        }
                    }
                }
            }
        }

        return Requirements(outGroups)
    }

    companion object {
        private const val MIN_BANNER_HEIGHT = 16
        private const val BLOCKED_BG = 0x33FF8888
        private const val TEXT_COLOR = -0x1
        private const val OVERLAY_BG = 0x55000000
        private const val ROW_BG = 0x22000000
        private const val BUTTON_BG = 0xFF2E7D32.toInt()
        private const val BUTTON_BORDER = 0xFF1B5E20.toInt()
        private const val BUTTON_HIGHLIGHT = 0xFF66BB6A.toInt()
        private const val BUTTON_HOVER_OVERLAY = 0x44333333
        // Debugging: when true, draw hitboxes for enable buttons
        private const val DEBUG_HITBOX = true
    }
 }
