package com.bepngon.merge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepngon.merge.ui.theme.CellWhite
import com.bepngon.merge.ui.theme.Cocoa
import com.bepngon.merge.ui.theme.Gold

@Composable
fun TopBar(
    score: Int,
    coins: Int,
    energy: Int,
    energyCap: Int,
    onRestart: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatChip("⭐", "$score", Modifier.weight(1f))
        StatChip("🪙", "$coins", Modifier.weight(1f))
        Row(
            Modifier
                .weight(1.4f)
                .clip(RoundedCornerShape(12.dp))
                .background(CellWhite)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("⚡", fontSize = 15.sp)
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
}

@Composable
private fun StatChip(emoji: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CellWhite)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 15.sp)
        Text(value, color = Cocoa, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
