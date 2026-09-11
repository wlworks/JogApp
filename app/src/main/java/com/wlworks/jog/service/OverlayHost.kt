package com.wlworks.jog.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * 把 Compose 掛到 WindowManager overlay 上的宿主。
 *
 * ComposeView 需要 ViewTree 上有 Lifecycle / ViewModelStore / SavedStateRegistry，
 * Service 裡沒有 Activity 提供，所以自己做一個最小實作 —— 少了任一個
 * ComposeView 會在 attach 時直接崩潰，這是這條路上最常見的坑。
 */
class OverlayHost(
    private val context: Context
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private companion object {

        /** 平常：不吃焦點，讓底下 App 正常操作。 */
        const val BASE_FLAGS =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

        /** 打字時：可取得焦點並叫出 IME。 */
        const val BASE_FLAGS_FOCUSABLE =
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    }

    private var composeView: ComposeView? = null

    val layoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        overlayType(),
        BASE_FLAGS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 24
        y = 240
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val lifecycleRegistry = LifecycleRegistry(this)

    private val savedStateController = SavedStateRegistryController.create(this).apply {
        performRestore(null)
    }

    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private val store = ViewModelStore()

    override val viewModelStore: ViewModelStore get() = store

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /** 一個 OverlayHost 實例 dismiss 後不可再 show()，LifecycleRegistry 不允許從 DESTROYED 回頭。 */
    fun dismiss() {
        composeView?.let { view -> runCatching { windowManager.removeView(view) } }
        composeView = null
        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
        store.clear()
    }

    /** 拖曳時呼叫，直接改 WindowManager 座標。 */
    fun moveBy(dx: Float, dy: Float) {
        val view = composeView ?: return
        layoutParams.x += dx.toInt()
        layoutParams.y += dy.toInt()
        windowManager.updateViewLayout(view, layoutParams)
    }

    /** 依 API 等級挑可用的 overlay 視窗類型。 */
    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    /**
     * 輸入框要打字時，必須把 FLAG_NOT_FOCUSABLE 拿掉，鍵盤才會出現；
     * 打完要加回去，否則 overlay 會吃掉返回鍵、讓底下 App 無法操作。
     */
    fun setInputFocusable(focusable: Boolean) {
        val view = composeView ?: return
        // 這個方法會從 Compose 的 onDispose 呼叫進來，view 可能已經脫離 WindowManager
        if (!view.isAttachedToWindow) return
        val target = if (focusable) BASE_FLAGS_FOCUSABLE else BASE_FLAGS
        // 旗標沒變就不要 updateViewLayout —— 多餘的更新會讓視窗重新取得焦點，
        // 有機會把 Compose 那側剛設好的焦點狀態打掉。
        if (layoutParams.flags == target) return
        layoutParams.flags = target
        layoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        runCatching { windowManager.updateViewLayout(view, layoutParams) }
    }

    /** 建立 ComposeView、補齊 ViewTree owner 並掛上 WindowManager。 */
    fun show(content: @Composable () -> Unit) {
        if (composeView != null) return
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@OverlayHost)
            setViewTreeViewModelStoreOwner(this@OverlayHost)
            setViewTreeSavedStateRegistryOwner(this@OverlayHost)
            setContent(content)
        }
        composeView = view
        windowManager.addView(view, layoutParams)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }
}

/** 判斷是否已取得懸浮視窗權限。 */
fun Context.canDrawOverlay(): Boolean =
    android.provider.Settings.canDrawOverlays(this)
