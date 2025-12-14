package at.hannibal2.skyhanni.config

/**
 * Marks a config element as depending on a third-party network/service.
 *
 * Minimal usage: `@ThirdPartyDependency(ThirdParty.BINGO_NET)`
 *
 * Inheritance/scoping:
 * - Can be placed on fields and classes. Field overrides class. For nested classes,
 *   the nearest enclosing annotated class provides defaults.
 *
 * Semantics:
 * - If the effective dependency requires a main toggle, and the third party uses a main toggle
 *   and it is disabled, the option is blocked until enabled.
 * - Otherwise a non-blocking warning ribbon is shown.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS)
annotation class ThirdPartyDependency(
    val value: ThirdParty,
    /** Explicitly require permanent connection. AUTO defaults to [useMainToggle]. */
    val requiresMainToggle: TriState = TriState.AUTO,
    /**
     * Optional: override the main toggle by referencing the owning class and field name.
     * If [mainToggleName] is blank, the enum default is used.
     */
    val mainToggleOwner: kotlin.reflect.KClass<*> = Any::class,
    val mainToggleName: String = "",
    /** Additional message to show in the UI. */
    val message: String = "",
)
