package at.hannibal2.skyhanni.features.misc.trevor
import at.hannibal2.skyhanni.data.ElectionApi.derpy
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.BlockUtils.isInLoadedChunk
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.EffectsCompat
import at.hannibal2.skyhanni.utils.compat.EffectsCompat.Companion.hasPotionEffect
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.block.Blocks
import java.awt.Color
import kotlin.math.abs
import kotlin.math.tan

object TrevorSolver {

    private val animalHealths = setOf(100, 200, 500, 1000, 2000, 5000, 10000, 30000)

    var currentMob: TrevorMob? = null
    private var maxHeight: Double = 0.0
    private var minHeight: Double = 0.0
    private var foundID = -1
    var mobCoordinates = LorenzVec(0.0, 0.0, 0.0)
    var mobLocation = TrapperMobArea.NONE
    var averageHeight = (minHeight + maxHeight) / 2

    private val activeTheodoliteTips = mutableListOf<TheodoliteTip>()
    private val theodoliteTipsWithOutdatedY = mutableListOf<TheodoliteTip>()
    private var questStartTime: SimpleTimeMark = SimpleTimeMark.farPast()

    private val cellCounts = mutableMapOf<LorenzVec, Int>()
    private var maxCount = 0

    // TODO: use entity events
    @OptIn(AllEntitiesGetter::class)
    fun findMob() {
        val hasBlindness = MinecraftCompat.localPlayer.hasPotionEffect(EffectsCompat.BLINDNESS)
        for (entity in EntityUtils.getAllEntities()) {
            if (entity is RemotePlayer) continue
            val mob = MobData.entityToMob[entity]
            if (mob?.isAlive == false) continue
            val name = entity.name.formattedTextCompatLessResets()
            val isTrevor = mob?.let { it.name != name && isTrevorMob(it) } ?: false
            val entityHealth = if (entity is LivingEntity) entity.baseMaxHealth.derpy() else 0
            currentMob = TrevorMob.findByName(name)
            if ((animalHealths.any { it == entityHealth } && currentMob != null) || isTrevor) {

                val currentMob = currentMob ?: ErrorManager.skyHanniError(
                    "Found trevor mob but current mob is null",
                    "entity" to entity,
                    "mobDataMob" to mob,
                )

                if (foundID == entity.id) {
                    val isOasisMob = currentMob == TrevorMob.RABBIT || currentMob == TrevorMob.SHEEP
                    if (isOasisMob && mobLocation == TrapperMobArea.OASIS && !isTrevor) return
                    val canSee = entity.canBeSeen(currentMob.renderDistance) && !entity.isInvisible && !hasBlindness
                    if (canSee) {
                        if (mobLocation != TrapperMobArea.FOUND) {
                            TrevorFeatures.lastTitle?.stop()
                            TrevorFeatures.lastTitle = TitleManager.sendTitle("§2Saw ${currentMob.mobName}!")
                        }
                        mobLocation = TrapperMobArea.FOUND
                        mobCoordinates = entity.blockPosition().toLorenzVec()
                    }
                } else {
                    foundID = entity.id
                }
                return
            }
        }
        if (foundID != -1) {
            mobCoordinates = LorenzVec(0.0, 0.0, 0.0)
            foundID = -1
        }
    }

    private fun isTrevorMob(mob: Mob): Boolean =
        TrevorTracker.TrapperMobRarity.entries.any { mob.name.startsWith(it.formattedName + " ", ignoreCase = true) }

    fun resetLocation() {
        maxHeight = 0.0
        minHeight = 0.0
        averageHeight = (minHeight + maxHeight) / 2
        foundID = -1
        mobCoordinates = LorenzVec(0.0, 0.0, 0.0)
        resetTheoTips()
    }

    fun addTheoTip(height: Int, angle: Int) {
        val playerPosition = LocationUtils.playerLocation()
        val playerHight = playerPosition.y.toInt()
        //3 allowed range since is probably rounded 2 and to ease rounding errors client side.
        val hightRange = ((height - 3) + playerHight)..((height + 3) + playerHight)
        val yawRange = (angle - 3)..(angle + 3)
        addTheoTip(TheodoliteTip(playerPosition, hightRange, yawRange))
    }

