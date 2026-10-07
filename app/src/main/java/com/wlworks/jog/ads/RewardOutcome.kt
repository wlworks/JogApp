package com.wlworks.jog.ads

/** 一次獎勵廣告的結果。只有 [EARNED] 會解鎖。 */
enum class RewardOutcome {

    /** 使用者沒看完就關掉，不解鎖。 */
    DISMISSED,

    /** 看完了，解鎖。 */
    EARNED,

    /** 需要廣告同意的地區，使用者還沒完成同意設定（例如關掉了同意表單），不能請求廣告。 */
    NO_CONSENT,

    /** 廣告載不到或播不出來（沒網路、沒有庫存）。不解鎖，使用者可以重試。 */
    UNAVAILABLE
}
