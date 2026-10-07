package com.wlworks.jog.ads

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.wlworks.jog.BuildConfig

/**
 * 廣告關卡的**啟用版**：`jog.ads=true` 時編進來的實作，用 AdMob 獎勵廣告。
 *
 * 關閉版在 `src/noAds/`，兩邊的 `RewardedGate` 必須維持同樣的公開介面。
 * 廣告單元 id 由 build 決定：debug 一律是 Google 公開的測試 id，release 讀 gitignore 的 admob.properties（見 app/build.gradle.kts）。
 */
object RewardedGate {

    /** 這個 build 是否有廣告關卡。 */
    const val ENABLED = true

    /**
     * 載入並播放一則獎勵廣告。廣告即將蓋到畫面上時呼叫 [onShowing]，
     * 結束後（不論結果）呼叫一次 [onResult]；載入期間 [activity] 已經結束的話兩者都不會被呼叫。
     */
    fun show(activity: Activity, onShowing: () -> Unit, onResult: (RewardOutcome) -> Unit) {
        // initialize 可重複呼叫；沒等它完成就 load 也行，SDK 會自己排隊
        MobileAds.initialize(activity.applicationContext)
        RewardedAd.load(
            activity,
            BuildConfig.ADMOB_REWARDED_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {

                /** 沒網路或沒有庫存。 */
                override fun onAdFailedToLoad(error: LoadAdError) {
                    onResult(RewardOutcome.UNAVAILABLE)
                }

                /** 載到了就立刻播；獎勵回呼先於關閉回呼，所以用旗標記到關閉時再回報。 */
                override fun onAdLoaded(ad: RewardedAd) {
                    if (activity.isFinishing || activity.isDestroyed) return
                    var earned = false
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {

                        /** 使用者關掉廣告：看完與否由 earned 決定。 */
                        override fun onAdDismissedFullScreenContent() {
                            onResult(if (earned) RewardOutcome.EARNED else RewardOutcome.DISMISSED)
                        }

                        /** 載到了卻播不出來，視同拿不到廣告。 */
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            onResult(RewardOutcome.UNAVAILABLE)
                        }
                    }
                    onShowing()
                    ad.show(activity) { earned = true }
                }
            }
        )
    }
}
