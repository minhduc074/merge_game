package com.bepngon.merge.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bepngon.merge.game.BoardItem
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.GameEvent
import com.bepngon.merge.game.ItemCatalog
import com.bepngon.merge.game.ItemSource
import com.bepngon.merge.game.LevelReward
import com.bepngon.merge.ui.components.BagScreen
import com.bepngon.merge.ui.components.OrderBar
import com.bepngon.merge.ui.components.SellZone
import com.bepngon.merge.ui.components.TopBar
import com.bepngon.merge.ui.theme.CellWhite
import com.bepngon.merge.ui.theme.Cocoa
import com.bepngon.merge.ui.theme.CreamBg
import com.bepngon.merge.ui.theme.Gold
import com.bepngon.merge.ui.theme.MergeGreen
import com.bepngon.merge.ui.theme.WarnRed
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun GameScreen(vm: GameViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Box(
        Modifier
            .fillMaxSize()
            .background(CreamBg)
            .statusBarsPadding(),
    ) {
        if (ui.loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            GameContent(vm, ui)
        }

        if (ui.showBag) {
            BagScreen(
                inventory = ui.game.inventory,
                cap = ui.game.inventoryCap,
                coins = ui.game.coins,
                onBuySlot = vm::buyInventorySlot,
                onRetrieve = vm::retrieveToBoard,
                onSell = vm::sellFromInventory,
                onClose = vm::closeBag,
            )
        }

        if (ui.showRestartConfirm) {
            AlertDialog(
                onDismissRequest = vm::dismissRestart,
                title = { Text("Chơi lại từ đầu?") },
                text = { Text("Toàn bộ tiến trình hiện tại sẽ bị xoá.") },
                confirmButton = {
                    TextButton(onClick = vm::restart) { Text("Chơi lại", color = WarnRed) }
                },
                dismissButton = {
                    TextButton(onClick = vm::dismissRestart) { Text("Huỷ") }
                },
            )
        }
    }
}

private data class DragState(
    val source: ItemSource,
    val index: Int,
    val uid: Long,
    val defId: String,
    val position: Offset,
)

private data class Floater(
    val id: Long,
    val text: String,
    val position: Offset,
    val color: Color,
)

