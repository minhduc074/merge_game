package com.bepngon.merge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.ui.theme.Cocoa
import com.bepngon.merge.ui.theme.MergeGreen

@Composable
fun DailyRewardDialog(
    ready: Boolean,
    msUntilReady: Long,
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
                    color = androidx.compose.ui.graphics.Color.White,
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
                Text("🪙 +${GameEngine.DAILY_COINS}    ⚡ +${GameEngine.DAILY_ENERGY}", fontSize = 22.sp)
                Text(
                    if (ready) {
                        "Quà của bạn hôm nay đây rồi!"
                    } else {
                        val h = msUntilReady / 3_600_000
                        val m = (msUntilReady % 3_600_000) / 60_000
                        "Quà tiếp theo sau ${h}h ${m}m"
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
