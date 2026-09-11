package com.wlworks.jog.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoordinateParserTest {

    private val eps = 1e-9

    private fun assertLatLng(expectedLat: Double, expectedLng: Double, actual: LatLng?) {
        checkNotNull(actual) { "expected a parse result, got null" }
        assertEquals(expectedLat, actual.lat, eps)
        assertEquals(expectedLng, actual.lng, eps)
    }

    // ---- 十進位 ----

    @Test
    fun `comma separated`() {
        assertLatLng(25.0330, 121.5654, CoordinateParser.parse("25.0330, 121.5654"))
    }

    @Test
    fun `space separated`() {
        assertLatLng(25.0330, 121.5654, CoordinateParser.parse("25.0330 121.5654"))
    }

    @Test
    fun `comma without space`() {
        assertLatLng(25.0330, 121.5654, CoordinateParser.parse("25.0330,121.5654"))
    }

    /** 面板上會遇到的完整精度輸入，小數位數不應被截斷。 */
    @Test
    fun `full precision is preserved`() {
        assertLatLng(
            35.039563227558034,
            135.72926455412235,
            CoordinateParser.parse("35.039563227558034, 135.72926455412235")
        )
    }

    @Test
    fun `negative values`() {
        assertLatLng(-33.868820, 151.209290, CoordinateParser.parse("-33.868820, 151.209290"))
    }

    @Test
    fun `explicit plus sign`() {
        assertLatLng(25.0, 121.0, CoordinateParser.parse("+25.0, +121.0"))
    }

    @Test
    fun `integers without decimal point`() {
        assertLatLng(25.0, 121.0, CoordinateParser.parse("25, 121"))
    }

    @Test
    fun `surrounding whitespace is tolerated`() {
        assertLatLng(25.0330, 121.5654, CoordinateParser.parse("   25.0330 , 121.5654   "))
    }

    @Test
    fun `zero zero is a valid coordinate`() {
        assertLatLng(0.0, 0.0, CoordinateParser.parse("0, 0"))
    }

    @Test
    fun `boundary values are valid`() {
        assertLatLng(-90.0, 180.0, CoordinateParser.parse("-90, 180"))
        assertLatLng(90.0, -180.0, CoordinateParser.parse("90, -180"))
    }

    // ---- DMS ----

    @Test
    fun `dms north east`() {
        val r = CoordinateParser.parse("""25°2'0"N 121°33'0"E""")
        assertLatLng(25.0 + 2.0 / 60.0, 121.0 + 33.0 / 60.0, r)
    }

    @Test
    fun `dms south west is negated`() {
        val r = CoordinateParser.parse("""33°52'4.8"S 151°12'33.4"W""")
        assertLatLng(
            -(33.0 + 52.0 / 60.0 + 4.8 / 3600.0),
            -(151.0 + 12.0 / 60.0 + 33.4 / 3600.0),
            r
        )
    }

    @Test
    fun `dms accepts lowercase hemisphere`() {
        val r = CoordinateParser.parse("""25°2'0"n 121°33'0"e""")
        assertLatLng(25.0 + 2.0 / 60.0, 121.0 + 33.0 / 60.0, r)
    }

    // ---- 不該解析成座標的輸入 ----

    @Test
    fun `place name falls through to null`() {
        assertNull(CoordinateParser.parse("Taipei 101"))
        assertNull(CoordinateParser.parse("台北車站"))
    }

    @Test
    fun `out of range is rejected rather than clamped`() {
        assertNull(CoordinateParser.parse("95.0, 121.0"))
        assertNull(CoordinateParser.parse("25.0, 200.0"))
    }

    @Test
    fun `more than three integer digits is not a coordinate`() {
        assertNull(CoordinateParser.parse("1000.0, 20.0"))
    }

    @Test
    fun `single number is not a coordinate`() {
        assertNull(CoordinateParser.parse("25.0330"))
    }

    @Test
    fun `trailing garbage is rejected`() {
        assertNull(CoordinateParser.parse("25.0330, 121.5654 near the station"))
    }

    @Test
    fun `empty and blank are null`() {
        assertNull(CoordinateParser.parse(""))
        assertNull(CoordinateParser.parse("   "))
    }
}
