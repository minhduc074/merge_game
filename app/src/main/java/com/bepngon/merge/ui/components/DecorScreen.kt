package com.bepngon.merge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.graphics.BitmapFactory
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.bepngon.merge.game.DecorCatalog
import com.bepngon.merge.game.DecorEngine
import com.bepngon.merge.game.DecorItem
import com.bepngon.merge.game.DecorLayer
import com.bepngon.merge.game.DecorState
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
    fun wallLen(side: Int) = if (side == 0) cols else floorRows

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

    /** Bottom band of a wall (h = 0..[hTop]) — used for the wainscot. */
    fun wallBandPath(side: Int, hTop: Float): List<Offset> {
        val len = wallLen(side).toFloat()
        return listOf(
            wallPoint(side, 0f, 0f), wallPoint(side, len, 0f),
            wallPoint(side, len, hTop), wallPoint(side, 0f, hTop),
        )
    }

    /** Both ends of a horizontal wall line at height [h] — wainscot rail. */
    fun wallRail(side: Int, h: Float): Pair<Offset, Offset> =
        wallPoint(side, 0f, h) to wallPoint(side, wallLen(side).toFloat(), h)

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
    onScale: (uid: Long, scale: Float) -> Unit,
    onPaint: (variantId: String, wallSide: Int?) -> Unit,
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
    var roomSize by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var contentOrigin by remember { mutableStateOf(Offset.Zero) }

    val iso = remember(def) { IsoMap(def.cols, def.floorRows, def.wallRows) }

    fun variantOf(item: DecorItem): DecorVariant =
        item.variantOf(pickedVariant[item.id] ?: item.variants.first().id)

    // Function, not a val: pointerInput closures capture `drop`/`snapToCell`
    // once, so sizes must re-read roomBounds every call (0 until measured).
    fun cellW(): Float = iso.ex.x * roomSize.width

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
        val cw = roomSize.width.toFloat()
        val ch = roomSize.height.toFloat()
        if (item.placement == Placement.WALL) {
            val side = if (p.rot == 90) 1 else 0
            val pt = iso.wallPoint(side, p.gx + w / 2f, (def.wallRows - p.gy - h / 2f) / def.wallRows)
            val size = w * cellW() * 1.45f * p.scale
            return Offset(pt.x * cw - size / 2, pt.y * ch - size * 0.55f) to size
        }
        val fy = p.gy - def.wallRows
        val cx = Iso.CF.x + (p.gx + w / 2f) * iso.ex.x + (fy + h / 2f) * iso.ey.x
        val botY = Iso.CF.y + (p.gx + w) * iso.ex.y + (fy + h) * iso.ey.y
        val size = (w + h) * cellW() * 0.8f * p.scale
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
                        // zoom/pan layer: frame + clip stay fixed, the room
                        // content scales inside. Pointer positions reported
                        // inside are unscaled — multiply by `zoom` where they
                        // are converted to window coords.
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = pan.x
                            translationY = pan.y
                        }
                        .onSizeChanged { roomSize = it }
                        .onGloballyPositioned { roomBounds = it.boundsInWindow() }
                        // sticky-move mode (from the item menu): the picked-up
                        // item follows a drag on the canvas, or a tap drops it.
                        .pointerInput(stickyMove) {
                            if (!stickyMove) return@pointerInput
                            detectTapGestures { local ->
                                val d = drag ?: return@detectTapGestures
                                snapToCell(roomBounds.topLeft + local * zoom, d.itemId, d.rot)
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
                                        pos = roomBounds.topLeft + change.position * zoom,
                                    )
                                },
                                onDragEnd = ::drop,
                                onDragCancel = {
                                    drag = null
                                    stickyMove = false
                                },
                            )
                        }
                        // pinch to zoom, drag empty space to pan; item drags
                        // are consumed by the tile child handlers first.
                        .pointerInput(Unit) {
                            detectTransformGestures { _, panDelta, zoomDelta, _ ->
                                val ns = (zoom * zoomDelta).coerceIn(1f, 3f)
                                zoom = ns
                                val mx = roomSize.width * (ns - 1f) / 2f + roomSize.width * 0.18f
                                val my = roomSize.height * (ns - 1f) / 2f + roomSize.height * 0.18f
                                pan = if (ns <= 1f) {
                                    Offset.Zero
                                } else {
                                    Offset(
                                        (pan.x + panDelta.x * ns).coerceIn(-mx, mx),
                                        (pan.y + panDelta.y * ns).coerceIn(-my, my),
                                    )
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (zoom > 1.05f) {
                                        zoom = 1f
                                        pan = Offset.Zero
                                    } else {
                                        zoom = 1.75f
                                    }
                                },
                            )
                        },
                ) {
                    val density = LocalDensity.current
                    RoomShell(game.decor, roomId)
                    // ghost preview, clipped to the room quads
                    Canvas(Modifier.fillMaxSize()) {
                        val cw = size.width
                        val ch = size.height
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
                                zoom = zoom,
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
                // undo/redo: round buttons floating at the room's bottom-left,
                // outside the zoom layer (reference layout)
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RoundBtn("↶", canUndo, onUndo)
                    RoundBtn("↷", canRedo, onRedo)
                }
                // selected-item menu: pill floating on the room's bottom
                // edge instead of a panel replacing the tray
                val menuPlaced = menuUid?.let { id -> room.placed.firstOrNull { it.uid == id } }
                if (menuPlaced != null) {
                    ItemMenu(
                        p = menuPlaced,
                        game = game,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        onSetVariant = { onSetVariant(menuPlaced.uid, it) },
                        onScale = { onScale(menuPlaced.uid, it) },
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
                                roomBounds.topLeft + Offset(bo.x + bs / 2, bo.y + bs * 0.8f) * zoom,
                                menuPlaced.rot, menuPlaced.flip,
                            )
                        },
                        onClose = { menuUid = null },
                    )
                }
            }

            // ---- bottom: catalog tray ----
            run {
                CatalogTray(
                    game = game,
                    roomId = roomId,
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
                    rot = d.rot, flip = d.flip,
                )
            }
        }
    }
}

