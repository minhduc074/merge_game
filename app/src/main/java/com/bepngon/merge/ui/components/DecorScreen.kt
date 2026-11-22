package com.bepngon.merge.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.bepngon.merge.game.DecorCatalog
import com.bepngon.merge.game.DecorEngine
import com.bepngon.merge.game.DecorItem
import com.bepngon.merge.game.DecorVariant
import com.bepngon.merge.game.GameState
import com.bepngon.merge.game.PlacedDecor
import com.bepngon.merge.game.Placement
import com.bepngon.merge.ui.theme.CellWhite
import com.bepngon.merge.ui.theme.Cocoa
import com.bepngon.merge.ui.theme.CreamBg
import com.bepngon.merge.ui.theme.Gold
import com.bepngon.merge.ui.theme.MergeGreen
import com.bepngon.merge.ui.theme.WarnRed
import kotlin.math.roundToInt

/** Where a decor drag came from: catalog uid = 0, a placed item's uid otherwise. */
private data class DecorDrag(
    val itemId: String,
    val variantId: String,
    val uid: Long,
    val pos: Offset, // window coords
    val rot: Int,
    val flip: Boolean,
)

@Composable
fun DecorScreen(
    game: GameState,
    roomId: String,
    canUndo: Boolean,
    canRedo: Boolean,
    onPlace: (itemId: String, variantId: String, gx: Int, gy: Int, rot: Int) -> Unit,
    onMove: (uid: Long, gx: Int, gy: Int, rot: Int) -> Unit,
    onRemove: (uid: Long) -> Unit,
    onRotate: (uid: Long) -> Unit,
    onSetVariant: (uid: Long, variantId: String) -> Unit,
    onPaint: (variantId: String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClose: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val def = DecorCatalog.room(roomId)
    val room = DecorEngine.roomOf(game.decor, roomId)

    var drag by remember { mutableStateOf<DecorDrag?>(null) }
    var menuUid by remember { mutableStateOf<Long?>(null) }
    var stickyMove by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(DecorCatalog.categories.first().first) }
    val pickedVariant = remember { mutableStateMapOf<String, String>() }
    var roomBounds by remember { mutableStateOf(Rect.Zero) }
    var contentOrigin by remember { mutableStateOf(Offset.Zero) }

    fun variantOf(item: DecorItem): DecorVariant =
        item.variantOf(pickedVariant[item.id] ?: item.variants.first().id)

    // Function, not a val: pointerInput closures capture `drop`/`snapToCell`
    // once, so cell size must re-read roomBounds every call (0 until measured).
    fun cellPxNow(): Float = roomBounds.width / def.cols

    /** Snap a window position to the grid cell under the footprint's center. */
    fun snapToCell(pos: Offset, itemId: String, rot: Int): Pair<Int, Int>? {
        val cell = cellPxNow()
        if (cell <= 0 || !roomBounds.inflate(cell * 2).contains(pos)) return null
        val (w, h) = DecorEngine.footprint(DecorCatalog[itemId], rot)
        val gx = ((pos.x - roomBounds.left) / cell - w / 2f).roundToInt()
        val gy = ((pos.y - roomBounds.top) / cell - h / 2f).roundToInt()
        return gx to gy
    }

    val ghost: Triple<Int, Int, Boolean>? = drag?.let { d ->
        snapToCell(d.pos, d.itemId, d.rot)?.let { (gx, gy) ->
            Triple(
                gx, gy,
                DecorEngine.canPlace(game.decor, roomId, d.itemId, gx, gy, d.rot, d.uid),
            )
        }
    }

    fun drop() {
        val d = drag ?: return
        when {
            roomBounds.contains(d.pos) -> {
                // let the engine re-validate — an invalid drop keeps state
                // untouched and answers InvalidDrop for the error feedback
                snapToCell(d.pos, d.itemId, d.rot)?.let { (gx, gy) ->
                    if (d.uid == 0L) onPlace(d.itemId, d.variantId, gx, gy, d.rot)
                    else onMove(d.uid, gx, gy, d.rot)
                }
            }
            // placed item dragged out → store it (except in sticky-move
            // mode, where dropping outside just cancels the move)
            d.uid != 0L && !stickyMove -> onRemove(d.uid)
            else -> Unit // catalog item released outside → cancel quietly
        }
        drag = null
        stickyMove = false
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(CreamBg)
            .onGloballyPositioned { contentOrigin = it.positionInWindow() },
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            // ---- top bar ----
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "🛋️ ${def.name}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Cocoa,
                    modifier = Modifier.weight(1f),
                )
                TopChip("🪙 ${game.coins}") {}
                TopChip("↶", enabled = canUndo) { onUndo() }
                TopChip("↷", enabled = canRedo) { onRedo() }
                Text(
                    "✕",
                    fontSize = 22.sp,
                    color = Cocoa,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFE0B2))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }

            // ---- room canvas ----
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .aspectRatio(def.cols.toFloat() / def.rows)
                        .clip(RoundedCornerShape(16.dp))
                        .onGloballyPositioned { roomBounds = it.boundsInWindow() }
                        // sticky-move mode (from the item menu): the picked-up
                        // item follows a drag on the canvas, or a tap drops it.
                        .pointerInput(stickyMove) {
                            if (!stickyMove) return@pointerInput
                            detectTapGestures { local ->
                                val d = drag ?: return@detectTapGestures
                                snapToCell(roomBounds.topLeft + local, d.itemId, d.rot)
                                    ?.let { (gx, gy) ->
                                        if (DecorEngine.canPlace(
                                                game.decor, roomId, d.itemId,
                                                gx, gy, d.rot, d.uid,
                                            )
                                        ) {
                                            onMove(d.uid, gx, gy, d.rot)
                                            drag = null
                                            stickyMove = false
                                        }
                                    }
                            }
                        }
                        .pointerInput(stickyMove) {
                            if (!stickyMove) return@pointerInput
                            detectDragGestures(
                                onDragStart = {},
                                onDrag = { change, _ ->
                                    drag = drag?.copy(
                                        pos = roomBounds.topLeft + change.position,
                                    )
                                },
                                onDragEnd = ::drop,
                                onDragCancel = {
                                    drag = null
                                    stickyMove = false
                                },
                            )
                        },
                ) {
                    val density = LocalDensity.current
                    val wallH = roomBounds.height * def.wallRows / def.rows
                    // wall band + floor — texture under a translucent tint
                    // when a paint variant is applied; flat color fallback
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxWidth().weight(def.wallRows.toFloat())) {
                            RoomBg(
                                "decor_bg_wall",
                                Color(DecorEngine.wallTint(game.decor, roomId)),
                                room.wallPaint != null,
                            )
                        }
                        Box(Modifier.fillMaxWidth().weight(def.floorRows.toFloat())) {
                            RoomBg(
                                "decor_bg_floor",
                                Color(DecorEngine.floorTint(game.decor, roomId)),
                                room.floorPaint != null,
                            )
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .offset(y = with(density) { wallH.toDp() })
                            .height(3.dp)
                            .background(Color(0x406D4C41)),
                    )
                    // placed items, back-to-front
                    room.placed.sortedBy { DecorEngine.sortKey(it) }.forEach { p ->
                        key(p.uid) {
                            PlacedTile(
                                p = p,
                                cellPx = cellPxNow(),
                                hidden = drag?.uid == p.uid,
                                interactive = !stickyMove,
                                onTap = {
                                    // a fresh tap discards any drag left
                                    // over by an interrupted gesture
                                    drag = null
                                    stickyMove = false
                                    menuUid = p.uid
                                },
                                onPickup = { start ->
                                    menuUid = null
                                    drag = DecorDrag(p.itemId, p.variantId, p.uid, start, p.rot, p.flip)
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragPos = { pos -> drag = drag?.copy(pos = pos) },
                                onDrop = ::drop,
                            )
                        }
                    }
                    // ghost preview while dragging
                    drag?.let { d ->
                        ghost?.let { (gx, gy, ok) ->
                            val (w, h) = DecorEngine.footprint(DecorCatalog[d.itemId], d.rot)
                            Box(
                                Modifier
                                    .offset {
                                        IntOffset(
                                            (gx * cellPxNow()).roundToInt(),
                                            (gy * cellPxNow()).roundToInt(),
                                        )
                                    }
                                    .size(
                                        with(density) { (w * cellPxNow()).toDp() },
                                        with(density) { (h * cellPxNow()).toDp() },
                                    )
                                    .background(
                                        if (ok) Color(0x8866BB6A) else Color(0x88EF5350),
                                        RoundedCornerShape(8.dp),
                                    ),
                            )
                        }
                    }
                }
                if (stickyMove) {
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xE6FFFFFF))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "✋ Đang di chuyển — chạm ô trống để đặt",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Cocoa,
                        )
                        Text(
                            "✕",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Cocoa,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clickable {
                                    drag = null
                                    stickyMove = false
                                },
                        )
                    }
                }
                if (room.placed.isEmpty() && drag == null) {
                    Text(
                        "Kéo đồ từ khay bên dưới vào tiệm nhé!\nKéo đồ đã đặt để dời, chạm để đổi màu / xoay / cất.",
                        fontSize = 13.sp,
                        color = Cocoa.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }

            // ---- bottom: context menu or catalog tray ----
            val menuPlaced = menuUid?.let { id -> room.placed.firstOrNull { it.uid == id } }
            if (menuPlaced != null) {
                ItemMenu(
                    p = menuPlaced,
                    game = game,
                    onSetVariant = { onSetVariant(menuPlaced.uid, it) },
                    onRotate = { onRotate(menuPlaced.uid) },
                    onRemove = {
                        onRemove(menuPlaced.uid)
                        menuUid = null
                    },
                    onMoveMode = {
                        menuUid = null
                        stickyMove = true
                        val (w, h) = DecorEngine.footprint(
                            DecorCatalog[menuPlaced.itemId], menuPlaced.rot,
                        )
                        val cell = cellPxNow()
                        drag = DecorDrag(
                            menuPlaced.itemId, menuPlaced.variantId,
                            menuPlaced.uid,
                            Offset(
                                roomBounds.left + (menuPlaced.gx + w / 2f) * cell,
                                roomBounds.top + (menuPlaced.gy + h / 2f) * cell,
                            ),
                            menuPlaced.rot, menuPlaced.flip,
                        )
                    },
                    onClose = { menuUid = null },
                )
            } else {
                CatalogTray(
                    game = game,
                    tab = tab,
                    onTab = { tab = it },
                    pickedVariant = pickedVariant,
                    onPickVariant = { itemId, v -> pickedVariant[itemId] = v },
                    onPaint = onPaint,
                    onDragStart = { item, start ->
                        if (stickyMove) return@CatalogTray
                        menuUid = null
                        val v = variantOf(item)
                        drag = DecorDrag(item.id, v.id, 0, start, 0, false)
                    },
                    onDragPos = { pos -> drag = drag?.copy(pos = pos) },
                    onDrop = ::drop,
                )
            }
        }

        // drag overlay (window coords -> local)
        drag?.let { d ->
            val item = DecorCatalog[d.itemId]
            val dv = DecorCatalog.variant(d.variantId)
            Box(
                Modifier
                    .offset {
                        IntOffset(
                            (d.pos.x - contentOrigin.x - cellPxNow()).roundToInt(),
                            (d.pos.y - contentOrigin.y - cellPxNow() * 1.5f).roundToInt(),
                        )
                    }
                    .zIndex(10f)
                    .size(with(LocalDensity.current) { (cellPxNow() * 2).toDp() })
                    .alpha(0.9f),
                contentAlignment = Alignment.Center,
            ) {
                DecorArt(
                    d.itemId, dv.tint,
                    with(LocalDensity.current) { (cellPxNow() * 1.9f).toDp() },
                    item.emoji,
                    rot = d.rot,
                )
            }
        }
    }
}

