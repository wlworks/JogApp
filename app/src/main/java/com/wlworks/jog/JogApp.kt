package com.wlworks.jog

import android.app.Application
import com.wlworks.jog.mock.MockLocationEngine

class JogApp : Application() {

    /** 清掉前一個行程遺留的 test provider，避免裝置定位一直卡在假座標。 */
    override fun onCreate() {
        super.onCreate()
        // 同步做：Application.onCreate 一定先於任何 Activity / Service 跑，
        // 不會和 Service 啟動模擬的註冊撞在一起。
        MockLocationEngine(this).clearStaleProviders()
    }
}
