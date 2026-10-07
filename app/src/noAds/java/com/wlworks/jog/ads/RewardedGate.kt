package com.wlworks.jog.ads

import android.app.Activity

/**
 * 廣告關卡的**關閉版**：`-Pjog.ads=false` 時編進來的實作。
 *
 * 這個 source set 不碰任何廣告 SDK，所以 App 不會多出 INTERNET / AD_ID 權限。
 * 真正的實作在 `src/ads/`，兩邊的 `RewardedGate` 必須維持同樣的公開介面。
 */
object RewardedGate {

    /** 這個 build 是否有廣告關卡。false 時呼叫端直接放行，不會走到 [show]。 */
    const val ENABLED = false

    /** 沒有廣告就沒有同意設定，永遠不需要「廣告隱私設定」入口。 */
    fun checkPrivacyOptions(activity: Activity, onResult: (Boolean) -> Unit) {
        onResult(false)
    }

    /** 沒有關卡可過，直接回報已解鎖。正常流程不會呼叫到這裡。 */
    fun show(activity: Activity, onShowing: () -> Unit, onResult: (RewardOutcome) -> Unit) {
        onResult(RewardOutcome.EARNED)
    }

    /** 沒有隱私選項可顯示。正常流程不會呼叫到這裡。 */
    fun showPrivacyOptions(activity: Activity) = Unit
}