@Composable
private fun TopChip(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = if (enabled) Cocoa else Cocoa.copy(alpha = 0.35f),
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, Color(0x33A0522D), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** Room band background: generated texture when present (with a
 *  translucent paint tint over it), flat paint color otherwise. */
@Composable
private fun RoomBg(resName: String, tint: Color, painted: Boolean) {
    val ctx = LocalContext.current
    val res = remember(resName) { artRes(ctx, resName) }
    if (res == 0) {
        Box(Modifier.fillMaxSize().background(tint))
        return
    }
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
    if (painted) {
        Box(Modifier.fillMaxSize().background(tint.copy(alpha = 0.5f)))
    }
}

/** Decor sprite `decor_<itemId>` recolored toward the variant tint
 *  (Modulate keeps cel shading); falls back to emoji when the PNG is
 *  missing. */
@Composable
private fun DecorArt(
    itemId: String,
    tint: Long?,
    size: Dp,
    emoji: String,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    rot: Int = 0,
) {
    val ctx = LocalContext.current
    val res = remember(itemId) { artRes(ctx, "decor_$itemId") }
    if (res != 0) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = tint?.let {
                ColorFilter.tint(Color(it).copy(alpha = 0.5f), BlendMode.Modulate)
            },
            modifier = modifier.size(size).alpha(alpha)
                .graphicsLayer {
                    // bitmap rotation only for top-down art (rugs) —
                    // oblique sprites mirror via scaleX on the tile
                    rotationZ = if (DecorCatalog[itemId].topDown) rot.toFloat() else 0f
                },
        )
    } else {
        Text(
            emoji,
            fontSize = (size.value * 0.6f).sp,
            modifier = modifier.alpha(alpha)
                .graphicsLayer {
                    rotationZ = if (DecorCatalog[itemId].topDown) rot.toFloat() else 0f
                },
        )
    }
}

