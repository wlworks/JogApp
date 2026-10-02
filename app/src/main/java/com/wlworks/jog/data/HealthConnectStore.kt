package com.wlworks.jog.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Length
import com.wlworks.jog.core.HealthTally
import java.time.Instant
import java.time.ZoneId

/**
 * Health Connect 的寫入端。Jog **只寫不讀**：只申請步數與距離的寫入權限，
 * 寫進去的是模擬移動換算出來的測試資料，用來驗證會讀 Health Connect 的 App。
 *
 * 資料只進到裝置上的 Health Connect，不經過 Jog 的任何伺服器。隱私權政策的
 * 「Health Connect」一節有對應描述，改動這裡（特別是多申請資料類型）要一併更新，
 * Play Console 的健康資料申報也要跟著改。
 */
class HealthConnectStore(context: Context) {

    companion object {

        /** 要向使用者申請的權限。只有寫入，沒有任何讀取。 */
        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getWritePermission(DistanceRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class)
        )
    }

    private val appContext = context.applicationContext

    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(appContext) }

    /** [PERMISSIONS] 是否全數取得。Health Connect 不可用或查詢失敗都當成沒有。 */
    suspend fun hasPermissions(): Boolean {
        if (!isAvailable()) return false
        return runCatching {
            client.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
        }.getOrDefault(false)
    }

    /** 這台裝置能不能用 Health Connect。Android 14+ 內建；13 以下要另外裝，沒裝或版本太舊都回 false。 */
    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(appContext) == HealthConnectClient.SDK_AVAILABLE

    /**
     * 把 [batch] 寫成涵蓋 [start]..[end] 的步數與距離紀錄，成功回 true。
     * 來源標成 manual entry —— 這些數字不是感測器量到的，不該冒充成自動記錄。
     */
    suspend fun write(batch: HealthTally.Batch, start: Instant, end: Instant): Boolean {
        if (batch.isEmpty || !end.isAfter(start)) return true
        val offset = ZoneId.systemDefault().rules.getOffset(end)
        val records = buildList<Record> {
            if (batch.steps > 0) {
                add(
                    StepsRecord(
                        startTime = start,
                        startZoneOffset = offset,
                        endTime = end,
                        endZoneOffset = offset,
                        count = batch.steps,
                        metadata = Metadata.manualEntry()
                    )
                )
            }
            if (batch.distanceM > 0.0) {
                add(
                    DistanceRecord(
                        startTime = start,
                        startZoneOffset = offset,
                        endTime = end,
                        endZoneOffset = offset,
                        distance = Length.meters(batch.distanceM),
                        metadata = Metadata.manualEntry()
                    )
                )
            }
        }
        return runCatching { client.insertRecords(records) }.isSuccess
    }
}
