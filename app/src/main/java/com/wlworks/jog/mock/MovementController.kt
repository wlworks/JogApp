package com.wlworks.jog.mock

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.wlworks.jog.core.AutoRoam
import com.wlworks.jog.core.GeoMath
import com.wlworks.jog.core.HealthTally
import com.wlworks.jog.core.LatLng
import com.wlworks.jog.state.MockStateHolder

/**
 * 定時把「目前座標 + 搖桿輸入（或自動移動）」換算成下一個座標並推給 [MockLocationEngine]。
 *
 * 即使沒在移動也要持續推送（TICK_MS），否則消費端會認為定位過期。
 *
 * 方向來源的優先順序：搖桿 > 自動移動 > 不動。自動移動中推桿等於暫時接手方向，
 * 放開後 [AutoRoam] 從推桿最後的方向接著走。
 */
class MovementController(
    private val engine: MockLocationEngine,
    private val scope: CoroutineScope,
    private val tally: HealthTally
) {
    private companion object {

        /** 推送間隔。10 Hz：足夠平滑，CPU 成本可忽略。 */
        const val TICK_MS = 100L

        /** 每 N 個 tick 才更新一次 UI 狀態。面板每秒重組 10 次沒有意義，降到 ~3Hz。 */
        const val UI_UPDATE_EVERY = 3
    }

    private var job: Job? = null

    /** 以 [origin] 為起點開始推送迴圈，重複呼叫會先收掉上一個。自動移動的開關不受影響。 */
    fun start(origin: LatLng) {
        job?.cancel()
        MockStateHolder.update { it.copy(current = origin, running = true) }
        // binder call 走背景執行緒，別佔用主執行緒
        job = scope.launch(Dispatchers.Default) {
            val roam = AutoRoam()
            val tickS = TICK_MS / 1000.0
            var position = origin
            var lastBearing = MockStateHolder.state.value.bearing
            var tick = 0
            var wasAuto = false
            while (isActive) {
                val snapshot = MockStateHolder.state.value
                val stick = snapshot.stick
                // 剛切到自動：從目前面向的方向出發，不要突然甩頭
                if (snapshot.autoMove && !wasAuto) roam.alignTo(lastBearing)
                wasAuto = snapshot.autoMove

                val throttle: Float
                if (!stick.isIdle) {
                    lastBearing = GeoMath.bearingFromStick(stick.x, stick.y)
                    // 推桿幅度 = 該檔次速度的百分比，給一個下限避免微推完全不動
                    throttle = stick.magnitude.coerceIn(0.15f, 1f)
                    roam.alignTo(lastBearing)
                } else if (snapshot.autoMove) {
                    val step = roam.advance(tickS)
                    lastBearing = step.bearing
                    throttle = step.throttle
                } else {
                    throttle = 0f
                }

                // 速度每個 tick 都重讀檔次：自動移動中切檔，下一個 tick 就換速度
                val speedMps = snapshot.speedTier.mps * throttle
                if (speedMps > 0.0) {
                    val movedM = speedMps * tickS
                    position = GeoMath.destination(
                        from = position,
                        bearingDeg = lastBearing,
                        distanceM = movedM
                    )
                    // 搖桿和自動移動走的路都算；瞬移不經過這裡所以不算
                    if (snapshot.healthSync) tally.add(movedM)
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

    /** 停止推送迴圈，把 running 與自動移動一起清掉 —— 沒在模擬時「自動移動中」沒有意義。 */
    fun stop() {
        job?.cancel()
        job = null
        MockStateHolder.update { it.copy(autoMove = false, running = false) }
    }

    /**
     * 直接瞬移到指定座標（搜尋地名／輸入經緯度時用）。
     * 不論目前是否在跑，都以新座標為起點重啟推送迴圈；自動移動開著的話會從新座標繼續走。
     */
    fun teleport(target: LatLng) = start(target)
}
