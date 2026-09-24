package com.wlworks.jog.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RandomJumpTest {

    @Test
    fun `range scales linearly with tier speed`() {
        val walk = RandomJump.rangeFor(SpeedTier.WALK)
        assertEquals(SpeedTier.WALK.mps * RandomJump.MIN_S, walk.start, 1e-9)
        assertEquals(SpeedTier.WALK.mps * RandomJump.MAX_S, walk.endInclusive, 1e-9)
    }

    @Test
    fun `picked distance always falls inside the advertised range`() {
        val random = Random(seed = 42)
        for (tier in SpeedTier.entries) {
            val range = RandomJump.rangeFor(tier)
            repeat(200) {
                val d = RandomJump.pickDistance(tier, random)
                assertTrue("$tier picked $d outside $range", d in range)
            }
        }
    }
}
