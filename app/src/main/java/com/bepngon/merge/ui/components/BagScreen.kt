package com.bepngon.merge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepngon.merge.game.BoardItem
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.ItemCatalog
import com.bepngon.merge.ui.theme.CellWhite
import com.bepngon.merge.ui.theme.Cocoa
import com.bepngon.merge.ui.theme.CreamBg
import com.bepngon.merge.ui.theme.Gold
import com.bepngon.merge.ui.theme.MergeGreen
import com.bepngon.merge.ui.theme.WarnRed

@Composable
fun BagScreen(
    inventory: List<BoardItem?>,
    cap: Int,
    coins: Int,
    onBuySlot: () -> Unit,
    onRetrieve: (Int) -> Unit,
    onClose: () -> Unit,
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    var bagMsg by remember { mutableStateOf<String?>(null) }
    val slotCost = GameEngine.inventorySlotCost(cap)

    androidx.compose.runtime.LaunchedEffect(bagMsg) {
        if (bagMsg != null) {
            kotlinx.coroutines.delay(2000)
            bagMsg = null
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(CreamBg)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "🎒 Kho đồ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Cocoa,
                modifier = Modifier.weight(1f),
            )
            Text("🪙 $coins", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Cocoa)
            Text(
                "✕",
                fontSize = 20.sp,
                color = Cocoa,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose)
                    .padding(6.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        // 2 rows x 4 slots
        for (row in 0 until 2) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for (col in 0 until 4) {
                    val slot = row * 4 + col
                    val item = inventory.getOrNull(slot)
                    val locked = slot >= cap
                    val isSelected = selected == slot
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when {
                                    isSelected -> Color(0xFFFFF9C4)
                                    locked -> Color(0xFFE0D5C8)
                                    else -> CellWhite
                                },
                            )
                            .border(
                                2.dp,
                                when {
                                    isSelected -> Gold
                                    locked -> Color(0xFFBCAAA4)
                                    else -> Color(0x33FF5D4037)
                                },
                                RoundedCornerShape(14.dp),
                            )
                            .clickable {
                                when {
                                    locked && slot == cap && slotCost != null -> {
                                        if (coins >= slotCost) {
                                            onBuySlot()
                                        } else {
                                            bagMsg = "Thiếu vàng! Cần $slotCost🪙"
                                        }
                                    }
                                    item != null -> selected = if (isSelected) null else slot
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            locked && slot == cap && slotCost != null -> Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("🔒", fontSize = 22.sp)
                                Text(
                                    "$slotCost🪙",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (coins >= slotCost) MergeGreen else WarnRed,
                                )
                            }
                            locked -> Text("🔒", fontSize = 22.sp)
                            item != null -> GameArt(
                                item.defId,
                                ItemCatalog[item.defId].emoji,
                                56.dp,
                                emojiSize = 34.sp,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Selected item footer
        val selItem = selected?.let { inventory.getOrNull(it) }
        if (selItem != null && selected != null) {
            val def = ItemCatalog[selItem.defId]
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CellWhite)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameArt(selItem.defId, def.emoji, 44.dp, emojiSize = 30.sp)
                Column(Modifier.padding(start = 8.dp).weight(1f)) {
                    Text(def.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Cocoa)
                    Text(
                        "Tier ${def.tier}",
                        fontSize = 11.sp,
                        color = Cocoa.copy(alpha = 0.7f),
                    )
                }
                BagButton("📤 Lấy ra", MergeGreen) {
                    onRetrieve(selected!!)
                    selected = null
                }
            }
        } else {
            Text(
                bagMsg
                    ?: "Chạm vào item để lấy ra.\nÔ 🔒 có thể mở bằng vàng!",
                fontSize = 12.sp,
                color = if (bagMsg != null) WarnRed else Cocoa.copy(alpha = 0.6f),
                fontWeight = if (bagMsg != null) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BagButton(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text,
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
