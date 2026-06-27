package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat.localWorldOrNull
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level

object WorldCompat {
    val worldTime: Long?
        get() = localWorldOrNull?.gameTime

    // TODO maybe make it so if absurd high number we use spooky new day every x ticks and use that to include in the calculation?
    val worldDay: Int?
        get() {
            val ticks = worldTime ?: return null
            return (ticks / (20 * 60 * 20)).toInt() // 20 ticks per second, 60 seconds per minute, 20 minutes per day
        }
}
