package com.wlworks.jog.data

import android.content.Context
import android.content.SharedPreferences
import com.wlworks.jog.core.Favorite
import com.wlworks.jog.core.LatLng
import org.json.JSONArray
import org.json.JSONObject

/**
 * 最愛地點的持久化。和 [LastLocationStore] 一樣只存使用者自己設的模擬座標與名稱，
 * 放在 App 私有的 SharedPreferences，不離開裝置；隱私權政策的「資料儲存」一節有對應描述。
 *
 * 整份清單存成一個 JSON 字串（最多 20 筆，沒必要上資料庫）。經緯度用 raw long bits，
 * 理由同 [LastLocationStore]；JSON 的數字是 double，long 會失真，所以 bits 存成字串。
 * 讀到壞掉的資料就當作空清單，不讓 App 因此開不起來。
 */
class FavoritesStore(context: Context) {

    private companion object {

        /** 緯度 raw long bits 欄位（十進位字串）。 */
        const val FIELD_LAT = "lat"

        /** 經度 raw long bits 欄位（十進位字串）。 */
        const val FIELD_LNG = "lng"

        /** 名稱欄位。 */
        const val FIELD_NAME = "name"

        /** 整份清單的 JSON 陣列。 */
        const val KEY_LIST = "favorites"

        /** SharedPreferences 檔名。 */
        const val PREFS = "jog_favorites"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 讀出整份清單，沒有或解析失敗就回空清單。 */
    fun load(): List<Favorite> {
        val raw = prefs.getString(KEY_LIST, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Favorite(
                    name = o.getString(FIELD_NAME),
                    point = LatLng(
                        lat = Double.fromBits(o.getString(FIELD_LAT).toLong()),
                        lng = Double.fromBits(o.getString(FIELD_LNG).toLong())
                    )
                )
            }
        }.getOrDefault(emptyList())
    }

    /** 非同步覆寫整份清單。 */
    fun save(list: List<Favorite>) {
        val array = JSONArray()
        list.forEach {
            array.put(
                JSONObject()
                    .put(FIELD_LAT, it.point.lat.toRawBits().toString())
                    .put(FIELD_LNG, it.point.lng.toRawBits().toString())
                    .put(FIELD_NAME, it.name)
            )
        }
        prefs.edit().putString(KEY_LIST, array.toString()).apply()
    }
}
