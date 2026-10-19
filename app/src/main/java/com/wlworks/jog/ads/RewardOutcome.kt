package com.wlworks.jog.ads

import com.wlworks.jog.R

/** 一次獎勵廣告的結果。只有 [Earned] 會解鎖。 */
sealed interface RewardOutcome {

    /** 使用者沒看完就關掉，不解鎖。 */
    data object Dismissed : RewardOutcome

    /** 看完了，解鎖。 */
    data object Earned : RewardOutcome

    /** 需要廣告同意的地區，使用者還沒完成同意設定（例如關掉了同意表單），不能請求廣告。 */
    data object NoConsent : RewardOutcome

    /**
     * 廣告載不到或播不出來。不解鎖，使用者可以重試。
     *
     * 解鎖畫面會把 [code] 印出來，使用者回報時截圖就帶得到 —— 光看「載入失敗」分不出是
     * 沒網路、沒有庫存（no fill）還是同意流程出錯，而這三種的處理方式完全不同。
     * [errorCode] 是 SDK 原始的錯誤碼：LoadAdError / AdError / UMP FormError 的 errorCode。
     */
    data class Unavailable(
        val errorCode: Int,
        val source: Source
    ) : RewardOutcome {

        /** 顯示給使用者的代碼，例如 L3。 */
        val code: String get() = "${source.prefix}$errorCode"

        /**
         * 說明文字。只分幾類：大部分錯誤碼對使用者來說處理方式都一樣（稍後再試），細節留給代碼。
         * L3（no fill）刻意不叫人換網路或 VPN —— 那多半是廣告庫存或 AdMob 後台狀態，使用者改不了。
         * L0 也歸到網路：實測飛航模式下 SDK 等約 10 秒逾時後回的是 0（INTERNAL_ERROR），不是 2。
         */
        val messageRes: Int
            get() = when (source) {
                Source.CONSENT -> R.string.unlock_unavailable_consent
                Source.LOAD -> when (errorCode) {
                    LOAD_INTERNAL_ERROR, LOAD_NETWORK_ERROR -> R.string.unlock_unavailable_network
                    LOAD_NO_FILL, LOAD_MEDIATION_NO_FILL -> R.string.unlock_unavailable_no_fill
                    else -> R.string.unlock_unavailable_service
                }
                Source.SHOW -> R.string.unlock_unavailable_show
            }
    }

    /** 錯誤發生在哪一步；[prefix] 是代碼的開頭字母。 */
    enum class Source(val prefix: Char) {

        /** UMP 同意流程：查同意狀態或顯示同意表單失敗。 */
        CONSENT('C'),

        /** 廣告載入（onAdFailedToLoad）。 */
        LOAD('L'),

        /** 廣告載到了卻播不出來（onAdFailedToShowFullScreenContent）。 */
        SHOW('S')
    }
}

/**
 * AdRequest.ERROR_CODE_INTERNAL_ERROR。抄成常數是因為 main source set 沒有廣告 SDK
 * （無廣告 build 不能相依它），下同。
 */
private const val LOAD_INTERNAL_ERROR = 0

/** AdRequest.ERROR_CODE_MEDIATION_NO_FILL。 */
private const val LOAD_MEDIATION_NO_FILL = 9

/** AdRequest.ERROR_CODE_NETWORK_ERROR。 */
private const val LOAD_NETWORK_ERROR = 2

/** AdRequest.ERROR_CODE_NO_FILL。 */
private const val LOAD_NO_FILL = 3
