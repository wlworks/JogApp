package com.wlworks.jog.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/** 把位移限制在半徑內，超出就沿同方向縮回圓周上。 */
private fun clampToCircle(offset: Offset, maxRadius: Float): Offset {
    val length = hypot(offset.x, offset.y)
    if (length <= maxRadius || length == 0f) return offset
    val scale = maxRadius / length
    return Offset(offset.x * scale, offset.y * scale)
}

/**
 * 類比搖桿。回報的是正規化向量（-1..1），未按壓時回 (0,0)。
 * 手指離開 → 自動歸零（不做慣性，避免使用者「放手還在飄」）。
 *
 * [enabled] 為 false 時整個搖桿置灰且不接觸控。沒在模擬時
 * MovementController 的迴圈不存在，推桿本來就不會讓座標改變。
 */
@Composable
fun Joystick(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 132.dp,
    accent: Color = Color(0xFF4DD0E1),
    onInput: (x: Float, y: Float) -> Unit
) {
    var knob by remember { mutableStateOf(Offset.Zero) }
    val latestOnInput by rememberUpdatedState(onInput)

    // 轉為停用時把旋鈕歸位並回報零向量。假如是在拖曳中途被停用，
    // onDragEnd 不會觸發，殘留的非零 stick 會在下次 start() 的第一個 tick
    // 被吃掉 —— 等於一按開始就往上次的方向衝。
    LaunchedEffect(enabled) {
        if (!enabled) {
            knob = Offset.Zero
            latestOnInput(0f, 0f)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .then(if (!enabled) Modifier else Modifier.pointerInput(Unit) {
                val radius = this.size.width / 2f
                val maxTravel = radius * 0.72f

                fun emit(offset: Offset) {
                    val clamped = clampToCircle(offset, maxTravel)
                    knob = clamped
                    onInput(clamped.x / maxTravel, clamped.y / maxTravel)
                }

                detectDragGestures(
                    onDragStart = { start ->
                        emit(Offset(start.x - radius, start.y - radius))
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        emit(knob + delta)
                    },
                    onDragEnd = {
                        knob = Offset.Zero
                        onInput(0f, 0f)
                    },
                    onDragCancel = {
                        knob = Offset.Zero
                        onInput(0f, 0f)
                    }
                )
            })
    ) {
        Canvas(Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outer = this.size.minDimension / 2f

            drawCircle(
                Color.White.copy(alpha = if (enabled) 0.06f else 0.03f),
                radius = outer,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = if (enabled) 0.22f else 0.10f),
                radius = outer,
                center = center,
                style = Stroke(width = 2f)
            )
            // 十字參考線
            val crossAlpha = if (enabled) 0.10f else 0.05f
            drawLine(
                Color.White.copy(alpha = crossAlpha),
                Offset(center.x, center.y - outer), Offset(center.x, center.y + outer)
            )
            drawLine(
                Color.White.copy(alpha = crossAlpha),
                Offset(center.x - outer, center.y), Offset(center.x + outer, center.y)
            )
            drawCircle(
                color = if (enabled) accent else Color.White.copy(alpha = 0.20f),
                radius = outer * 0.3f,
                center = center + knob
            )
        }
    }
}
