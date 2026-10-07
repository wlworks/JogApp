package com.wlworks.jog.ads

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wlworks.jog.R
import com.wlworks.jog.service.FloatingWindowService

/**
 * 廣告關卡的畫面：先問使用者要不要看廣告，同意才載入並播放獎勵廣告。
 * 看完才用 [FloatingWindowService.ACTION_UNLOCKED] 通知 Service 解鎖；
 * 沒看完或廣告載不到都不解鎖，留在這個畫面讓使用者重試或放棄。
 *
 * 需要一個 Activity 是因為廣告 SDK 只能掛在 Activity 上，懸浮視窗的 Service 播不了。
 * 這個畫面或廣告在前景的期間，懸浮面板會讓開（見 [GateScreenTracker]）。
 *
 * 使用者中途按 Home 離開時，除非廣告正在播，否則直接結束自己 —— 這個 task 不在最近工作列裡，
 * 留著也回不來；廣告在背景載完還會自己跳出來。廣告播到一半離開的話 task 會留著，
 * 下次從面板再按一次會用 CLEAR_TASK 重新開始。
 *
 * 廣告關閉的 build 不會有人啟動它。
 */
class UnlockActivity : ComponentActivity() {

    companion object {

        /** intent extra：使用者原本想開的功能（[GatedFeature] 的 name）。 */
        private const val EXTRA_FEATURE = "feature"

        /** 組出啟動本畫面的 intent。從 Service 啟動，所以要帶 NEW_TASK。 */
        fun intent(context: Context, feature: GatedFeature): Intent =
            Intent(context, UnlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(EXTRA_FEATURE, feature.name)
    }

    /** 畫面目前在哪一步。 */
    private enum class Stage { LOADING, NOT_FINISHED, PROMPT, SHOWING, UNAVAILABLE }

    private var stage by mutableStateOf(Stage.PROMPT)

    /** 顯示事前說明；按下同意才開始載入廣告。 */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                AlertDialog(
                    onDismissRequest = { finish() },
                    title = { Text(stringResource(R.string.unlock_title)) },
                    text = {
                        when (stage) {
                            Stage.LOADING, Stage.SHOWING -> Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(stringResource(R.string.unlock_loading))
                            }
                            Stage.NOT_FINISHED -> Text(stringResource(R.string.unlock_not_finished))
                            Stage.PROMPT -> Text(stringResource(R.string.unlock_body))
                            Stage.UNAVAILABLE -> Text(stringResource(R.string.unlock_unavailable))
                        }
                    },
                    confirmButton = {
                        if (stage != Stage.LOADING && stage != Stage.SHOWING) {
                            TextButton(onClick = ::watch) {
                                Text(
                                    stringResource(
                                        if (stage == Stage.PROMPT) R.string.unlock_watch
                                        else R.string.unlock_retry
                                    )
                                )
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { finish() }) {
                            Text(stringResource(R.string.disclosure_cancel))
                        }
                    }
                )
            }
        }
    }

    /** 廣告結束。看完才通知 Service 解鎖並關掉自己；其餘留在畫面上說明原因，讓使用者重試。 */
    private fun onOutcome(outcome: RewardOutcome) {
        if (isFinishing || isDestroyed) return
        when (outcome) {
            RewardOutcome.EARNED -> {
                // Service 還活著（面板就是它開的），startService 只會走到 onStartCommand
                startService(
                    Intent(this, FloatingWindowService::class.java)
                        .setAction(FloatingWindowService.ACTION_UNLOCKED)
                        .putExtra(
                            FloatingWindowService.EXTRA_FEATURE,
                            intent.getStringExtra(EXTRA_FEATURE)
                        )
                )
                finish()
            }
            RewardOutcome.DISMISSED -> stage = Stage.NOT_FINISHED
            RewardOutcome.UNAVAILABLE -> stage = Stage.UNAVAILABLE
        }
    }

    /** 離開前景。廣告正在播的話是被它蓋住，屬正常；其餘情況是使用者走掉了，直接結束。 */
    override fun onStop() {
        super.onStop()
        if (stage != Stage.SHOWING && !isChangingConfigurations) finish()
    }

    /** 使用者同意看廣告（或重試）：切到載入中並交給 [RewardedGate]。 */
    private fun watch() {
        stage = Stage.LOADING
        RewardedGate.show(
            activity = this,
            onShowing = { stage = Stage.SHOWING },
            onResult = ::onOutcome
        )
    }
}
