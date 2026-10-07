package com.wlworks.jog.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.wlworks.jog.MainActivity
import com.wlworks.jog.state.MockStateHolder

/**
 * 追蹤「解鎖畫面或廣告是否在前景」，結果寫進 rewardUnlocking，Service 據此把懸浮面板藏起來。
 *
 * 算的是本行程裡 MainActivity 以外、處於 started 狀態的 Activity 數量 —— 也就是 [UnlockActivity]
 * 和廣告 SDK 自己的全螢幕 Activity。用數的而不是讓 UnlockActivity 自己設旗標，原因有兩個：
 *  - 廣告播放時 UnlockActivity 是 stopped 的，蓋在上面的是 SDK 的 Activity，旗標交給它管會提早放掉；
 *  - 使用者在解鎖畫面或廣告中按 Home，Activity 只會 stop 不會 destroy，旗標綁在 onDestroy 的話
 *    面板就再也不會回來（實測遇過）。
 *
 * 只在廣告開啟的 build 註冊，見 JogApp。
 */
class GateScreenTracker : Application.ActivityLifecycleCallbacks {

    private var startedCount = 0

    /** 不關心。 */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    /** 不關心。 */
    override fun onActivityDestroyed(activity: Activity) = Unit

    /** 不關心。 */
    override fun onActivityPaused(activity: Activity) = Unit

    /** 不關心。 */
    override fun onActivityResumed(activity: Activity) = Unit

    /** 不關心。 */
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    /** 解鎖畫面或廣告進到前景：面板讓開。 */
    override fun onActivityStarted(activity: Activity) {
        if (activity is MainActivity) return
        startedCount++
        publish()
    }

    /** 離開前景（看完、取消、或按了 Home）：都沒有了就把面板放回來。 */
    override fun onActivityStopped(activity: Activity) {
        if (activity is MainActivity) return
        startedCount = (startedCount - 1).coerceAtLeast(0)
        publish()
    }

    /** 把目前的結果寫進共用狀態。 */
    private fun publish() {
        val unlocking = startedCount > 0
        MockStateHolder.update { it.copy(rewardUnlocking = unlocking) }
    }
}
