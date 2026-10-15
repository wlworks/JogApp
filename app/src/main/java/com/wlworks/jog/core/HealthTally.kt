package com.wlworks.jog.core

/**
 * 累計「還沒寫進 Health Connect」的移動量。移動迴圈每個 tick 呼叫 [add]，
 * Service 定時 [drain] 拿走一批去寫。兩邊在不同執行緒，所以全部上鎖。
 *
 * 只處理用腳移動的檔次（慢行／步行／跑步），步數依各檔次的步幅換算，見 [strideFor]。
 * Health 同步開著的期間更快的檔次會被擋掉（見 MockStateHolder.setSpeed）——
 * 單車以上沒有步數可言，寫進去的數字不合理。
 *
 * 瞬移與隨機跳不經過這裡，所以不會憑空多出幾百公尺。
 */
class HealthTally {

    companion object {

        /** 慢行的步幅（公尺）。配上慢行檔 0.5 m/s 約是每分鐘 60 步。 */
        const val CREEP_STRIDE_M = 0.5

        /** 跑步的步幅（公尺）。配上跑步檔 3.0 m/s 約是每分鐘 164 步。 */
        const val RUN_STRIDE_M = 1.1

        /** 步行的步幅（公尺）。配上步行檔 1.4 m/s 約是每分鐘 112 步。 */
        const val WALK_STRIDE_M = 0.75

        /** Health 同步開著時能不能用這個檔次。 */
        fun allows(tier: SpeedTier): Boolean = strideFor(tier) != null

        /** 該檔次的步幅（公尺）；不是用腳移動的檔次回傳 null。 */
        fun strideFor(tier: SpeedTier): Double? = when (tier) {
            SpeedTier.CREEP -> CREEP_STRIDE_M
            SpeedTier.WALK -> WALK_STRIDE_M
            SpeedTier.RUN -> RUN_STRIDE_M
            SpeedTier.BIKE, SpeedTier.DRIVE, SpeedTier.RAIL -> null
        }
    }

    /** 一次 [drain] 拿到的量。 */
    data class Batch(val distanceM: Double, val steps: Long) {

        /** 這段期間沒有任何可寫的移動。 */
        val isEmpty: Boolean get() = distanceM <= 0.0 && steps == 0L
    }

    private var distanceM = 0.0

    private val lock = Any()

    private var steps = 0.0

    /** 記下用 [tier] 又走了 [movedM] 公尺。不是用腳移動的檔次整筆不算。 */
    fun add(movedM: Double, tier: SpeedTier) {
        if (movedM <= 0.0) return
        val stride = strideFor(tier) ?: return
        synchronized(lock) {
            distanceM += movedM
            steps += movedM / stride
        }
    }

    /** 取走目前累計的量並歸零。不滿一步的零頭留到下一批，長時間下來步數才不會少算。 */
    fun drain(): Batch = synchronized(lock) {
        val whole = steps.toLong()
        val batch = Batch(distanceM = distanceM, steps = whole)
        distanceM = 0.0
        steps -= whole
        batch
    }
}
