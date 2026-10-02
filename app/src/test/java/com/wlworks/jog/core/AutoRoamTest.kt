package com.wlworks.jog.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class AutoRoamTest {

    /** 兩個方位角之間的最短夾角。 */
    private fun gap(a: Float, b: Float): Float = abs(((a - b + 540f) % 360f) - 180f)

    @Test
    fun `bearing and throttle stay inside their ranges`() {
        val roam = AutoRoam(Random(seed = 7))
        repeat(10_000) {
            val step = roam.advance(0.1)
            assertTrue("bearing ${step.bearing}", step.bearing >= 0f && step.bearing < 360f)
            assertTrue(
                "throttle ${step.throttle}",
                step.throttle in AutoRoam.THROTTLE_MIN..AutoRoam.THROTTLE_MAX
            )
        }
    }

    @Test
    fun `heading never turns faster than the turn rate`() {
        val roam = AutoRoam(Random(seed = 11))
        var previous = roam.advance(0.1).bearing
        repeat(10_000) {
            val bearing = roam.advance(0.1).bearing
            val limit = AutoRoam.TURN_RATE_DEG_PER_S * 0.1f
            assertTrue("turned ${gap(bearing, previous)}", gap(bearing, previous) <= limit + 1e-3f)
            previous = bearing
        }
    }

    @Test
    fun `alignTo makes the next step start from the given bearing`() {
        val roam = AutoRoam(Random(seed = 3))
        repeat(500) { roam.advance(0.1) }
        roam.alignTo(350f)
        val step = roam.advance(0.1)
        assertTrue(gap(step.bearing, 350f) <= AutoRoam.TURN_RATE_DEG_PER_S * 0.1f + 1e-3f)
    }

    @Test
    fun `alignTo normalises out of range bearings`() {
        val roam = AutoRoam(Random(seed = 3))
        roam.alignTo(-90f)
        assertTrue(gap(roam.advance(0.0001).bearing, 270f) < 1f)
    }

    @Test
    fun `heading actually wanders over time`() {
        val roam = AutoRoam(Random(seed = 5))
        roam.alignTo(0f)
        val seen = HashSet<Int>()
        repeat(200_000) { seen += GeoMath.compassIndex(roam.advance(0.1).bearing) }
        assertEquals("should visit every compass octant in 20000 s", 8, seen.size)
    }

    /** 走 [seconds] 秒後「離起點的直線距離 / 實際走的路徑長」。1 = 筆直，接近 0 = 原地繞。 */
    private fun netOverPath(seed: Int, seconds: Int): Double {
        val roam = AutoRoam(Random(seed))
        var x = 0.0
        var y = 0.0
        var path = 0.0
        repeat(seconds * 10) {
            val step = roam.advance(0.1)
            val moved = SpeedTier.WALK.mps * step.throttle * 0.1
            val rad = Math.toRadians(step.bearing.toDouble())
            x += moved * sin(rad)
            y += moved * cos(rad)
            path += moved
        }
        return hypot(x, y) / path
    }

    @Test
    fun `net displacement stays close to the path length so it does not circle in place`() {
        // 實機測試回報「在原地繞圈圈」：當時 5 分鐘的中位數只有 0.46
        val ratios = (0 until 300).map { netOverPath(seed = it, seconds = 300) }.sorted()
        assertTrue("median ${ratios[150]}", ratios[150] >= 0.8)
        assertTrue("10th percentile ${ratios[30]}", ratios[30] >= 0.5)
    }
}
