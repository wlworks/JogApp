package com.wlworks.jog.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wlworks.jog.R
import com.wlworks.jog.core.DistanceFormat
import com.wlworks.jog.core.HealthTally
import com.wlworks.jog.core.RandomJump
import com.wlworks.jog.core.SpeedTier
import com.wlworks.jog.state.MockUiState
import java.util.Locale

/** 主色，用在啟用狀態的控制項上。 */
private val Accent = Color(0xFF4DD0E1)

/** 「廣告」小標的底色。刻意不用主色：主色代表「已啟用」，小標要表達的是「按了會先看廣告」。 */
private val AdBadge = Color(0xFFFFD54F)

/** 面板底色，帶一點透明度讓底下的 App 隱約透出來。 */
private val Panel = Color(0xF01A1C1E)

data class PanelActions(
    val onClose: () -> Unit,
    val onCollapse: () -> Unit,
    /**
     * 輸入框取得／失去焦點時通知 Service 切換 WindowManager 的 FLAG_NOT_FOCUSABLE。
     * 這是**唯一**會改動視窗焦點旗標的路徑 —— 其他地方一律透過清除 Compose 焦點
     * 來間接觸發，避免旗標與 Compose 焦點狀態各走各的而失去同步。
     */
    val onInputFocusChanged: (Boolean) -> Unit,
    val onQueryChange: (String) -> Unit,
    val onRandomNearby: () -> Unit,
    val onSearch: () -> Unit,
    val onSelectTab: (PanelTab) -> Unit,
    val onSpeed: (SpeedTier) -> Unit,
    val onStick: (Float, Float) -> Unit,
    val onToggleAuto: () -> Unit,
    val onToggleHealth: () -> Unit,
    val onToggleRun: () -> Unit
)

/**
 * 展開面板右半邊的分頁。選哪一頁由 Service 記著，收合再展開後還停在同一頁。
 * entries 依字母排剛好也是畫面上由左到右的順序。
 */
enum class PanelTab(@StringRes val labelRes: Int) {
    LOCATION(R.string.panel_tab_location),
    MOVE(R.string.panel_tab_move)
}

/**
 * 收合後的精簡面板：上方一顆狀態圓球（點一下展開），下方保留搖桿。
 * [dragArea] 套在整個面板上：搖桿以外的地方都能拖，理由同 [FloatingPanel]。
 * 封測回饋：收合後還是要能微調位置，不然每次推桿都得先展開、再收回去。
 * 自動移動中圓球外面多一圈，收合時也看得出來它自己在走。
 */
@Composable
fun CollapsedPanel(
    autoMove: Boolean,
    running: Boolean,
    speedTier: SpeedTier,
    dragArea: Modifier,
    onExpand: () -> Unit,
    onRandomNearby: () -> Unit,
    onStick: (Float, Float) -> Unit
) {
    // 寬度要寫死：overlay 視窗是 WRAP_CONTENT，裡面的 fillMaxWidth 會撐到整個螢幕寬
    Column(
        modifier = dragArea
            .width(112.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val expandDescription = stringResource(R.string.cd_expand)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onExpand)
                .semantics { contentDescription = expandDescription },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (autoMove) Accent.copy(alpha = 0.3f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (running) Accent else Color.White.copy(alpha = 0.35f))
                )
            }
        }
        Joystick(
            accent = Accent,
            enabled = running,
            size = 96.dp,
            onInput = onStick
        )
        // 收合時放不下完整句子，只印範圍，和展開面板用同一套數字
        RandomNearbyButton(
            label = randomJumpRange(speedTier),
            modifier = Modifier.fillMaxWidth(),
            onClick = onRandomNearby
        )
    }
}

