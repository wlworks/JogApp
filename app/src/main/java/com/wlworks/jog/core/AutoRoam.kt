package com.wlworks.jog.core

import kotlin.random.Random

/**
 * 自動移動的方向與油門產生器：一段一段地走，每段隨機挑一個新方向和油門，
 * 方向以有限的轉向速率慢慢轉過去，軌跡才會是弧線而不是折線。
 *
 * 這裡只產生「往哪走、推幾成」，實際速度仍是 [SpeedTier.mps] 乘上油門 ——
 * 使用者中途切檔次，下一個 tick 速度就跟著變，不必重啟自動移動。
 *
 * 不碰 Android API，可直接單測；[random] 可注入固定種子。
 */
class AutoRoam(private val random: Random = Random.Default) {

    companion object {

        /** 一段路最長走幾秒。 */
        const val LEG_MAX_S = 45.0

        /**
         * 一段路最短走幾秒。段長和 [TURN_MAX_DEG] 一起決定「會不會在原地繞」：
         * 第一版是 6–20 秒、±75°，方向每分鐘就被洗掉一輪，步行 5 分鐘走了 360 m 卻只離開起點
         * 160 m 左右，地圖上看起來就是在原地繞圈。現在的組合 5 分鐘能離開約 320 m（路徑的九成）。
         * 調這幾個數字時 AutoRoamTest 的「net displacement」那條會守住下限。
         */
        const val LEG_MIN_S = 15.0

        /** 油門上限：該檔次速度的 100%。 */
        const val THROTTLE_MAX = 1.0f

        /** 油門下限。不低於七成，免得自動移動看起來像快停了。 */
        const val THROTTLE_MIN = 0.7f

        /** 換段時新方向最多偏離目前方向幾度。大致維持原本的去向，只是慢慢偏。 */
        const val TURN_MAX_DEG = 45f

        /** 每秒最多轉幾度。轉得慢，彎才是大弧線而不是原地甩頭（步行時轉彎半徑約 5 m）。 */
        const val TURN_RATE_DEG_PER_S = 15f

        /** 把角度收進 0..360。 */
        private fun normalize(deg: Float): Float = ((deg % 360f) + 360f) % 360f
    }

    /** 一個 tick 的輸出：方位角（正北 = 0，順時針）與油門（0..1）。 */
    data class Step(val bearing: Float, val throttle: Float)

    private var heading = 0f

    private var legRemainingS = 0.0

    private var targetHeading = 0f

    private var throttle = THROTTLE_MAX

    /** 推進 [dtS] 秒，回傳這個 tick 該用的方位角與油門。這段走完就抽下一段。 */
    fun advance(dtS: Double): Step {
        legRemainingS -= dtS
        if (legRemainingS <= 0.0) {
            legRemainingS = random.nextDouble(LEG_MIN_S, LEG_MAX_S)
            targetHeading = normalize(heading + (random.nextFloat() * 2f - 1f) * TURN_MAX_DEG)
            throttle = THROTTLE_MIN + random.nextFloat() * (THROTTLE_MAX - THROTTLE_MIN)
        }
        // 走最短的那一邊轉過去：差值收進 -180..180
        val diff = ((targetHeading - heading + 540f) % 360f) - 180f
        val maxTurn = (TURN_RATE_DEG_PER_S * dtS).toFloat()
        heading = normalize(heading + diff.coerceIn(-maxTurn, maxTurn))
        return Step(bearing = heading, throttle = throttle)
    }

    /** 以 [bearing] 為目前行進方向並重新抽一段。剛開自動移動、或搖桿接手後交還時用。 */
    fun alignTo(bearing: Float) {
        heading = normalize(bearing)
        targetHeading = heading
        legRemainingS = 0.0
    }
}
