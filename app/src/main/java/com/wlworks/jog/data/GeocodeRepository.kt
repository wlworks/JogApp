package com.wlworks.jog.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.wlworks.jog.R
import com.wlworks.jog.core.CoordinateParser
import com.wlworks.jog.core.LatLng
import com.wlworks.jog.core.runCatchingCancellable
import java.util.Locale
import kotlin.coroutines.resume

data class GeocodeHit(
    /**
     * 給人看的解析結果。**只用來顯示，不要拿去再查一次**：Geocoder 的 label 不保證能
     * 查回同一點 —— 「淺草寺」解析正確，但把它的 label 再丟回去查會掉到台東區中心（上野）。
     */
    val label: String,
    val point: LatLng
)

/**
 * 地點解析。刻意**不依賴任何外部 HTTP 服務**：
 *  1. 先當座標解析（離線可用，永遠有效）
 *  2. 再交給系統 Geocoder
 *
 * 不做遠端備援是為了上架考量 —— 少一個 API key、可以完全不要 INTERNET 權限，
 * Data Safety 也不必申報資料傳輸。代價是少數沒有 geocoding backend 的機型
 * 查不到地名，那些情況下使用者仍可直接輸入經緯度。
 */
class GeocodeRepository(context: Context) {

    private companion object {

        /** 單次查詢最多取幾筆結果。 */
        const val MAX_RESULTS = 5

        /** Logcat 標籤。 */
        const val TAG = "GeocodeRepository"

        /** 查詢逾時。部分機型即使 isPresent() 為 true 也可能長時間沒回應。 */
        const val TIMEOUT_MS = 4_000L
    }

    sealed interface Outcome {
        data class Found(val hits: List<GeocodeHit>) : Outcome
        /** 查得動但沒結果。 */
        data object NoMatch : Outcome
        /** 這台裝置沒有 geocoding backend、backend 回報錯誤（多半是沒網路），或逾時無回應。 */
        data object Unavailable : Outcome
    }

    private val appContext = context.applicationContext

    /** 這台裝置是否有地名搜尋能力。false 時 UI 應引導使用者改輸入座標。 */
    val canSearchByName: Boolean get() = geocoder != null

    private val geocoder: Geocoder? =
        if (Geocoder.isPresent()) Geocoder(appContext, Locale.getDefault()) else null

    /**
     * 座標轉顯示字串。
     *
     * 固定用 Locale.US：法德等語系的預設格式會把小數點印成逗號，
     * 與座標的逗號分隔符撞在一起就完全讀不出來了。
     */
    private fun fmt(p: LatLng) = String.format(Locale.US, "%.5f, %.5f", p.lat, p.lng)

    /** 先試座標解析，不成再走系統 Geocoder。 */
    suspend fun resolve(query: String): Outcome {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return Outcome.NoMatch

        CoordinateParser.parse(trimmed)?.let {
            val label = appContext.getString(R.string.label_coordinates, fmt(it))
            return Outcome.Found(listOf(GeocodeHit(label = label, point = it)))
        }

        if (geocoder == null) return Outcome.Unavailable

        val hits = withTimeoutOrNull(TIMEOUT_MS) { systemGeocode(trimmed) }
            ?: return Outcome.Unavailable

        return if (hits.isEmpty()) Outcome.NoMatch else Outcome.Found(hits)
    }

    /**
     * 呼叫系統 Geocoder，依 API 等級走 callback 或阻塞版本。
     * 回 null 代表 backend 出錯或拋例外（沒網路時 Play 服務會回 UNAVAILABLE），
     * 和「查了但沒結果」的空清單分開 —— 混在一起使用者會看到「找不到」而一直換字重試。
     */
    private suspend fun systemGeocode(name: String): List<GeocodeHit>? {
        val gc = geocoder ?: return null
        return runCatchingCancellable {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine<List<GeocodeHit>?> { cont ->
                    gc.getFromLocationName(name, MAX_RESULTS, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (cont.isActive) cont.resume(addresses.map(::toHit))
                        }

                        override fun onError(errorMessage: String?) {
                            Log.w(TAG, "geocode error: $errorMessage")
                            if (cont.isActive) cont.resume(null)
                        }
                    })
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    gc.getFromLocationName(name, MAX_RESULTS).orEmpty().map(::toHit)
                }
            }
        }.getOrElse {
            Log.w(TAG, "system geocode failed", it)
            null
        }
    }

    /**
     * 把系統 Address 攤平成一行可讀的地名。優先用 Geocoder 排好版的 addressLine；
     * 自己用 featureName 拼的話，POI 的 featureName 常常只是門牌號
     * （淺草寺回「1」→ 拼出「1, Taito City, 日本」），完全看不出是哪裡。
     */
    private fun toHit(address: Address) = GeocodeHit(
        label = address.getAddressLine(0)?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(
                address.featureName,
                address.locality ?: address.subAdminArea,
                address.countryName
            ).distinct().joinToString(", "),
        point = LatLng(address.latitude, address.longitude)
    )
}
