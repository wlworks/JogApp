package com.wlworks.jog.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wlworks.jog.core.SpeedTier

/**
 * 選中那一顆佔的寬度是其他顆的幾倍。要放得下「圖示 + 文字」：
 * 右欄約 226dp，扣掉間距後一份約 28dp，選中的約 67dp，英文最長的 "Creep" 也放得下。
 */
private const val SELECTED_WEIGHT = 2.4f

/** 檔次對應的圖示。蝸行沒有現成的「很慢」圖示，用拄拐杖的人代表慢慢走。 */
private fun iconFor(tier: SpeedTier): ImageVector = when (tier) {
    SpeedTier.CREEP -> Icons.Filled.Elderly
    SpeedTier.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
    SpeedTier.RUN -> Icons.AutoMirrored.Filled.DirectionsRun
    SpeedTier.BIKE -> Icons.AutoMirrored.Filled.DirectionsBike
    SpeedTier.DRIVE -> Icons.Filled.DirectionsCar
    SpeedTier.RAIL -> Icons.Filled.Train
}

/**
 * 單一檔次藥丸。沒選中時只放圖示；選中時展開成「圖示 + 文字」，一眼就知道目前是哪一檔。
 * enabled 為 false 時置灰且不接觸控。TalkBack 一律唸完整的檔次名稱。
 */
@Composable
private fun SpeedPill(
    tier: SpeedTier,
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val label = stringResource(tier.labelRes)
    val tint = when {
        active -> Color.Black
        enabled -> Color.White.copy(alpha = 0.8f)
        else -> Color.White.copy(alpha = 0.22f)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                when {
                    active -> accent
                    enabled -> Color.White.copy(alpha = 0.08f)
                    else -> Color.White.copy(alpha = 0.03f)
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                contentDescription = label
                role = Role.Tab
                selected = active
            }
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 文字已經由 semantics 唸過，圖示不必再唸一次
        Icon(
            imageVector = iconFor(tier),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        if (active) {
            Spacer(Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = tint,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * 速度檔次選擇器：一列六顆，慢到快由左而右。
 *
 * 放在面板分頁列的外面，兩頁都看得到 —— 隨機跳的距離、搖桿與自動移動的速度都跟著檔次走，
 * 不該為了換檔還要切頁。一列放六顆文字太擠，所以平常只放圖示，選中的那顆才展開帶文字。
 *
 * [isEnabled] 回傳 false 的檔次置灰且不接觸控（Health 同步中只開放蝸行／步行／慢跑）。
 * 目前選中的那一檔一律可按，不會出現「選著卻是灰的」。
 */
@Composable
fun SpeedSelector(
    selected: SpeedTier,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFF4DD0E1),
    isEnabled: (SpeedTier) -> Boolean = { true },
    onSelect: (SpeedTier) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SpeedTier.entries.forEach { tier ->
            val active = tier == selected
            // 選中的那顆變寬時用動畫過去，看起來像一顆膠囊在列上移動
            val weight by animateFloatAsState(
                targetValue = if (active) SELECTED_WEIGHT else 1f,
                label = "speedPillWeight"
            )
            SpeedPill(
                tier = tier,
                active = active,
                accent = accent,
                modifier = Modifier.weight(weight),
                enabled = active || isEnabled(tier),
                onClick = { onSelect(tier) }
            )
        }
    }
}