/** Tray tab icons, keyed by category id (reference-style icon chips). */
private val CATEGORY_ICONS = mapOf(
    "seating" to "🪑",
    "bed" to "🛏️",
    "table" to "🗄️",
    "decor" to "🎀",
    "fun" to "🐱",
    "wall" to "🖼️",
    "room" to "🏠",
)

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

/** Procedural isometric room shell: painted sky behind, both wall quads
 *  filled with tint + a seamless pattern tile (wainscot band on `panel`
 *  styles), floor diamond plus a front skirt for slab thickness, and dark
 *  outlines along the shell edges. Fully recolorable — the saved variant
 *  picks both the pattern (via its item's `tex`) and the tint. */
@Composable
private fun RoomShell(decor: DecorState, roomId: String) {
    val def = DecorCatalog.room(roomId)
    val room = DecorEngine.roomOf(decor, roomId)
    val iso = remember(def) { IsoMap(def.cols, def.floorRows, def.wallRows) }
    val wallVL = DecorEngine.wallVariant(room, def, 1) // left wall
    val wallVR = DecorEngine.wallVariant(room, def, 0) // right wall
    val floorV = DecorEngine.floorVariant(room, def)
    val skyV = DecorEngine.skyVariant(room, def)
    val wallItemL = DecorCatalog.itemOfVariant(wallVL.id)
    val wallItemR = DecorCatalog.itemOfVariant(wallVR.id)
    val floorItem = DecorCatalog.itemOfVariant(floorV.id)
    val skyType = DecorCatalog.itemOfVariant(skyV.id).tex ?: "day"
    val wallTexL = rememberTex(wallItemL.tex?.takeIf { wallItemL.layer == DecorLayer.WALL_PAINT })
    val wallTexR = rememberTex(wallItemR.tex?.takeIf { wallItemR.layer == DecorLayer.WALL_PAINT })
    val floorTex = rememberTex(floorItem.tex?.takeIf { floorItem.layer == DecorLayer.FLOOR_PAINT })
    val panelTex = rememberTex("wtex_panel")
    val floorTint = Color(floorV.tint)
    Canvas(Modifier.fillMaxSize()) {
        val cw = size.width
        val ch = size.height
        drawSky(skyType)
        // floor thickness skirt under the two front edges
        val dy = 0.052f
        drawPath(
            iso.path(
                listOf(Iso.FL, Iso.FB, Iso.FB + Offset(0f, dy), Iso.FL + Offset(0f, dy)),
                cw, ch,
            ),
            shade(floorTint, 0.74f),
        )
        drawPath(
            iso.path(
                listOf(Iso.FB, Iso.FR, Iso.FR + Offset(0f, dy), Iso.FB + Offset(0f, dy)),
                cw, ch,
            ),
            shade(floorTint, 0.62f),
        )
        // walls, then the floor face on top of the skirt seam
        drawWall(iso, 1, Color(wallVL.tint), wallTexL, if (wallItemL.panel) panelTex else null)
        drawWall(iso, 0, Color(wallVR.tint), wallTexR, if (wallItemR.panel) panelTex else null)
        val floor = iso.path(iso.floorPath(), cw, ch)
        drawPath(floor, floorTint)
        floorTex?.let {
            tileQuad(
                clip = floor,
                bmp = it,
                uMax = def.cols.toFloat(),
                vMax = def.floorRows.toFloat(),
                uStep = 3f,
                vStep = 3f,
                corner = { u, v -> iso.cellOrigin(u, v) },
            )
        }
        drawPath(
            floor,
            Brush.verticalGradient(
                listOf(Color(0x17000000), Color(0x00000000)),
                startY = Iso.CF.y * ch, endY = Iso.FB.y * ch,
            ),
        )
        drawEdges(iso, dy)
    }
}

