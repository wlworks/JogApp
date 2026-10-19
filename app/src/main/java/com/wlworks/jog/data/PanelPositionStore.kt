package com.wlworks.jog.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point

/**
 * 記住懸浮面板上次被拖到的位置（WindowManager 座標，px），下次開面板出現在同一個地方。
 *
 * 只是 UI 偏好，不是個人資料；但仍屬持久化資料，隱私權政策的「資料儲存」一節有提到，
 * 改動這裡要一併更新。存的是面板左上角，展開與收合共用；換了螢幕方向或尺寸後
 * 可能跑出畫面，由 OverlayHost 在顯示時拉回螢幕內，這裡不管。
 */
class PanelPositionStore(context: Context) {

    private companion object {

        /** 左上角 x（px）。 */
        const val KEY_X = "x"

        /** 左上角 y（px）。 */
        const val KEY_Y = "y"

        /** SharedPreferences 檔名。和 LastLocationStore 分開，清掉其中一個不影響另一個。 */
        const val PREFS = "jog_panel_position"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 讀出上次儲存的位置，沒有就回 null（用 OverlayHost 的預設位置）。 */
    fun load(): Point? {
        if (!prefs.contains(KEY_X) || !prefs.contains(KEY_Y)) return null
        return Point(prefs.getInt(KEY_X, 0), prefs.getInt(KEY_Y, 0))
    }

    /** 非同步寫入；只在拖曳結束時呼叫，拖曳途中不寫。 */
    fun save(position: Point) {
        prefs.edit()
            .putInt(KEY_X, position.x)
            .putInt(KEY_Y, position.y)
            .apply()
    }
}
