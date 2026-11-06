package com.wlworks.jog.core

/** 一筆最愛地點。[name] 是存下來當時的地名或座標字串，只給人看。 */
data class Favorite(
    val name: String,
    val point: LatLng
)

/**
 * 最愛清單的純邏輯：新增、移除、找「目前位置是哪一筆」。清單一律新的在前。
 *
 * 不碰儲存也不碰 UI，所以能用單元測試蓋住；持久化在 data/FavoritesStore。
 */
object Favorites {

    /** 清單上限。面板的最愛分頁一次只看得到四、五筆，太多反而找不到。 */
    const val MAX = 20

    /**
     * 兩點相距多少公尺內算「同一個地方」。決定星號亮不亮、以及新增時算不算重複。
     * 30 m 大約是一個路口：定位到同一個地標兩次，Geocoder 給的點常差個十幾公尺。
     */
    const val SAME_PLACE_M = 30.0

    sealed interface AddResult {
        /** 加進去了，[list] 是新的清單。 */
        data class Added(val list: List<Favorite>) : AddResult
        /** 附近已經有一筆，沒有加。 */
        data object Duplicate : AddResult
        /** 已達 [MAX] 筆，沒有加。 */
        data object Full : AddResult
    }

    /** 把 [favorite] 加到最前面。重複優先於已滿：已經存過的地方就不必叫人去刪別筆。 */
    fun add(list: List<Favorite>, favorite: Favorite): AddResult = when {
        indexNear(list, favorite.point) != null -> AddResult.Duplicate
        list.size >= MAX -> AddResult.Full
        else -> AddResult.Added(listOf(favorite) + list)
    }

    /** [point] 在 [SAME_PLACE_M] 內最近的那一筆的索引；沒有就回 null。 */
    fun indexNear(list: List<Favorite>, point: LatLng): Int? =
        list.indices
            .map { it to GeoMath.distance(list[it].point, point) }
            .filter { it.second <= SAME_PLACE_M }
            .minByOrNull { it.second }
            ?.first

    /** 移除第 [index] 筆；索引超出範圍就原樣回傳（UI 和狀態之間可能差一個 frame）。 */
    fun removeAt(list: List<Favorite>, index: Int): List<Favorite> =
        if (index in list.indices) list.filterIndexed { i, _ -> i != index } else list
}
