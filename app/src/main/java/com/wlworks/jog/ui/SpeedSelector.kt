package com.wlworks.jog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wlworks.jog.core.SpeedTier

/** 每列幾顆。6 個檔次剛好排成 2x3。 */
private const val PER_ROW = 3

/** 單一檔次藥丸。用 weight 撐成等寬，文字置中限一行。 */
@Composable
private fun SpeedPill(
    tier: SpeedTier,
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Text(
        text = stringResource(tier.labelRes),
        fontSize = 11.sp,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        color = if (active) Color.Black else Color.White.copy(alpha = 0.75f),
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) accent else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    )
}

/**
 * 速度檔次選擇器。
 *
 * 六個檔次擠不進 268dp 面板的單一橫列 —— 扣掉內距只剩 244dp，平均每顆不到 38dp，
 * 光標籤本身就放不下，所以改排成兩列、每顆等寬。慢到快依然是左到右、上到下。
 */
@Composable
fun SpeedSelector(
    selected: SpeedTier,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFF4DD0E1),
    onSelect: (SpeedTier) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SpeedTier.entries.chunked(PER_ROW).forEach { rowTiers ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowTiers.forEach { tier ->
                    SpeedPill(
                        tier = tier,
                        active = tier == selected,
                        accent = accent,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(tier) }
                    )
                }
                // 檔次數不是 PER_ROW 的倍數時，補空位撐住寬度，
                // 免得最後一列的藥丸被拉寬成跟上一列對不齊。
                repeat(PER_ROW - rowTiers.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
