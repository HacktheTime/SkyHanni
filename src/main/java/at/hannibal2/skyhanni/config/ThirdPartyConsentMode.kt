package at.hannibal2.skyhanni.config

/**
 * Global policy for third-party consent.
 */
enum class ThirdPartyConsentMode {
    /** No third parties are consented by default (recommended). */
    NONE,
    /**
     * The user selects which third parties are allowed. Others remain blocked
     * unless their main toggle is explicitly enabled.
     */
    SELECT,
    /** All third parties are allowed. */
    ALL,
}
