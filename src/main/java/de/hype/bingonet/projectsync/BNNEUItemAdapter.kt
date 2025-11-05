package de.hype.bingonet.projectsync

import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import de.hype.bingonet.sharedcompilation.sbenums.BNNEUItem

/**
 * This Adapter is managed by Project Sync handeling serialisation of environment remaps to common code
 */
internal class BNNEUItemAdapter : ProjectSyncTypeAdapter<BNNEUItem, NeuInternalName>() {
    override fun toShared(env: NeuInternalName?): BNNEUItem? {
        return env?.let { BNNEUItem(it.internalName) }
    }

    override fun toEnv(shared: BNNEUItem?): NeuInternalName? {
        return shared?.internalName?.toInternalName()
    }
}
