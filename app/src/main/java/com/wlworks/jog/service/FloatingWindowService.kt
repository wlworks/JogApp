package com.wlworks.jog.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.wlworks.jog.MainActivity
import com.wlworks.jog.R
import com.wlworks.jog.core.LatLng
import com.wlworks.jog.data.GeocodeRepository
import com.wlworks.jog.mock.MockLocationEngine
import com.wlworks.jog.mock.MovementController
import com.wlworks.jog.state.MockStateHolder
import com.wlworks.jog.ui.CollapsedBubble
import com.wlworks.jog.ui.FloatingPanel
import com.wlworks.jog.ui.PanelActions

/**
 * 懸浮視窗 + 模擬定位的宿主。用前景服務是為了：
 *  - 使用者切走／鎖螢幕時不被回收
 *  - Android 12+ 對背景啟動 overlay 的限制
 */
class FloatingWindowService : LifecycleService() {

    companion object {

        /** 通知列「停止」動作帶的 intent action。 */
        const val ACTION_STOP = "com.wlworks.jog.STOP"

        /** 前景服務通知所屬的頻道 id。 */
        private const val CHANNEL_ID = "jog_running"

        /** 前景服務通知 id。 */
        private const val NOTIF_ID = 1001

        /** 依 API 等級用正確的方式拉起前景服務。 */
        fun start(context: Context) {
            val intent = Intent(context, FloatingWindowService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** 停掉服務，連帶收掉懸浮視窗與模擬定位。 */
        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingWindowService::class.java))
        }
    }

    private lateinit var engine: MockLocationEngine
    private lateinit var geocoder: GeocodeRepository
    private lateinit var movement: MovementController
    private lateinit var overlay: OverlayHost

    /** 以 [point] 為起點開始注入，並處理未被選為 mock app 等失敗情形。 */
    private fun applyTarget(point: LatLng) {
        when (val result = engine.start()) {
            MockLocationEngine.StartResult.Ok -> movement.teleport(point)
            MockLocationEngine.StartResult.NotMockApp -> {
                MockStateHolder.update {
                    it.copy(
                        needsMockAppSetup = true,
                        message = getString(R.string.msg_need_mock_app)
                    )
                }
            }
            is MockLocationEngine.StartResult.Failed ->
                MockStateHolder.message(
                    getString(R.string.msg_start_failed, result.throwable.message.orEmpty())
                )
        }
    }

    /** 建立前景服務通知，必要時順便補上通知頻道。 */
    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.notif_channel_name),
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, FloatingWindowService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(R.drawable.ic_pin)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_pin),
                    getString(R.string.notif_action_stop),
                    stop
                ).build()
            )
            .build()
    }

    /** 是否已取得精確定位權限。 */
    private fun hasLocationPermission() = ContextCompat.checkSelfPermission(
        this, android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    /** 建好相依元件、確認權限、進入前景並掛上懸浮視窗。 */
    override fun onCreate() {
        super.onCreate()
        engine = MockLocationEngine(this)
        movement = MovementController(engine, lifecycleScope)
        geocoder = GeocodeRepository(this)
        overlay = OverlayHost(this)

        // 權限在這裡再確認一次：使用者可能事後撤銷，而 START_STICKY 會把我們重新拉起來。
        // Android 14+ 沒有定位權限就 startForeground(TYPE_LOCATION) 會直接 SecurityException。
        if (!canDrawOverlay() || !hasLocationPermission()) {
            stopSelf()
            return
        }

        val started = runCatching {
            // Android 10+ 起前景服務必須在啟動時就宣告 type
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIF_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIF_ID, buildNotification())
            }
        }.isSuccess

        if (!started) {
            stopSelf()
            return
        }
        showOverlay()
    }

    /** 收掉模擬、移除懸浮視窗。 */
    override fun onDestroy() {
        movement.stop()
        engine.stop()
        overlay.dismiss()
        super.onDestroy()
    }

    /** 處理通知列的停止動作，其餘情況維持 START_STICKY。 */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    /** 解析輸入框內容並瞬移過去，查不到就把訊息寫回面板。 */
    private fun search() {
        val query = MockStateHolder.state.value.query
        if (query.isBlank()) return
        MockStateHolder.releaseInputFocus()
        MockStateHolder.update { it.copy(searching = true, message = null) }

        lifecycleScope.launch {
            val outcome = geocoder.resolve(query)
            MockStateHolder.update { it.copy(searching = false) }
            when (outcome) {
                is GeocodeRepository.Outcome.Found -> {
                    val first = outcome.hits.first()
                    MockStateHolder.update {
                        // 座標解析出來的 label 帶有「座標 」前綴，寫回輸入框會讓
                        // 下一次搜尋解析失敗，所以只回填地名查詢的結果
                        it.copy(
                            message = null,
                            query = if (first.fromCoordinates) it.query else first.label
                        )
                    }
                    applyTarget(first.point)
                }
                GeocodeRepository.Outcome.NoMatch ->
                    MockStateHolder.message(getString(R.string.msg_not_found, query))
                GeocodeRepository.Outcome.Unavailable ->
                    MockStateHolder.message(getString(R.string.msg_geocoder_unavailable))
            }
        }
    }

    /** 把面板／收合泡泡的 Compose 內容掛進 overlay，並接好所有互動回呼。 */
    private fun showOverlay() {
        overlay.show {
            val state by MockStateHolder.state.collectAsState()
            var collapsed by remember { mutableStateOf(false) }

            // 這個 effect 必須放在 collapsed 分支**之外**：
            // 收合時面板整棵子樹會被移除，放在裡面的 LaunchedEffect 是被丟棄而不是重啟，
            // 收合路徑的 releaseInputFocus() 就會變成死碼。
            val focusManager = LocalFocusManager.current
            var handledTick by remember { mutableStateOf(state.focusReleaseTick) }
            LaunchedEffect(state.focusReleaseTick) {
                if (state.focusReleaseTick != handledTick) {
                    handledTick = state.focusReleaseTick
                    focusManager.clearFocus(force = true)
                }
            }

            val dragHandle = Modifier.pointerInput(Unit) {
                detectDragGestures { change, delta ->
                    change.consume()
                    overlay.moveBy(delta.x, delta.y)
                }
            }

            if (collapsed) {
                CollapsedBubble(
                    running = state.running,
                    dragHandle = dragHandle,
                    onExpand = { collapsed = false }
                )
            } else {
                FloatingPanel(
                    state = state,
                    dragHandle = dragHandle,
                    actions = PanelActions(
                        onClose = { stopSelf() },
                        onCollapse = {
                            // 不直接動視窗旗標：清掉輸入框焦點，讓 onFocusChanged 去關
                            MockStateHolder.releaseInputFocus()
                            collapsed = true
                        },
                        onInputFocusChanged = overlay::setInputFocusable,
                        onQueryChange = MockStateHolder::setQuery,
                        onSearch = ::search,
                        onSpeed = MockStateHolder::setSpeed,
                        onStick = MockStateHolder::setStick,
                        onToggleRun = ::toggleRun
                    )
                )
            }
        }
    }

    /** 開始／停止模擬。沒有起點座標時提示使用者先設定。 */
    private fun toggleRun() {
        val state = MockStateHolder.state.value
        if (state.running) {
            movement.stop()
            engine.stop()
        } else {
            val origin = state.current
            if (origin == null) {
                MockStateHolder.message(getString(R.string.msg_need_target))
                return
            }
            applyTarget(origin)
        }
    }
}
