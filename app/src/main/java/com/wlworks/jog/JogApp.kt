package com.wlworks.jog

import android.app.Application
import com.wlworks.jog.data.LastLocationStore
import com.wlworks.jog.mock.MockLocationEngine
import com.wlworks.jog.state.MockStateHolder

class JogApp : Application() {

    /** 清掉前一個行程遺留的 test provider，並把上次的模擬座標放回狀態。 */
    override fun onCreate() {
        super.onCreate()
        // 同步做：Application.onCreate 一定先於任何 Activity / Service 跑，
        // 不會和 Service 啟動模擬的註冊撞在一起。
        MockLocationEngine(this).clearStaleProviders()
        // 只回填 current，不自動開始注入 —— 開 App 就靜默改掉裝置定位太突然。
        // 面板會顯示上次座標，使用者按「開始」才會回到那裡。
        LastLocationStore(this).load()?.let { last ->
            MockStateHolder.update { it.copy(current = last) }
        }
    }
}
