package com.bepngon.merge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepngon.merge.ui.theme.CellWhite
import com.bepngon.merge.ui.theme.Cocoa
import com.bepngon.merge.ui.theme.WarnRed

@Composable
fun SellZone(active: Boolean, highlighted: Boolean, modifier: Modifier = Modifier) {
    val bg = when {
        highlighted -> WarnRed
        active -> Color(0xFFFFCDD2)
        else -> CellWhite
    }
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .border(
                width = 2.dp,
                color = if (highlighted) WarnRed else Color(0xFFE0BFB8),
                shape = RoundedCornerShape(14.dp),
            )
            .background(bg, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (highlighted) "Thả ra để bán! 🪙" else "🗑️ Kéo item vào đây để bán",
            color = if (highlighted) Color.White else Cocoa,
            fontSize = 14.sp,
        )
    }
}
