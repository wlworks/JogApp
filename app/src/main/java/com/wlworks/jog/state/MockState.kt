package com.wlworks.jog.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.wlworks.jog.core.LatLng
import com.wlworks.jog.core.SpeedTier

/** 搖桿輸入：單位圓上的向量，長度 0..1。 */
data class StickInput(val x: Float = 0f, val y: Float = 0f) {

    /** 推桿幅度小到可視為沒推。 */
    val isIdle: Boolean get() = magnitude < 0.05f

    /** 推桿幅度，上限 1。 */
    val magnitude: Float get() = kotlin.math.min(1f, kotlin.math.hypot(x, y))
}

data class MockUiState(
    val bearing: Float = 0f,
    val current: LatLng? = null,
    /**
     * 遞增這個值就等於要求 UI 清除輸入框焦點。
     * 用 tick 而不是 boolean，是為了讓連續兩次「收鍵盤」都能觸發 LaunchedEffect。
     */
    val focusReleaseTick: Int = 0,
    val message: String? = null,
    val needsMockAppSetup: Boolean = false,
    val query: String = "",
    val running: Boolean = false,
    val searching: Boolean = false,
    val speedTier: SpeedTier = SpeedTier.DEFAULT,
    val stick: StickInput = StickInput()
)

/**
 * 程序內唯一的狀態來源。Service 與 overlay UI 共用同一份，
 * 不經過 Activity，所以懸浮視窗可以在 App 不在前景時獨立運作。
 */
object MockStateHolder {

    private val _state = MutableStateFlow(MockUiState())

    val state: StateFlow<MockUiState> = _state.asStateFlow()

    /** 設定面板底部的提示訊息，傳 null 清除。 */
    fun message(text: String?) = update { it.copy(message = text) }

    /** 要求輸入框放開焦點（進而讓視窗恢復不可聚焦、鍵盤收起）。 */
    fun releaseInputFocus() = update { it.copy(focusReleaseTick = it.focusReleaseTick + 1) }

    /** 更新輸入框內容。 */
    fun setQuery(q: String) = update { it.copy(query = q) }

    /** 切換速度檔次。 */
    fun setSpeed(tier: SpeedTier) = update { it.copy(speedTier = tier) }

    /** 寫入搖桿向量，由 MovementController 在每個 tick 讀取。 */
    fun setStick(x: Float, y: Float) = update { it.copy(stick = StickInput(x, y)) }

    /** 以 CAS 方式套用一次狀態轉換。 */
    fun update(block: (MockUiState) -> MockUiState) = _state.update(block)
}