/**
 * 懸浮面板本體，橫式兩欄：
 *  - 左欄固定：標題、搖桿、開始／停止 —— 不管在哪一頁都推得到桿、停得下來。
 *  - 右欄分頁：「位置」放輸入、定位與隨機跳、解析結果；「移動」放自動移動與 Health 同步。
 *    速度檔（分頁列下方）與目前座標（分頁內容下方）放在分頁外，兩頁共用。
 *
 * 直式面板塞滿控件後超過半個螢幕高，會蓋住底下 App 的大半畫面；橫式加分頁後約 372 × 245 dp，
 * 直拿手機也放得下。
 *
 * [adLocked] 為 true 表示這個 build 有廣告關卡且還沒解鎖：自動移動與 Health 同步的按鈕會帶
 * 「廣告」小標，讓使用者按之前就知道會先看廣告。功能已經開著時不標（關閉不必看廣告）。
 *
 * [dragArea] 套在最外層，而且要排在 padding 之前，邊框留白也算在可拖範圍內。以前只有標題列能拖，
 * 使用者常抓不到。子元件先拿到事件：搖桿自己吃掉拖曳，推桿不會拖到面板；
 * 從按鈕上按住拖動則會拖面板並取消那次點擊，輕點不受影響。
 */
@Composable
fun FloatingPanel(
    state: MockUiState,
    tab: PanelTab,
    actions: PanelActions,
    dragArea: Modifier,
    adLocked: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val latestActions by rememberUpdatedState(actions)

    // 輸入框目前是否持有焦點。面板自己記著，才能在使用者去碰搖桿／速度／開始鈕時
    // 主動收掉鍵盤 —— 否則視窗會一直停在 focusable，持續吃掉底層 App 的返回鍵。
    var queryFocused by remember { mutableStateOf(false) }
    val dismissKeyboard = {
        if (queryFocused) focusManager.clearFocus(force = true)
    }

    // 面板整個離開組合（收合成泡泡、Service 結束）時，onFocusChanged 不保證會被呼叫，
    // 所以在這裡補一刀。
    DisposableEffect(Unit) {
        onDispose { latestActions.onInputFocusChanged(false) }
    }

    // 寬度要寫死，理由同 CollapsedPanel
    Row(
        modifier = dragArea
            .width(372.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ---- 左欄：標題、搖桿、開始／停止 ----
        Column(
            modifier = Modifier.width(112.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (state.running) Accent else Color.White.copy(alpha = 0.3f))
                )
                Text(
                    text = "  " + stringResource(R.string.app_name),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            Joystick(
                accent = Accent,
                // 沒按「開始」時推桿不會改變座標，索性置灰並停接觸控。
                // 自動移動中仍可推：推桿暫時接手方向，放開後從那個方向繼續自動走
                enabled = state.running,
                size = 104.dp,
                onInput = { x, y ->
                    dismissKeyboard()   // queryFocused 為 false 時是 no-op
                    actions.onStick(x, y)
                }
            )
            PillButton(
                text = stringResource(
                    if (state.running) R.string.panel_stop else R.string.panel_start
                ),
                modifier = Modifier.fillMaxWidth(),
                filled = state.running,
                onClick = {
                    dismissKeyboard()
                    actions.onToggleRun()
                }
            )
        }

        // ---- 右欄：分頁列與分頁內容 ----
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PanelTab.entries.forEach { entry ->
                    TabButton(
                        text = stringResource(entry.labelRes),
                        selected = entry == tab,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            // 換頁前先收鍵盤：輸入框在「位置」頁，換走之後就不在組合裡了
                            dismissKeyboard()
                            actions.onSelectTab(entry)
                        }
                    )
                }
                IconText("—", stringResource(R.string.cd_collapse), actions.onCollapse)
                IconText("✕", stringResource(R.string.cd_close), actions.onClose)
            }

            // ---- 速度檔次 ----
            // 放在分頁外面，兩頁都看得到：隨機跳在「位置」頁、自動移動在「移動」頁，都跟著檔次走
            SpeedSelector(
                selected = state.speedTier,
                accent = Accent,
                // Health 同步中只開放用腳移動的檔次（蝸行／步行／慢跑），其他置灰
                isEnabled = { !state.healthSync || HealthTally.allows(it) },
                onSelect = {
                    dismissKeyboard()
                    actions.onSpeed(it)
                }
            )

            when (tab) {
                PanelTab.LOCATION -> LocationTab(
                    state = state,
                    actions = actions,
                    dismissKeyboard = dismissKeyboard,
                    onQueryFocusChanged = { focused ->
                        queryFocused = focused
                        actions.onInputFocusChanged(focused)
                    }
                )
                PanelTab.MOVE -> MoveTab(
                    state = state,
                    actions = actions,
                    adBadge = if (adLocked) stringResource(R.string.panel_ad_badge) else null,
                    dismissKeyboard = dismissKeyboard
                )
            }

            // ---- 目前座標 ----
            // 放在分頁外面，兩頁都看得到：在「移動」頁開自動移動時也要能看座標一路在變
            Text(
                text = state.current?.let {
                    // 刻意用 Locale.US：逗號小數點的語系會和座標分隔符撞在一起
                    String.format(Locale.US, "%.6f, %.6f · %.0f°", it.lat, it.lng, state.bearing)
                } ?: stringResource(R.string.panel_no_location),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )

            state.message?.let {
                Text(it, color = Color(0xFFFFB4A9), fontSize = 11.sp)
            }
        }
    }
}

