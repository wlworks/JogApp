package com.wlworks.jog.core

import androidx.annotation.StringRes
import com.wlworks.jog.R

/**
 * 微調移動的速度檔次。mps = 公尺／秒。
 * 搖桿推桿幅度會再乘上去（0..1），所以同一檔內也能連續變速。
 *
 * label 走字串資源，不寫死文字 —— UI 用 stringResource(tier.labelRes) 取。
 *
 * 兩個刻意違反本專案排版慣例的地方：
 *  1. entries 依速度由慢到快排，不依字母 —— [Companion.next] 靠 ordinal 遞增，
 *     UI 也依這個順序由左而右、由上而下呈現。
 *  2. companion object 只能放在 entries 之後 —— Kotlin 規定 enum entries
 *     必須是類別主體的第一個元素。
 */
enum class SpeedTier(@StringRes val labelRes: Int, val mps: Double) {
    CREEP(R.string.speed_creep, 0.5),
    WALK(R.string.speed_walk, 1.4),
    RUN(R.string.speed_run, 3.0),
    BIKE(R.string.speed_bike, 6.0),
    DRIVE(R.string.speed_drive, 16.7),
    RAIL(R.string.speed_rail, 83.0);

    companion object {

        /** 面板開起來時的預設檔次。 */
        val DEFAULT = WALK

        /** 取下一個檔次，走到底再繞回第一個。 */
        fun next(current: SpeedTier) = entries[(current.ordinal + 1) % entries.size]
    }
}
