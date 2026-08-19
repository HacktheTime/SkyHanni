package at.hannibal2.skyhanni.config

import java.lang.reflect.Field
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/** Aggregates dependency metadata for config fields/classes. */
object FeatureDependencyResolver {
    sealed interface DependencySource {
        data class ThirdParty(val value: at.hannibal2.skyhanni.config.ThirdParty) : DependencySource
        data class BooleanField(
            val owner: Class<*>,
            val fieldName: String,
            val getter: (Any) -> Boolean,
            val property: KMutableProperty1<Any, Boolean>?,
            /** Field the dependency should jump to / get its config name from (delegate targets). */
            val effectiveFieldName: String = fieldName,
            /** Custom way to enable the dependency (e.g. third-party main toggle). */
            val enabler: ((Any) -> Unit)? = null,
        ) : DependencySource
    }

    data class Dependency(
        val label: String,
        val description: String,
        val source: DependencySource,
    )

    data class RequirementGroup(
        val dependencies: List<Dependency>,
        val requireAll: Boolean,
        val message: String,
    )

    data class Requirements(val groups: List<RequirementGroup>) {
        val isEmpty: Boolean get() = groups.isEmpty()
    }

    fun resolve(field: Field): Requirements {
        val annotations = mutableListOf<FeatureDependencyRequirement>()
        collectAnnotations(field, annotations)
        val thirdParty = collectThirdParty(field)
        if (annotations.isEmpty() && thirdParty == null) return Requirements(emptyList())

        val groups = annotations.mapNotNull { group ->
            val deps = group.value.mapNotNull { parseDependency(it, field.declaringClass) }
            if (deps.isEmpty()) null else RequirementGroup(deps, group.requireAll, group.message)
        }.toMutableList()
        // A @ThirdPartyDependency acts as a requirement on the third party's main toggle,
        // except when the field itself is that main toggle (no self-dependency).
        if (thirdParty != null && !isMainToggleField(thirdParty.value, field)) {
            dependencyForThirdParty(thirdParty.value).let { dep ->
                groups.add(RequirementGroup(listOf(dep), requireAll = true, message = thirdParty.message))
            }
        }
        return Requirements(groups)
    }

    private fun isMainToggleField(tp: ThirdParty, field: Field): Boolean {
        val mainToggle = tp.mainToggleField?.javaField ?: return false
        return mainToggle.declaringClass == field.declaringClass && mainToggle.name == field.name
    }

    /**
     * Resolves a third party to a dependency on its main toggle field (so it behaves like a
     * regular boolean requirement: config name label, jump and enable via the toggle).
     * Falls back to a plain third-party dependency when the third party has no main toggle.
     */
    private fun dependencyForThirdParty(tp: ThirdParty): Dependency {
        val mainToggle = tp.mainToggleField?.javaField
        if (mainToggle != null) {
            val owner = mainToggle.declaringClass
            val fieldName = mainToggle.name
            val label = configOptionName(owner, fieldName) ?: fieldName
            return Dependency(
                label,
                "Requires '$label' to be enabled",
                DependencySource.BooleanField(
                    owner = owner,
                    fieldName = fieldName,
                    getter = { tp.isEnabled() },
                    property = null,
                    effectiveFieldName = fieldName,
                    enabler = { tp.setEnabled(true) },
                ),
            )
        }
        return Dependency(tp.displayName, tp.description, DependencySource.ThirdParty(tp))
    }

    private fun parseDependency(raw: String, context: Class<*>): Dependency? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        ThirdParty.entries.firstOrNull { it.id.equals(trimmed, true) || it.name.equals(trimmed, true) }?.let { tp ->
            return dependencyForThirdParty(tp)
        }

        val ownerPart = trimmed.substringBefore('#', "")
        val fieldName = trimmed.substringAfter('#', trimmed)
        val owner = when {
            ownerPart.isBlank() -> context
            ownerPart.contains('.') -> runCatching { Class.forName(ownerPart) }.getOrNull()
            else -> findRelativeClass(context, ownerPart)
        } ?: return null

