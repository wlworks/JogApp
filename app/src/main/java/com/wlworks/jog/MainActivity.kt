package com.wlworks.jog

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.wlworks.jog.ads.RewardedGate
import com.wlworks.jog.data.HealthConnectStore
import com.wlworks.jog.service.FloatingWindowService
import com.wlworks.jog.state.MockStateHolder

/**
 * 只負責權限引導 —— 真正的操作全在懸浮視窗裡。
 *
 * 四道關卡（缺一不可）：
 *  1. 懸浮視窗權限（SYSTEM_ALERT_WINDOW，需跳系統設定）
 *  2. 定位權限（runtime）—— 系統對話框之前先跳一段事前說明，交代用途
 *  3. 通知權限（Android 13+，前景服務通知要顯示）
 *  4. 開發者選項裡把本 App 選為「模擬位置資訊應用程式」（無法用程式代勞，只能引導）。
 *     選了沒有讀得到（mock_location appop），開發者選項開了沒有也讀得到，所以這列會打勾，
 *     點下去依狀態跳出對應的步驟說明（[MockGuideDialog]）。仍不算進「可以啟動」的條件：
 *     少數機型的 appop 回報不可靠，擋下來反而讓人卡死；真的沒選，開始模擬時面板會再提示一次。
 *
 * 另有一列選用的 Health Connect 寫入權限：只有要用「Health 同步」才需要，不影響啟動。
 * 權限只能從 Activity 申請，所以懸浮面板發現缺權限時會帶 [EXTRA_REQUEST_HEALTH] 把人送回這裡。
 *
 * 含廣告的 build 在需要同意的地區（歐盟等）會多一列「廣告隱私設定」，讓使用者隨時改變同意選擇 ——
 * 這是 UMP 的規定，入口只在 [RewardedGate.checkPrivacyOptions] 說需要時才出現。
 *
 * 畫面上同時放了用途說明與隱私權政策連結。SYSTEM_ALERT_WINDOW 與定位權限是 Play 審核
 * 最在意的兩項，App 內講清楚比只在商店文案講有用。
 */
class MainActivity : ComponentActivity() {

    companion object {

        /** intent extra：一進來就跳 Health Connect 的事前說明。 */
        private const val EXTRA_REQUEST_HEALTH = "request_health"

        /** intent extra：一進來就跳「選為模擬位置 App」的步驟說明。 */
        private const val EXTRA_REQUEST_MOCK_GUIDE = "request_mock_guide"

        /**
         * 系統設定的「捲到並標亮這一項」參數，值是偏好設定的 key。AOSP 與 Pixel 有效，
         * 其他廠牌不認得就忽略，不會出錯。
         */
        private const val EXTRA_SETTINGS_HIGHLIGHT = ":settings:fragment_args_key"

        /** 開發者選項裡「選取模擬位置資訊應用程式」那一項的偏好設定 key（AOSP）。 */
        private const val MOCK_LOCATION_PREF_KEY = "mock_location_app"

        /** 隱私權政策公開網址，由 repo 的 docs/ 透過 GitHub Pages 發佈。 */
        const val PRIVACY_POLICY_URL = "https://wlworks.github.io/JogApp/privacy-policy"

        /** 組出「回設定畫面申請 Health Connect 權限」的 intent。從 Service 啟動，所以要帶 NEW_TASK。 */
        fun healthPermissionIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_REQUEST_HEALTH, true)

