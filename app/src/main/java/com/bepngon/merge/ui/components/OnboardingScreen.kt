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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepngon.merge.ui.theme.CellWhite
import com.bepngon.merge.ui.theme.Cocoa
import com.bepngon.merge.ui.theme.Gold
import com.bepngon.merge.ui.theme.MergeGreen
import com.bepngon.merge.game.ItemCatalog

private data class Step(
    val emoji: String,
    val title: String,
    val desc: String,
    val genId: String? = null,
)

private val STEPS = listOf(
    Step(
        "🧺", "Bấm vào giỏ",
        "Giỏ đồ sẽ cho ra nguyên liệu mới.\nMỗi lần bấm tốn 1⚡ năng lượng.\nMón thường ra nhiều, món ngon hiếm hơn —\nbấm giữ giỏ để xem chi tiết.",
        genId = "gen_fruit_1",
    ),
    Step("🍒", "Merge 2 item giống nhau", "Kéo item chồng lên item giống nó\nđể tạo ra món cao cấp hơn!"),
    Step("🐰", "Giao đơn cho khách", "Khách đòi món gì thì kéo món đó lên thẻ,\nhoặc bấm nút ✓ khi đủ đồ. Kiếm 🪙 + ⭐!"),
    Step("🎒", "Kho đồ & túi mở rộng", "Kéo item xuống 🎒 để cất giữ.\nDùng vàng mua thêm ô kho trong trang Kho đồ."),
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val s = STEPS[step]
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xAA000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(32.dp)
                .border(3.dp, Gold, RoundedCornerShape(24.dp))
                .background(CellWhite, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🍳 Bếp Ngon Merge", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Cocoa)
            Text(s.emoji, fontSize = 64.sp, modifier = Modifier.padding(vertical = 16.dp))
            Text(s.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Cocoa)
            Text(
                s.desc,
                fontSize = 13.sp,
                color = Cocoa.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            s.genId?.let { SpawnRateRow(it) }
            Row(
                Modifier.padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                STEPS.forEachIndexed { i, _ ->
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (i == step) MergeGreen else Color(0xFFE0E0E0)),
                    )
                }
            }
            Text(
                if (step == STEPS.lastIndex) "Bắt đầu chơi! 🎉" else "Tiếp ➜",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MergeGreen)
                    .clickable {
                        if (step == STEPS.lastIndex) onDone() else step++
                    }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            )
            if (step < STEPS.lastIndex) {
                Text(
                    "Bỏ qua",
                    fontSize = 12.sp,
                    color = Cocoa.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .clickable(onClick = onDone),
                )
            }
        }
    }
}

/** Live spawn-rate row for [genId] — reads the catalog so it never goes
 *  stale when spawn tables are retuned. */
@Composable
private fun SpawnRateRow(genId: String) {
    val gen = ItemCatalog[genId]
    val total = gen.spawnTable.sumOf { it.weight }
    Row(
        Modifier.padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        gen.spawnTable.forEach { entry ->
            val spawnDef = ItemCatalog[entry.itemId]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GameArt(spawnDef.id, spawnDef.emoji, 30.dp)
                Text(
                    "${if (spawnDef.isRare) "✨" else ""}${entry.weight * 100 / total}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (spawnDef.isRare) Gold else Cocoa.copy(alpha = 0.6f),
                )
            }
        }
    }
}