private fun shade(c: Color, f: Float) = Color(c.red * f, c.green * f, c.blue * f)

private fun skyStops(type: String): List<Color> = when (type) {
    "sunset" -> listOf(Color(0xFFF0B39B), Color(0xFFF9D6C6), Color(0xFFFBE9DC))
    "night" -> listOf(Color(0xFF2B3660), Color(0xFF4B4378), Color(0xFF6A5C95))
    "garden" -> listOf(
        Color(0xFF9FCFEF), Color(0xFFC8E4F8), Color(0xFFA9D98E), Color(0xFF8CC473),
    )
    else -> listOf(Color(0xFF9FCFEF), Color(0xFFC8E4F8), Color(0xFFE4F3FC))
}

/** Tile [bmp] across a quad grid: each grid cell gets the bitmap
 *  perspective-mapped onto its own sub-quad via poly-to-poly, so the
 *  pattern follows the surface direction (iso floor diamond, sloped
 *  walls) instead of lying axis-aligned on screen. [corner] maps
 *  texture space (u → image x, v → image y) to canvas fractions; tiles
 *  overrunning [uMax]/[vMax] are clipped, never squashed. */
private fun DrawScope.tileQuad(
    clip: Path,
    bmp: ImageBitmap,
    uMax: Float,
    vMax: Float,
    uStep: Float,
    vStep: Float,
    corner: (u: Float, v: Float) -> Offset,
) {
    val cw = size.width
    val ch = size.height
    val ab = bmp.asAndroidBitmap()
    val src = floatArrayOf(
        0f, 0f, ab.width.toFloat(), 0f,
        ab.width.toFloat(), ab.height.toFloat(), 0f, ab.height.toFloat(),
    )
    val dst = FloatArray(8)
    val m = android.graphics.Matrix()
    val p = android.graphics.Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    clipPath(clip) {
        var v = 0f
        while (v < vMax) {
            var u = 0f
            while (u < uMax) {
                val tl = corner(u, v)
                val tr = corner(u + uStep, v)
                val br = corner(u + uStep, v + vStep)
                val bl = corner(u, v + vStep)
                dst[0] = tl.x * cw; dst[1] = tl.y * ch
                dst[2] = tr.x * cw; dst[3] = tr.y * ch
                dst[4] = br.x * cw; dst[5] = br.y * ch
                dst[6] = bl.x * cw; dst[7] = bl.y * ch
                m.setPolyToPoly(src, 0, dst, 0, 4)
                drawContext.canvas.nativeCanvas.drawBitmap(ab, m, p)
                u += uStep
            }
            v += vStep
        }
    }
}

