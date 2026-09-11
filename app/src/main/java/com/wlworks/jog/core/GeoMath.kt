package com.wlworks.jog.core

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class LatLng(val lat: Double, val lng: Double) {

    /** 是否落在合法的經緯度範圍內。 */
    fun isValid() = lat in -90.0..90.0 && lng in -180.0..180.0
}

object GeoMath {

    /** 地球平均半徑（公尺）。短距離推算用球面模型即可，誤差遠小於 GPS 精度。 */
    private const val EARTH_RADIUS_M = 6_371_008.8

    /** 把搖桿的 (x, y) 向量換成方位角。x 右為正、y 下為正（螢幕座標）。 */
    fun bearingFromStick(x: Float, y: Float): Float {
        val deg = Math.toDegrees(atan2(x.toDouble(), -y.toDouble())).toFloat()
        return (deg + 360f) % 360f
    }

    /**
     * 從 [from] 沿 [bearingDeg]（正北 = 0，順時針）前進 [distanceM] 公尺後的座標。
     * 使用大圓航線 destination point 公式。
     */
    fun destination(from: LatLng, bearingDeg: Float, distanceM: Double): LatLng {
        if (distanceM == 0.0) return from
        val angular = distanceM / EARTH_RADIUS_M
        val bearing = Math.toRadians(bearingDeg.toDouble())
        val lat1 = Math.toRadians(from.lat)
        val lng1 = Math.toRadians(from.lng)

        val sinLat2 = sin(lat1) * cos(angular) + cos(lat1) * sin(angular) * cos(bearing)
        val lat2 = asin(sinLat2.coerceIn(-1.0, 1.0))
        val lng2 = lng1 + atan2(
            sin(bearing) * sin(angular) * cos(lat1),
            cos(angular) - sin(lat1) * sinLat2
        )

        return LatLng(
            lat = Math.toDegrees(lat2),
            lng = ((Math.toDegrees(lng2) + 540.0) % 360.0) - 180.0 // normalize 到 -180..180
        )
    }
}
