package com.bepngon.merge.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
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

/** Isometric corner-room landmarks, in canvas fractions, measured on
 *  decor_bg_room.jpg (cropped to 2000×1536 → aspect 1.302). */
private object Iso {
    const val ASPECT = 2000f / 1536f
    val CT = Offset(0.497f, 0.0195f) // top of the corner (ceiling)
    val CF = Offset(0.497f, 0.4121f) // corner where walls meet the floor
    val FL = Offset(0.1195f, 0.6823f) // floor diamond: left vertex
    val FR = Offset(0.8765f, 0.7025f) // floor diamond: right vertex
    val FB = Offset(0.496f, 0.9759f) // floor diamond: bottom vertex
    val LT = Offset(0.1195f, 0.3132f) // left wall outer-top (above FL)
    val RT = Offset(0.8765f, 0.3008f) // right wall outer-top (above FR)
}

/** Grid ↔ screen mapping for the isometric room. All math happens in
 *  canvas fractions (0..1); multiply by the canvas size for pixels.
 *
 *  Floor: gx runs down-right along [ex], floor-row gy down-left along [ey].
 *  Walls: rot encodes the side — 0 = right wall (+x axis), 90 = left
 *  wall (+y axis). gx = cells along the wall, gy = rows up from the floor
 *  (gy = 0 is the top wall row, like the old flat band). */