private fun DrawScope.drawWall(
    iso: IsoMap,
    side: Int,
    tint: Color,
    tex: ImageBitmap?,
    panelTex: ImageBitmap?,
) {
    val cw = size.width
    val ch = size.height
    val wall = iso.path(iso.wallPath(side), cw, ch)
    drawPath(wall, tint)
    tex?.let {
        tileQuad(
            clip = wall,
            bmp = it,
            uMax = iso.wallLen(side).toFloat(),
            vMax = 1f,
            uStep = 2.5f,
            vStep = 0.3f,
            corner = { u, v -> iso.wallPoint(side, u, 1f - v) },
        )
    }
    // ambient shadow deepening toward the floor line
    drawPath(
        wall,
        Brush.verticalGradient(
            listOf(Color(0x00000000), Color(0x15000000)),
            startY = Iso.CT.y * ch, endY = Iso.CF.y * ch,
        ),
    )
    if (panelTex != null) {
        val hTop = 0.42f
        val band = iso.path(iso.wallBandPath(side, hTop), cw, ch)
        drawPath(band, shade(tint, 1.06f).let { Color(it.red, it.green, it.blue).lerpToWhite(0.55f) })
        tileQuad(
            clip = band,
            bmp = panelTex,
            uMax = iso.wallLen(side).toFloat(),
            vMax = hTop,
            uStep = 1.5f,
            vStep = hTop,
            corner = { u, v -> iso.wallPoint(side, u, hTop - v) },
        )
        val (a, b) = iso.wallRail(side, hTop)
        drawLine(
            Color(0xFF8A6A55).copy(alpha = 0.6f),
            Offset(a.x * cw, a.y * ch), Offset(b.x * cw, b.y * ch),
            strokeWidth = cw * 0.0035f,
        )
    }
}

private fun Color.lerpToWhite(f: Float) = Color(
    red + (1f - red) * f,
    green + (1f - green) * f,
    blue + (1f - blue) * f,
)

private fun DrawScope.cloud(cx: Float, cy: Float, r: Float, color: Color) {
    drawRect(color, Offset(cx - r * 1.6f, cy + r * 0.15f), Size(r * 3.2f, r * 0.75f))
    drawCircle(color, r * 0.72f, Offset(cx - r * 0.85f, cy + r * 0.2f))
    drawCircle(color, r, Offset(cx, cy))
    drawCircle(color, r * 0.68f, Offset(cx + r * 0.9f, cy + r * 0.3f))
}

private fun DrawScope.drawSky(type: String) {
    val cw = size.width
    val ch = size.height
    if (type == "garden") {
        drawGarden()
        return
    }
    drawRect(Brush.verticalGradient(skyStops(type)))
    when (type) {
        "night" -> {
            // stars (deterministic scatter) + pale moon with craters
            val rnd = kotlin.random.Random(7)
            repeat(42) {
                drawCircle(
                    Color.White.copy(alpha = 0.5f + rnd.nextFloat() * 0.4f),
                    cw * (0.003f + rnd.nextFloat() * 0.004f),
                    Offset(rnd.nextFloat() * cw, rnd.nextFloat() * ch),
                )
            }
            val mc = Offset(cw * 0.76f, ch * 0.11f)
            drawCircle(Color(0xFFF6ECD0), cw * 0.045f, mc)
            drawCircle(Color(0xFFE3D6B8), cw * 0.011f, mc + Offset(cw * 0.012f, -cw * 0.008f))
            drawCircle(Color(0xFFE3D6B8), cw * 0.008f, mc + Offset(-cw * 0.014f, cw * 0.006f))
            cloud(cw * 0.18f, ch * 0.16f, cw * 0.04f, Color(0xFF8F86B8).copy(alpha = 0.7f))
            cloud(cw * 0.55f, ch * 0.05f, cw * 0.05f, Color(0xFF8F86B8).copy(alpha = 0.7f))
        }
        "sunset" -> {
            drawCircle(Color(0xFFFFB27D), cw * 0.05f, Offset(cw * 0.20f, ch * 0.30f))
            cloud(cw * 0.22f, ch * 0.10f, cw * 0.055f, Color(0xFFF9DCCF).copy(alpha = 0.9f))
            cloud(cw * 0.65f, ch * 0.05f, cw * 0.045f, Color(0xFFF9DCCF).copy(alpha = 0.9f))
            cloud(cw * 0.88f, ch * 0.22f, cw * 0.05f, Color(0xFFF9DCCF).copy(alpha = 0.9f))
        }
        else -> {
            val sc = Offset(cw * 0.80f, ch * 0.09f)
            drawCircle(Color(0xFFFFE9A8), cw * 0.048f, sc)
            drawCircle(Color(0xFFFFF6D8), cw * 0.030f, sc)
            cloud(cw * 0.20f, ch * 0.10f, cw * 0.055f, Color.White.copy(alpha = 0.95f))
            cloud(cw * 0.58f, ch * 0.05f, cw * 0.042f, Color.White.copy(alpha = 0.95f))
            cloud(cw * 0.90f, ch * 0.20f, cw * 0.048f, Color.White.copy(alpha = 0.95f))
        }
    }
}

