package com.wlworks.jog.core

/**
 * 累計「還沒寫進 Health Connect」的移動量。移動迴圈每個 tick 呼叫 [add]，
 * Service 定時 [drain] 拿走一批去寫。兩邊在不同執行緒，所以全部上鎖。
 *
 * 只處理步行：Health 同步開著的期間速度檔被鎖在步行（見 MockStateHolder.setSpeed），
 * 所以步數一律用步行的步幅換算，不必分檔次。
 *
 * 瞬移與隨機跳不經過這裡，所以不會憑空多出幾百公尺。
 */
class HealthTally {

    companion object {

        /** 步行的步幅（公尺）。配上步行檔 1.4 m/s 約是每分鐘 112 步。 */
        const val STRIDE_M = 0.75
    }

    /** 一次 [drain] 拿到的量。 */
    data class Batch(val distanceM: Double, val steps: Long) {

        /** 這段期間沒有任何可寫的移動。 */
        val isEmpty: Boolean get() = distanceM <= 0.0 && steps == 0L
    }

    private var distanceM = 0.0

    private val lock = Any()

    private var steps = 0.0

    /** 記下又走了 [movedM] 公尺。 */
    fun add(movedM: Double) {
        if (movedM <= 0.0) return
        synchronized(lock) {
            distanceM += movedM
            steps += movedM / STRIDE_M
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
