package com.wlworks.jog.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesTest {

    private val taipei101 = Favorite(name = "Taipei 101", point = LatLng(25.03364, 121.56477))
    private val sensoji = Favorite(name = "Senso-ji", point = LatLng(35.71476, 139.79666))

    /** 往北偏移 [meters] 公尺的同名地點。 */
    private fun Favorite.shifted(meters: Double) =
        copy(point = GeoMath.destination(from = point, bearingDeg = 0f, distanceM = meters))

    @Test
    fun `add puts the newest first`() {
        val r = Favorites.add(listOf(taipei101), sensoji) as Favorites.AddResult.Added
        assertEquals(listOf(sensoji, taipei101), r.list)
    }

    @Test
    fun `add rejects a point within the same-place radius`() {
        val r = Favorites.add(listOf(taipei101), taipei101.shifted(20.0).copy(name = "again"))
        assertEquals(Favorites.AddResult.Duplicate, r)
    }

    @Test
    fun `add accepts a point just outside the same-place radius`() {
        val r = Favorites.add(listOf(taipei101), taipei101.shifted(40.0))
        assertTrue(r is Favorites.AddResult.Added)
    }

    @Test
    fun `add rejects when full`() {
        val full = (0 until Favorites.MAX).map { taipei101.shifted(it * 100.0) }
        assertEquals(Favorites.AddResult.Full, Favorites.add(full, sensoji))
    }

    @Test
    fun `duplicate wins over full`() {
        val full = (0 until Favorites.MAX).map { taipei101.shifted(it * 100.0) }
        assertEquals(Favorites.AddResult.Duplicate, Favorites.add(full, taipei101))
    }

    @Test
    fun `indexNear picks the closest match`() {
        val list = listOf(taipei101.shifted(25.0), taipei101.shifted(5.0))
        assertEquals(1, Favorites.indexNear(list, taipei101.point))
    }

    @Test
    fun `indexNear returns null when nothing is close`() {
        assertNull(Favorites.indexNear(listOf(sensoji), taipei101.point))
    }

    @Test
    fun `removeAt drops only that entry and ignores bad indices`() {
        val list = listOf(sensoji, taipei101)
        assertEquals(listOf(taipei101), Favorites.removeAt(list, 0))
        assertEquals(list, Favorites.removeAt(list, 5))
    }

    @Test
    fun `distance matches a known pair`() {
        // 台北 101 到淺草寺約 2,100 km
        val d = GeoMath.distance(taipei101.point, sensoji.point)
        assertEquals(2_100_000.0, d, 30_000.0)
    }
}
