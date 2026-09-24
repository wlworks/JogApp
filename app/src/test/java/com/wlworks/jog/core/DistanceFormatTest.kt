package com.wlworks.jog.core

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceFormatTest {

    @Test
    fun `range under a kilometre rounds to ten metres`() {
        assertEquals("80–250 m", DistanceFormat.range(84.0, 252.0))
        assertEquals("30–90 m", DistanceFormat.range(30.0, 90.0))
    }

    @Test
    fun `range switches to kilometres once the upper bound reaches one`() {
        assertEquals("0.4–1.1 km", DistanceFormat.range(360.0, 1080.0))
        assertEquals("1–3 km", DistanceFormat.range(1002.0, 3006.0))
        assertEquals("5–15 km", DistanceFormat.range(4980.0, 14940.0))
    }

    @Test
    fun `every speed tier formats to a clean range`() {
        val expected = mapOf(
            SpeedTier.CREEP to "30–90 m",
            SpeedTier.WALK to "80–250 m",
            SpeedTier.RUN to "180–540 m",
            SpeedTier.BIKE to "0.4–1.1 km",
            SpeedTier.DRIVE to "1–3 km",
            SpeedTier.RAIL to "5–15 km"
        )
        for ((tier, text) in expected) {
            val r = RandomJump.rangeFor(tier)
            assertEquals(tier.name, text, DistanceFormat.range(r.start, r.endInclusive))
        }
    }

    @Test
    fun `single distance uses metres below a kilometre and kilometres above`() {
        assertEquals("143 m", DistanceFormat.single(143.4))
        assertEquals("1.5 km", DistanceFormat.single(1480.0))
        assertEquals("15 km", DistanceFormat.single(14923.0))
    }
}