/** 標題列上的純符號按鈕，補上 TalkBack 唸得出來的描述。 */
@Composable
private fun IconText(symbol: String, description: String, onClick: () -> Unit) {
    Text(
        text = symbol,
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 14.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics { contentDescription = description }
    )
}

/** 「位置」分頁：輸入框、定位與隨機跳、解析結果。 */
@Composable
private fun LocationTab(
    state: MockUiState,
    actions: PanelActions,
    dismissKeyboard: () -> Unit,
    onQueryFocusChanged: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        QueryField(
            value = state.query,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = actions.onQueryChange,
            onSearch = actions.onSearch,
            onFocusChanged = onQueryFocusChanged
        )

        // 定位鈕不和輸入框擠同一列：右欄只有約 226dp，並排的話輸入框剩不到 110dp 的字寬，
        // 連提示文字都要折三行。改和隨機跳並排，兩個「把位置換掉」的動作放在一起。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillButton(
                text = stringResource(R.string.panel_teleport),
                modifier = Modifier.width(64.dp),
                busy = state.searching,
                busyDescription = stringResource(R.string.panel_searching),
                enabled = !state.searching,
                onClick = actions.onSearch
            )
            // ---- 隨機跳到附近 ----
            // 範圍數字跟著上方的速度檔變，靠這串數字讓人看出距離是由檔次決定的。
            // 並排後放不下完整句子，和收合面板一樣只印範圍；完整句子給 TalkBack 唸。
            val randomDescription =
                stringResource(R.string.panel_random_nearby, randomJumpRange(state.speedTier))
            RandomNearbyButton(
                label = randomJumpRange(state.speedTier),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = randomDescription },
                onClick = {
                    dismissKeyboard()
                    actions.onRandomNearby()
                }
            )
        }

        // ---- 解析結果（不寫回輸入框，只顯示）----
        // 自動移動中一律換成「自動移動中」：定位／隨機跳的結果描述的是出發點，走遠了就不對了
        val label = if (state.autoMove) {
            stringResource(R.string.panel_auto_moving)
        } else {
            state.resolvedLabel
        }
        label?.let {
            Text(
                text = "→ $it",
                color = Accent.copy(alpha = 0.85f),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

    }
}