/** Garden scenery behind the room (reference look): sky + clouds peeking
 *  over a grass field, white picket fence, trees at the top corners,
 *  bushes, stepping stones and scattered daisies. */
private fun DrawScope.drawGarden() {
    val cw = size.width
    val ch = size.height
    // sky band behind everything
    drawRect(Brush.verticalGradient(listOf(Color(0xFF9FCFEF), Color(0xFFD4ECFA))))
    cloud(cw * 0.30f, ch * 0.06f, cw * 0.05f, Color.White.copy(alpha = 0.95f))
    cloud(cw * 0.66f, ch * 0.10f, cw * 0.04f, Color.White.copy(alpha = 0.9f))
    // grass field
    val grassTop = ch * 0.24f
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFFA9D98E), Color(0xFF8CC473))),
        Offset(0f, grassTop),
        Size(cw, ch - grassTop),
    )
    // white picket fence: two rails behind pointed pickets
    val fenceTop = ch * 0.28f
    val picketH = ch * 0.085f
    val picketW = cw * 0.013f
    val picketStep = cw * 0.028f
    val fence = Color(0xFFFAF8F0)
    val fenceShade = Color(0xFFE8E2D4)
    drawRect(fenceShade, Offset(0f, fenceTop + picketH * 0.20f), Size(cw, picketH * 0.13f))
    drawRect(fenceShade, Offset(0f, fenceTop + picketH * 0.64f), Size(cw, picketH * 0.13f))
    var fx = -picketW
    while (fx < cw) {
        drawPath(
            Path().apply {
                moveTo(fx, fenceTop + picketH)
                lineTo(fx, fenceTop + picketH * 0.22f)
                lineTo(fx + picketW / 2f, fenceTop)
                lineTo(fx + picketW, fenceTop + picketH * 0.22f)
                lineTo(fx + picketW, fenceTop + picketH)
                close()
            },
            fence,
        )
        fx += picketStep
    }
    // trees at the top corners
    gardenTree(cw * 0.07f, ch * 0.13f, cw * 0.085f)
    gardenTree(cw * 0.94f, ch * 0.10f, cw * 0.075f)
    gardenTree(cw * 0.88f, ch * 0.24f, cw * 0.05f)
    // bushes along the grass edges
    gardenBush(cw * 0.04f, ch * 0.42f, cw * 0.055f)
    gardenBush(cw * 0.96f, ch * 0.44f, cw * 0.055f)
    gardenBush(cw * 0.07f, ch * 0.95f, cw * 0.06f)
    gardenBush(cw * 0.93f, ch * 0.95f, cw * 0.06f)
    // stepping stones, lower right
    val stone = Color(0xFFE3DECE)
    drawOval(stone, Offset(cw * 0.86f, ch * 0.60f), Size(cw * 0.045f, cw * 0.028f))
    drawOval(stone, Offset(cw * 0.90f, ch * 0.72f), Size(cw * 0.05f, cw * 0.030f))
    drawOval(stone, Offset(cw * 0.87f, ch * 0.85f), Size(cw * 0.048f, cw * 0.029f))
    // daisies + grass speckles (deterministic scatter)
    val rnd = kotlin.random.Random(11)
    val petals = listOf(Color.White, Color(0xFFF7B8CE), Color(0xFFF8E08A))
    repeat(46) {
        val x = rnd.nextFloat() * cw
        val y = grassTop + (ch - grassTop) * (0.12f + rnd.nextFloat() * 0.85f)
        if (rnd.nextFloat() < 0.4f) {
            drawCircle(Color(0xFF7AB464), cw * 0.006f, Offset(x, y))
        } else {
            val petal = petals[rnd.nextInt(petals.size)]
            drawCircle(petal, cw * 0.006f, Offset(x, y))
            drawCircle(Color(0xFFF2C94C), cw * 0.0026f, Offset(x, y))
        }
    }
}

private fun DrawScope.gardenTree(cx: Float, cy: Float, r: Float) {
    drawRect(Color(0xFF8A6A55), Offset(cx - r * 0.11f, cy + r * 0.4f), Size(r * 0.22f, r * 1.2f))
    drawCircle(Color(0xFF6FB35B), r, Offset(cx, cy))
    drawCircle(Color(0xFF7CC268), r * 0.68f, Offset(cx - r * 0.72f, cy + r * 0.45f))
    drawCircle(Color(0xFF7CC268), r * 0.65f, Offset(cx + r * 0.7f, cy + r * 0.5f))
    drawCircle(Color(0xFF8BC97A), r * 0.38f, Offset(cx - r * 0.22f, cy - r * 0.32f))
}

