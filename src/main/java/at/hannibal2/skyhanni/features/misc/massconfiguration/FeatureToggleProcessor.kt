package at.hannibal2.skyhanni.features.misc.massconfiguration

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.ThirdParty
import at.hannibal2.skyhanni.config.ThirdPartyDependency
import at.hannibal2.skyhanni.config.TriState
import at.hannibal2.skyhanni.config.FeatureDependencyResolver
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import io.github.notenoughupdates.moulconfig.processor.ConfigStructureReader
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.util.Stack

class FeatureToggleProcessor : ConfigStructureReader {
    companion object {
        val thirdPartyRegistry = mutableMapOf<ThirdParty, MutableList<FeatureToggleableOption>>()
    }

    private var latestCategory: Category? = null
    private val pathStack = Stack<String>()
    private val accordionStack = Stack<String>()

    val allOptions = mutableListOf<FeatureToggleableOption>()
    val orderedOptions by lazy {
        allOptions.groupBy { it.category }
    }

    override fun beginCategory(baseObject: Any?, field: Field?, name: String, description: String) {
        latestCategory = Category(name, description)
    }

    override fun endCategory() = Unit

    override fun beginAccordion(baseObject: Any?, field: Field?, o: ConfigOption?, id: Int) {
        val option = o ?: return
        accordionStack.push(option.name)
    }

    override fun endAccordion() {
        accordionStack.pop()
    }

    override fun pushPath(fieldPath: String) {
        pathStack.push(fieldPath)
    }

    override fun popPath() {
        pathStack.pop()
    }

    // java.lang.Boolean required: generic type arguments are always boxed, so kotlin.Boolean would not match here
    @Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    override fun emitOption(baseObject: Any, field: Field, option: ConfigOption) {
        val featureToggle = field.getAnnotation(FeatureToggle::class.java) ?: return
        field.getAnnotation(ConfigEditorBoolean::class.java)
            ?: error("Feature toggle found without ConfigEditorBoolean: $field")
        val setter: (Boolean) -> Unit
        val value: Boolean
        when (field.type) {
            java.lang.Boolean.TYPE -> {
                setter = { field.setBoolean(baseObject, it) }
                value = field.getBoolean(baseObject)
            }

            Property::class.java -> {
                val genericType = field.genericType
                require(genericType is ParameterizedType)
                require((genericType.actualTypeArguments[0] as Class<*>) == (java.lang.Boolean::class.java))
                val prop = field.get(baseObject) as Property<Boolean>
                setter = { prop.set(it) }
                value = prop.get()
            }

            else -> error("Invalid FeatureToggle type: $field")
        }

        var name = option.name
        if ((name == "Enable" || name == "Enabled") && !accordionStack.empty()) {
            name = accordionStack.peek()
        }

        // Resolve third-party dependency via annotation on field or owning classes
        val tpResolved = resolveThirdParty(field)

        // If no explicit annotation, detect third-party dependencies referenced via FeatureDependencyResolver
        val autoTpResolved = if (tpResolved == null) {
            try {
                val reqs = FeatureDependencyResolver.resolve(field)
                if (!reqs.isEmpty) {
                    // collect all third-party dependencies referenced
                    val deps = reqs.groups.flatMap { it.dependencies }
                    val tps = deps.mapNotNull { dep ->
                        when (val s = dep.source) {
                            is FeatureDependencyResolver.DependencySource.ThirdParty -> s.value
                            else -> null
                        }
                    }.distinct()
                    // Only mark as third-party when ALL dependencies are third-party (conservative)
                    val allAreThird = deps.isNotEmpty() && deps.all { it.source is FeatureDependencyResolver.DependencySource.ThirdParty }
                    if (tps.isNotEmpty() && allAreThird) {
                        // requiresMainToggle true if any referenced third-party actually has a mainToggleField
                        val requiresMain = tps.any { it.mainToggleField != null }
                        tps.first() to requiresMain
                    } else null
                } else null
            } catch (_: Throwable) {
                null
            }
        } else null

        val finalTp = tpResolved ?: autoTpResolved

        // Build consistent option path without leading dots when the path stack is empty
        val optionPath = listOfNotNull(pathStack.joinToString(".").takeIf { it.isNotEmpty() }, field.name).joinToString(".")

        val optionEntry = FeatureToggleableOption(
            name,
            option.desc,
            value,
            featureToggle.trueIsEnabled,
            latestCategory!!,
            setter,
            optionPath,
            finalTp?.first,
            finalTp?.second ?: true,
        )
        allOptions.add(optionEntry)
        finalTp?.first?.let { tp ->
            thirdPartyRegistry.getOrPut(tp) { mutableListOf() }.add(optionEntry)
        }
    }

    private fun resolveThirdParty(field: Field): Pair<ThirdParty, Boolean>? {
        var anno: ThirdPartyDependency? = field.getAnnotation(ThirdPartyDependency::class.java)
        var clazz: Class<*>? = field.declaringClass
        var classAnno: ThirdPartyDependency? = null
        while (clazz != null && classAnno == null) {
            classAnno = clazz.getAnnotation(ThirdPartyDependency::class.java)
            clazz = clazz.enclosingClass
        }
        val source = anno ?: classAnno ?: return null
        val tp = source.value
        val requires = when (anno?.requiresMainToggle ?: classAnno?.requiresMainToggle ?: TriState.AUTO) {
            TriState.YES -> true
            TriState.NO -> false
            TriState.AUTO -> true
        }
        return tp to requires
    }
}
