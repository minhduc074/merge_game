package com.bepngon.merge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepngon.merge.game.Achievement
import com.bepngon.merge.game.Achievements
import com.bepngon.merge.game.GameState
import com.bepngon.merge.ui.theme.CellWhite
import com.bepngon.merge.ui.theme.Cocoa
import com.bepngon.merge.ui.theme.CreamBg
import com.bepngon.merge.ui.theme.Gold
import com.bepngon.merge.ui.theme.MergeGreen

@Composable
fun AchievementsScreen(
    game: GameState,
    onClaim: (String) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(CreamBg),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "🏆 Thành tựu",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Cocoa,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "✕",
                    fontSize = 22.sp,
                    color = Cocoa,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFFFE0B2))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            val done = Achievements.ALL.count { it.id in game.claimedAchievements }
            Text(
                "Đã nhận $done/${Achievements.ALL.size}",
                fontSize = 13.sp,
                color = Cocoa,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(Achievements.CHAINS) { chain ->
                    val current = chain.firstOrNull { it.id !in game.claimedAchievements }
                        ?: chain.last()
                    val tier = chain.indexOf(current) + 1
                    AchievementRow(
                        a = current,
                        tierLabel = if (chain.size > 1) "Bậc $tier/${chain.size}" else null,
                        progress = game.metricOf(current.metric),
                        reached = current.id in game.reachedAchievements,
                        claimed = current.id in game.claimedAchievements,
                        onClaim = { onClaim(current.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(
    a: Achievement,
    tierLabel: String?,
    progress: Int,
    reached: Boolean,
    claimed: Boolean,
    onClaim: () -> Unit,
) {
    val borderColor = when {
        claimed -> Color(0xFFE8D5B7)
        reached -> MergeGreen
        else -> Color(0xFFE8D5B7)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (claimed) 0.55f else 1f)
            .border(2.dp, borderColor, RoundedCornerShape(14.dp))
            .background(CellWhite, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .clip(CircleShape)
                .background(Color(0xFFF3E5F5))
                .padding(8.dp),
        ) {
            Text(a.emoji, fontSize = 24.sp)
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Cocoa)
                if (tierLabel != null) {
                    Text(
                        tierLabel,
                        fontSize = 9.sp,
                        color = Color.White,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Gold)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
            Text(a.desc, fontSize = 11.sp, color = Cocoa.copy(alpha = 0.7f))
            val clamped = minOf(progress, a.goal)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                LinearProgressIndicator(
                    progress = { clamped / a.goal.toFloat() },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (reached) MergeGreen else Gold,
                    trackColor = Color(0xFFFFF8E1),
                )
                Text("$clamped/${a.goal}", fontSize = 10.sp, color = Cocoa)
            }
        }
        when {
            claimed -> Text("✅", fontSize = 18.sp)
            reached -> Text(
                buildString {
                    append("Nhận\n")
                    if (a.rewardCoins > 0) append("+${a.rewardCoins}🪙")
                    if (a.rewardEnergy > 0) append("+${a.rewardEnergy}⚡")
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MergeGreen)
                    .clickable(onClick = onClaim)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
            else -> Text(
                buildString {
                    if (a.rewardCoins > 0) append("+${a.rewardCoins}🪙")
                    if (a.rewardEnergy > 0) append("+${a.rewardEnergy}⚡")
                },
                fontSize = 11.sp,
                color = Cocoa.copy(alpha = 0.5f),
            )
        }
    }
}