        // Getter: resolve Kotlin property or Java field on-demand when getter is first invoked.
        val getter = fun(receiver: Any): Boolean {
            try {
                val kotlinProp = owner.kotlin.memberProperties
                    .firstOrNull { it.name == fieldName }
                    ?.takeIf { it.returnType.classifier == Boolean::class }
                    ?.also { it.isAccessible = true }
                if (kotlinProp != null) {
                    @Suppress("UNCHECKED_CAST")
                    val typed = kotlinProp as KProperty1<Any, *>
                    return (typed.get(receiver) as? Boolean) ?: false
                }
                val javaField = owner.declaredFields.firstOrNull {
                    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                    it.name == fieldName && (it.type == java.lang.Boolean.TYPE || it.type == java.lang.Boolean::class.java)
                }
                    ?.apply { isAccessible = true }
                if (javaField != null) {
                    return javaField.getBoolean(receiver)
                }
            } catch (_: Throwable) {
                // ignore and fall through
            }
            return false
        }
        // Resolve the mutable Kotlin property so it can be enabled later via reflection.
        // Private delegate get/set properties have no backing field and are accessed
        // through the Kotlin property with isAccessible = true.
        val mutableProp: KMutableProperty1<Any, Boolean>? = runCatching {
            owner.kotlin.memberProperties
                .firstOrNull { it.name == fieldName }
                ?.takeIf { it is KMutableProperty1<*, *> && it.returnType.classifier == Boolean::class }
                ?.also { it.isAccessible = true }
                ?.let {
                    @Suppress("UNCHECKED_CAST")
                    it as KMutableProperty1<Any, Boolean>
                }
        }.getOrNull()
        // A delegate property references the real config option via @DependencyDelegate;
        // use the referenced field for the label and for jumping, while the delegate stays the gate.
        val effectiveFieldName = owner.kotlin.memberProperties
            .firstOrNull { it.name == fieldName }
            ?.findAnnotation<DependencyDelegate>()
            ?.field
            ?: fieldName
        val label = configOptionName(owner, effectiveFieldName) ?: effectiveFieldName
        return Dependency(
            label,
            "Requires '$label' to be enabled",
            DependencySource.BooleanField(owner, fieldName, getter, mutableProp, effectiveFieldName),
        )
    }

    /** Returns the user-facing config name of the given field, falling back to the internal name. */
    private fun configOptionName(owner: Class<*>, fieldName: String): String? {
        runCatching { owner.getDeclaredField(fieldName) }.getOrNull()?.let { javaField ->
            javaField.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigOption::class.java)?.let { ann ->
                if (ann.name.isNotBlank()) return ann.name
            }
        }
        owner.kotlin.memberProperties.firstOrNull { it.name == fieldName }?.let { kp ->
            kp.findAnnotation<io.github.notenoughupdates.moulconfig.annotations.ConfigOption>()?.let { ann ->
                if (ann.name.isNotBlank()) return ann.name
            }
        }
        return null
    }

    private fun findRelativeClass(context: Class<*>, simpleName: String): Class<*>? {
        val packageName = context.`package`?.name ?: return null
        return runCatching { Class.forName("$packageName.$simpleName") }.getOrNull()
    }

    private fun collectAnnotations(field: Field, sink: MutableList<FeatureDependencyRequirement>) {
        field.getAnnotationsByType(FeatureDependencyRequirement::class.java).let { sink.addAll(it) }
        var owner: Class<*>? = field.declaringClass
        while (owner != null) {
            owner.getAnnotationsByType(FeatureDependencyRequirement::class.java).let { sink.addAll(it) }
            owner = owner.enclosingClass
        }
    }

    /** Nearest @ThirdPartyDependency annotation (field first, then enclosing classes). */
    private fun collectThirdParty(field: Field): ThirdPartyDependency? =
        field.getAnnotation(ThirdPartyDependency::class.java)
            ?: generateSequence(field.declaringClass) { it.enclosingClass }
                .mapNotNull { it.getAnnotation(ThirdPartyDependency::class.java) }
                .firstOrNull()
}