@Composable
private fun GameContent(vm: GameViewModel, ui: GameUiState) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    val cellBounds = remember { mutableStateMapOf<Int, Rect>() }
    val invBounds = remember { mutableStateMapOf<Int, Rect>() }
    val orderBounds = remember { mutableStateMapOf<Long, Rect>() }
    var sellBounds by remember { mutableStateOf(Rect.Zero) }

    var drag by remember { mutableStateOf<DragState?>(null) }
    var hoverCell by remember { mutableStateOf<Int?>(null) }
    var hoverInv by remember { mutableStateOf<Int?>(null) }
    var sellHover by remember { mutableStateOf(false) }
    var floaters by remember { mutableStateOf(listOf<Floater>()) }
    var floaterId by remember { mutableStateOf(0L) }
    var levelBanner by remember { mutableStateOf<String?>(null) }
    var infoItem by remember { mutableStateOf<BoardItem?>(null) }

    fun pushFloater(text: String, pos: Offset, color: Color) {
        val f = Floater(floaterId++, text, pos, color)
        floaters = floaters + f
        scope.launch {
            kotlinx.coroutines.delay(900)
            floaters = floaters - f
        }
    }

    val onDragStart: (ItemSource, Int, Long, String, Offset) -> Unit =
        { source, index, uid, defId, pos ->
            drag = DragState(source, index, uid, defId, pos)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }

    val onDrag: (Offset) -> Unit = { pos ->
        drag?.let { d ->
            drag = d.copy(position = pos)
            val overOrder = ui.game.orders.any { o ->
                orderBounds[o.id]?.contains(pos) == true
            }
            sellHover = !overOrder && sellBounds.contains(pos)
            hoverInv = invBounds.entries.firstOrNull { it.value.contains(pos) }?.key
            hoverCell = if (sellHover || overOrder || hoverInv != null) {
                null
            } else {
                cellBounds.entries.firstOrNull { it.value.contains(pos) }?.key
            }
        }
    }

    val onDragEnd: () -> Unit = {
        val d = drag
        if (d != null) {
            val orderHit = ui.game.orders.firstOrNull { o ->
                orderBounds[o.id]?.contains(d.position) == true
            }?.id
            val invHit = invBounds.entries.firstOrNull { it.value.contains(d.position) }?.key
            val cellHit = cellBounds.entries.firstOrNull { it.value.contains(d.position) }?.key
            when {
                orderHit != null -> vm.deliverToOrder(orderHit, d.source, d.index)
                sellHover -> when (d.source) {
                    ItemSource.BOARD -> vm.sell(d.index)
                    ItemSource.INVENTORY -> vm.sellFromInventory(d.index)
                }
                invHit != null -> when (d.source) {
                    ItemSource.BOARD -> vm.boardToInventory(d.index, invHit)
                    ItemSource.INVENTORY -> vm.moveInInventory(d.index, invHit)
                }
                cellHit != null -> when (d.source) {
                    ItemSource.BOARD -> vm.moveOrMerge(d.index, cellHit)
                    ItemSource.INVENTORY -> vm.inventoryToBoard(d.index, cellHit)
                }
            }
        }
        drag = null
        hoverCell = null
        hoverInv = null
        sellHover = false
    }

    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                is GameEvent.Merged -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    cellBounds[e.cellIndex]?.let { r ->
                        pushFloater("+${e.scoreGained}", r.center, MergeGreen)
                    }
                }
                is GameEvent.Spawned -> {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                is GameEvent.Sold -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val pos = if (e.cellIndex >= 0) {
                        cellBounds[e.cellIndex]?.center
                    } else {
                        sellBounds.center
                    }
                    pos?.let { pushFloater("+${e.coinsGained}🪙", it, Gold) }
                }
                is GameEvent.OrderSpawned -> {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                is GameEvent.OrderDelivered -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                is GameEvent.OrderCompleted -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val pos = orderBounds[e.order.id]?.center
                        ?: Offset(300f, 200f)
                    pushFloater("🎉 +${e.coinsGained}🪙 +${e.xpGained}XP", pos, MergeGreen)
                }
                is GameEvent.LevelUp -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    levelBanner = "🎉 Lên cấp ${e.level}! ${rewardText(e.reward)}"
                    scope.launch {
                        kotlinx.coroutines.delay(2800)
                        levelBanner = null
                    }
                }
                is GameEvent.CellsUnlocked -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    levelBanner = "🔓 Mở khóa ${e.count} ô mới!"
                    scope.launch {
                        kotlinx.coroutines.delay(2500)
                        levelBanner = null
                    }
                }
                is GameEvent.GeneratorReady -> {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    cellBounds[e.cellIndex]?.let { r ->
                        pushFloater("⚡ xong!", r.center, MergeGreen)
                    }
                }
                is GameEvent.SlotBought -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    pushFloater("+1 ô kho! 🎒", Offset(300f, 1400f), MergeGreen)
                }
                GameEvent.NotEnoughCoins -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    pushFloater("Không đủ vàng! 🪙", Offset(300f, 1400f), WarnRed)
                }
                GameEvent.GeneratorEmpty,
                GameEvent.DeliverRejected,
                GameEvent.NoEnergy,
                GameEvent.BoardFull,
                GameEvent.InventoryFull,
                GameEvent.InvalidDrop -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            score = ui.game.score,
            coins = ui.game.coins,
            energy = ui.game.energy,
            energyCap = GameEngine.ENERGY_CAP,
            level = ui.game.level,
            xp = ui.game.xp,
            xpNeeded = GameEngine.xpNeededForLevel(ui.game.level),
            onRestart = vm::askRestart,
        )

        OrderBar(
            orders = ui.game.orders,
            dragDefId = drag?.defId,
            onPositioned = { id, r -> orderBounds[id] = r },
            modifier = Modifier.padding(bottom = 6.dp),
        )

        if (ui.game.boardFull) {
            Text(
                "Bàn đầy rồi! Merge hoặc bán bớt đồ nhé 😅",
                color = WarnRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                textAlign = TextAlign.Center,
            )
        }

        BoardGrid(
            cells = ui.game.cells,
            lockedCells = ui.game.lockedCells,
            nowMs = ui.nowMs,
            drag = drag,
            hoverCell = hoverCell,
            cellBounds = cellBounds,
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            onTapGenerator = vm::tapGenerator,
            onTapInfo = { infoItem = it },
            modifier = Modifier.weight(1f),
        )

        InventoryBar(
            inventory = ui.game.inventory,
            cap = ui.game.inventoryCap,
            coins = ui.game.coins,
            nowMs = ui.nowMs,
            drag = drag,
            hoverInv = hoverInv,
            invBounds = invBounds,
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            onTapInfo = { infoItem = it },
            onOpenBag = vm::openBag,
        )

        SellZone(
            active = drag != null,
            highlighted = sellHover,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .onGloballyPositioned { sellBounds = it.boundsInWindow() },
        )
    }

    // Floaters + banners (window-coordinate space, same as drag overlay)
    floaters.forEach { f ->
        key(f.id) { ScoreFloater(f) }
    }
    levelBanner?.let { text ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LevelBanner(text)
        }
    }
    infoItem?.let { item ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            ItemInfoCard(
                item = item,
                nowMs = ui.nowMs,
                onDismiss = { infoItem = null },
            )
        }
    }

    // Drag overlay
    drag?.let { d ->
        val emoji = ItemCatalog[d.defId].emoji
        val tileSize = 52.dp
        Box(
            Modifier
                .offset {
                    IntOffset(
                        (d.position.x - tileSize.toPx() / 2).roundToInt(),
                        (d.position.y - tileSize.toPx() / 2).roundToInt(),
                    )
                }
                .size(tileSize)
                .zIndex(10f)
                .shadow(8.dp, RoundedCornerShape(14.dp))
                .background(CellWhite, RoundedCornerShape(14.dp))
                .scale(1.1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 30.sp)
        }
    }
}

