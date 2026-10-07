package com.wlworks.jog.ads

import android.app.Activity

/**
 * 廣告關卡的**關閉版**：`jog.ads=false`（預設）時編進來的實作。
 *
 * 這個 source set 不碰任何廣告 SDK，所以 App 不會多出 INTERNET / AD_ID 權限，
 * Data Safety 與隱私權政策也維持「不蒐集、不傳輸」。真正的實作在 `src/ads/`，
 * 兩邊的 `RewardedGate` 必須維持同樣的公開介面。
 */
object RewardedGate {

    /** 這個 build 是否有廣告關卡。false 時呼叫端直接放行，不會走到 [show]。 */
    const val ENABLED = false

    /** 沒有關卡可過，直接回報已解鎖。正常流程不會呼叫到這裡。 */
    fun show(activity: Activity, onShowing: () -> Unit, onResult: (RewardOutcome) -> Unit) {
        onResult(RewardOutcome.EARNED)
    }
}
