package com.bepngon.merge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepngon.merge.game.ItemCatalog
import com.bepngon.merge.game.Order
import androidx.compose.ui.text.font.FontWeight
import com.bepngon.merge.ui.theme.CellWhite
import com.bepngon.merge.ui.theme.Cocoa
import com.bepngon.merge.ui.theme.Gold
import com.bepngon.merge.ui.theme.MergeGreen

@Composable
fun OrderBar(
    orders: List<Order>,
    dragDefId: String?,
    availableDefIds: List<String>,
    canFulfill: (Order) -> Boolean,
    onFulfill: (Long) -> Unit,
    onItemTap: (String) -> Unit,
    onPositioned: (Long, Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (orders.isEmpty()) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cardWidth = (maxWidth - 24.dp - 16.dp) / 3
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            orders.forEach { order ->
                OrderCard(
                    order = order,
                    accepts = dragDefId != null && dragDefId in order.remaining,
                    availableDefIds = availableDefIds,
                    fulfillable = canFulfill(order),
                    onFulfill = { onFulfill(order.id) },
                    onItemTap = onItemTap,
                    modifier = Modifier
                        .width(cardWidth)
                        .onGloballyPositioned { onPositioned(order.id, it.boundsInWindow()) },
                )
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    accepts: Boolean,
    availableDefIds: List<String>,
    fulfillable: Boolean,
    onFulfill: () -> Unit,
    onItemTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = when {
        fulfillable -> MergeGreen
        accepts -> Gold
        else -> Color(0xFFE8D5B7)
    }
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                .background(CellWhite, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .then(if (fulfillable) Modifier.clickable(onClick = onFulfill) else Modifier)
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .background(Color(0xFFF3E5F5), CircleShape)
                    .padding(4.dp),
            ) {
                GameArt(
                    "cust_${order.customer.id}",
                    order.customer.emoji,
                    34.dp,
                    emojiSize = 20.sp,
                )
            }
            Column(Modifier.padding(start = 6.dp)) {
                Text(order.customer.name, fontSize = 10.sp, color = Cocoa, maxLines = 1)
                val coins = order.required.sumOf { ItemCatalog[it].sellPrice } * 3 / 2
                Text("+$coins🪙", fontSize = 9.sp, color = Color(0xFFE65100))
            }
        }
        Row(
            Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val fulfilledLeft = order.fulfilled.toMutableList()
            val availLeft = availableDefIds.toMutableList()
            order.required.forEach { req ->
                val done = fulfilledLeft.remove(req)
                val have = !done && availLeft.remove(req)
                Box {
                    GameArt(
                        req,
                        ItemCatalog[req].emoji,
                        28.dp,
                        alpha = if (done) 0.3f else 1f,
                        emojiSize = 16.sp,
                        modifier = Modifier
                            .then(
                                if (have) Modifier.border(1.5.dp, MergeGreen, CircleShape)
                                else Modifier,
                            )
                            .clip(CircleShape)
                            .clickable { onItemTap(req) }
                            .padding(2.dp),
                    )
                    if (done) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(12.dp)
                                .background(MergeGreen, CircleShape)
                                .padding(1.5.dp),
                        )
                    }
                }
            }
        }
        }
        if (fulfillable) {
            Text(
                "✓",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(MergeGreen, CircleShape)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
    }
}
