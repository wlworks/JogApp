package com.wlworks.jog.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthTallyTest {

    @Test
    fun `movement counts both distance and steps`() {
        val tally = HealthTally()
        tally.add(75.0)
        val batch = tally.drain()
        assertEquals(75.0, batch.distanceM, 1e-9)
        assertEquals(100L, batch.steps)
        assertFalse(batch.isEmpty)
    }

    @Test
    fun `drain resets the totals`() {
        val tally = HealthTally()
        tally.add(110.0)
        assertFalse(tally.drain().isEmpty)
        assertTrue(tally.drain().isEmpty)
    }

    @Test
    fun `fractional steps carry over to the next batch`() {
        val tally = HealthTally()
        // 步行每 tick 0.14 m = 0.1867 步；單看一個 tick 永遠不滿一步
        var steps = 0L
        repeat(1_000) {
            tally.add(0.14)
            steps += tally.drain().steps
        }
        // 140 m / 0.75 m = 186.67 步
        assertEquals(186L, steps)
    }

    @Test
    fun `non positive movement is ignored`() {
        val tally = HealthTally()
        tally.add(0.0)
        tally.add(-3.0)
        assertTrue(tally.drain().isEmpty)
    }
}
