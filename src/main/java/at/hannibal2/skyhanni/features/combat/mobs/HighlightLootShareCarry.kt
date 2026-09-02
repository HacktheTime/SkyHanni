package at.hannibal2.skyhanni.features.combat.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.IslandLeaveEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.compat.EntityCompat.findHealthReal

@SkyHanniModule
object HighlightLootShareCarry {

    private val config get() = SkyHanniMod.feature.combat.mobs.highlightLootShareCarry

    private var tempEnabled: Boolean? = null
    private var tempThreshold: Float? = null

    private val isEnabled: Boolean get() = tempEnabled ?: config.enabled
    private val threshold: Float get() = tempThreshold ?: config.damageThreshold

    private data class SpawnData(
        val spawnedWithHP: Float,
        val maxHP: Int,
    )

    private val trackedMobs = mutableMapOf<Mob, SpawnData>()
    private val highlightedMobs = mutableSetOf<Mob>()

    @HandleEvent(onlyOnSkyblock = true)
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!isEnabled) return
        val mob = event.mob
        val entity = mob.baseEntity
        val spawnedWithHP = entity.findHealthReal()
        val maxHP = entity.baseMaxHealth
        if (maxHP <= 0) return
        trackedMobs[mob] = SpawnData(spawnedWithHP, maxHP)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) {
        trackedMobs.remove(event.mob)
        highlightedMobs.remove(event.mob)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onIslandChange(event: IslandLeaveEvent) {
        trackedMobs.clear()
        highlightedMobs.clear()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled) return
        val currentThreshold = threshold.toDouble()
        val color = config.highlightColor

        val iterator = trackedMobs.iterator()
        while (iterator.hasNext()) {
            val (mob, spawnData) = iterator.next()
            if (!mob.isAlive) {
                iterator.remove()
                highlightedMobs.remove(mob)
                continue
            }

            val currentHP = mob.baseEntity.findHealthReal()
            val maxHP = spawnData.maxHP
            if (maxHP <= 0) continue

            val damageTaken = spawnData.spawnedWithHP - currentHP
            val damagePercentage = (damageTaken / maxHP) * 100.0

            if (damagePercentage >= currentThreshold) {
                if (mob !in highlightedMobs) {
                    mob.highlight(color) { isEnabled }
                    highlightedMobs.add(mob)
                }
            } else {
                if (mob in highlightedMobs) {
                    mob.removeHighlight()
                    highlightedMobs.remove(mob)
                }
            }
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shhighlightlootsharecarry") {
            description = "Toggle highlighting of mobs that have taken damage (loot share carries)"
            category = SHORTENED_COMMANDS
            argCallback("percentage", BrigadierArguments.float(0.5f, 100f)) { percentage ->
                tempEnabled = true
                tempThreshold = percentage
                ChatUtils.chat("§aLoot share carry highlight §aenabled§a temporarily (threshold: §e${percentage}%§a).")
            }
            simpleCallback {
                tempEnabled = null
                tempThreshold = null
                config.enabled = !config.enabled
                val state = if (config.enabled) "§aenabled" else "§cdisabled"
                ChatUtils.chat("§aLoot share carry highlight $state§a (threshold: §e${config.damageThreshold}%§a).")
                if (!config.enabled) {
                    clearHighlights()
                }
            }
        }
    }

    private fun clearHighlights() {
        for (mob in highlightedMobs) {
            mob.removeHighlight()
        }
        highlightedMobs.clear()
    }
}
