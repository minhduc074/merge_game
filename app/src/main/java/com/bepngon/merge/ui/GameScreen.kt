package com.bepngon.merge.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bepngon.merge.game.BoardItem
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.GameEvent
import com.bepngon.merge.game.ItemCatalog
import com.bepngon.merge.ui.components.SellZone
import com.bepngon.merge.ui.components.TopBar
import com.bepngon.merge.ui.theme.BepNgonTheme
import com.bepngon.merge.ui.theme.CellBorder
import com.bepngon.merge.ui.theme.CellWhite
import com.bepngon.merge.ui.theme.CreamBg
import com.bepngon.merge.ui.theme.CreamBgDeep
import com.bepngon.merge.ui.theme.Gold
import com.bepngon.merge.ui.theme.MergeGreen
import com.bepngon.merge.ui.theme.WarnRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class AnimKind { MERGE, SPAWN }

private data class DragState(
    val fromCell: Int,
    val uid: Long,
    val defId: String,
    val position: Offset,
)

private data class Floater(
    val id: Long,
    val position: Offset,
    val text: String,
)

@Composable
fun GameScreen(vm: GameViewModel = viewModel()) {
    BepNgonTheme {
        val ui by vm.ui.collectAsState()
        if (ui.loading) {
            Box(
                Modifier.fillMaxSize().background(CreamBg),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = Gold) }
            return@BepNgonTheme
        }
        GameContent(ui, vm)
    }
}

@Composable
private fun GameContent(ui: GameUiState, vm: GameViewModel) {
    val haptics = LocalHapticFeedback.current

    var rootBounds by remember { mutableStateOf(Rect.Zero) }
    var sellBounds by remember { mutableStateOf(Rect.Zero) }
    var drag by remember { mutableStateOf<DragState?>(null) }
    val cellBounds = remember { mutableStateMapOf<Int, Rect>() }
    val animCells = remember { mutableStateMapOf<Int, AnimKind>() }
    val floaters = remember { mutableStateListOf<Floater>() }
    var floaterSeq by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            when (ev) {
                is GameEvent.Merged -> {
                    animCells[ev.cellIndex] = AnimKind.MERGE
                    cellBounds[ev.cellIndex]?.let {
                        floaters += Floater(++floaterSeq, it.center, "+${ev.scoreGained}")
                    }
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                is GameEvent.Spawned -> animCells[ev.cellIndex] = AnimKind.SPAWN
                is GameEvent.Sold -> {
                    cellBounds[ev.cellIndex]?.let {
                        floaters += Floater(++floaterSeq, it.center, "+${ev.coinsGained}🪙")
                    }
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                GameEvent.NoEnergy, GameEvent.BoardFull ->
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                GameEvent.InvalidDrop -> {}
            }
            when (ev) {
                is GameEvent.Merged -> launch { delay(650); animCells.remove(ev.cellIndex) }
                is GameEvent.Spawned -> launch { delay(450); animCells.remove(ev.cellIndex) }
                else -> {}
            }
        }
    }

    val dragState = drag
    val hoverCell = dragState?.let { d ->
        cellBounds.entries.firstOrNull { it.value.contains(d.position) }?.key
    }
    val overSell = dragState?.let { sellBounds.contains(it.position) } == true

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CreamBg, CreamBgDeep)))
            .onGloballyPositioned { rootBounds = it.boundsInWindow() },
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopBar(
                score = ui.game.score,
                coins = ui.game.coins,
                energy = ui.game.energy,
                energyCap = GameEngine.ENERGY_CAP,
                onRestart = vm::askRestart,
            )
            if (ui.game.boardFull) {
                Text(
                    "Board đầy rồi! Merge hoặc bán bớt để có chỗ nhé 🧺",
                    color = WarnRed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
            BoardGrid(
                ui = ui,
                drag = dragState,
                hoverCell = hoverCell,
                animCells = animCells,
                cellBounds = cellBounds,
                onDragStart = { cell, uid, defId, pos -> drag = DragState(cell, uid, defId, pos) },
                onDrag = { amount -> drag = drag?.let { it.copy(position = it.position + amount) } },
                onDragEnd = {
                    val d = drag
                    if (d != null) {
                        val hit = cellBounds.entries
                            .firstOrNull { it.value.contains(d.position) }?.key
                        when {
                            sellBounds.contains(d.position) -> vm.sell(d.fromCell)
                            hit != null -> vm.moveOrMerge(d.fromCell, hit)
                        }
                    }
                    drag = null
                },
                onDragCancel = { drag = null },
                onTapItem = { vm.tapGenerator(it) },
                modifier = Modifier.weight(1f),
            )
            SellZone(
                active = dragState != null,
                highlighted = overSell,
                modifier = Modifier.onGloballyPositioned { sellBounds = it.boundsInWindow() },
            )
        }

        dragState?.let { d ->
            DraggedOverlay(
                defId = d.defId,
                windowPos = d.position,
                rootTopLeft = rootBounds.topLeft,
            )
        }

        floaters.forEach { f ->
            ScoreFloater(
                floater = f,
                rootTopLeft = rootBounds.topLeft,
                onDone = { floaters.remove(f) },
            )
        }

        if (ui.showRestartConfirm) {
            AlertDialog(
                onDismissRequest = vm::dismissRestart,
                title = { Text("Chơi lại từ đầu?") },
                text = { Text("Toàn bộ tiến trình hiện tại sẽ bị xoá đó nha.") },
                confirmButton = {
                    TextButton(onClick = vm::restart) { Text("Chơi lại", color = WarnRed) }
                },
                dismissButton = {
                    TextButton(onClick = vm::dismissRestart) { Text("Thôi") }
                },
            )
        }
    }
}