    /**
     * This method checks all existing theodolite tips and removes the ones that are not possible anymore with the new tip. Only then it
     * adds the new tip to the list.
     */
    private fun addTheoTip(newTip: TheodoliteTip) {
        if (activeTheodoliteTips.isEmpty() && theodoliteTipsWithOutdatedY.isEmpty()) {
            questStartTime = SimpleTimeMark.now()
        }
        val newHeightRange = newTip.heightRange
        activeTheodoliteTips.removeIf {
            val currentHighRange = it.heightRange

            val outOfScope = currentHighRange.first > newHeightRange.last || currentHighRange.last < newHeightRange.first
            if (outOfScope){
                theodoliteTipsWithOutdatedY.add(newTip)
            }
            return@removeIf outOfScope
        }
        activeTheodoliteTips.add(newTip)
        computePossibleLocations()
    }

    fun resetTheoTips() {
        activeTheodoliteTips.clear()
        theodoliteTipsWithOutdatedY.clear()
        questStartTime = SimpleTimeMark.farPast()
        computePossibleLocations()
    }

    private data class TheodoliteTip(
        val playerPosition: LorenzVec,
        val heightRange: IntRange,
        val yawRange: IntRange?,
    ) {
        /**
         * Uses vector calculation of player position and yaw range to calculate a possible distance range to the mob. Since the yaw range is not exact, it calculates the distance for both ends of the yaw range and uses the min and max of those as distance range.
         */
        val distance : IntRange? by lazy {
            val yawRange = yawRange ?: return@lazy null
            // hightRange stores absolute world Y; we need the magnitude of the relative delta.
            // The sign only encodes above/below — horizontal distance is always positive.
            val playerY = playerPosition.y.toInt()
            val relA = abs(heightRange.first - playerY)
            val relB = abs(heightRange.last - playerY)
            val minRelHeight = minOf(relA, relB)
            val maxRelHeight = maxOf(relA, relB)
            val minYaw = yawRange.first
            val maxYaw = yawRange.last
            val distances = listOf(
                calculateDistance(minRelHeight, minYaw),
                calculateDistance(minRelHeight, maxYaw),
                calculateDistance(maxRelHeight, minYaw),
                calculateDistance(maxRelHeight, maxYaw),
            )
            val minDistance = distances.min()
            val maxDistance = distances.max()
            return@lazy minDistance - 10..maxDistance + 10
        }

        companion object {
            private fun calculateDistance(height: Int, yaw: Int): Int {
                val yawInRadians = Math.toRadians(yaw.toDouble())
                val horizontalDistance = height / tan(yawInRadians)
                return horizontalDistance.toInt()
            }
        }
    }

    fun renderWorld(event: SkyHanniRenderWorldEvent) {
        if (activeTheodoliteTips.isEmpty() && theodoliteTipsWithOutdatedY.isEmpty()) return


        if (cellCounts.isEmpty()) return

        // Color heatmap: best overlap = yellow, -1 = gold, -2 = orange/red, -3+ = dark red
        // Yellow/orange/red scale avoids clashing with the green found-mob waypoint
        val heatColors = listOf(
            LorenzColor.YELLOW.toColor().addAlpha(30),       // offset 0 — best overlap
            LorenzColor.GOLD.toColor().addAlpha(30),         // offset 1
            Color(220, 80, 0).addAlpha(30),                  // offset 2 — orange
            LorenzColor.RED.toColor().addAlpha(30),          // offset 3
            Color(120, 0, 0).addAlpha(30),                   // offset 4+ — dark red
        )

        // Determine the best-cluster centroid so we can check player proximity
        val bestCells = cellCounts.filter { it.value == maxCount }.keys.toList()
        val bestCentroid = if (bestCells.isNotEmpty()) LorenzVec(
            bestCells.sumOf { it.x } / bestCells.size,
            bestCells.sumOf { it.y } / bestCells.size,
            bestCells.sumOf { it.z } / bestCells.size,
        ) else null

        // Decide how many offset steps below maxCount we're willing to show:
        //   • Mob already found (FOUND state): always show only the best overlap (0)
        //   • First 60 s of quest: show only best overlap (0), but loosen by 1 if the
        //     player is already standing near the cluster and the mob still hasn't been spotted
        //   • After 60 s: show the full heatmap (all offsets)
        val mobFound = mobLocation == TrapperMobArea.FOUND
        val elapsedSeconds = questStartTime.passedSince().inWholeSeconds
        val playerNearBestCluster = bestCentroid != null && bestCentroid.distanceToPlayer() < 20.0

        val maxAllowedOffset = when {
            mobFound -> 0
            elapsedSeconds < 60 -> if (playerNearBestCluster) 1 else 0
            else -> heatColors.lastIndex
        }

        for ((pos, count) in cellCounts) {
            val offset = (maxCount - count).coerceAtMost(heatColors.lastIndex)
            if (offset > maxAllowedOffset) continue
            val color = heatColors[offset]
            event.drawWaypointFilled(pos, color, seeThroughBlocks = true, extraSize = 0.2)
        }
    }

