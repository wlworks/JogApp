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
    /** 自動移動：搖桿沒推時由 AutoRoam 接手方向。只在 running 時有意義，停止模擬會一併關掉。 */
    val autoMove: Boolean = false,
    val bearing: Float = 0f,
    val current: LatLng? = null,
    /**
     * 遞增這個值就等於要求 UI 清除輸入框焦點。
     * 用 tick 而不是 boolean，是為了讓連續兩次「收鍵盤」都能觸發 LaunchedEffect。
     */
    val focusReleaseTick: Int = 0,
    /** 這次面板開著的期間已成功寫進 Health Connect 的距離（公尺），只給面板顯示。 */
    val healthDistanceM: Double = 0.0,
    /** 這次面板開著的期間已成功寫進 Health Connect 的步數，只給面板顯示。 */
    val healthSteps: Long = 0L,
    /**
     * 移動時是否定時把步數／距離寫進 Health Connect。
     * 開著的期間速度檔鎖在步行 —— 步數是用步行步幅換算的，其他檔次寫進去的數字不合理。
     */
    val healthSync: Boolean = false,
    val message: String? = null,
    val needsMockAppSetup: Boolean = false,
    val query: String = "",
    /** 最近一次成功解析到的地址，只給面板顯示；null 表示還沒搜過或是從上次座標還原。 */
    val resolvedLabel: String? = null,
    /**
     * 廣告關卡是否已過。廣告關閉的 build 裡不會有人讀它。
     * 以「面板開著的這一次」為單位，Service 結束時清掉。
     */
    val rewardUnlocked: Boolean = false,
    /** 解鎖畫面或廣告正在前景（由 GateScreenTracker 維護）。這段期間懸浮面板要讓開，不可以蓋在廣告上。 */
    val rewardUnlocking: Boolean = false,
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

    /** 切換速度檔次。Health 同步開著時檔次鎖在步行，這裡直接不理會（面板上其他檔次也已置灰）。 */
    fun setSpeed(tier: SpeedTier) = update { if (it.healthSync) it else it.copy(speedTier = tier) }

    /** 寫入搖桿向量，由 MovementController 在每個 tick 讀取。 */
    fun setStick(x: Float, y: Float) = update { it.copy(stick = StickInput(x, y)) }

    /** 以 CAS 方式套用一次狀態轉換。 */
    fun update(block: (MockUiState) -> MockUiState) = _state.update(block)
}
