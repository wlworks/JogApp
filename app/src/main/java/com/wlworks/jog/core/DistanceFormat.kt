package com.wlworks.jog.core

import java.util.Locale
import kotlin.math.roundToInt

/**
 * 距離顯示用的格式化。固定 Locale.US，理由同座標顯示：逗號小數點會和範圍的分隔符打架。
 *
 * 數字刻意取整（84–252 m → 80–250 m），目的是讓範圍看起來像設計出來的刻度，
 * 而不是浮點運算的殘渣；實際抽到的距離仍用原始值。
 */
object DistanceFormat {

    /** 一公里的公尺數，決定何時從 m 換成 km。 */
    private const val KM = 1000.0

    /** 公里數取到一位小數，整數就不留 .0；10 km 以上直接取整。 */
    private fun km(meters: Double): String {
        val value = meters / KM
        return if (value >= 10) {
            value.roundToInt().toString()
        } else {
            String.format(Locale.US, "%.1f", value).removeSuffix(".0")
        }
    }

    /** 範圍。上限未達 1 km 用公尺並取到 10 m；否則兩端都用公里。 */
    fun range(minMeters: Double, maxMeters: Double): String =
        if (maxMeters < KM) {
            "${roundTo10(minMeters)}–${roundTo10(maxMeters)} m"
        } else {
            "${km(minMeters)}–${km(maxMeters)} km"
        }

    /** 公尺取到最接近的 10。 */
    private fun roundTo10(meters: Double): Int = (meters / 10).roundToInt() * 10

    /** 單一距離。未達 1 km 用整數公尺，否則用公里。 */
    fun single(meters: Double): String =
        if (meters < KM) "${meters.roundToInt()} m" else "${km(meters)} km"
}
