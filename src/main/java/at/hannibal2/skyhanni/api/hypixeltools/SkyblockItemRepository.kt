package at.hannibal2.skyhanni.api.hypixeltools

import de.hype.hypixeltools.canonical.CanonicalItemRecord
import de.hype.hypixeltools.data.SkyblockItemReference

/**
 * Repository-neutral item API used by migrated SkyHanni code.
 *
 * A reference is the only identity exposed to callers. The canonical id is
 * deliberately not exposed as a comparison-oriented String.
 */
interface SkyblockItemRepository {
    fun find(id: SkyblockItemReference): CanonicalItemRecord?
    fun findById(id: ItemId): SkyblockItemReference?
    fun displayName(item: SkyblockItemReference): String?
}

@JvmInline
value class ItemId(val value: String) {
    init {
        require(value.isNotBlank()) { "Item id must not be blank" }
    }
}