@Composable
private fun BoardGrid(
    ui: GameUiState,
    drag: DragState?,
    hoverCell: Int?,
    animCells: Map<Int, AnimKind>,
    cellBounds: MutableMap<Int, Rect>,
    onDragStart: (cell: Int, uid: Long, defId: String, pos: Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onTapItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth().padding(10.dp)) {
        val gap = 4.dp
        val cellDp = minOf(
            (maxWidth - gap * (GameEngine.COLS - 1)) / GameEngine.COLS,
            (maxHeight - gap * (GameEngine.ROWS - 1)) / GameEngine.ROWS,
        )

        val mergePartners = remember(drag, ui.game.cells) {
            if (drag == null) emptySet()
            else ui.game.cells.indices.filter { i ->
                i != drag.fromCell &&
                    GameEngine.canMerge(ui.game.cells[i], ui.game.cells[drag.fromCell])
            }.toSet()
        }

        Column(
            Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (r in 0 until GameEngine.ROWS) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (c in 0 until GameEngine.COLS) {
                        val index = GameEngine.idx(r, c)
                        CellBox(
                            index = index,
                            item = ui.game.cells[index],
                            size = cellDp,
                            borderColor = cellBorder(
                                index, ui, drag, hoverCell, mergePartners,
                            ),
                            ghosted = drag?.uid == ui.game.cells[index]?.uid,
                            anim = animCells[index],
                            onPositioned = { cellBounds[index] = it },
                            onDragStart = onDragStart,
                            onDrag = onDrag,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                            onTap = onTapItem,
                        )
                    }
                }
            }
        }
    }
}

private fun cellBorder(
    index: Int,
    ui: GameUiState,
    drag: DragState?,
    hoverCell: Int?,
    mergePartners: Set<Int>,
): Color {
    val item = ui.game.cells[index]
    if (drag == null) {
        return if (item != null && ItemCatalog[item.defId].isGenerator) Gold else CellBorder
    }
    if (index == drag.fromCell) return CellBorder
    if (index in mergePartners) return Gold
    if (index != hoverCell) return CellBorder
    val target = ui.game.cells[index]
    return when {
        target == null -> MergeGreen
        GameEngine.canMerge(target, ui.game.cells[drag.fromCell]) -> Gold
        else -> WarnRed
    }
}

