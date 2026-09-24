package com.wlworks.jog.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {

    /** 1 公尺對應的緯度度數，用來把公尺誤差換算成度數容差。 */
    private val degPerMeter = 1.0 / 111_194.9

    // ---- destination ----

    @Test
    fun `zero distance returns the same point`() {
        val from = LatLng(25.0330, 121.5654)
        assertEquals(from, GeoMath.destination(from, 42f, 0.0))
    }

    @Test
    fun `due north increases latitude only`() {
        val r = GeoMath.destination(LatLng(0.0, 0.0), 0f, 1000.0)
        assertEquals(1000.0 * degPerMeter, r.lat, 1e-5)
        assertEquals(0.0, r.lng, 1e-9)
    }

    @Test
    fun `due south decreases latitude`() {
        val r = GeoMath.destination(LatLng(0.0, 0.0), 180f, 1000.0)
        assertEquals(-1000.0 * degPerMeter, r.lat, 1e-5)
    }

    @Test
    fun `due east on the equator increases longitude only`() {
        val r = GeoMath.destination(LatLng(0.0, 0.0), 90f, 1000.0)
        assertEquals(0.0, r.lat, 1e-9)
        assertEquals(1000.0 * degPerMeter, r.lng, 1e-5)
    }

    @Test
    fun `due west decreases longitude`() {
        val r = GeoMath.destination(LatLng(0.0, 0.0), 270f, 1000.0)
        assertEquals(-1000.0 * degPerMeter, r.lng, 1e-5)
    }

    /** 同樣的東向距離，緯度越高經度變化越大（緯線圈變短）。 */
    @Test
    fun `eastward longitude change grows with latitude`() {
        val atEquator = GeoMath.destination(LatLng(0.0, 0.0), 90f, 1000.0).lng
        val atSixty = GeoMath.destination(LatLng(60.0, 0.0), 90f, 1000.0).lng
        assertTrue("$atSixty should be about twice $atEquator", atSixty > atEquator * 1.9)
        assertTrue("$atSixty should be about twice $atEquator", atSixty < atEquator * 2.1)
    }

    @Test
    fun `north then south returns to the start`() {
        val from = LatLng(35.039563, 135.729265)
        val there = GeoMath.destination(from, 0f, 500.0)
        val back = GeoMath.destination(there, 180f, 500.0)
        assertEquals(from.lat, back.lat, 1e-9)
        assertEquals(from.lng, back.lng, 1e-9)
    }

    @Test
    fun `crossing the antimeridian eastward wraps to negative longitude`() {
        val r = GeoMath.destination(LatLng(0.0, 179.99), 90f, 5000.0)
        assertTrue("expected wrap to negative, got ${r.lng}", r.lng < 0)
        assertTrue("expected near -180, got ${r.lng}", r.lng > -180.0)
        assertTrue(r.isValid())
    }

    @Test
    fun `crossing the antimeridian westward wraps to positive longitude`() {
        val r = GeoMath.destination(LatLng(0.0, -179.99), 270f, 5000.0)
        assertTrue("expected wrap to positive, got ${r.lng}", r.lng > 0)
        assertTrue(r.isValid())
    }

    @Test
    fun `result stays within valid range at high latitude`() {
        val r = GeoMath.destination(LatLng(89.999, 0.0), 0f, 1000.0)
        assertTrue("lat out of range: ${r.lat}", r.isValid())
    }

    // ---- bearingFromStick ----
    // 螢幕座標：x 右為正、y 下為正，所以「上」是 y 為負。

    @Test
    fun `stick up is north`() {
        assertEquals(0f, GeoMath.bearingFromStick(0f, -1f), 1e-4f)
    }

    @Test
    fun `stick right is east`() {
        assertEquals(90f, GeoMath.bearingFromStick(1f, 0f), 1e-4f)
    }

    @Test
    fun `stick down is south`() {
        assertEquals(180f, GeoMath.bearingFromStick(0f, 1f), 1e-4f)
    }

    @Test
    fun `stick left is west`() {
        assertEquals(270f, GeoMath.bearingFromStick(-1f, 0f), 1e-4f)
    }

    @Test
    fun `diagonals land on the 45 degree marks`() {
        assertEquals(45f, GeoMath.bearingFromStick(1f, -1f), 1e-4f)
        assertEquals(135f, GeoMath.bearingFromStick(1f, 1f), 1e-4f)
        assertEquals(225f, GeoMath.bearingFromStick(-1f, 1f), 1e-4f)
        assertEquals(315f, GeoMath.bearingFromStick(-1f, -1f), 1e-4f)
    }

    @Test
    fun `bearing is always normalised to 0 until 360`() {
        val samples = listOf(
            0f to -1f, 1f to -1f, 1f to 0f, 1f to 1f,
            0f to 1f, -1f to 1f, -1f to 0f, -1f to -1f
        )
        samples.forEach { (x, y) ->
            val b = GeoMath.bearingFromStick(x, y)
            assertTrue("($x,$y) gave $b", b >= 0f && b < 360f)
        }
    }

    /** 推桿幅度不影響方向，只有比例影響。 */
    @Test
    fun `magnitude does not change the bearing`() {
        assertEquals(
            GeoMath.bearingFromStick(0.1f, -0.1f),
            GeoMath.bearingFromStick(0.9f, -0.9f),
            1e-4f
        )
    }

    // ---- compassIndex ----

    @Test
    fun `compass index centres each sector on its cardinal direction`() {
        assertEquals(0, GeoMath.compassIndex(0f))
        assertEquals(0, GeoMath.compassIndex(22.4f))
        assertEquals(1, GeoMath.compassIndex(22.5f))
        assertEquals(2, GeoMath.compassIndex(90f))
        assertEquals(4, GeoMath.compassIndex(180f))
        assertEquals(6, GeoMath.compassIndex(270f))
        assertEquals(7, GeoMath.compassIndex(337.4f))
        assertEquals(0, GeoMath.compassIndex(337.5f)) // 北的扇區從 337.5 開始
        assertEquals(0, GeoMath.compassIndex(360f))
    }

    @Test
    fun `compass index tolerates negative and oversized bearings`() {
        assertEquals(7, GeoMath.compassIndex(-45f))
        assertEquals(2, GeoMath.compassIndex(450f))
    }

    // ---- LatLng ----

    @Test
    fun `isValid accepts the boundaries and rejects beyond them`() {
        assertTrue(LatLng(90.0, 180.0).isValid())
        assertTrue(LatLng(-90.0, -180.0).isValid())
        assertFalse(LatLng(90.1, 0.0).isValid())
        assertFalse(LatLng(0.0, 180.1).isValid())
    }
}
