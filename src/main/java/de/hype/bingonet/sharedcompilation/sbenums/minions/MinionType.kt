package de.hype.bingonet.sharedcompilation.sbenums.minions
import at.hannibal2.skyhanni.utils.NeuInternalName

open class MinionType(
    open val typeId: String,
    open val category: MinionCategory,
    open val drops: Map<NeuInternalName, Double>,
    open val requiredActions: Int,
) {

    override fun hashCode(): Int {
        return typeId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return typeId == (other as? MinionType)?.typeId
    }
}
enum class MinionCategory {
    MINING,
    FARMING,
    COMBAT,
    FORAGING,
    FISHING,
    OTHER,
}
