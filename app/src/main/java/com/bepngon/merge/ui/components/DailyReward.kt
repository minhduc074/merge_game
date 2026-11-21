package com.bepngon.merge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.ui.theme.Cocoa
import com.bepngon.merge.ui.theme.Gold
import com.bepngon.merge.ui.theme.MergeGreen

@Composable
fun DailyRewardDialog(
    ready: Boolean,
    msUntilReady: Long,
    nextDay: Int,
    onClaim: () -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            if (ready) {
                Text(
                    "Nhận quà! 🎉",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MergeGreen)
                        .clickable(onClick = onClaim)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Đóng", color = Cocoa) }
        },
        title = {
            Text(
                "🎁 Quà hằng ngày",
                fontWeight = FontWeight.ExtraBold,
                color = Cocoa,
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    GameEngine.DAILY_REWARDS.forEachIndexed { i, (coins, _) ->
                        val claimed = i < nextDay
                        val isNext = i == nextDay
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .alpha(if (claimed || isNext) 1f else 0.5f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isNext && ready -> Gold
                                        claimed || isNext -> Color(0xFFDCEDC8)
                                        else -> Color(0xFFF5F0E8)
                                    },
                                )
                                .padding(horizontal = 5.dp, vertical = 3.dp),
                        ) {
                            Text(
                                "N${i + 1}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Cocoa,
                            )
                            Text(
                                if (i == GameEngine.DAILY_REWARDS.lastIndex) {
                                    "${coins}🪙🪄"
                                } else {
                                    "${coins}🪙"
                                },
                                fontSize = 8.sp,
                                color = Cocoa,
                            )
                        }
                    }
                }
                val (coins, energy) = GameEngine.DAILY_REWARDS[nextDay]
                Text(
                    "🪙 +$coins    ⚡ +$energy" +
                        if (nextDay == GameEngine.DAILY_REWARDS.lastIndex) "   🪄 Đũa phép" else "",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    if (ready) {
                        "🔥 Chuỗi ngày ${nextDay + 1}/7 — quà của bạn đây rồi!"
                    } else {
                        val h = msUntilReady / 3_600_000
                        val m = (msUntilReady % 3_600_000) / 60_000
                        "Quà tiếp theo sau ${h}h ${m}m — giữ streak nhé!"
                    },
                    fontSize = 13.sp,
                    color = Cocoa,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        },
    )
}