private fun DrawScope.gardenBush(cx: Float, cy: Float, r: Float) {
    drawCircle(Color(0xFF7BBD64), r, Offset(cx - r * 0.6f, cy))
    drawCircle(Color(0xFF8BC97A), r * 1.15f, Offset(cx, cy - r * 0.25f))
    drawCircle(Color(0xFF7BBD64), r * 0.9f, Offset(cx + r * 0.6f, cy))
}

private fun DrawScope.drawEdges(iso: IsoMap, dy: Float) {
    val cw = size.width
    val ch = size.height
    val ink = Color(0xFF7A5A48).copy(alpha = 0.55f)
    val w = cw * 0.0028f
    fun p(o: Offset) = Offset(o.x * cw, o.y * ch)
    // wall top edges + verticals + the corner seam
    drawLine(ink, p(Iso.LT), p(Iso.CT), w)
    drawLine(ink, p(Iso.CT), p(Iso.RT), w)
    drawLine(ink, p(Iso.CT), p(Iso.CF), w)
    drawLine(ink, p(Iso.LT), p(Iso.FL), w)
    drawLine(ink, p(Iso.RT), p(Iso.FR), w)
    // wall/floor seams + floor outline + skirt bottom
    drawLine(ink, p(Iso.CF), p(Iso.FL), w * 0.8f)
    drawLine(ink, p(Iso.CF), p(Iso.FR), w * 0.8f)
    drawLine(ink, p(Iso.FL), p(Iso.FB), w)
    drawLine(ink, p(Iso.FB), p(Iso.FR), w)
    drawLine(ink, p(Iso.FL + Offset(0f, dy)), p(Iso.FB + Offset(0f, dy)), w * 0.8f)
    drawLine(ink, p(Iso.FB + Offset(0f, dy)), p(Iso.FR + Offset(0f, dy)), w * 0.8f)
}

@Composable
private fun rememberTex(name: String?): ImageBitmap? {
    val ctx = LocalContext.current
    return remember(name) {
        name?.let { artRes(ctx, it) }
            ?.takeIf { it != 0 }
            ?.let { BitmapFactory.decodeResource(ctx.resources, it).asImageBitmap() }
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
    flip: Boolean = false,
) {
    val ctx = LocalContext.current
    val res = remember(itemId) { artRes(ctx, "decor_$itemId") }
    // rotation + mirror live on the sprite only — transforming the
    // gesture container would mirror/rotate pointer coordinates too
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
                    scaleX = if (flip) -1f else 1f
                    rotationZ = if (DecorCatalog[itemId].topDown) rot.toFloat() else 0f
                },
        )
    } else {
        Text(
            emoji,
            fontSize = (size.value * 0.6f).sp,
            modifier = modifier.alpha(alpha)
                .graphicsLayer {
                    scaleX = if (flip) -1f else 1f
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
    zoom: Float,
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
            .pointerInput(p.uid, interactive) {
                if (!interactive) return@pointerInput
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(p.uid, interactive, zoom) {
                if (!interactive) return@pointerInput
                detectDragGestures(
                    onDragStart = { start -> onPickup(winPos + start * zoom) },
                    onDrag = { change, _ -> onDragPos(winPos + change.position * zoom) },
                    onDragEnd = onDrop,
                    onDragCancel = onDrop,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        DecorArt(
            p.itemId, variant.tint, with(density) { sizePx.toDp() }, item.emoji,
            rot = p.rot, flip = p.flip,
        )
    }
}

/** Tap-menu for a placed item: floating pill on the room's bottom edge —
 *  action buttons (icon over label), then a scrollable variant row with
 *  the size presets appended. */
@Composable
private fun ItemMenu(
    p: PlacedDecor,
    game: GameState,
    onSetVariant: (String) -> Unit,
    onScale: (Float) -> Unit,
    onRotate: () -> Unit,
    onRemove: () -> Unit,
    onMoveMode: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = DecorCatalog[p.itemId]
    Column(
        modifier
            .widthIn(max = 340.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Color(0x33A0522D), RoundedCornerShape(18.dp))
            .padding(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MenuIconBtn("✋", "Di chuyển", onMoveMode)
            // Xoay: rugs rotate for real; oblique art mirrors — same button
            if (item.canRotate || item.canFlip) {
                MenuIconBtn("🔄", "Xoay", onRotate)
            }
            MenuIconBtn("📦", "Cất vào kho", onRemove)
            MenuIconBtn("✕", "Đóng", onClose)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
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
            // sprite scale stepper — footprint on the grid stays the same.
            // Steps anchor to the 1+k*0.15 grid so ± always returns to 100%.
            val k = ((p.scale - 1f) / 0.15f).roundToInt()
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScaleBtn("➖", k > -3) { onScale(1f + (k - 1) * 0.15f) }
                Text(
                    "${(p.scale * 100).roundToInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Cocoa,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                ScaleBtn("➕", k < 3) { onScale(1f + (k + 1) * 0.15f) }
            }
        }
    }
}

/** Round white icon button (undo/redo under the room). */
@Composable
private fun RoundBtn(icon: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        icon,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = if (enabled) Cocoa else Cocoa.copy(alpha = 0.3f),
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Color(0x33A0522D), CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .wrapContentSize(Alignment.Center),
    )
}

/** Small +/- stepper button in the item menu. */
@Composable
private fun ScaleBtn(icon: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        icon,
        fontSize = 13.sp,
        color = if (enabled) Cocoa else Cocoa.copy(alpha = 0.3f),
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color(0xFFFFE0B2))
            .clickable(enabled = enabled, onClick = onClick)
            .wrapContentSize(Alignment.Center),
    )
}

/** Pill-menu button: emoji icon stacked over a small label. */
@Composable
private fun MenuIconBtn(icon: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(icon, fontSize = 17.sp)
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Cocoa)
    }
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

