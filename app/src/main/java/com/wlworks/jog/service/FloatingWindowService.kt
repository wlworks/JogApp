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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.wlworks.jog.MainActivity
import com.wlworks.jog.R
import com.wlworks.jog.core.DistanceFormat
import com.wlworks.jog.core.GeoMath
import com.wlworks.jog.core.HealthTally
import com.wlworks.jog.core.LatLng
import com.wlworks.jog.core.RandomJump
import com.wlworks.jog.core.SpeedTier
import com.wlworks.jog.data.GeocodeRepository
import com.wlworks.jog.data.HealthConnectStore
import com.wlworks.jog.data.LastLocationStore
import com.wlworks.jog.mock.MockLocationEngine
import com.wlworks.jog.mock.MovementController
import com.wlworks.jog.state.MockStateHolder
import com.wlworks.jog.ui.CollapsedPanel
import com.wlworks.jog.ui.FloatingPanel
import com.wlworks.jog.ui.PanelActions
import java.time.Instant
import kotlin.random.Random

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

        /**
         * 多久把累計的步數／距離寫進 Health Connect 一次。官方對運動中步數的預期粒度是每分鐘一筆，
         * 寫入間隔上限是 15 分鐘；60 秒剛好對上前者，也讓測試時不必等太久才看得到資料。
         */
        private const val HEALTH_FLUSH_MS = 60_000L

        /** 前景服務通知 id。 */
        private const val NOTIF_ID = 1001

        /** 模擬座標寫入 SharedPreferences 的最小間隔。移動中 3Hz 的更新沒必要每次都落地。 */
        private const val SAVE_INTERVAL_MS = 2_000L

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
    private lateinit var health: HealthConnectStore

    /** 寫 Health Connect 用的 scope。不跟 Service 的 lifecycle 綁，onDestroy 補寫的最後一批才不會被取消。 */
    private val healthScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 下一批 Health Connect 紀錄的起始時間；每次 flush 後往前推，紀錄之間不重疊。 */
    private var healthWindowStart: Instant = Instant.now()
    private lateinit var lastLocation: LastLocationStore
    private lateinit var movement: MovementController
    private lateinit var overlay: OverlayHost
    private val tally = HealthTally()

    /** 以 [point] 為起點開始注入，並處理未被選為 mock app 等失敗情形。 */
    private fun applyTarget(point: LatLng) {
        when (val result = engine.start()) {
            MockLocationEngine.StartResult.Ok -> {
                // 成功了就把上一次「請先選為 mock app」的提示收掉，不然它會一直掛在面板底部
                MockStateHolder.update { it.copy(message = null, needsMockAppSetup = false) }
                movement.teleport(point)
            }
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

    /** 建立前景服務通知，必要時順便補上通知頻道。內文依模擬狀態顯示「待命」、目前速度檔或自動移動中。 */
    private fun buildNotification(running: Boolean, tier: SpeedTier, auto: Boolean): Notification {
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
        val text = when {
            running && auto -> getString(R.string.notif_text_auto, getString(tier.labelRes))
            running -> getString(R.string.notif_text_running, getString(tier.labelRes))
            else -> getString(R.string.notif_text)
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(text)
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

    /**
     * 把 [tally] 累計到現在的量寫成一批 Health Connect 紀錄，時間涵蓋上次 flush 到現在。
     * 只在主執行緒呼叫（定時迴圈、關閉同步、onDestroy），所以時間窗的推進不必上鎖；
     * 真正的寫入丟到 [healthScope]。寫失敗多半是權限被撤銷，直接關掉同步並提示。
     */
    private fun flushHealth() {
        val batch = tally.drain()
        val start = healthWindowStart
        val end = Instant.now()
        healthWindowStart = end
        if (batch.isEmpty) return
        healthScope.launch {
            if (health.write(batch = batch, start = start, end = end)) {
                MockStateHolder.update {
                    it.copy(
                        healthDistanceM = it.healthDistanceM + batch.distanceM,
                        healthSteps = it.healthSteps + batch.steps
                    )
                }
            } else {
                MockStateHolder.update {
                    it.copy(healthSync = false, message = getString(R.string.msg_health_write_failed))
                }
            }
        }
    }

    /** 是否已取得精確定位權限。 */
    private fun hasLocationPermission() = ContextCompat.checkSelfPermission(
        this, android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * Health Connect 同步開著的期間，每 HEALTH_FLUSH_MS 寫一次。
     * 關掉時 collectLatest 會取消迴圈；最後一段由關閉的那一方（toggleHealthSync / onDestroy）補寫。
     */
    private fun observeHealthSync() {
        lifecycleScope.launch {
            MockStateHolder.state
                .map { it.healthSync }
                .distinctUntilChanged()
                .collectLatest { on ->
                    if (!on) return@collectLatest
                    // 開啟前累計的不算，時間窗從現在起算
                    tally.drain()
                    healthWindowStart = Instant.now()
                    while (true) {
                        delay(HEALTH_FLUSH_MS)
                        flushHealth()
                    }
                }
        }
    }

    /** 模擬開始／停止、切換速度檔或自動移動時更新常駐通知內文。座標變化不更新，避免每秒刷三次通知。 */
    private fun observeStateForNotification() {
        lifecycleScope.launch {
            MockStateHolder.state
                .map { Triple(it.running, it.speedTier, it.autoMove) }
                .distinctUntilChanged()
                .drop(1) // 第一份已經用在 startForeground
                .collect { (running, tier, auto) ->
                    getSystemService(NotificationManager::class.java)
                        .notify(NOTIF_ID, buildNotification(running, tier, auto))
                }
        }
    }

    /** 座標一變就存下來，但兩次寫入至少隔 SAVE_INTERVAL_MS；conflate 保證存到的永遠是最新值。 */
    private fun observeStateForPersistence() {
        lifecycleScope.launch {
            MockStateHolder.state
                .map { it.current }
                .filterNotNull()
                .distinctUntilChanged()
                .conflate()
                .collect { point ->
                    lastLocation.save(point)
                    delay(SAVE_INTERVAL_MS)
                }
        }
    }

    /** 建好相依元件、確認權限、進入前景並掛上懸浮視窗。 */
    override fun onCreate() {
        super.onCreate()
        engine = MockLocationEngine(this)
        movement = MovementController(engine = engine, scope = lifecycleScope, tally = tally)
        geocoder = GeocodeRepository(this)
        health = HealthConnectStore(this)
        lastLocation = LastLocationStore(this)
        overlay = OverlayHost(this)

        // 權限在這裡再確認一次：使用者可能事後撤銷，而 START_STICKY 會把我們重新拉起來。
        // Android 14+ 沒有定位權限就 startForeground(TYPE_LOCATION) 會直接 SecurityException。
        if (!canDrawOverlay() || !hasLocationPermission()) {
            stopSelf()
            return
        }

        val started = runCatching {
            // 狀態是行程層級的，Service 被 START_STICKY 拉起時可能已經在模擬中
            val state = MockStateHolder.state.value
            val notification = buildNotification(state.running, state.speedTier, state.autoMove)
            // Android 10+ 起前景服務必須在啟動時就宣告 type
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIF_ID, notification)
            }
        }.isSuccess

        if (!started) {
            stopSelf()
            return
        }
        observeHealthSync()
        observeStateForNotification()
        observeStateForPersistence()
        showOverlay()
    }

    /**
     * 收掉模擬、移除懸浮視窗，並把最後座標補存一次（可能落在節流間隔內還沒寫）。
     * Health Connect 還沒寫的那一段也在這裡補寫；狀態是行程層級的，所以同步開關與累計數字
     * 都要清掉，下次開面板從頭來（速度檔的鎖也跟著解開）。
     */
    override fun onDestroy() {
        movement.stop()
        engine.stop()
        overlay.dismiss()
        if (MockStateHolder.state.value.healthSync) flushHealth()
        MockStateHolder.state.value.current?.let(lastLocation::save)
        MockStateHolder.update {
            it.copy(healthDistanceM = 0.0, healthSteps = 0L, healthSync = false)
        }
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
                    // 使用者打的字留在輸入框，解析結果另外顯示。以前會把 label 寫回輸入框，
                    // 結果封測有人再按一次「定位」就被帶到別處 —— 見 GeocodeHit.label 的說明。
                    MockStateHolder.update {
                        it.copy(message = null, resolvedLabel = first.label)
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
        // 打字中按返回鍵或點到面板外面 → 放掉輸入框焦點，讓 onFocusChanged 把視窗改回不可聚焦。
        // 不直接動視窗旗標，理由同 onCollapse。
        overlay.show(onDismissInput = MockStateHolder::releaseInputFocus) {
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
                CollapsedPanel(
                    autoMove = state.autoMove,
                    running = state.running,
                    speedTier = state.speedTier,
                    dragHandle = dragHandle,
                    onExpand = { collapsed = false },
                    onRandomNearby = ::teleportRandomNearby,
                    onStick = MockStateHolder::setStick
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
                        onRandomNearby = ::teleportRandomNearby,
                        onSearch = ::search,
                        onSpeed = MockStateHolder::setSpeed,
                        onStick = MockStateHolder::setStick,
                        onToggleAuto = ::toggleAutoMove,
                        onToggleHealth = ::toggleHealthSync,
                        onToggleRun = ::toggleRun
                    )
                )
            }
        }
    }

    /**
     * 以目前座標為中心，隨機挑一個方向，跳到 [RandomJump] 依目前速度檔算出的距離外。
     * 沒在模擬時也能按 —— 走 applyTarget 會順便開始注入，行為和「定位到這裡」一致。
     * 跳完把「往哪跳、跳多遠、當時哪一檔」寫進 resolvedLabel，讓使用者把距離和檔次連起來。
     */
    private fun teleportRandomNearby() {
        val state = MockStateHolder.state.value
        val origin = state.current
        if (origin == null) {
            MockStateHolder.message(getString(R.string.msg_need_target))
            return
        }
        val bearing = Random.nextFloat() * 360f
        val distance = RandomJump.pickDistance(state.speedTier)
        val target = GeoMath.destination(from = origin, bearingDeg = bearing, distanceM = distance)
        val summary = getString(
            R.string.msg_random_jumped,
            resources.getStringArray(R.array.compass_points)[GeoMath.compassIndex(bearing)],
            DistanceFormat.single(distance),
            getString(state.speedTier.labelRes)
        )
        MockStateHolder.update { it.copy(message = null, resolvedLabel = summary) }
        applyTarget(target)
    }

    /**
     * 開／關自動移動。關掉只是交還方向，模擬繼續跑；要整個停下來按「停止模擬」。
     * 還沒在模擬時按下去會順便開始注入，起點是目前座標。
     *
     * 開和關都順手清掉 resolvedLabel：那行字講的是「上次定位到哪／上次往哪跳了多遠」，
     * 自動走過一段之後已經和目前位置對不上。自動移動期間面板改顯示「自動移動中」。
     */
    private fun toggleAutoMove() {
        val state = MockStateHolder.state.value
        if (state.autoMove) {
            MockStateHolder.update { it.copy(autoMove = false, resolvedLabel = null) }
            return
        }
        val origin = state.current
        if (origin == null) {
            MockStateHolder.message(getString(R.string.msg_need_target))
            return
        }
        if (!state.running) applyTarget(origin)
        // applyTarget 可能失敗（沒被選為 mock app），沒跑起來就不要亮自動移動
        MockStateHolder.update {
            it.copy(autoMove = it.running, resolvedLabel = if (it.running) null else it.resolvedLabel)
        }
    }

    /**
     * 開／關 Health Connect 同步。開之前過兩關：裝置支援、寫入權限；
     * 權限只能從 Activity 申請，所以缺權限時把使用者帶回設定畫面。
     * 開啟的同時把速度檔切到步行並鎖住（見 MockUiState.healthSync）。
     */
    private fun toggleHealthSync() {
        if (MockStateHolder.state.value.healthSync) {
            flushHealth()
            MockStateHolder.update { it.copy(healthSync = false) }
            return
        }
        if (!health.isAvailable()) {
            MockStateHolder.message(getString(R.string.msg_health_unavailable))
            return
        }
        lifecycleScope.launch {
            if (health.hasPermissions()) {
                MockStateHolder.update {
                    it.copy(healthSync = true, message = null, speedTier = SpeedTier.WALK)
                }
            } else {
                MockStateHolder.message(getString(R.string.msg_health_need_permission))
                startActivity(MainActivity.healthPermissionIntent(this@FloatingWindowService))
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