/** One placed item on the floor/wall: tinted tile + emoji, tap = menu,
 *  drag = move / drag outside room = store. [interactive] disables all
 *  gestures so events pass through to the canvas in sticky-move mode. */
@Composable
private fun PlacedTile(
    p: PlacedDecor,
    cellPx: Float,
    hidden: Boolean,
    interactive: Boolean,
    onTap: () -> Unit,
    onPickup: (Offset) -> Unit,
    onDragPos: (Offset) -> Unit,
    onDrop: () -> Unit,
) {
    val item = DecorCatalog[p.itemId]
    val variant = item.variantOf(p.variantId)
    val (w, h) = DecorEngine.footprint(item, p.rot)
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .offset { IntOffset((p.gx * cellPx).roundToInt(), (p.gy * cellPx).roundToInt()) }
            .size(
                with(density) { (w * cellPx).toDp() },
                with(density) { (h * cellPx).toDp() },
            )
            .onGloballyPositioned { origin = it.positionInWindow() }
            .alpha(if (hidden) 0.25f else 1f)
            .graphicsLayer { scaleX = if (p.flip) -1f else 1f }
            .pointerInput(p.uid, interactive) {
                if (!interactive) return@pointerInput
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(p.uid, interactive) {
                if (!interactive) return@pointerInput
                detectDragGestures(
                    onDragStart = { start -> onPickup(origin + start) },
                    onDrag = { change, _ -> onDragPos(origin + change.position) },
                    onDragEnd = onDrop,
                    onDragCancel = {},
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(1.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(variant.tint).copy(alpha = 0.45f))
                .border(
                    1.dp,
                    Color(variant.tint).copy(alpha = 0.8f),
                    RoundedCornerShape(8.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            DecorArt(
                p.itemId, variant.tint,
                with(density) { (cellPx * 0.92f * maxOf(w, h)).toDp() },
                item.emoji,
                rot = p.rot,
            )
        }
    }
}

/** Tap-menu for a placed item: variant carousel, rotate, flip, store. */
@Composable
private fun ItemMenu(
    p: PlacedDecor,
    game: GameState,
    onSetVariant: (String) -> Unit,
    onRotate: () -> Unit,
    onRemove: () -> Unit,
    onMoveMode: () -> Unit,
    onClose: () -> Unit,
) {
    val item = DecorCatalog[p.itemId]
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DecorArt(item.id, item.variantOf(p.variantId).tint, 26.dp, item.emoji)
            Text(
                item.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Cocoa,
                modifier = Modifier.weight(1f).padding(start = 6.dp),
            )
            MenuBtn("📦 Cất", onRemove)
            MenuBtn("✕", onClose)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item.variants.forEach { v ->
                VariantChip(
                    v = v,
                    selected = v.id == p.variantId,
                    owned = DecorCatalog.isOwned(game.decor, v.id),
                    levelOk = v.unlockLevel <= game.level,
                    onClick = { onSetVariant(v.id) },
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MenuBtn("✋ Di chuyển", onMoveMode)
            // Xoay: rugs rotate for real; oblique art mirrors — same button
            if (item.canRotate || item.canFlip) {
                MenuBtn("🔄 Xoay", onRotate)
            }
        }
    }
}

@Composable
private fun MenuBtn(label: String, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Cocoa,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFE0B2))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun VariantChip(
    v: DecorVariant,
    selected: Boolean,
    owned: Boolean,
    levelOk: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFFFFE0B2) else CellWhite)
            .border(
                2.dp,
                if (selected) Gold else Color(0x226D4C41),
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (levelOk) Color(v.tint) else Color(0xFFBBBBBB)),
            contentAlignment = Alignment.Center,
        ) {
            if (!levelOk) Text("🔒", fontSize = 8.sp)
        }
        Text(v.name, fontSize = 9.sp, color = Cocoa, fontWeight = FontWeight.Bold)
        Text(
            when {
                owned -> "✓"
                !levelOk -> "Lv${v.unlockLevel}"
                else -> "🪙${v.priceCoins}"
            },
            fontSize = 9.sp,
            color = if (owned) MergeGreen else if (!levelOk) WarnRed else Cocoa,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Bottom tray: category tabs + draggable item cards. */
@Composable
private fun CatalogTray(
    game: GameState,
    tab: String,
    onTab: (String) -> Unit,
    pickedVariant: Map<String, String>,
    onPickVariant: (String, String) -> Unit,
    onPaint: (String) -> Unit,
    onDragStart: (DecorItem, Offset) -> Unit,
    onDragPos: (Offset) -> Unit,
    onDrop: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DecorCatalog.categories.forEach { (id, label) ->
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (tab == id) Color.White else Cocoa,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (tab == id) Cocoa else Color.White)
                        .border(1.dp, Color(0x33A0522D), RoundedCornerShape(10.dp))
                        .clickable { onTab(id) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DecorCatalog.inCategory(tab).forEach { item ->
                key(item.id) {
                    CatalogCard(
                        item = item,
                        game = game,
                        variant = item.variantOf(
                            pickedVariant[item.id] ?: item.variants.first().id,
                        ),
                        onPickVariant = { onPickVariant(item.id, it) },
                        onPaint = onPaint,
                        onDragStart = onDragStart,
                        onDragPos = onDragPos,
                        onDrop = onDrop,
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogCard(
    item: DecorItem,
    game: GameState,
    variant: DecorVariant,
    onPickVariant: (String) -> Unit,
    onPaint: (String) -> Unit,
    onDragStart: (DecorItem, Offset) -> Unit,
    onDragPos: (Offset) -> Unit,
    onDrop: () -> Unit,
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val isPaint = item.placement == Placement.PAINT
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0x33A0522D), RoundedCornerShape(12.dp))
            .onGloballyPositioned { origin = it.positionInWindow() }
            .then(
                if (isPaint) {
                    Modifier.clickable { onPaint(variant.id) }
                } else {
                    Modifier.pointerInput(item.id) {
                        detectDragGestures(
                            onDragStart = { start -> onDragStart(item, origin + start) },
                            onDrag = { change, _ -> onDragPos(origin + change.position) },
                            onDragEnd = onDrop,
                            onDragCancel = {},
                        )
                    }
                },
            )
            .padding(8.dp),
    ) {
        if (isPaint) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(variant.tint))
                    .border(1.dp, Color(0x446D4C41), RoundedCornerShape(8.dp)),
            )
        } else {
            DecorArt(item.id, variant.tint, 40.dp, item.emoji)
        }
        Text(item.name, fontSize = 10.sp, color = Cocoa, fontWeight = FontWeight.Bold)
        Text(
            when {
                isPaint -> "Chạm để dùng"
                DecorCatalog.isOwned(game.decor, variant.id) -> "✓ đã có"
                variant.unlockLevel > game.level -> "🔒 Lv${variant.unlockLevel}"
                else -> "🪙${variant.priceCoins}"
            },
            fontSize = 9.sp,
            color = when {
                isPaint -> MergeGreen
                DecorCatalog.isOwned(game.decor, variant.id) -> MergeGreen
                variant.unlockLevel > game.level -> WarnRed
                else -> Cocoa
            },
        )
        // variant dots
        Row(
            Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item.variants.forEach { v ->
                val locked = v.unlockLevel > game.level
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (locked) Color(0xFFBBBBBB) else Color(v.tint))
                        .border(
                            2.dp,
                            if (v.id == variant.id) Gold else Color(0x336D4C41),
                            CircleShape,
                        )
                        .clickable { onPickVariant(v.id) },
                )
            }
        }
    }
}
