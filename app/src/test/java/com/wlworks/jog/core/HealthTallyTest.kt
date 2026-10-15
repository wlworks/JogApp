package com.wlworks.jog.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthTallyTest {

    @Test
    fun `movement counts both distance and steps`() {
        val tally = HealthTally()
        tally.add(movedM = 75.0, tier = SpeedTier.WALK)
        val batch = tally.drain()
        assertEquals(75.0, batch.distanceM, 1e-9)
        assertEquals(100L, batch.steps)
        assertFalse(batch.isEmpty)
    }

    @Test
    fun `steps follow the stride of each tier`() {
        val tally = HealthTally()
        tally.add(movedM = 50.0, tier = SpeedTier.CREEP)
        assertEquals(100L, tally.drain().steps)
        // 111 m / 1.1 m = 100.9 步；不用 110 m，免得浮點誤差算成 99.999 步
        tally.add(movedM = 111.0, tier = SpeedTier.RUN)
        assertEquals(100L, tally.drain().steps)
    }

    @Test
    fun `tiers faster than running are not counted`() {
        val tally = HealthTally()
        tally.add(movedM = 100.0, tier = SpeedTier.BIKE)
        tally.add(movedM = 100.0, tier = SpeedTier.DRIVE)
        tally.add(movedM = 100.0, tier = SpeedTier.RAIL)
        assertTrue(tally.drain().isEmpty)
    }

    @Test
    fun `only creep walk and run are allowed`() {
        val allowed = SpeedTier.entries.filter(HealthTally::allows)
        assertEquals(listOf(SpeedTier.CREEP, SpeedTier.WALK, SpeedTier.RUN), allowed)
        assertNull(HealthTally.strideFor(SpeedTier.BIKE))
    }

    @Test
    fun `drain resets the totals`() {
        val tally = HealthTally()
        tally.add(movedM = 110.0, tier = SpeedTier.WALK)
        assertFalse(tally.drain().isEmpty)
        assertTrue(tally.drain().isEmpty)
    }

    @Test
    fun `fractional steps carry over to the next batch`() {
        val tally = HealthTally()
        // 步行每 tick 0.14 m = 0.1867 步；單看一個 tick 永遠不滿一步
        var steps = 0L
        repeat(1_000) {
            tally.add(movedM = 0.14, tier = SpeedTier.WALK)
            steps += tally.drain().steps
        }
        // 140 m / 0.75 m = 186.67 步
        assertEquals(186L, steps)
    }

    @Test
    fun `non positive movement is ignored`() {
        val tally = HealthTally()
        tally.add(movedM = 0.0, tier = SpeedTier.WALK)
        tally.add(movedM = -3.0, tier = SpeedTier.WALK)
        assertTrue(tally.drain().isEmpty)
    }
}