private class IsoMap(
    private val cols: Int,
    private val floorRows: Int,
    private val wallRows: Int,
) {
    val ex = Offset((Iso.FR.x - Iso.CF.x) / cols, (Iso.FR.y - Iso.CF.y) / cols)
    val ey = Offset((Iso.FL.x - Iso.CF.x) / floorRows, (Iso.FL.y - Iso.CF.y) / floorRows)
    private val wallH0 = Iso.CF.y - Iso.CT.y // wall height at the corner
    private val wallHL = Iso.FL.y - Iso.LT.y // at the left outer edge
    private val wallHR = Iso.FR.y - Iso.RT.y // at the right outer edge
    private val det = ex.x * ey.y - ex.y * ey.x

    fun cellOrigin(gx: Float, fy: Float) = Offset(
        Iso.CF.x + gx * ex.x + fy * ey.x,
        Iso.CF.y + gx * ex.y + fy * ey.y,
    )

    /** Continuous floor coords of a canvas-fraction point (unclamped). */
    fun solveFloor(p: Offset): Offset {
        val dx = p.x - Iso.CF.x
        val dy = p.y - Iso.CF.y
        return Offset(
            (dx * ey.y - dy * ey.x) / det,
            (ex.x * dy - ex.y * dx) / det,
        )
    }

    private fun wallAxis(side: Int) = if (side == 0) ex else ey
    private fun wallLen(side: Int) = if (side == 0) cols else floorRows

    private fun wallHAt(side: Int, a: Float): Float {
        val h1 = if (side == 0) wallHR else wallHL
        return wallH0 + (h1 - wallH0) * a / wallLen(side)
    }

    /** Point on a wall: [a] cells along the wall, [h] 0=floor → 1=ceiling. */
    fun wallPoint(side: Int, a: Float, h: Float): Offset {
        val axis = wallAxis(side)
        val bx = Iso.CF.x + a * axis.x
        val by = Iso.CF.y + a * axis.y
        return Offset(bx, by - h * wallHAt(side, a))
    }

    /** Continuous (a, h) on the wall plane; null when clearly outside. */
    fun solveWall(side: Int, p: Offset): Offset? {
        val axis = wallAxis(side)
        val a = (p.x - Iso.CF.x) / axis.x
        if (a < -0.7f || a > wallLen(side) + 0.7f) return null
        val baseY = Iso.CF.y + a * axis.y
        val h = (baseY - p.y) / wallHAt(side, a)
        if (h < -0.15f || h > 1.15f) return null
        return Offset(a, h)
    }

    fun heightFracAt(gy: Int, h: Int): Pair<Float, Float> {
        val hTop = (wallRows - gy).toFloat() / wallRows
        return hTop to hTop - h.toFloat() / wallRows
    }

    fun path(pts: List<Offset>, w: Float, h: Float): Path = Path().apply {
        pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x * w, o.y * h) else lineTo(o.x * w, o.y * h) }
        close()
    }

    fun floorPath(): List<Offset> =
        listOf(Iso.CF, Iso.FR, Iso.FB, Iso.FL)

    fun wallPath(side: Int): List<Offset> {
        val len = wallLen(side).toFloat()
        return listOf(
            wallPoint(side, 0f, 0f), wallPoint(side, len, 0f),
            wallPoint(side, len, 1f), wallPoint(side, 0f, 1f),
        )
    }

    fun footprintPts(gx: Int, fy: Int, w: Int, h: Int): List<Offset> = listOf(
        cellOrigin(gx.toFloat(), fy.toFloat()),
        cellOrigin(gx + w.toFloat(), fy.toFloat()),
        cellOrigin(gx + w.toFloat(), fy + h.toFloat()),
        cellOrigin(gx.toFloat(), fy + h.toFloat()),
    )

    fun wallFootprintPts(side: Int, gx: Int, gy: Int, w: Int, h: Int): List<Offset> {
        val (hTop, hBot) = heightFracAt(gy, h)
        return listOf(
            wallPoint(side, gx.toFloat(), hBot),
            wallPoint(side, gx + w.toFloat(), hBot),
            wallPoint(side, gx + w.toFloat(), hTop),
            wallPoint(side, gx.toFloat(), hTop),
        )
    }
}

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

    val iso = remember(def) { IsoMap(def.cols, def.floorRows, def.wallRows) }

    fun variantOf(item: DecorItem): DecorVariant =
        item.variantOf(pickedVariant[item.id] ?: item.variants.first().id)

    // Function, not a val: pointerInput closures capture `drop`/`snapToCell`
    // once, so sizes must re-read roomBounds every call (0 until measured).
    fun cellW(): Float = iso.ex.x * roomBounds.width

    /** Snap a window position to a grid cell: (gx, gy, rot). For wall
     *  items the wall plane under the pointer picks the side in rot. */
    fun snapToCell(pos: Offset, itemId: String, rot: Int): Triple<Int, Int, Int>? {
        if (roomBounds.width <= 0 || !roomBounds.inflate(roomBounds.width).contains(pos)) return null
        val item = DecorCatalog[itemId]
        val (w, h) = DecorEngine.footprint(item, rot)
        val local = Offset(
            (pos.x - roomBounds.left) / roomBounds.width,
            (pos.y - roomBounds.top) / roomBounds.height,
        )
        return when (item.placement) {
            Placement.FLOOR -> {
                val s = iso.solveFloor(local)
                if (s.x < -w || s.x > def.cols + w || s.y < -h || s.y > def.floorRows + h) return null
                Triple(
                    (s.x - w / 2f).roundToInt(),
                    (s.y - h / 2f).roundToInt() + def.wallRows,
                    rot,
                )
            }
            Placement.WALL -> listOf(0, 90).firstNotNullOfOrNull { side ->
                iso.solveWall(if (side == 0) 0 else 1, local)?.let { ah ->
                    Triple(
                        (ah.x - w / 2f).roundToInt(),
                        (def.wallRows - ah.y * def.wallRows - h / 2f).roundToInt(),
                        side,
                    )
                }
            }
            Placement.PAINT -> null
        }
    }

    /** Sprite box (top-left px in canvas coords + square size) for a
     *  placed item: floor sprites stand on their footprint's bottom
     *  vertex, wall sprites hang centered on their wall patch. */
    fun itemBox(p: PlacedDecor): Pair<Offset, Float> {
        val item = DecorCatalog[p.itemId]
        val (w, h) = DecorEngine.footprint(item, p.rot)
        val cw = roomBounds.width
        val ch = roomBounds.height
        if (item.placement == Placement.WALL) {
            val side = if (p.rot == 90) 1 else 0
            val pt = iso.wallPoint(side, p.gx + w / 2f, (def.wallRows - p.gy - h / 2f) / def.wallRows)
            val size = w * cellW() * 1.45f
            return Offset(pt.x * cw - size / 2, pt.y * ch - size * 0.55f) to size
        }
        val fy = p.gy - def.wallRows
        val cx = Iso.CF.x + (p.gx + w / 2f) * iso.ex.x + (fy + h / 2f) * iso.ey.x
        val botY = Iso.CF.y + (p.gx + w) * iso.ex.y + (fy + h) * iso.ey.y
        val size = (w + h) * cellW() * 0.8f
        return Offset(cx * cw - size / 2, botY * ch - size * 0.92f) to size
    }

    val ghost: Triple<Triple<Int, Int, Int>, Boolean, List<Offset>>? = drag?.let { d ->
        snapToCell(d.pos, d.itemId, d.rot)?.let { (gx, gy, rot) ->
            val item = DecorCatalog[d.itemId]
            val (w, h) = DecorEngine.footprint(item, rot)
            val pts = if (item.placement == Placement.WALL) {
                iso.wallFootprintPts(if (rot == 90) 1 else 0, gx, gy, w, h)
            } else {
                iso.footprintPts(gx, gy - def.wallRows, w, h)
            }
            Triple(
                Triple(gx, gy, rot),
                DecorEngine.canPlace(game.decor, roomId, d.itemId, gx, gy, rot, d.uid),
                pts,
            )
        }
    }

    fun drop() {
        val d = drag ?: return
        when {
            roomBounds.contains(d.pos) -> {
                // let the engine re-validate — an invalid drop keeps state
                // untouched and answers InvalidDrop for the error feedback
                snapToCell(d.pos, d.itemId, d.rot)?.let { (gx, gy, rot) ->
                    if (d.uid == 0L) onPlace(d.itemId, d.variantId, gx, gy, rot)
                    else onMove(d.uid, gx, gy, rot)
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

            // ---- room canvas (isometric corner room) ----
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .aspectRatio(Iso.ASPECT)
                        .clip(RoundedCornerShape(16.dp))
                        .onGloballyPositioned { roomBounds = it.boundsInWindow() }
                        // sticky-move mode (from the item menu): the picked-up
                        // item follows a drag on the canvas, or a tap drops it.
                        .pointerInput(stickyMove) {
                            if (!stickyMove) return@pointerInput
                            detectTapGestures { local ->
                                val d = drag ?: return@detectTapGestures
                                snapToCell(roomBounds.topLeft + local, d.itemId, d.rot)
                                    ?.let { (gx, gy, rot) ->
                                        if (DecorEngine.canPlace(
                                                game.decor, roomId, d.itemId,
                                                gx, gy, rot, d.uid,
                                            )
                                        ) {
                                            onMove(d.uid, gx, gy, rot)
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
                    RoomShell(room.wallPaint != null, room.floorPaint != null)
                    // paint tints + ghost preview, clipped to the room quads
                    Canvas(Modifier.fillMaxSize()) {
                        val cw = size.width
                        val ch = size.height
                        if (room.wallPaint != null) {
                            val tint = Color(DecorEngine.wallTint(game.decor, roomId))
                            drawPath(iso.path(iso.wallPath(1), cw, ch), tint.copy(alpha = 0.35f))
                            drawPath(iso.path(iso.wallPath(0), cw, ch), tint.copy(alpha = 0.35f))
                        }
                        if (room.floorPaint != null) {
                            drawPath(
                                iso.path(iso.floorPath(), cw, ch),
                                Color(DecorEngine.floorTint(game.decor, roomId)).copy(alpha = 0.35f),
                            )
                        }
                        ghost?.let { (_, ok, pts) ->
                            drawPath(
                                iso.path(pts, cw, ch),
                                if (ok) Color(0x8866BB6A) else Color(0x88EF5350),
                            )
                        }
                    }
                    // placed items, back-to-front
                    room.placed.sortedBy { DecorEngine.sortKey(it) }.forEach { p ->
                        key(p.uid) {
                            val (origin, sizePx) = itemBox(p)
                            PlacedTile(
                                p = p,
                                origin = origin,
                                sizePx = sizePx,
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
                        val (bo, bs) = itemBox(menuPlaced)
                        drag = DecorDrag(
                            menuPlaced.itemId, menuPlaced.variantId,
                            menuPlaced.uid,
                            roomBounds.topLeft + Offset(bo.x + bs / 2, bo.y + bs * 0.8f),
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
            val cw = cellW() * 2f // cell diagonal ≈ flat-view cell size
            Box(
                Modifier
                    .offset {
                        IntOffset(
                            (d.pos.x - contentOrigin.x - cw).roundToInt(),
                            (d.pos.y - contentOrigin.y - cw * 1.5f).roundToInt(),
                        )
                    }
                    .zIndex(10f)
                    .size(with(LocalDensity.current) { (cw * 2).toDp() })
                    .alpha(0.9f),
                contentAlignment = Alignment.Center,
            ) {
                DecorArt(
                    d.itemId, dv.tint,
                    with(LocalDensity.current) { (cw * 1.9f).toDp() },
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

/** Isometric room shell: the generated corner-room art filling the
 *  canvas. Falls back to flat wall/floor bands when the JPG is missing. */
@Composable
private fun RoomShell(wallPainted: Boolean, floorPainted: Boolean) {
    val ctx = LocalContext.current
    val res = remember { artRes(ctx, "decor_bg_room") }
    if (res != 0) {
        Image(
            painter = painterResource(res),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(0.41f).background(Color(0xFFE8DFF0)))
            Box(Modifier.fillMaxWidth().weight(0.59f).background(Color(0xFFF5DDBB)))
        }
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

/** One placed item in the iso room: sprite standing on its footprint.
 *  [origin]/[sizePx] come from `itemBox` (canvas px). Tap = menu,
 *  drag = move / drag outside room = store. [interactive] disables all
 *  gestures so events pass through to the canvas in sticky-move mode. */
@Composable
private fun PlacedTile(
    p: PlacedDecor,
    origin: Offset,
    sizePx: Float,
    hidden: Boolean,
    interactive: Boolean,
    onTap: () -> Unit,
    onPickup: (Offset) -> Unit,
    onDragPos: (Offset) -> Unit,
    onDrop: () -> Unit,
) {
    val item = DecorCatalog[p.itemId]
    val variant = item.variantOf(p.variantId)
    val density = LocalDensity.current
    var winPos by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .offset { IntOffset(origin.x.roundToInt(), origin.y.roundToInt()) }
            .size(with(density) { sizePx.toDp() })
            .onGloballyPositioned { winPos = it.positionInWindow() }
            .alpha(if (hidden) 0.25f else 1f)
            .graphicsLayer { scaleX = if (p.flip) -1f else 1f }
            .pointerInput(p.uid, interactive) {
                if (!interactive) return@pointerInput
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(p.uid, interactive) {
                if (!interactive) return@pointerInput
                detectDragGestures(
                    onDragStart = { start -> onPickup(winPos + start) },
                    onDrag = { change, _ -> onDragPos(winPos + change.position) },
                    onDragEnd = onDrop,
                    onDragCancel = {},
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        DecorArt(p.itemId, variant.tint, with(density) { sizePx.toDp() }, item.emoji, rot = p.rot)
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
