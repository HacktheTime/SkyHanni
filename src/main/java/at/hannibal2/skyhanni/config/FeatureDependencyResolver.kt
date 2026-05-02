package at.hannibal2.skyhanni.config

import java.lang.reflect.Field
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/** Aggregates dependency metadata for config fields/classes. */
object FeatureDependencyResolver {
    sealed interface DependencySource {
        data class ThirdParty(val value: at.hannibal2.skyhanni.config.ThirdParty) : DependencySource
        data class BooleanField(
            val owner: Class<*>,
            val fieldName: String,
            val getter: (Any) -> Boolean,
            val property: KMutableProperty1<Any, Boolean>?,
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
        if (annotations.isEmpty()) return Requirements(emptyList())

        val groups = annotations.mapNotNull { group ->
            val deps = group.value.mapNotNull { parseDependency(it, field.declaringClass) }
            if (deps.isEmpty()) null else RequirementGroup(deps, group.requireAll, group.message)
        }
        return Requirements(groups)
    }

    private fun parseDependency(raw: String, context: Class<*>): Dependency? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        ThirdParty.entries.firstOrNull { it.id.equals(trimmed, true) || it.name.equals(trimmed, true) }?.let { tp ->
            return Dependency(tp.displayName, tp.description, DependencySource.ThirdParty(tp))
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
                val javaField = owner.declaredFields.firstOrNull { it.name == fieldName && (it.type == java.lang.Boolean.TYPE || it.type == java.lang.Boolean::class.java) }
                    ?.apply { isAccessible = true }
                if (javaField != null) {
                    return javaField.getBoolean(receiver)
                }
            } catch (_: Throwable) {
                // ignore and fall through
            }
            return false
        }
        // do not resolve mutable property now; attempt when enabling
        val mutableProp: KMutableProperty1<Any, Boolean>? = null
        // Prefer to use the @ConfigOption name if present for a user-friendly label
        var optionName: String? = null
        // try java field first
        val javaFieldForLabel = runCatching { owner.getDeclaredField(fieldName) }.getOrNull()?.apply { isAccessible = true }
        javaFieldForLabel?.getAnnotation(io.github.notenoughupdates.moulconfig.annotations.ConfigOption::class.java)?.let { ann ->
            if (ann.name.isNotBlank()) optionName = ann.name
        }
        // fallback: check kotlin property annotations (best-effort) without resolving mutable property
        if (optionName == null) {
            val kp = owner.kotlin.memberProperties.firstOrNull { it.name == fieldName }
            val ann = kp?.annotations?.firstOrNull { it.annotationClass.simpleName == "ConfigOption" }
            if (ann != null) {
                optionName = kp.name
            }
        }
        val label = optionName ?: fieldName
        return Dependency(
            label,
            if (optionName != null) "Requires '$optionName' to be enabled" else "Requires $label to be enabled",
            DependencySource.BooleanField(owner, fieldName, getter, mutableProp),
        )
    }

    private fun findRelativeClass(context: Class<*>, simpleName: String): Class<*>? {
        val packageName = context.`package`?.name ?: return null
        return runCatching { Class.forName("$packageName.$simpleName") }.getOrNull()
    }

    private fun collectAnnotations(field: Field, sink: MutableList<FeatureDependencyRequirement>) {
        field.getAnnotationsByType(FeatureDependencyRequirement::class.java)?.let { sink.addAll(it) }
        var owner: Class<*>? = field.declaringClass
        while (owner != null) {
            owner.getAnnotationsByType(FeatureDependencyRequirement::class.java)?.let { sink.addAll(it) }
            owner = owner.enclosingClass
        }
    }
}
