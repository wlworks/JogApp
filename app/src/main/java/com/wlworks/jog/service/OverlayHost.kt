package com.wlworks.jog.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
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
 *
 * 視窗平常是 FLAG_NOT_FOCUSABLE，只有輸入框打字時才可聚焦。可聚焦期間兩件事會呼叫
 * [show] 傳進來的 onDismissInput：
 *  - 返回鍵。targetSdk 36 起 KEYCODE_BACK 不再派送給視窗，要改走
 *    OnBackInvokedDispatcher（API 33+）；API 32 以下仍走 KeyEvent，兩條路都接。
 *    沒接的話鍵盤收起後返回鍵會被這個視窗吃掉，底下的 App 永遠收不到。
 *  - 點到面板外面（FLAG_WATCH_OUTSIDE_TOUCH 的 ACTION_OUTSIDE）。
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

        /**
         * 打字時：可取得焦點並叫出 IME。
         * 一定要帶 FLAG_NOT_TOUCH_MODAL：可聚焦的視窗少了它就是 touch modal，會把面板外的觸控
         * 全部吃掉（底下 App 點不到），而且 FLAG_WATCH_OUTSIDE_TOUCH 也不會送 ACTION_OUTSIDE。
         * FLAG_NOT_FOCUSABLE 本身隱含 NOT_TOUCH_MODAL，所以 BASE_FLAGS 不必另外加。
         */
        const val BASE_FLAGS_FOCUSABLE =
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    }

    private val backCallback: OnBackInvokedCallback? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OnBackInvokedCallback { dismissInput() }
        } else null

    private var backRegistered = false

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

    private var onDismissInput: () -> Unit = {}

    private var rootView: FrameLayout? = null

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
        updateBackCallback(register = false)
        rootView?.let { view -> runCatching { windowManager.removeView(view) } }
        rootView = null
        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
        store.clear()
    }

    /** 返回鍵／面板外點擊的共同出口：只在視窗可聚焦（打字中）時轉發，其餘情況忽略。 */
    private fun dismissInput() {
        if (layoutParams.flags == BASE_FLAGS_FOCUSABLE) onDismissInput()
    }

    /** 拖曳時呼叫，直接改 WindowManager 座標。 */
    fun moveBy(dx: Float, dy: Float) {
        val view = rootView ?: return
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
        val view = rootView ?: return
        // 這個方法會從 Compose 的 onDispose 呼叫進來，view 可能已經脫離 WindowManager
        if (!view.isAttachedToWindow) return
        val target = if (focusable) BASE_FLAGS_FOCUSABLE else BASE_FLAGS
        // 旗標沒變就不要 updateViewLayout —— 多餘的更新會讓視窗重新取得焦點，
        // 有機會把 Compose 那側剛設好的焦點狀態打掉。
        if (layoutParams.flags == target) return
        layoutParams.flags = target
        layoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        runCatching { windowManager.updateViewLayout(view, layoutParams) }
        updateBackCallback(register = focusable)
    }

    /**
     * 建立 ComposeView、補齊 ViewTree owner 並掛上 WindowManager。
     * [onDismissInput] 在打字中按返回鍵或點到面板外面時被呼叫，呼叫端應放掉輸入框焦點。
     */
    fun show(onDismissInput: () -> Unit, content: @Composable () -> Unit) {
        if (rootView != null) return
        this.onDismissInput = onDismissInput
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        // ComposeView 是 final，鍵盤與觸控事件的攔截包在外層 FrameLayout 上
        val root = object : FrameLayout(context) {

            /** API 32 以下返回鍵仍以 KeyEvent 派送；33+ 走 OnBackInvokedDispatcher，這裡收不到。 */
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.action == KeyEvent.ACTION_UP) dismissInput()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }

            /** FLAG_WATCH_OUTSIDE_TOUCH：點到面板外面就放掉輸入框焦點、收鍵盤。 */
            override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                    dismissInput()
                    return true
                }
                return super.dispatchTouchEvent(event)
            }
        }
        // ViewTree owner 要掛在視窗的 root view 上：Compose 的 WindowRecomposer 是從
        // rootView 往上找 LifecycleOwner，設在子層 ComposeView 會找不到而直接崩潰。
        root.setViewTreeLifecycleOwner(this)
        root.setViewTreeViewModelStoreOwner(this)
        root.setViewTreeSavedStateRegistryOwner(this)
        root.addView(ComposeView(context).apply { setContent(content) })
        rootView = root
        windowManager.addView(root, layoutParams)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    /** 視窗可聚焦期間才向 OnBackInvokedDispatcher 註冊返回鍵回呼，其餘時間拿掉，避免攔到不該攔的返回。 */
    private fun updateBackCallback(register: Boolean) {
        val callback = backCallback ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val dispatcher = rootView?.findOnBackInvokedDispatcher() ?: return
        if (register && !backRegistered) {
            dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback)
            backRegistered = true
        } else if (!register && backRegistered) {
            dispatcher.unregisterOnBackInvokedCallback(callback)
            backRegistered = false
        }
    }
}

/** 判斷是否已取得懸浮視窗權限。 */
fun Context.canDrawOverlay(): Boolean =
    android.provider.Settings.canDrawOverlays(this)
