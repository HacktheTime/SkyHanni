package at.hannibal2.skyhanni.config

import kotlin.reflect.KMutableProperty1

/** Represents a toggleable integration that can back feature requirements. */
interface FeatureDependencyToggle {
    val id: String
    val displayName: String
    val description: String
    val toggleField: KMutableProperty1<out Any, Boolean>?
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)
}

/** Adapter to reuse [ThirdParty] enum as a dependency provider. */
fun ThirdParty.asDependencyToggle(): FeatureDependencyToggle {
    return object : FeatureDependencyToggle {
        override val id: String = this@asDependencyToggle.id
        override val displayName: String = this@asDependencyToggle.displayName
        override val description: String = this@asDependencyToggle.description
        override val toggleField: KMutableProperty1<out Any, Boolean>? = this@asDependencyToggle.mainToggleField
        override fun isEnabled(): Boolean = this@asDependencyToggle.isEnabled()
        override fun setEnabled(enabled: Boolean) = this@asDependencyToggle.setEnabled(enabled)
    }
}
