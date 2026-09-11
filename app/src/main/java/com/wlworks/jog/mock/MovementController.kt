package com.wlworks.jog.mock

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.wlworks.jog.core.GeoMath
import com.wlworks.jog.core.LatLng
import com.wlworks.jog.state.MockStateHolder

/**
 * 定時把「目前座標 + 搖桿輸入」換算成下一個座標並推給 [MockLocationEngine]。
 *
 * 即使搖桿沒動也要持續推送（TICK_MS），否則消費端會認為定位過期。
 */
class MovementController(
    private val engine: MockLocationEngine,
    private val scope: CoroutineScope
) {
    private companion object {

        /** 推送間隔。10 Hz：足夠平滑，CPU 成本可忽略。 */
        const val TICK_MS = 100L

        /** 每 N 個 tick 才更新一次 UI 狀態。面板每秒重組 10 次沒有意義，降到 ~3Hz。 */
        const val UI_UPDATE_EVERY = 3
    }

    private var job: Job? = null

    /** 以 [origin] 為起點開始推送迴圈，重複呼叫會先收掉上一個。 */
    fun start(origin: LatLng) {
        stop()
        MockStateHolder.update { it.copy(current = origin, running = true) }
        // binder call 走背景執行緒，別佔用主執行緒
        job = scope.launch(Dispatchers.Default) {
            var position = origin
            var lastBearing = MockStateHolder.state.value.bearing
            var tick = 0
            while (isActive) {
                val snapshot = MockStateHolder.state.value
                val stick = snapshot.stick
                val speedMps: Double
                if (stick.isIdle) {
                    speedMps = 0.0
                } else {
                    lastBearing = GeoMath.bearingFromStick(stick.x, stick.y)
                    // 推桿幅度 = 該檔次速度的百分比，給一個下限避免微推完全不動
                    val throttle = stick.magnitude.coerceIn(0.15f, 1f)
                    speedMps = snapshot.speedTier.mps * throttle
                    position = GeoMath.destination(
                        from = position,
                        bearingDeg = lastBearing,
                        distanceM = speedMps * (TICK_MS / 1000.0)
                    )
                }

                engine.push(position, lastBearing, speedMps.toFloat())

                if (++tick % UI_UPDATE_EVERY == 0) {
                    val shown = position
                    val shownBearing = lastBearing
                    MockStateHolder.update {
                        it.copy(current = shown, bearing = shownBearing)
                    }
                }
                delay(TICK_MS)
            }
        }
    }

    /** 停止推送迴圈並把 running 標記清掉。 */
    fun stop() {
        job?.cancel()
        job = null
        MockStateHolder.update { it.copy(running = false) }
    }

    /**
     * 直接瞬移到指定座標（搜尋地名／輸入經緯度時用）。
     * 不論目前是否在跑，都以新座標為起點重啟推送迴圈。
     */
    fun teleport(target: LatLng) = start(target)
}
