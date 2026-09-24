package com.wlworks.jog.core

import kotlin.random.Random

/**
 * 「隨機跳到附近」的距離規則：以目前速度檔走 [MIN_S]..[MAX_S] 秒的距離。
 *
 * 綁在速度檔上是刻意的 —— 「附近」對步行和高鐵是兩個尺度。UI 會把 [rangeFor]
 * 直接印在按鈕上，讓使用者切檔次時看到數字跟著變，才知道這兩件事有關。
 */
object RandomJump {

    /** 最長行進秒數。步行約 250 m，高鐵約 15 km。 */
    const val MAX_S = 180.0

    /** 最短行進秒數。給個下限，不然蝸行抽到幾公尺會像沒按到。 */
    const val MIN_S = 60.0

    /** 依 [tier] 抽一個跳躍距離（公尺）。 */
    fun pickDistance(tier: SpeedTier, random: Random = Random.Default): Double =
        random.nextDouble(tier.mps * MIN_S, tier.mps * MAX_S)

    /** [tier] 可能跳出的距離範圍（公尺），給 UI 顯示。 */
    fun rangeFor(tier: SpeedTier): ClosedFloatingPointRange<Double> =
        (tier.mps * MIN_S)..(tier.mps * MAX_S)
}