/** Bottom tray: category tabs + draggable item cards (or the room-shell
 *  style picker on the "room" tab). */
@Composable
private fun CatalogTray(
    game: GameState,
    roomId: String,
    tab: String,
    onTab: (String) -> Unit,
    pickedVariant: Map<String, String>,
    onPickVariant: (String, String) -> Unit,
    onPaint: (String, Int?) -> Unit,
    onDragStart: (DecorItem, Offset) -> Unit,
    onDragPos: (Offset) -> Unit,
    onDrop: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.85f))
            .padding(8.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DecorCatalog.categories.forEach { (id, label) ->
                val sel = tab == id
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) Color(0xFFFADADD) else Color.White)
                        .border(1.dp, Color(0x33A0522D), RoundedCornerShape(12.dp))
                        .clickable { onTab(id) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(CATEGORY_ICONS[id] ?: "🧺", fontSize = 15.sp)
                    Text(
                        label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Cocoa,
                    )
                }
            }
        }
        if (tab == "room") {
            RoomStyleTray(
                game = game,
                roomId = roomId,
                pickedVariant = pickedVariant,
                onPickVariant = onPickVariant,
                onPaint = onPaint,
            )
            return@Column
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

/** Room-shell style picker (reference UI): one row of pattern tiles per
 *  surface (Tường / Sàn / Trời); the color dots under a section recolor
 *  whichever pattern is applied there. Tapping applies instantly via
 *  [onPaint] — unowned variants charge their price through setPaint. */
@Composable
private fun RoomStyleTray(
    game: GameState,
    roomId: String,
    pickedVariant: Map<String, String>,
    onPickVariant: (String, String) -> Unit,
    onPaint: (String, Int?) -> Unit,
) {
    val def = DecorCatalog.room(roomId)
    val room = DecorEngine.roomOf(game.decor, roomId)
    // wall target: -1 = both walls, 0 = right wall, 1 = left wall
    var wallSide by rememberSaveable { mutableIntStateOf(-1) }
    val wallVL = DecorEngine.wallVariant(room, def, 1)
    val wallVR = DecorEngine.wallVariant(room, def, 0)
    val sections = listOf(
        Triple("Tường", DecorLayer.WALL_PAINT, null as DecorVariant?),
        Triple("Sàn", DecorLayer.FLOOR_PAINT, DecorEngine.floorVariant(room, def)),
        Triple("Trời", DecorLayer.SKY_PAINT, DecorEngine.skyVariant(room, def)),
    )
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        sections.forEach { (label, layer, sectionV) ->
            val isWall = layer == DecorLayer.WALL_PAINT
            val appliedV = if (isWall) when (wallSide) {
                0 -> wallVR
                else -> wallVL
            } else sectionV!!
            val appliedItem = DecorCatalog.itemOfVariant(appliedV.id)
            val appliedItemR = DecorCatalog.itemOfVariant(wallVR.id)
            val appliedSuffix = appliedV.id.removePrefix("${appliedItem.id}_")
            val items = DecorCatalog.inCategory("room").filter { it.layer == layer }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Cocoa.copy(alpha = 0.65f),
                )
                if (isWall) {
                    listOf("Trái" to 1, "Cả 2" to -1, "Phải" to 0).forEach { (sideLabel, side) ->
                        Text(
                            sideLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (wallSide == side) Color.White else Cocoa.copy(alpha = 0.7f),
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (wallSide == side) Cocoa else Color(0x146D4C41),
                                )
                                .clickable { wallSide = side }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items.forEach { item ->
                    // keep the same color suffix when switching patterns —
                    // the variant the card would apply on tap
                    val targetV = pickedVariant[item.id]?.let { item.variantOf(it) }
                        ?: item.variants.firstOrNull {
                            it.id.removePrefix("${item.id}_") == appliedSuffix
                        }
                        ?: item.variants.first()
                    val applied = if (isWall && wallSide < 0) {
                        item.id == appliedItem.id && item.id == appliedItemR.id
                    } else {
                        item.id == appliedItem.id
                    }
                    SurfaceCard(
                        item = item,
                        variant = targetV,
                        game = game,
                        applied = applied,
                        onClick = {
                            onPaint(targetV.id, wallSide.takeIf { isWall && it >= 0 })
                        },
                    )
                }
            }
            if (items.isNotEmpty() && appliedItem.category == "room") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    appliedItem.variants.forEach { v ->
                        val locked = v.unlockLevel > game.level
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (locked) Color(0xFFBBBBBB) else Color(v.tint))
                                .border(
                                    2.dp,
                                    if (v.id == appliedV.id) Gold else Color(0x336D4C41),
                                    CircleShape,
                                )
                                .clickable {
                                    onPickVariant(appliedItem.id, v.id)
                                    onPaint(v.id, wallSide.takeIf { isWall && it >= 0 })
                                },
                        )
                    }
                }
            }
        }
    }
}