private fun rewardText(r: LevelReward): String = buildList {
    if (r.coins > 0) add("+${r.coins}🪙")
    r.generatorDefId?.let { add("+${ItemCatalog[it].emoji} ${ItemCatalog[it].name}") }
    if (r.inventoryCapBonus > 0) add("+${r.inventoryCapBonus} ô kho")
}.joinToString("  ")

@Composable
private fun BoardGrid(
    cells: List<BoardItem?>,
    lockedCells: Set<Int>,
    nowMs: Long,
    drag: DragState?,
    hoverCell: Int?,
    cellBounds: MutableMap<Int, Rect>,
    onDragStart: (ItemSource, Int, Long, String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onTapGenerator: (Int) -> Unit,
    onTapInfo: (BoardItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (row in 0 until GameEngine.ROWS) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (col in 0 until GameEngine.COLS) {
                    val cell = GameEngine.idx(row, col)
                    val item = cells[cell]
                    CellBox(
                        index = cell,
                        item = item,
                        locked = cell in lockedCells,
                        unlockLevel = GameEngine.unlockLevelForCell(cell),
                        nowMs = nowMs,
                        hidden = drag?.source == ItemSource.BOARD && drag.index == cell,
                        hover = hoverCell == cell,
                        canDrop = if (drag != null && item != null && hoverCell == cell) {
                            GameEngine.canMerge(
                                BoardItem(drag.uid, drag.defId),
                                item,
                            )
                        } else {
                            null
                        },
                        cellBounds = cellBounds,
                        onDragStart = onDragStart,
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onTapGenerator = onTapGenerator,
                        onTapInfo = onTapInfo,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CellBox(
    index: Int,
    item: BoardItem?,
    locked: Boolean,
    unlockLevel: Int?,
    nowMs: Long,
    hidden: Boolean,
    hover: Boolean,
    canDrop: Boolean?,
    cellBounds: MutableMap<Int, Rect>,
    onDragStart: (ItemSource, Int, Long, String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onTapGenerator: (Int) -> Unit,
    onTapInfo: (BoardItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val bg = when {
        locked -> Color(0xFFD7CCC8)
        canDrop == true -> Color(0xFFA5D6A7)
        canDrop == false -> Color(0xFFEF9A9A)
        hover -> Color(0xFFFFF9C4)
        else -> CellWhite
    }
    Box(
        modifier
            .aspectRatio(1f)
            .onGloballyPositioned {
                bounds = it.boundsInWindow()
                cellBounds[index] = bounds
            }
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, Color(0x1A5D4037), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (locked) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🕸️", fontSize = 18.sp)
                if (unlockLevel != null) {
                    Text("Lv$unlockLevel", fontSize = 8.sp, color = Cocoa.copy(alpha = 0.7f))
                }
            }
        } else if (item != null) {
            ItemTile(
                index = index,
                item = item,
                nowMs = nowMs,
                hidden = hidden,
                cellBoundsInWindow = bounds,
                source = ItemSource.BOARD,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onTapGenerator = onTapGenerator,
                onTapInfo = onTapInfo,
            )
        }
    }
}

@Composable
private fun InventoryBar(
    inventory: List<BoardItem?>,
    cap: Int,
    coins: Int,
    nowMs: Long,
    drag: DragState?,
    hoverInv: Int?,
    invBounds: MutableMap<Int, Rect>,
    onDragStart: (ItemSource, Int, Long, String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onTapInfo: (BoardItem) -> Unit,
    onOpenBag: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "🎒",
            fontSize = 20.sp,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onOpenBag)
                .padding(4.dp),
        )
        for (slot in 0 until GameEngine.MAX_INVENTORY) {
            val locked = slot >= cap
            var bounds by remember { mutableStateOf(Rect.Zero) }
            val item = inventory[slot]
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .onGloballyPositioned {
                        bounds = it.boundsInWindow()
                        if (!locked) invBounds[slot] = bounds
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            locked -> Color(0xFFDCCFC2)
                            hoverInv == slot && item == null && drag != null ->
                                Color(0xFFA5D6A7)
                            hoverInv == slot -> Color(0xFFFFF9C4)
                            else -> Color(0xFFEDE0CF)
                        },
                    )
                    .border(
                        1.dp,
                        if (drag != null && item == null && !locked) Gold else Color(0x335D4037),
                        RoundedCornerShape(10.dp),
                    )
                    .then(if (locked) Modifier.clickable(onClick = onOpenBag) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    locked -> {
                        val cost = GameEngine.inventorySlotCost(slot)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔒", fontSize = 12.sp)
                            if (cost != null && slot == cap) {
                                Text(
                                    "$cost",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (coins >= cost) MergeGreen else WarnRed,
                                )
                            }
                        }
                    }
                    item != null -> ItemTile(
                        index = slot,
                        item = item,
                        nowMs = nowMs,
                        hidden = drag?.source == ItemSource.INVENTORY && drag.index == slot,
                        cellBoundsInWindow = bounds,
                        source = ItemSource.INVENTORY,
                        onDragStart = onDragStart,
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onTapGenerator = {},
                        onTapInfo = onTapInfo,
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemTile(
    index: Int,
    item: BoardItem,
    nowMs: Long,
    hidden: Boolean,
    cellBoundsInWindow: Rect,
    source: ItemSource,
    onDragStart: (ItemSource, Int, Long, String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onTapGenerator: (Int) -> Unit,
    onTapInfo: (BoardItem) -> Unit,
) {
    val def = ItemCatalog[item.defId]
    val scope = rememberCoroutineScope()

    val scale = remember { Animatable(1f) }
    val pop = remember { Animatable(0f) }
    val currentOnDragStart by androidx.compose.runtime.rememberUpdatedState(onDragStart)
    val currentOnDrag by androidx.compose.runtime.rememberUpdatedState(onDrag)
    val currentOnDragEnd by androidx.compose.runtime.rememberUpdatedState(onDragEnd)
    val currentBounds by androidx.compose.runtime.rememberUpdatedState(cellBoundsInWindow)
    val currentOnTapInfo by androidx.compose.runtime.rememberUpdatedState(onTapInfo)
    val currentItem by androidx.compose.runtime.rememberUpdatedState(item)
    val currentNowMs by androidx.compose.runtime.rememberUpdatedState(nowMs)

    val cooling = item.cooldownUntilMs > nowMs
    val depleted = def.isGenerator && (cooling || item.charges == 0)

    LaunchedEffect(item.uid) {
        pop.snapTo(0f)
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = if (hidden) 0.25f else if (cooling) 0.55f else 1f
            }
            .scale(scale.value * (0.6f + 0.4f * pop.value))
            .pointerInput(item.uid, source) {
                detectTapGestures {
                    val it = currentItem
                    val d = ItemCatalog[it.defId]
                    val dep = d.isGenerator &&
                        (it.cooldownUntilMs > currentNowMs || it.charges == 0)
                    if (d.isGenerator && !dep) {
                        onTapGenerator(index)
                        scope.launch {
                            scale.animateTo(0.85f, tween(70))
                            scale.animateTo(
                                1f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            )
                        }
                    } else {
                        currentOnTapInfo(it)
                    }
                }
            }
            .pointerInput(item.uid, source) {
                detectDragGestures(
                    onDragStart = {
                        currentOnDragStart(
                            source,
                            index,
                            item.uid,
                            item.defId,
                            currentBounds.center,
                        )
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentOnDrag(currentBounds.topLeft + change.position)
                    },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragEnd() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(def.emoji, fontSize = 28.sp)
        if (def.isGenerator) {
            if (cooling) {
                Text(
                    "💤 ${(item.cooldownUntilMs - nowMs) / 1000 + 1}s",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cocoa,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Color(0xCCFFFFFF), RoundedCornerShape(4.dp))
                        .padding(horizontal = 3.dp),
                )
            } else {
                Text(
                    "x${item.charges}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(1.dp)
                        .background(MergeGreen, CircleShape)
                        .padding(horizontal = 3.dp),
                )
                Text(
                    "tap",
                    fontSize = 8.sp,
                    color = Cocoa.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun ScoreFloater(f: Floater) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(f.id) {
        anim.animateTo(1f, tween(850))
    }
    Text(
        f.text,
        color = f.color,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        modifier = Modifier
            .offset {
                IntOffset(
                    (f.position.x - 40.dp.toPx()).roundToInt(),
                    (f.position.y - 30.dp.toPx() - anim.value * 70.dp.toPx()).roundToInt(),
                )
            }
            .alpha(1f - anim.value),
    )
}

@Composable
private fun LevelBanner(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xEE3E2723))
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ItemInfoCard(item: BoardItem, nowMs: Long, onDismiss: () -> Unit) {
    val def = ItemCatalog[item.defId]
    LaunchedEffect(item.uid) {
        kotlinx.coroutines.delay(6_000)
        onDismiss()
    }

    Column(
        Modifier
            .padding(top = 150.dp, start = 24.dp, end = 24.dp)
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(16.dp))
            .background(CellWhite, RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFFE8D5B7), RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .background(Color(def.family.tint), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(def.emoji, fontSize = 30.sp)
            }
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text(def.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Cocoa)
                Text(
                    "Tier ${def.tier}  •  🪙 ${def.sellPrice}",
                    fontSize = 12.sp,
                    color = Cocoa.copy(alpha = 0.75f),
                )
            }
            Text(
                "✕",
                fontSize = 18.sp,
                color = Cocoa.copy(alpha = 0.5f),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .padding(6.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        if (def.isGenerator) {
            val cooling = item.cooldownUntilMs > nowMs
            Text(
                if (cooling) {
                    "💤 Hết lượt — hồi sau ${(item.cooldownUntilMs - nowMs) / 1000 + 1}s"
                } else {
                    "🔋 Lượt sinh: ${if (item.charges < 0) def.maxCharges else item.charges}/${def.maxCharges}"
                },
                fontSize = 12.sp,
                color = if (cooling) WarnRed else MergeGreen,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text("Sinh ra:", fontSize = 11.sp, color = Cocoa.copy(alpha = 0.7f))
            val total = def.spawnTable.sumOf { it.weight }
            Row(
                Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                def.spawnTable.forEach { entry ->
                    val spawnDef = ItemCatalog[entry.itemId]
                    Text(
                        "${spawnDef.emoji} ${entry.weight * 100 / total}%",
                        fontSize = 12.sp,
                        color = Cocoa,
                    )
                }
            }
        } else {
            val next = def.mergeResult?.let { ItemCatalog[it] }
            Text(
                if (next != null) "➡️ Merge lên: ${next.emoji} ${next.name}" else "🏆 Đã max tier!",
                fontSize = 12.sp,
                color = Cocoa,
            )
            val gens = ItemCatalog.generatorsFor(item.defId)
            Spacer(Modifier.height(4.dp))
            Text(
                if (gens.isNotEmpty()) {
                    "🧺 Sinh từ: " + gens.joinToString(" / ") { "${it.emoji} ${it.name}" }
                } else {
                    "🧺 Sinh từ: không có (item đặc biệt)"
                },
                fontSize = 12.sp,
                color = Cocoa.copy(alpha = 0.8f),
            )
        }
    }
}
