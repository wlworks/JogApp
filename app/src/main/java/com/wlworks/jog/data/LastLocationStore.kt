package com.wlworks.jog.data

import android.content.Context
import android.content.SharedPreferences
import com.wlworks.jog.core.LatLng

/**
 * 記住上一次的模擬座標，下次開 App 直接接續。
 *
 * 只存**使用者輸入／推桿推到的模擬座標**，不碰裝置真實位置；存在 App 私有的
 * SharedPreferences，不離開裝置。這是本 App 唯一的持久化資料，隱私權政策的
 * 「資料儲存」一節有對應描述，改動這裡要一併更新。
 *
 * 經緯度用 raw long bits 存而不是 Float：Float 在緯度 35° 附近只有約 1 m 的解析度，
 * 每次重開都會漂一點。
 */
class LastLocationStore(context: Context) {

    private companion object {

        /** 緯度的 raw long bits。 */
        const val KEY_LAT = "last_lat"

        /** 經度的 raw long bits。 */
        const val KEY_LNG = "last_lng"

        /** SharedPreferences 檔名。 */
        const val PREFS = "jog_last_location"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 讀出上次儲存的座標，沒有就回 null。 */
    fun load(): LatLng? {
        if (!prefs.contains(KEY_LAT) || !prefs.contains(KEY_LNG)) return null
        return LatLng(
            lat = Double.fromBits(prefs.getLong(KEY_LAT, 0L)),
            lng = Double.fromBits(prefs.getLong(KEY_LNG, 0L))
        )
    }

    /** 非同步寫入；呼叫端負責節流，別每個 tick 都呼叫。 */
    fun save(point: LatLng) {
        prefs.edit()
            .putLong(KEY_LAT, point.lat.toRawBits())
            .putLong(KEY_LNG, point.lng.toRawBits())
            .apply()
    }
}
