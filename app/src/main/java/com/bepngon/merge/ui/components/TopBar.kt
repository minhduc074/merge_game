package com.bepngon.merge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepngon.merge.ui.theme.CellWhite
import com.bepngon.merge.ui.theme.Cocoa
import com.bepngon.merge.ui.theme.Gold
import com.bepngon.merge.ui.theme.MergeGreen
import com.bepngon.merge.ui.theme.WarnRed

@Composable
fun TopBar(
    score: Int,
    coins: Int,
    energy: Int,
    energyCap: Int,
    level: Int,
    xp: Int,
    xpNeeded: Int,
    dailyReady: Boolean,
    onRestart: () -> Unit,
    onAchievements: () -> Unit,
    onDaily: () -> Unit,
    onGuide: () -> Unit,
    onRoom: () -> Unit,
    onCoinBounds: (Rect) -> Unit,
    onStatTap: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatChip("icon_star", "⭐", "$score", Modifier.weight(1f)) { onStatTap("score") }
            StatChip(
                "icon_coin", "🪙", "$coins",
                Modifier
                    .weight(1f)
                    .onGloballyPositioned { onCoinBounds(it.boundsInWindow()) },
            ) { onStatTap("coins") }
            Row(
                Modifier
                    .weight(1.4f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CellWhite)
                    .clickable { onStatTap("energy") }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GameArt("icon_energy", "⚡", 22.dp, emojiSize = 15.sp)
                Column {
                    Text(
                        "$energy/$energyCap",
                        color = Cocoa,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    LinearProgressIndicator(
                        progress = { energy / energyCap.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = Gold,
                        trackColor = Color(0xFFFFECB3),
                    )
                }
            }
            IconButton(onClick = onRestart) {
                Icon(Icons.Default.Refresh, contentDescription = "Chơi lại", tint = Cocoa)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Lv.$level",
                color = Cocoa,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE3F2D9))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            LinearProgressIndicator(
                progress = { xp / xpNeeded.toFloat().coerceAtLeast(1f) },
                modifier = Modifier.weight(1f),
                color = MergeGreen,
                trackColor = Color(0xFFDCEDC8),
            )
            Text("$xp/$xpNeeded", fontSize = 10.sp, color = Cocoa)
            Box(
                Modifier.clickable(onClick = onDaily).padding(horizontal = 4.dp),
            ) {
                GameArt("icon_gift", "🎁", 28.dp, emojiSize = 20.sp)
                if (dailyReady) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(8.dp)
                            .background(WarnRed, CircleShape),
                    )
                }
            }
            GameArt(
                "icon_trophy",
                "🏆",
                28.dp,
                emojiSize = 20.sp,
                modifier = Modifier.clickable(onClick = onAchievements).padding(horizontal = 4.dp),
            )
            GameArt(
                "icon_question",
                "❓",
                26.dp,
                emojiSize = 18.sp,
                modifier = Modifier.clickable(onClick = onGuide).padding(horizontal = 4.dp),
            )
            Text(
                "🏠",
                fontSize = 20.sp,
                modifier = Modifier.clickable(onClick = onRoom).padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun StatChip(
    resName: String,
    emoji: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CellWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameArt(resName, emoji, 22.dp, emojiSize = 15.sp)
        Text(value, color = Cocoa, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