@Composable
private fun CellBox(
    index: Int,
    item: BoardItem?,
    size: Dp,
    borderColor: Color,
    ghosted: Boolean,
    anim: AnimKind?,
    onPositioned: (Rect) -> Unit,
    onDragStart: (cell: Int, uid: Long, defId: String, pos: Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onTap: (Int) -> Unit,
) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    Box(
        Modifier
            .size(size)
            .onGloballyPositioned {
                bounds = it.boundsInWindow()
                onPositioned(bounds)
            }
            .background(CellWhite, RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (item != null) {
            ItemTile(
                item = item,
                cellIndex = index,
                cellSize = size,
                cellBoundsInWindow = bounds,
                ghosted = ghosted,
                anim = anim,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
                onTap = onTap,
            )
        }
    }
}

@Composable
private fun ItemTile(
    item: BoardItem,
    cellIndex: Int,
    cellSize: Dp,
    cellBoundsInWindow: Rect,
    ghosted: Boolean,
    anim: AnimKind?,
    onDragStart: (cell: Int, uid: Long, defId: String, pos: Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onTap: (Int) -> Unit,
) {
    val def = ItemCatalog[item.defId]
    val scale = remember { Animatable(1f) }

    val curDragStart by rememberUpdatedState(onDragStart)
    val curDrag by rememberUpdatedState(onDrag)
    val curDragEnd by rememberUpdatedState(onDragEnd)
    val curDragCancel by rememberUpdatedState(onDragCancel)
    val curTap by rememberUpdatedState(onTap)
    val curBounds by rememberUpdatedState(cellBoundsInWindow)

    LaunchedEffect(anim) {
        when (anim) {
            AnimKind.MERGE -> {
                scale.snapTo(0.4f)
                scale.animateTo(1.25f, spring())
                scale.animateTo(1f, tween(150))
            }
            AnimKind.SPAWN -> {
                scale.snapTo(0.1f)
                scale.animateTo(1f, spring())
            }
            null -> {}
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(item.uid) {
                detectDragGestures(
                    onDragStart = { offset ->
                        curDragStart(
                            cellIndex, item.uid, item.defId,
                            curBounds.topLeft + offset,
                        )
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        curDrag(amount)
                    },
                    onDragEnd = { curDragEnd() },
                    onDragCancel = { curDragCancel() },
                )
            }
            .then(
                if (def.isGenerator) {
                    Modifier.pointerInput(item.uid) {
                        detectTapGestures { curTap(cellIndex) }
                    }
                } else Modifier,
            )
            .scale(scale.value)
            .alpha(if (ghosted) 0.3f else 1f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize(0.78f)
                .background(Color(def.family.tint), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(def.emoji, fontSize = with(LocalDensity.current) { (cellSize * 0.42f).toSp() })
        }
        if (def.isGenerator) {
            Text(
                "⚡",
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(1.dp),
            )
        }
    }
}

@Composable
private fun DraggedOverlay(defId: String, windowPos: Offset, rootTopLeft: Offset) {
    val def = ItemCatalog[defId]
    val boxSize = 60.dp
    val halfPx = with(LocalDensity.current) { (boxSize.toPx() * 1.15f) / 2f }
    val local = windowPos - rootTopLeft
    Box(
        Modifier
            .offset { IntOffset((local.x - halfPx).roundToInt(), (local.y - halfPx).roundToInt()) }
            .size(boxSize)
            .graphicsLayer { scaleX = 1.15f; scaleY = 1.15f },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxSize().background(Color(def.family.tint), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(def.emoji, fontSize = 30.sp)
        }
    }
}

@Composable
private fun ScoreFloater(floater: Floater, rootTopLeft: Offset, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(floater.id) {
        progress.animateTo(1f, tween(700))
        onDone()
    }
    val local = floater.position - rootTopLeft
    val rise = progress.value * 90f
    Text(
        floater.text,
        color = Color(0xFFE65100),
        fontSize = 16.sp,
        modifier = Modifier
            .offset { IntOffset((local.x - 40f).roundToInt(), (local.y - rise - 60f).roundToInt()) }
            .alpha(1f - progress.value * progress.value),
    )
}