/** 「移動」分頁：自動移動與 Health 同步、已寫入的量。 */
@Composable
private fun MoveTab(
    state: MockUiState,
    actions: PanelActions,
    adBadge: String?,
    dismissKeyboard: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PillButton(
                text = stringResource(R.string.panel_auto_move),
                modifier = Modifier.weight(1f),
                badge = adBadge.takeUnless { state.autoMove },
                filled = state.autoMove,
                onClick = {
                    dismissKeyboard()
                    actions.onToggleAuto()
                }
            )
            PillButton(
                text = stringResource(R.string.panel_health_sync),
                modifier = Modifier.weight(1f),
                badge = adBadge.takeUnless { state.healthSync },
                filled = state.healthSync,
                onClick = {
                    dismissKeyboard()
                    actions.onToggleHealth()
                }
            )
        }

        // ---- 已寫進 Health Connect 的量。每分鐘才寫一次，剛開的前一分鐘是 0 ----
        if (state.healthSync) {
            Text(
                text = stringResource(
                    R.string.panel_health_written,
                    state.healthSteps,
                    DistanceFormat.single(state.healthDistanceM)
                ),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
    }
}

/**
 * 圓角長按鈕，filled 為 true 時用主色實心。[badge] 不是 null 時在文字前面加一個小標（例：「廣告」）。
 * [busy] 為 true 時文字換成轉圈，[busyDescription] 給 TalkBack 唸。
 */
@Composable
private fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    busy: Boolean = false,
    busyDescription: String? = null,
    enabled: Boolean = true,
    filled: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (filled) Accent else Color.White.copy(alpha = 0.10f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (busy) {
            CircularProgressIndicator(
                color = Accent,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(16.dp)
                    .semantics { contentDescription = busyDescription.orEmpty() }
            )
            return@Row
        }
        if (badge != null) {
            Text(
                text = badge,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(AdBadge)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = text,
            // 右欄兩顆並排各只有約 105dp，小標加「Health 同步」用 12sp 會被截掉，帶小標時縮一級
            fontSize = if (badge != null) 11.sp else 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (filled) Color.Black else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 地點／座標輸入框。只在焦點狀態**真的改變**時才回報，避免重複觸發 updateViewLayout。
 * 換到別的分頁時輸入框會離開組合，那時若還持有焦點就補報一次失焦，視窗才會變回不可聚焦。
 */
@Composable
private fun QueryField(
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFocusChanged: (Boolean) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val latestFocused by rememberUpdatedState(focused)
    val latestOnFocusChanged by rememberUpdatedState(onFocusChanged)

    DisposableEffect(Unit) {
        onDispose { if (latestFocused) latestOnFocusChanged(false) }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        // 座標串（例：35.039563227558034, 135.72926455412235）有38個字，
        // 右欄約 226dp 寬，允許折到第三行。
        // imeAction 已明確指定為 Search，多行下依然會走 onSearch，不會變成換行鍵。
        maxLines = 3,
        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
        placeholder = { Text(stringResource(R.string.panel_query_hint), fontSize = 11.sp) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Accent,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            cursorColor = Accent
        ),
        modifier = modifier
            .heightIn(min = 52.dp)
            .onFocusChanged { focusState ->
                if (focusState.isFocused != focused) {
                    focused = focusState.isFocused
                    onFocusChanged(focused)
                }
            }
    )
}

/** 該速度檔的隨機跳躍範圍字串，例：80–250 m、5–15 km。 */
private fun randomJumpRange(tier: SpeedTier): String {
    val range = RandomJump.rangeFor(tier)
    return DistanceFormat.range(range.start, range.endInclusive)
}

/** 「隨機跳到附近」按鈕，展開與收合面板共用；[label] 帶著依速度檔算出的距離範圍。 */
@Composable
private fun RandomNearbyButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 文字已經說明用途，圖示不必再給 TalkBack 唸一次
        Icon(
            imageVector = Icons.Filled.Shuffle,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

/** 分頁列上的一個分頁：選中時文字變亮、底下一條主色底線。 */
@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth(0.6f)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (selected) Accent else Color.Transparent)
        )
    }
}