        /** 組出「回設定畫面看模擬位置 App 設定步驟」的 intent，理由同 [healthPermissionIntent]。 */
        fun mockGuideIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_REQUEST_MOCK_GUIDE, true)
    }

    /** 是否要顯示「廣告隱私設定」列；查詢是非同步的，所以另外存一份給畫面讀。 */
    private var adPrivacyAvailable by mutableStateOf(false)

    private val health by lazy { HealthConnectStore(this) }

    /** Health Connect 寫入權限是否已取得；查詢是 suspend 的，所以另外存一份給畫面讀。 */
    private var healthGranted by mutableStateOf(false)

    private val openSettings = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { permissionTick++ }

    private var permissionTick by mutableStateOf(0)

    private val requestHealth = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { refreshHealth() }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionTick++ }

    private var showHealthDisclosure by mutableStateOf(false)

    private var showLocationDisclosure by mutableStateOf(false)

    private var showMockGuide by mutableStateOf(false)

    /** 面板把人送回來申請 Health Connect 權限時，直接跳事前說明；extra 用過就清掉，轉向不會重跳。 */
    private fun handleHealthRequest(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_REQUEST_HEALTH, false) != true) return
        intent.removeExtra(EXTRA_REQUEST_HEALTH)
        if (health.isAvailable()) showHealthDisclosure = true
    }

    /** 面板把人送回來看模擬位置 App 的設定步驟時，直接跳說明；extra 用過就清掉，理由同上。 */
    private fun handleMockGuideRequest(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_REQUEST_MOCK_GUIDE, false) != true) return
        intent.removeExtra(EXTRA_REQUEST_MOCK_GUIDE)
        showMockGuide = true
    }

    /** 是否已取得精確定位權限。 */
    private fun hasLocationPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    /** 是否已取得通知權限，Android 13 以下一律視為已取得。 */
    private fun hasNotificationPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

    /** 開發者選項是否已開啟（連點版本號碼七次那一步）。讀不到就當作沒開。 */
    private fun isDeveloperOptionsEnabled(): Boolean =
        Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1

    /**
     * 本 App 是否已被選為模擬位置 App。開發者選項裡的那個選擇，實際上就是把本 App 的
     * mock_location appop 設成 allow（adb 用 appops set 給的也是同一個開關），查自己的 appop 不需要權限。
     */
    private fun isMockAppSelected(): Boolean = runCatching {
        val appOps = getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, Process.myUid(), packageName)
        }
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    /** 掛上設定畫面，並把各權限狀態接到勾選列。 */
    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 36 起無法退出 edge-to-edge，內容會被狀態列／導覽列蓋住
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                @Suppress("UNUSED_EXPRESSION") permissionTick // 觸發重組
                SetupScreen(
                    overlayGranted = Settings.canDrawOverlays(this),
                    locationGranted = hasLocationPermission(),
                    notificationGranted = hasNotificationPermission(),
                    onRequestOverlay = ::requestOverlay,
                    onRequestLocation = { showLocationDisclosure = true },
                    onRequestNotification = ::requestNotification,
                    healthAvailable = health.isAvailable(),
                    healthGranted = healthGranted,
                    onRequestHealth = { showHealthDisclosure = true },
                    adPrivacyAvailable = adPrivacyAvailable,
                    onOpenAdPrivacy = { RewardedGate.showPrivacyOptions(this) },
                    mockAppSelected = isMockAppSelected(),
                    onOpenMockGuide = { showMockGuide = true },
                    onOpenPrivacyPolicy = ::openPrivacyPolicy,
                    onStart = { FloatingWindowService.start(this) },
                    onStop = { FloatingWindowService.stop(this) }
                )
                if (showLocationDisclosure) {
                    LocationDisclosureDialog(
                        onConfirm = {
                            showLocationDisclosure = false
                            requestLocation()
                        },
                        onDismiss = { showLocationDisclosure = false }
                    )
                }
                if (showMockGuide) {
                    // 對話框開著的期間請懸浮面板讓開，關掉（或離開組合）時放回來
                    DisposableEffect(Unit) {
                        MockStateHolder.update { it.copy(mockGuideShowing = true) }
                        onDispose { MockStateHolder.update { it.copy(mockGuideShowing = false) } }
                    }
                    val developerOptions = isDeveloperOptionsEnabled()
                    MockGuideDialog(
                        appLabel = applicationInfo.loadLabel(packageManager).toString(),
                        developerOptionsEnabled = developerOptions,
                        mockAppSelected = isMockAppSelected(),
                        onConfirm = {
                            showMockGuide = false
                            if (developerOptions) openDeveloperOptions() else openAboutPhone()
                        },
                        onDismiss = { showMockGuide = false }
                    )
                }
                if (showHealthDisclosure) {
                    DisclosureDialog(
                        title = stringResource(R.string.disclosure_health_title),
                        body = stringResource(R.string.disclosure_health_body),
                        onConfirm = {
                            showHealthDisclosure = false
                            requestHealth.launch(HealthConnectStore.PERMISSIONS)
                        },
                        onDismiss = { showHealthDisclosure = false }
                    )
                }
            }
        }
        handleHealthRequest(intent)
        handleMockGuideRequest(intent)
    }

    /** singleTop：設定畫面已經開著時，面板送來的申請走這裡。 */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleHealthRequest(intent)
        handleMockGuideRequest(intent)
    }

    /** 從系統設定返回時重新檢查權限。 */
    override fun onResume() {
        super.onResume()
        permissionTick++
        refreshAdPrivacy()
        refreshHealth()
    }

    /** 跳到「關於手機」，版本號碼在那裡（部分廠牌在下一層）；跳不動就退回主設定頁。 */
    private fun openAboutPhone() {
        runCatching {
            openSettings.launch(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
        }.onFailure {
            openSettings.launch(Intent(Settings.ACTION_SETTINGS))
        }
    }

    /** 跳到開發者選項並請系統標亮模擬位置那一項，跳不動就退回主設定頁。 */
    private fun openDeveloperOptions() {
        runCatching {
            openSettings.launch(
                Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                    .putExtra(EXTRA_SETTINGS_HIGHLIGHT, MOCK_LOCATION_PREF_KEY)
            )
        }.onFailure {
            openSettings.launch(Intent(Settings.ACTION_SETTINGS))
        }
    }

    /** 用外部瀏覽器開隱私權政策；本 App 自己沒有也不需要網路權限。沒有瀏覽器就靜默略過。 */
    private fun openPrivacyPolicy() {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
        }
    }

    /** 含廣告的 build 才查：這個使用者需不需要「廣告隱私設定」入口。 */
    private fun refreshAdPrivacy() {
        if (!RewardedGate.ENABLED) return
        RewardedGate.checkPrivacyOptions(this) { adPrivacyAvailable = it }
    }

    /** 重新查一次 Health Connect 權限並更新畫面。 */
    private fun refreshHealth() {
        lifecycleScope.launch { healthGranted = health.hasPermissions() }
    }

    /** 要求精確與概略定位權限。只在使用者看過事前說明並按下繼續後才呼叫。 */
    private fun requestLocation() {
        requestPermissions.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /** 要求通知權限，Android 13 以下不需要。 */
    private fun requestNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    /** 跳到系統的懸浮視窗權限設定頁。 */
    private fun requestOverlay() {
        openSettings.launch(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }
}

/** 設定畫面底部的長按鈕。 */
@Composable
private fun Action(text: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (enabled) Color.Black else Color.White.copy(alpha = 0.4f),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) Color(0xFF4DD0E1) else Color.White.copy(alpha = 0.08f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        textAlign = TextAlign.Center
    )
}

