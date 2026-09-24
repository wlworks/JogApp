package com.wlworks.jog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
import com.wlworks.jog.core.SpeedTier
import com.wlworks.jog.state.MockUiState
import java.util.Locale

/** 主色，用在啟用狀態的控制項上。 */
private val Accent = Color(0xFF4DD0E1)

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
    val onSearch: () -> Unit,
    val onSpeed: (SpeedTier) -> Unit,
    val onStick: (Float, Float) -> Unit,
    val onToggleRun: () -> Unit
)

/**
 * 收合後的精簡面板：上方一顆狀態圓球（拖曳把手，點一下展開），下方保留搖桿。
 * 封測回饋：收合後還是要能微調位置，不然每次推桿都得先展開、再收回去。
 */
@Composable
fun CollapsedPanel(
    running: Boolean,
    dragHandle: Modifier,
    onExpand: () -> Unit,
    onStick: (Float, Float) -> Unit
) {
    // 寬度要寫死：overlay 視窗是 WRAP_CONTENT，裡面的 fillMaxWidth 會撐到整個螢幕寬
    Column(
        modifier = Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val expandDescription = stringResource(R.string.cd_expand)
        Box(
            modifier = dragHandle
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onExpand)
                .semantics { contentDescription = expandDescription },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (running) Accent else Color.White.copy(alpha = 0.35f))
            )
        }
        Joystick(
            accent = Accent,
            enabled = running,
            size = 96.dp,
            onInput = onStick
        )
    }
}

/** 懸浮面板本體：標題列、座標輸入、速度檔次、搖桿與開始／停止。 */
@Composable
fun FloatingPanel(
    state: MockUiState,
    actions: PanelActions,
    dragHandle: Modifier
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

    Column(
        modifier = Modifier
            .width(268.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ---- 標題列（同時是拖曳把手） ----
        Row(
            modifier = dragHandle.fillMaxWidth(),
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
                modifier = Modifier.weight(1f)
            )
            IconText("—", stringResource(R.string.cd_collapse), actions.onCollapse)
            IconText("✕", stringResource(R.string.cd_close), actions.onClose)
        }

        // ---- 地點／座標輸入 ----
        QueryField(
            value = state.query,
            onValueChange = actions.onQueryChange,
            onSearch = actions.onSearch,
            onFocusChanged = { focused ->
                queryFocused = focused
                actions.onInputFocusChanged(focused)
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillButton(
                text = stringResource(
                    if (state.searching) R.string.panel_searching else R.string.panel_teleport
                ),
                enabled = !state.searching,
                modifier = Modifier.weight(1f),
                onClick = actions.onSearch
            )
            if (state.searching) {
                CircularProgressIndicator(
                    color = Accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ---- 解析結果（不寫回輸入框，只顯示）----
        state.resolvedLabel?.let {
            Text(
                text = "→ $it",
                color = Accent.copy(alpha = 0.85f),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ---- 目前座標 ----
        Text(
            text = state.current?.let {
                // 刻意用 Locale.US：逗號小數點的語系會和座標分隔符撞在一起
                String.format(Locale.US, "%.6f, %.6f · %.0f°", it.lat, it.lng, state.bearing)
            } ?: stringResource(R.string.panel_no_location),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp
        )

        // ---- 速度檔次 ----
        SpeedSelector(
            selected = state.speedTier,
            accent = Accent,
            onSelect = {
                dismissKeyboard()
                actions.onSpeed(it)
            }
        )

        // ---- 搖桿 ----
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Joystick(
                accent = Accent,
                // 沒按「開始」時推桿不會改變座標，索性置灰並停接觸控
                enabled = state.running,
                onInput = { x, y ->
                    dismissKeyboard()   // queryFocused 為 false 時是 no-op
                    actions.onStick(x, y)
                }
            )
        }

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

        state.message?.let {
            Text(it, color = Color(0xFFFFB4A9), fontSize = 11.sp)
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

/** 圓角長按鈕，filled 為 true 時用主色實心。 */
@Composable
private fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = if (filled) Color.Black else Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (filled) Accent else Color.White.copy(alpha = 0.10f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        textAlign = TextAlign.Center
    )
}

/**
 * 地點／座標輸入框。只在焦點狀態**真的改變**時才回報，避免重複觸發 updateViewLayout。
 */
@Composable
private fun QueryField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFocusChanged: (Boolean) -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        // 座標串（例：35.039563227558034, 135.72926455412235）有38個字，
        // 在 268dp 寬的面板裡單行放不下，允許折到第二行。
        // imeAction 已明確指定為 Search，多行下依然會走 onSearch，不會變成換行鍵。
        maxLines = 2,
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
        placeholder = { Text(stringResource(R.string.panel_query_hint), fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Accent,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            cursorColor = Accent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .onFocusChanged { focusState ->
                if (focusState.isFocused != focused) {
                    focused = focusState.isFocused
                    onFocusChanged(focused)
                }
            }
    )
}