/** One pattern/sky tile in the room picker: texture or gradient preview
 *  over the variant tint, name + price/status underneath. */
@Composable
private fun SurfaceCard(
    item: DecorItem,
    variant: DecorVariant,
    game: GameState,
    applied: Boolean,
    onClick: () -> Unit,
) {
    val ctx = LocalContext.current
    val locked = variant.unlockLevel > game.level
    val owned = DecorCatalog.isOwned(game.decor, variant.id)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(
                if (applied) 2.dp else 1.dp,
                if (applied) Gold else Color(0x33A0522D),
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(6.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (item.layer == DecorLayer.SKY_PAINT) {
                        Modifier.background(Brush.verticalGradient(skyStops(item.tex ?: "day")))
                    } else {
                        Modifier.background(Color(variant.tint))
                    },
                ),
        ) {
            item.tex?.takeIf { item.layer != DecorLayer.SKY_PAINT }
                ?.let { artRes(ctx, it) }
                ?.takeIf { it != 0 }
                ?.let { res ->
                    Image(
                        painter = painterResource(res),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            if (locked) {
                Text(
                    "🔒",
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                )
            }
        }
        Text(item.name, fontSize = 9.sp, color = Cocoa, fontWeight = FontWeight.Bold)
        Text(
            when {
                applied -> "Đang dùng"
                owned -> "✓"
                locked -> "Lv${variant.unlockLevel}"
                else -> "🪙${variant.priceCoins}"
            },
            fontSize = 8.sp,
            color = when {
                applied || owned -> MergeGreen
                locked -> WarnRed
                else -> Cocoa
            },
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CatalogCard(
    item: DecorItem,
    game: GameState,
    variant: DecorVariant,
    onPickVariant: (String) -> Unit,
    onPaint: (String, Int?) -> Unit,
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
                    Modifier.clickable { onPaint(variant.id, null) }
                } else {
                    Modifier.pointerInput(item.id) {
                        detectDragGestures(
                            onDragStart = { start -> onDragStart(item, origin + start) },
                            onDrag = { change, _ -> onDragPos(origin + change.position) },
                            onDragEnd = onDrop,
                            onDragCancel = onDrop,
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