    private fun computePossibleLocations() {
        // Only grid-sample using tips that have distance data
        val tipsWithDistance = activeTheodoliteTips.filter { it.distance != null }
        if (tipsWithDistance.isEmpty()) return

        // Determine the height to sample at — the narrowest overlapping Y range of active tips
        val activeHeightRange = run {
            val minY = activeTheodoliteTips.maxOf { it.heightRange.first }
            val maxY = activeTheodoliteTips.minOf { it.heightRange.last }
            if (minY <= maxY) minY..maxY else null
        }
        val sampleY = activeHeightRange?.let { (it.first + it.last) / 2.0 }
            ?: activeTheodoliteTips.map { (it.heightRange.first + it.heightRange.last) / 2.0 }.average()


        // Build the search bounds as the union of each tip's own bounding box
        // so we never search outside where any tip can reach
        val step = 2 // grid resolution in blocks
        var searchMinX = Int.MAX_VALUE
        var searchMaxX = Int.MIN_VALUE
        var searchMinZ = Int.MAX_VALUE
        var searchMaxZ = Int.MIN_VALUE
        for (tip in tipsWithDistance) {
            val r = (tip.distance!!.last).coerceAtMost(300)
            val ox = tip.playerPosition.x.toInt()
            val oz = tip.playerPosition.z.toInt()
            if (ox - r < searchMinX) searchMinX = ox - r
            if (ox + r > searchMaxX) searchMaxX = ox + r
            if (oz - r < searchMinZ) searchMinZ = oz - r
            if (oz + r > searchMaxZ) searchMaxZ = oz + r
        }

        // Count how many tips' annuli each grid cell falls into, keep track of max
        cellCounts.clear()
        maxCount = 0

        var x = searchMinX
        while (x <= searchMaxX) {
            var z = searchMinZ
            while (z <= searchMaxZ) {
                // Use block-center coordinates (+0.5) for a more accurate distance check
                val cx = x + 0.5
                val cz = z + 0.5
                val surfaceY = findSurfaceY(x, sampleY.toInt(), z, scanRange = 16)
                if (surfaceY != null) {
                    val testPos = LorenzVec(cx, surfaceY.toDouble(), cz)
                    val count = tipsWithDistance.count { tip -> isWithinTipAnnulus(testPos, tip) }
                    if (count > 0) {
                        cellCounts[testPos] = count
                        if (count > maxCount) maxCount = count
                    }
                }
                z += step
            }
            x += step
        }
    }

    /**
     * Returns true if the given [pos] falls within the horizontal distance annulus defined by [tip].
     * Only checks XZ distance from the tip's player position, ignoring Y.
     * Uses double precision with a 0.5-block tolerance on range edges for sub-block accuracy.
     */
    private fun isWithinTipAnnulus(pos: LorenzVec, tip: TheodoliteTip): Boolean {
        val distRange = tip.distance ?: return false
        val dx = pos.x - tip.playerPosition.x
        val dz = pos.z - tip.playerPosition.z
        val dist = Math.sqrt(dx * dx + dz * dz)
        return dist >= distRange.first - 0.5 && dist <= distRange.last + 0.5
    }

    /**
     * Scans from [startY] downward then upward within [scanRange] blocks at the given [x]/[z] column
     * to find a Y where a mob can stand: feet and head are air, floor below is solid.
     * Returns the feet Y, or null if no valid surface is found or the chunk is not loaded.
     */
    private fun findSurfaceY(x: Int, startY: Int, z: Int, scanRange: Int): Int? {
        val probe = LorenzVec(x.toDouble(), startY.toDouble(), z.toDouble())
        if (!probe.isInLoadedChunk()) return null

        // Try downward first (mob is usually on the ground below the measurement point), then upward
        for (delta in 0..scanRange) {
            for (sign in listOf(0, -1, 1)) {
                if (sign == 0 && delta != 0) continue // only test 0 once
                val dy = if (sign == 0) 0 else sign * delta
                val y = startY + dy
                val feet = LorenzVec(x.toDouble(), y.toDouble(), z.toDouble())
                val head = LorenzVec(x.toDouble(), y + 1.0, z.toDouble())
                val floor = LorenzVec(x.toDouble(), y - 1.0, z.toDouble())
                if (feet.getBlockAt() == Blocks.AIR &&
                    head.getBlockAt() == Blocks.AIR &&
                    floor.getBlockAt() != Blocks.AIR
                ) return y
            }
        }
        return null
    }
}
