package com.wlworks.jog.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.wlworks.jog.BuildConfig

/**
 * 廣告關卡的**啟用版**：`jog.ads=true` 時編進來的實作，用 AdMob 獎勵廣告，前面接 UMP 同意流程。
 *
 * 關閉版在 `src/noAds/`，兩邊的 `RewardedGate` 必須維持同樣的公開介面。
 * 廣告單元 id 由 build 決定：debug 一律是 Google 公開的測試 id，release 讀 gitignore 的 admob.properties（見 app/build.gradle.kts）。
 *
 * 同意流程（UMP）在使用者按「看廣告」時才跑，不在開 App 時跑 —— 不用自動移動／Health 同步的人
 * 永遠不會看到廣告，沒必要先問。歐盟等需要同意的地區會先跳 Google 的同意表單，
 * 有了結果（同意或拒絕都算，拒絕時 SDK 會改出非個人化廣告）才載入廣告。
 * 同意表單的內容在 AdMob 後台 Privacy & messaging 設定，這裡不寫任何文案。
 */
object RewardedGate {

    /** 這個 build 是否有廣告關卡。 */
    const val ENABLED = true

    /**
     * 更新同意資訊，並回報這個使用者需不需要「廣告隱私設定」入口（歐盟等地區才需要）。
     * 設定畫面每次回到前景時呼叫；查詢失敗就沿用上次已知的狀態。
     */
    fun checkPrivacyOptions(activity: Activity, onResult: (Boolean) -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        val report = {
            onResult(
                info.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            )
        }
        info.requestConsentInfoUpdate(activity, consentParams(activity), { report() }, { report() })
    }

    /**
     * 同意查詢的參數。debug build 帶了 jog.ump.debugDevice 的話，把這台裝置當成在歐盟，
     * 人在台灣也能測完整的同意流程；release 一律不帶。
     */
    private fun consentParams(context: Context): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        if (BuildConfig.UMP_DEBUG_DEVICE.isNotEmpty()) {
            builder.setConsentDebugSettings(
                ConsentDebugSettings.Builder(context)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .addTestDeviceHashedId(BuildConfig.UMP_DEBUG_DEVICE)
                    .build()
            )
        }
        return builder.build()
    }

    /** 已確定可以請求廣告：載入並播放一則獎勵廣告，結束後恰好呼叫一次 [onResult]。 */
    private fun loadAndShow(
        activity: Activity,
        onShowing: () -> Unit,
        onResult: (RewardOutcome) -> Unit
    ) {
        // initialize 可重複呼叫；沒等它完成就 load 也行，SDK 會自己排隊。
        // 刻意排在同意流程之後：UMP 規定取得同意結果前不可以初始化廣告 SDK。
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

    /**
     * 先走同意流程，再載入並播放一則獎勵廣告。廣告即將蓋到畫面上時呼叫 [onShowing]，
     * 結束後（不論結果）呼叫一次 [onResult]；過程中 [activity] 已經結束的話兩者都不會被呼叫。
     */
    fun show(activity: Activity, onShowing: () -> Unit, onResult: (RewardOutcome) -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            consentParams(activity),
            {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    // 需要同意且還沒問過才會跳表單；不需要或已經問過就直接回呼
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                        when {
                            activity.isFinishing || activity.isDestroyed -> Unit
                            info.canRequestAds() -> loadAndShow(activity, onShowing, onResult)
                            else -> onResult(RewardOutcome.NO_CONSENT)
                        }
                    }
                }
            },
            {
                // 查不到同意狀態（多半是沒網路）：先前已取得結果就照樣載入，否則視同拿不到廣告
                if (info.canRequestAds()) {
                    loadAndShow(activity, onShowing, onResult)
                } else {
                    onResult(RewardOutcome.UNAVAILABLE)
                }
            }
        )
    }

    /** 打開 Google 的隱私選項表單，讓使用者改變先前的同意選擇。 */
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }
}