/** 單一權限勾選列。granted 為 null 代表程式無從得知，只能顯示箭頭引導。 */
@Composable
private fun CheckRow(title: String, granted: Boolean?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (granted) {
                true -> "✓"
                false -> "!"
                null -> "→"
            },
            color = if (granted == true) Color(0xFF4DD0E1) else Color(0xFFFFB4A9),
            fontSize = 15.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(title, color = Color.White, fontSize = 14.sp)
    }
}

/** 事前說明（prominent disclosure）的共用對話框：在系統對話框之前用自己的 UI 交代用途，按繼續才真正發出請求。 */
@Composable
private fun DisclosureDialog(title: String, body: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.disclosure_continue)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.disclosure_cancel)) }
        }
    )
}

/** 定位權限的事前說明。 */
@Composable
private fun LocationDisclosureDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    DisclosureDialog(
        title = stringResource(R.string.disclosure_location_title),
        body = stringResource(R.string.disclosure_location_body),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * 「選為模擬位置 App」的步驟說明，依目前卡在哪一步顯示不同內容：
 * 開發者選項還沒開 → 教連點版本號碼；開了還沒選 → 教去哪裡選；已經選好 → 告訴他完成了、哪裡可以改。
 * [appLabel] 用實際的 App 名稱，debug 版在選單裡叫 Jog Debug，照抄才找得到。
 */
@Composable
private fun MockGuideDialog(
    appLabel: String,
    developerOptionsEnabled: Boolean,
    mockAppSelected: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, body) = when {
        mockAppSelected ->
            stringResource(R.string.guide_done_title) to stringResource(R.string.guide_done_body, appLabel)
        developerOptionsEnabled ->
            stringResource(R.string.guide_select_title, appLabel) to
                stringResource(R.string.guide_select_body, appLabel)
        else ->
            stringResource(R.string.guide_devopts_title) to stringResource(R.string.guide_devopts_body)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(
                        if (developerOptionsEnabled) R.string.guide_open_devopts else R.string.guide_open_about
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.guide_close)) }
        }
    )
}

/** 權限引導畫面：用途說明、四道關卡與選用的 Health Connect、啟動／關閉懸浮視窗，底下附隱私權政策連結。 */
@Composable
private fun SetupScreen(
    overlayGranted: Boolean,
    locationGranted: Boolean,
    notificationGranted: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestNotification: () -> Unit,
    healthAvailable: Boolean,
    healthGranted: Boolean,
    onRequestHealth: () -> Unit,
    adPrivacyAvailable: Boolean,
    onOpenAdPrivacy: () -> Unit,
    mockAppSelected: Boolean,
    onOpenMockGuide: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val ready = overlayGranted && locationGranted && notificationGranted
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121416))
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.setup_title),
            color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.setup_subtitle),
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
        Text(
            stringResource(R.string.setup_purpose),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        CheckRow(stringResource(R.string.setup_overlay), overlayGranted, onRequestOverlay)
        CheckRow(stringResource(R.string.setup_location), locationGranted, onRequestLocation)
        CheckRow(stringResource(R.string.setup_notification), notificationGranted, onRequestNotification)
        CheckRow(
            title = stringResource(R.string.setup_mock_app),
            granted = mockAppSelected,
            onClick = onOpenMockGuide
        )
        // 選用，不算進 ready：不用 Health 同步的人完全不必理它
        if (healthAvailable) {
            CheckRow(stringResource(R.string.setup_health), healthGranted, onRequestHealth)
        } else {
            CheckRow(stringResource(R.string.setup_health_unavailable), granted = false, onClick = {})
        }
        // 沒有「已完成」的狀態可言，只顯示箭頭
        if (adPrivacyAvailable) {
            CheckRow(stringResource(R.string.setup_ad_privacy), granted = null, onClick = onOpenAdPrivacy)
        }

        Action(stringResource(R.string.setup_start), enabled = ready, onClick = onStart)
        Action(stringResource(R.string.setup_stop), enabled = true, onClick = onStop)

        Text(
            text = stringResource(R.string.setup_privacy_policy),
            color = Color(0xFF4DD0E1),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenPrivacyPolicy)
                .padding(vertical = 8.dp)
        )
    }
}
