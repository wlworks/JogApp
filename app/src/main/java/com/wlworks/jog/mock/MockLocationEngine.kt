package com.wlworks.jog.mock

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.wlworks.jog.core.LatLng

/**
 * 把假座標塞進系統的實際執行者。
 *
 * 兩條注入路徑同時打，才能同時騙到兩類消費者：
 *  1. LocationManager test provider  → 走 android.location API 的 App（含大多數系統元件）
 *  2. FusedLocationProviderClient.setMockMode → 走 Play Services 的 App（Google Maps 等）
 *
 * 前提：本 App 必須在「開發者選項 > 選取模擬位置資訊應用程式」被選中，
 * 否則 addTestProvider 會丟 SecurityException。
 */
class MockLocationEngine(private val context: Context) {

    private companion object {

        /** Logcat 標籤。 */
        const val TAG = "MockLocationEngine"
    }

    sealed interface StartResult {
        data class Failed(val throwable: Throwable) : StartResult
        /** 沒被選為 mock location app，引導使用者去開發者選項。 */
        data object NotMockApp : StartResult
        data object Ok : StartResult
    }

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val providers: List<String> = buildList {
        add(LocationManager.GPS_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
    }

    private var started = false

    /** 組出一筆可被系統接受的 Location。 */
    private fun buildLocation(
        provider: String,
        point: LatLng,
        bearingDeg: Float,
        speedMps: Float,
        altitude: Double
    ): Location = Location(provider).apply {
        latitude = point.lat
        longitude = point.lng
        this.altitude = altitude
        accuracy = 3f
        bearing = bearingDeg
        speed = speedMps
        time = System.currentTimeMillis()
        // 少了這行，Android 8+ 會直接丟掉這筆 location
        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            verticalAccuracyMeters = 3f
            bearingAccuracyDegrees = 2f
            speedAccuracyMetersPerSecond = 0.5f
        }
    }

    /**
     * 清掉前一個行程遺留的 test provider。
     *
     * 行程被 force-stop 或從最近工作列滑掉時收不到 onDestroy，[stop] 沒機會跑，
     * test provider 會留在系統裡繼續餵最後一筆假座標 —— 裝置定位就一直卡著，
     * 直到使用者下次啟動模擬（[registerProvider] 會先移除再重註冊）為止。
     * 在行程啟動時先清一次，把這段空窗補起來。
     *
     * 已在模擬中就不動作，避免誤清掉正在用的 provider。
     */
    fun clearStaleProviders() {
        if (started) return
        safeStop()
    }

    /**
     * 推一筆座標。必須被高頻率重複呼叫（見 [MovementController]），
     * 否則消費端會判定位置過期而退回真實定位。
     */
    fun push(point: LatLng, bearingDeg: Float, speedMps: Float, altitude: Double = 20.0) {
        if (!started) return
        providers.forEach { provider ->
            val location = buildLocation(provider, point, bearingDeg, speedMps, altitude)
            runCatching { locationManager.setTestProviderLocation(provider, location) }
                .onFailure { Log.w(TAG, "setTestProviderLocation($provider) failed", it) }
        }
        val fusedLocation = buildLocation(
            LocationManager.GPS_PROVIDER, point, bearingDeg, speedMps, altitude
        )
        runCatching { fusedClient.setMockLocation(fusedLocation) }
            .onFailure { Log.w(TAG, "fused setMockLocation failed", it) }
    }

    /** 註冊單一 test provider 並啟用，已存在就先移除避免屬性殘留。 */
    private fun registerProvider(provider: String) {
        runCatching { locationManager.removeTestProvider(provider) }

        // ProviderProperties 與對應的 addTestProvider overload 是 API 31 才有
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            locationManager.addTestProvider(
                provider,
                /* requiresNetwork = */ false,
                /* requiresSatellite = */ false,
                /* requiresCell = */ false,
                /* hasMonetaryCost = */ false,
                /* supportsAltitude = */ true,
                /* supportsSpeed = */ true,
                /* supportsBearing = */ true,
                ProviderProperties.POWER_USAGE_LOW,
                ProviderProperties.ACCURACY_FINE
            )
        } else {
            @Suppress("DEPRECATION")
            locationManager.addTestProvider(
                provider, false, false, false, false, true, true, true,
                /* powerRequirement = */ 1, /* accuracy = */ 1
            )
        }
        locationManager.setTestProviderEnabled(provider, true)
    }

    /** 拆掉所有注入路徑，不看 [started]、不丟例外，失敗路徑也能安全呼叫。 */
    private fun safeStop() {
        runCatching { fusedClient.setMockMode(false) }
        providers.forEach { provider ->
            runCatching { locationManager.setTestProviderEnabled(provider, false) }
            runCatching { locationManager.removeTestProvider(provider) }
        }
    }

    /** 開啟兩條注入路徑。沒被選為 mock location app 會回 [StartResult.NotMockApp]。 */
    fun start(): StartResult {
        if (started) return StartResult.Ok
        return try {
            providers.forEach { registerProvider(it) }
            runCatching { fusedClient.setMockMode(true) }
                .onFailure { Log.w(TAG, "fused setMockMode failed", it) }
            started = true
            StartResult.Ok
        } catch (e: SecurityException) {
            Log.w(TAG, "not selected as mock location app", e)
            safeStop()
            StartResult.NotMockApp
        } catch (e: Exception) {
            Log.e(TAG, "start failed", e)
            safeStop()
            StartResult.Failed(e)
        }
    }

    /** 關掉注入並把裝置定位還給真實 provider。 */
    fun stop() {
        if (!started) return
        safeStop()
        started = false
    }
}
