package com.bepngon.merge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.drawToBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.bepngon.merge.game.DecorCatalog
import com.bepngon.merge.game.DecorEngine
import com.bepngon.merge.game.GameEngine
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
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where a decor drag came from: catalog uid = 0, a placed item's uid otherwise. */
private data class DecorDrag(
    val itemId: String,
    val variantId: String,
    val uid: Long,
    val pos: Offset, // window coords
    val rot: Int,
    val flip: Boolean,
)

/** Isometric corner-room landmarks, in canvas fractions. The floor
 *  diamond spans 90% of the width (5% margins both sides), with a sky
 *  band above the walls and a ground band below the floor vertex. */
private object Iso {
    const val ASPECT = 1.15f
    val CT = Offset(0.5f, 0.07f) // top of the corner (ceiling)
    val CF = Offset(0.5f, 0.35f) // corner where walls meet the floor
    val FL = Offset(0.05f, 0.62f) // floor diamond: left vertex
    val FR = Offset(0.95f, 0.62f) // floor diamond: right vertex
    val FB = Offset(0.5f, 0.89f) // floor diamond: bottom vertex
    val LT = Offset(0.05f, 0.36f) // left wall outer-top (above FL)
    val RT = Offset(0.95f, 0.36f) // right wall outer-top (above FR)
}

/** Largest ASPECT-ratio rect centered inside a (possibly taller) canvas.
 *  The iso room keeps its proportions and is drawn inside this rect; the
 *  rest of the canvas is extra sky/ground around it. */
private fun Size.roomRect(): Rect {
    val s = min(width, height * Iso.ASPECT)
    val w = s
    val h = s / Iso.ASPECT
    return Rect((width - w) / 2f, (height - h) / 2f, (width + w) / 2f, (height + h) / 2f)
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

    fun path(pts: List<Offset>, r: Rect): Path = Path().apply {
        pts.forEachIndexed { i, o ->
            val p = Offset(r.left + o.x * r.width, r.top + o.y * r.height)
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }

    fun p(o: Offset, r: Rect) = Offset(r.left + o.x * r.width, r.top + o.y * r.height)

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

/** Cornerlet-style design tokens: warm paper cards with a soft line
 *  border, pink accents for selection and primary actions. */
private val Paper = Color(0xFFFFF7E8)
private val LineC = Color(0xFFE8CFC4)
private val Accent = Color(0xFFE0708F)
private val PinkSel = Color(0xFFF6B3C4)
private val ToastBg = Color(0xFF5A3A3A)

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
    onReorder: (uid: Long, front: Boolean) -> Unit,
    onCycleVariant: (uid: Long) -> Unit,
    onAutoPlace: (itemId: String, variantId: String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClose: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val def = DecorCatalog.room(roomId)
    val room = DecorEngine.roomOf(game.decor, roomId)
    val ctx = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var drag by remember { mutableStateOf<DecorDrag?>(null) }
    var menuUid by remember { mutableStateOf<Long?>(null) }
    var stickyMove by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(DecorCatalog.categories.first().first) }
    val pickedVariant = remember { mutableStateMapOf<String, String>() }
    var roomBounds by remember { mutableStateOf(Rect.Zero) }
    var roomSize by remember { mutableStateOf(IntSize.Zero) }
    var toast by remember { mutableStateOf<String?>(null) }
    var resizePreview by remember { mutableStateOf<Pair<Long, Float>?>(null) }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(1800)
            toast = null
        }
    }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var contentOrigin by remember { mutableStateOf(Offset.Zero) }
    // hides floating overlays for one frame while a room photo is captured
    var photoMode by remember { mutableStateOf(false) }

    val iso = remember(def) { IsoMap(def.cols, def.floorRows, def.wallRows) }

    fun variantOf(item: DecorItem): DecorVariant =
        item.variantOf(pickedVariant[item.id] ?: item.variants.first().id)

    // Function, not a val: pointerInput closures capture `drop`/`snapToCell`
    // once, so sizes must re-read roomBounds every call (0 until measured).
    fun cellW(): Float {
        val rr = roomSize.toSize().roomRect()
        return iso.ex.x * rr.width
    }

    /** Window-space bounds of the iso room rect (canvas ⊃ room since the
     *  canvas now bleeds sky/ground top and bottom). */
    fun roomWin(): Rect {
        val rr = roomSize.toSize().roomRect()
        return Rect(
            roomBounds.topLeft + Offset(rr.left, rr.top) * zoom,
            Size(rr.width, rr.height) * zoom,
        )
    }

    /** Snap a window position to a grid cell: (gx, gy, rot). For wall
     *  items the wall plane under the pointer picks the side in rot. */
    fun snapToCell(pos: Offset, itemId: String, rot: Int): Triple<Int, Int, Int>? {
        val win = roomWin()
        if (win.width <= 0 || !win.inflate(win.width).contains(pos)) return null
        val item = DecorCatalog[itemId]
        val (w, h) = DecorEngine.footprint(item, rot)
        val local = Offset(
            (pos.x - win.left) / win.width,
            (pos.y - win.top) / win.height,
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
        val rr = roomSize.toSize().roomRect()
        if (item.placement == Placement.WALL) {
            val side = if (p.rot == 90) 1 else 0
            val pt = iso.wallPoint(side, p.gx + w / 2f, (def.wallRows - p.gy - h / 2f) / def.wallRows)
            val size = w * cellW() * 1.45f * p.scale
            val bp = iso.p(pt, rr)
            return Offset(bp.x - size / 2, bp.y - size * 0.55f) to size
        }
        val fy = p.gy - def.wallRows
        val cx = Iso.CF.x + (p.gx + w / 2f) * iso.ex.x + (fy + h / 2f) * iso.ey.x
        val botY = Iso.CF.y + (p.gx + w) * iso.ex.y + (fy + h) * iso.ey.y
        val size = (w + h) * cellW() * 0.8f * p.scale
        val bp = Offset(rr.left + cx * rr.width, rr.top + botY * rr.height)
        return Offset(bp.x - size / 2, bp.y - size * 0.92f) to size
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
                DecorEngine.canPlace(game.decor, roomId, d.itemId, gx, gy, rot),
                pts,
            )
        }
    }

    fun drop() {
        val d = drag ?: return
        when {
            roomWin().contains(d.pos) -> {
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
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            // ---- HUD: avatar + level/xp, currency pills, close ----
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3DEDE))
                        .border(2.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("🐱", fontSize = 26.sp) }
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(
                        "Lv.${game.level}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Cocoa,
                    )
                    val xpNeed = GameEngine.xpNeededForLevel(game.level).toFloat()
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 3.dp)
                            .height(11.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Cocoa.copy(alpha = 0.18f))
                            .border(1.5.dp, LineC, RoundedCornerShape(6.dp)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((game.xp / xpNeed).coerceIn(0f, 1f))
                                .height(11.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(PinkSel),
                        )
                        Text(
                            "${game.xp} / ${xpNeed.toInt()}",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Cocoa,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
                HudPill("🪙", "${game.coins}")
                HudPill("⚡", "${game.energy}/${GameEngine.ENERGY_CAP}")
                Text(
                    "✕",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cocoa,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Paper)
                        .border(2.dp, LineC, CircleShape)
                        .clickable(onClick = onClose)
                        .wrapContentSize(Alignment.Center),
                )
            }

            // ---- room canvas (isometric corner room) ----
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxSize()
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
                                                gx, gy, rot,
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
                                // empty-space tap deselects the item (tile
                                // taps are consumed by the tiles first)
                                onTap = { menuUid = null },
                            )
                        },
                ) {
                    val density = LocalDensity.current
                    RoomShell(game.decor, roomId)
                    // ghost preview, clipped to the room quads
                    Canvas(Modifier.fillMaxSize()) {
                        val rr = size.roomRect()
                        ghost?.let { (_, ok, pts) ->
                            drawPath(
                                iso.path(pts, rr),
                                if (ok) Color(0x8866BB6A) else Color(0x88EF5350),
                            )
                        }
                    }
                    // placed items, back-to-front
                    room.placed.sortedBy { DecorEngine.sortKey(it) }.forEach { p ->
                        key(p.uid) {
                            val pv = if (resizePreview?.first == p.uid) {
                                p.copy(scale = resizePreview!!.second)
                            } else {
                                p
                            }
                            val (origin, sizePx) = itemBox(pv)
                            PlacedTile(
                                p = pv,
                                origin = origin,
                                sizePx = sizePx,
                                zoom = zoom,
                                hidden = drag?.uid == p.uid,
                                interactive = !stickyMove,
                                selected = menuUid == p.uid,
                                onTap = {
                                    // a fresh tap discards any drag left
                                    // over by an interrupted gesture
                                    drag = null
                                    stickyMove = false
                                    menuUid = p.uid
                                },
                                onPickup = { start ->
                                    menuUid = null
                                    resizePreview = null
                                    drag = DecorDrag(p.itemId, p.variantId, p.uid, start, p.rot, p.flip)
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragPos = { pos -> drag = drag?.copy(pos = pos) },
                                onDrop = ::drop,
                                onResizeDrag = { s -> resizePreview = p.uid to s },
                                onResizeEnd = {
                                    resizePreview?.let { (uid, s) -> onScale(uid, s) }
                                    resizePreview = null
                                },
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
                        "Chạm hoặc kéo một nhãn dán vào phòng",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Cocoa,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 86.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Paper)
                            .border(2.dp, LineC, RoundedCornerShape(999.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
                if (!photoMode) {
                    // left column: snapshot button (quest/daily have no system
                    // yet — only features that exist get a button)
                    Column(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 8.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SideBtn("📷", "Chụp ảnh") {
                            val r = roomBounds
                            if (r.width > 0 && r.height > 0) {
                                scope.launch {
                                    // hide the floating controls for one frame,
                                    // then capture — drawToBitmap must run on
                                    // the main thread or Compose crashes
                                    photoMode = true
                                    withFrameNanos { }
                                    val full = view.drawToBitmap()
                                    photoMode = false
                                    val bmp = Bitmap.createBitmap(
                                        full,
                                        r.left.roundToInt().coerceIn(0, full.width),
                                        r.top.roundToInt().coerceIn(0, full.height),
                                        r.width.roundToInt().coerceAtMost(
                                            full.width - r.left.roundToInt(),
                                        ),
                                        r.height.roundToInt().coerceAtMost(
                                            full.height - r.top.roundToInt(),
                                        ),
                                    )
                                    toast = withContext(Dispatchers.IO) {
                                        saveRoomPhoto(ctx, bmp)
                                    }
                                }
                            }
                        }
                    }
                    // right column: zoom controls
                    Column(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 8.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ZoomBtn("+") { zoom = (zoom * 1.25f).coerceAtMost(3f) }
                        ZoomBtn("−") { zoom = (zoom / 1.25f).coerceAtLeast(1f) }
                        ZoomBtn("⌂") {
                            zoom = 1f
                            pan = Offset.Zero
                        }
                    }
                    // undo/redo: round buttons floating at the room's
                    // bottom-left, outside the zoom layer (reference layout)
                    Row(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RoundBtn("↶", canUndo, onUndo)
                        RoundBtn("↷", canRedo, onRedo)
                    }
                    // big yellow save button — state auto-saves on every op,
                    // so it just confirms with a toast (reference layout)
                    Column(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 8.dp, bottom = 6.dp)
                            .size(60.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFFFD96A), Color(0xFFFFBD2E)),
                                ),
                            )
                            .border(3.dp, Color.White, RoundedCornerShape(20.dp))
                            .clickable { toast = "Đã lưu góc nhỏ của bạn 💾" },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("💾", fontSize = 22.sp)
                        Text("Lưu", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7A4A10))
                    }
                    // selected-item action bar (Cornerlet's 7-button bar)
                    val menuPlaced = menuUid?.let { id -> room.placed.firstOrNull { it.uid == id } }
                    if (menuPlaced != null) {
                        ActionBar(
                            p = menuPlaced,
                            game = game,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            onRotate = { onRotate(menuPlaced.uid) },
                            onReorder = { front -> onReorder(menuPlaced.uid, front) },
                            onCycle = { onCycleVariant(menuPlaced.uid) },
                            onScale = { onScale(menuPlaced.uid, it) },
                            onRemove = {
                                onRemove(menuPlaced.uid)
                                menuUid = null
                            },
                        )
                    }
                }
                toast?.let {
                    Text(
                        it,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(ToastBg)
                            .padding(horizontal = 16.dp, vertical = 7.dp),
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
                    onAutoPlace = { item -> onAutoPlace(item.id, variantOf(item).id) },
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
                    .size(with(LocalDensity.current) { (cw * 2).toDp() }),
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
    val groundV = DecorEngine.groundVariant(room, def)
    val skyType = DecorCatalog.itemOfVariant(skyV.id).tex ?: "day"
    val wallTexL = rememberTex(wallItemL.tex?.takeIf { wallItemL.layer == DecorLayer.WALL_PAINT })
    val wallTexR = rememberTex(wallItemR.tex?.takeIf { wallItemR.layer == DecorLayer.WALL_PAINT })
    val floorTex = rememberTex(floorItem.tex?.takeIf { floorItem.layer == DecorLayer.FLOOR_PAINT })
    val panelTex = rememberTex("wtex_panel")
    // optional painted backdrop: drop bg_garden.png into drawable-nodpi
    // and it replaces the procedural garden
    val gardenBg = rememberTex("bg_garden")
    val floorTint = Color(floorV.tint)
    val groundTint = Color(groundV.tint)
    Canvas(Modifier.fillMaxSize()) {
        val rr = size.roomRect()
        if (skyType == "garden") {
            if (gardenBg != null) {
                // cover-fit the canvas like CSS background-size: cover
                val s = maxOf(
                    size.width / gardenBg.width,
                    size.height / gardenBg.height,
                )
                val w = gardenBg.width * s
                val h = gardenBg.height * s
                drawImage(
                    gardenBg,
                    dstOffset = IntOffset(
                        ((size.width - w) / 2f).roundToInt(),
                        ((size.height - h) / 2f).roundToInt(),
                    ),
                    dstSize = IntSize(w.roundToInt(), h.roundToInt()),
                )
            } else {
                drawGarden(groundTint, rr)
            }
        } else {
            drawSky(skyType)
            drawGroundBand(groundTint, rr)
        }
        // floor thickness skirt under the two front edges
        val dy = 0.052f
        drawPath(
            iso.path(
                listOf(Iso.FL, Iso.FB, Iso.FB + Offset(0f, dy), Iso.FL + Offset(0f, dy)),
                rr,
            ),
            shade(floorTint, 0.74f),
        )
        drawPath(
            iso.path(
                listOf(Iso.FB, Iso.FR, Iso.FR + Offset(0f, dy), Iso.FB + Offset(0f, dy)),
                rr,
            ),
            shade(floorTint, 0.62f),
        )
        // walls, then the floor face on top of the skirt seam
        drawWall(iso, 1, Color(wallVL.tint), wallTexL, if (wallItemL.panel) panelTex else null, rr)
        drawWall(iso, 0, Color(wallVR.tint), wallTexR, if (wallItemR.panel) panelTex else null, rr)
        val floor = iso.path(iso.floorPath(), rr)
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
                rr = rr,
            )
        }
        drawPath(
            floor,
            Brush.verticalGradient(
                listOf(Color(0x17000000), Color(0x00000000)),
                startY = rr.top + Iso.CF.y * rr.height,
                endY = rr.top + Iso.FB.y * rr.height,
            ),
        )
        drawEdges(iso, dy, rr)
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
    rr: Rect,
) {
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
    fun map(o: Offset) = Offset(rr.left + o.x * rr.width, rr.top + o.y * rr.height)
    clipPath(clip) {
        var v = 0f
        while (v < vMax) {
            var u = 0f
            while (u < uMax) {
                val tl = map(corner(u, v))
                val tr = map(corner(u + uStep, v))
                val br = map(corner(u + uStep, v + vStep))
                val bl = map(corner(u, v + vStep))
                dst[0] = tl.x; dst[1] = tl.y
                dst[2] = tr.x; dst[3] = tr.y
                dst[4] = br.x; dst[5] = br.y
                dst[6] = bl.x; dst[7] = bl.y
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
    rr: Rect,
) {
    val wall = iso.path(iso.wallPath(side), rr)
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
            rr = rr,
        )
    }
    // ambient shadow deepening toward the floor line
    drawPath(
        wall,
        Brush.verticalGradient(
            listOf(Color(0x00000000), Color(0x15000000)),
            startY = rr.top + Iso.CT.y * rr.height,
            endY = rr.top + Iso.CF.y * rr.height,
        ),
    )
    if (panelTex != null) {
        val hTop = 0.42f
        val band = iso.path(iso.wallBandPath(side, hTop), rr)
        drawPath(band, shade(tint, 1.06f).let { Color(it.red, it.green, it.blue).lerpToWhite(0.55f) })
        tileQuad(
            clip = band,
            bmp = panelTex,
            uMax = iso.wallLen(side).toFloat(),
            vMax = hTop,
            uStep = 1.5f,
            vStep = hTop,
            corner = { u, v -> iso.wallPoint(side, u, hTop - v) },
            rr = rr,
        )
        val (a, b) = iso.wallRail(side, hTop)
        drawLine(
            Color(0xFF8A6A55).copy(alpha = 0.6f),
            iso.p(a, rr), iso.p(b, rr),
            strokeWidth = rr.width * 0.0035f,
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

/** Outdoor ground band wrapping the room from just under the wall/floor
 *  edges down to the frame bottom — painted with the chosen Đất variant. */
private fun DrawScope.drawGroundBand(tint: Color, rr: Rect) {
    val ch = size.height
    val top = rr.top + rr.height * 0.61f
    drawRect(
        Brush.verticalGradient(
            listOf(shade(tint, 1.08f), shade(tint, 0.82f)),
            startY = top, endY = ch,
        ),
        Offset(0f, top),
        Size(size.width, ch - top),
    )
}

private fun DrawScope.drawSky(type: String) {
    val cw = size.width
    val ch = size.height
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
private fun DrawScope.drawGarden(grassTint: Color = Color(0xFF9CCC65), rr: Rect) {
    val cw = size.width
    val ch = size.height
    // sky band behind everything — extra canvas above the room is just
    // more sky, below is more lawn
    drawRect(Brush.verticalGradient(listOf(Color(0xFF9FCFEF), Color(0xFFD4ECFA))))
    cloud(cw * 0.30f, ch * 0.06f, cw * 0.05f, Color.White.copy(alpha = 0.95f))
    cloud(cw * 0.66f, ch * 0.10f, cw * 0.04f, Color.White.copy(alpha = 0.9f))
    // grass field (tinted by the Đất variant) — sits just behind the wall
    // tops and fills everything below
    val grassTop = rr.top + rr.height * 0.24f
    drawRect(
        Brush.verticalGradient(listOf(shade(grassTint, 1.1f), shade(grassTint, 0.88f))),
        Offset(0f, grassTop),
        Size(cw, ch - grassTop),
    )
    // white picket fence: two rails behind pointed pickets
    val fenceTop = rr.top + rr.height * 0.28f
    val picketH = rr.height * 0.085f
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
    gardenTree(cw * 0.07f, rr.top + rr.height * 0.13f, cw * 0.085f)
    gardenTree(cw * 0.94f, rr.top + rr.height * 0.10f, cw * 0.075f)
    gardenTree(cw * 0.88f, rr.top + rr.height * 0.24f, cw * 0.05f)
    // bushes along the grass edges
    gardenBush(cw * 0.04f, rr.top + rr.height * 0.42f, cw * 0.055f)
    gardenBush(cw * 0.96f, rr.top + rr.height * 0.44f, cw * 0.055f)
    gardenBush(cw * 0.07f, rr.top + rr.height * 0.95f, cw * 0.06f)
    gardenBush(cw * 0.93f, rr.top + rr.height * 0.95f, cw * 0.06f)
    // stepping stones, lower right
    val stone = Color(0xFFE3DECE)
    drawOval(stone, Offset(cw * 0.86f, rr.top + rr.height * 0.60f), Size(cw * 0.045f, cw * 0.028f))
    drawOval(stone, Offset(cw * 0.90f, rr.top + rr.height * 0.72f), Size(cw * 0.05f, cw * 0.030f))
    drawOval(stone, Offset(cw * 0.87f, rr.top + rr.height * 0.85f), Size(cw * 0.048f, cw * 0.029f))
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

private fun DrawScope.drawEdges(iso: IsoMap, dy: Float, rr: Rect) {
    val ink = Color(0xFF7A5A48).copy(alpha = 0.55f)
    val w = rr.width * 0.0028f
    fun p(o: Offset) = iso.p(o, rr)
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
        val painter = painterResource(res)
        Box(
            modifier.size(size).alpha(alpha)
                .graphicsLayer {
                    scaleX = if (flip) -1f else 1f
                    rotationZ = if (DecorCatalog[itemId].topDown) rot.toFloat() else 0f
                },
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            // Modulate multiplies alpha too — tinting directly at 0.5 alpha
            // would make the whole sprite see-through. Keep the base opaque
            // and lay a half-strength tinted copy on top instead.
            if (tint != null) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Color(tint), BlendMode.Modulate),
                    modifier = Modifier.fillMaxSize().alpha(0.5f),
                )
            }
        }
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
 *  gestures so events pass through to the canvas in sticky-move mode.
 *  [selected] draws the dashed pink sticker outline + the resize knob
 *  (drag it to scale — commits once on release via [onResizeEnd]). */
@Composable
private fun PlacedTile(
    p: PlacedDecor,
    origin: Offset,
    sizePx: Float,
    zoom: Float,
    hidden: Boolean,
    interactive: Boolean,
    selected: Boolean,
    onTap: () -> Unit,
    onPickup: (Offset) -> Unit,
    onDragPos: (Offset) -> Unit,
    onDrop: () -> Unit,
    onResizeDrag: (Float) -> Unit,
    onResizeEnd: () -> Unit,
) {
    val item = DecorCatalog[p.itemId]
    val variant = item.variantOf(p.variantId)
    val density = LocalDensity.current
    var winPos by remember { mutableStateOf(Offset.Zero) }
    var handleWinPos by remember { mutableStateOf(Offset.Zero) }
    var startScale = 0f
    var startDist = 1f
    Box(
        Modifier
            .offset { IntOffset(origin.x.roundToInt(), origin.y.roundToInt()) }
            .size(with(density) { sizePx.toDp() })
            .onGloballyPositioned { winPos = it.positionInWindow() }
            .alpha(if (hidden) 0f else 1f)
            .then(
                if (selected) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = Accent,
                            cornerRadius = CornerRadius(14.dp.toPx()),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(9f, 7f),
                                ),
                            ),
                        )
                    }
                } else {
                    Modifier
                },
            )
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
        if (selected) {
            // resize knob: dragging it away/toward the tile center scales
            // the sprite, like the Cornerlet bottom-right handle
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 6.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Accent)
                    .border(2.5.dp, Color.White, CircleShape)
                    .onGloballyPositioned { handleWinPos = it.positionInWindow() }
                    .pointerInput(p.uid) {
                        detectDragGestures(
                            onDragStart = { start ->
                                val center = winPos + Offset(sizePx, sizePx) * zoom / 2f
                                startScale = p.scale
                                startDist = (handleWinPos + start * zoom - center)
                                    .getDistance()
                                    .coerceAtLeast(1f)
                            },
                            onDrag = { change, _ ->
                                val center = winPos + Offset(sizePx, sizePx) * zoom / 2f
                                val dist = (handleWinPos + change.position * zoom - center)
                                    .getDistance()
                                onResizeDrag(
                                    (startScale * dist / startDist).coerceIn(0.6f, 1.5f),
                                )
                            },
                            onDragEnd = onResizeEnd,
                            onDragCancel = onResizeEnd,
                        )
                    },
            )
        }
    }
}

/** Selected-item action bar (Cornerlet's floating 7-button bar): flip,
 *  stack back/front, cycle design, shrink/grow, delete. A little top tab
 *  makes it read as a tooltip bubble anchored to the item. */
@Composable
private fun ActionBar(
    p: PlacedDecor,
    game: GameState,
    onRotate: () -> Unit,
    onReorder: (front: Boolean) -> Unit,
    onCycle: () -> Unit,
    onScale: (Float) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = DecorCatalog[p.itemId]
    val ownedCount = item.variants.count { DecorCatalog.isOwned(game.decor, it.id) }
    // steps anchor to the 1+k*0.15 grid so ± always returns to 100%
    val k = ((p.scale - 1f) / 0.15f).roundToInt()
    Row(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Paper)
            .border(2.5.dp, LineC, RoundedCornerShape(22.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (item.canRotate || item.canFlip) {
            BarBtn("⇆", "Lật", onRotate)
        }
        BarBtn("⤓", "Sau", { onReorder(false) })
        BarBtn("⤒", "Trước", { onReorder(true) })
        BarBtn("⟳", "Đổi mẫu", onCycle, enabled = ownedCount >= 2)
        BarBtn("−", "Nhỏ", { onScale(1f + (k - 1) * 0.15f) }, enabled = k > -3)
        BarBtn("+", "Lớn", { onScale(1f + (k + 1) * 0.15f) }, enabled = k < 3)
        BarBtn("✕", "Xóa", onRemove)
    }
}

/** One action-bar button: bold icon over a tiny label. */
@Composable
private fun BarBtn(icon: String, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            icon,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (enabled) Cocoa else Cocoa.copy(alpha = 0.3f),
        )
        Text(
            label,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (enabled) Cocoa else Cocoa.copy(alpha = 0.3f),
        )
    }
}

/** HUD currency pill (coins / energy). */
@Composable
private fun HudPill(icon: String, text: String) {
    Row(
        Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Paper)
            .border(2.dp, LineC, RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 13.sp)
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Cocoa,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** Paper side-button: emoji + label stacked (📷 in the left column). */
@Composable
private fun SideBtn(icon: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Paper)
            .border(2.dp, LineC, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
    ) {
        Text(icon, fontSize = 21.sp)
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Cocoa)
    }
}

/** Round paper zoom button (right column: + − ⌂). */
@Composable
private fun ZoomBtn(icon: String, onClick: () -> Unit) {
    Text(
        icon,
        fontSize = 17.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Cocoa,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Paper)
            .border(2.dp, LineC, CircleShape)
            .clickable(onClick = onClick)
            .wrapContentSize(Alignment.Center),
    )
}

/** Round white icon button (undo/redo under the room). */
@Composable
private fun RoundBtn(icon: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        icon,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (enabled) Cocoa else Cocoa.copy(alpha = 0.3f),
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Paper)
            .border(2.dp, LineC, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .wrapContentSize(Alignment.Center),
    )
}

/** Writes the room bitmap to the gallery; returns the toast message. */
private fun saveRoomPhoto(ctx: android.content.Context, bmp: Bitmap): String {
    val name = "bepngon_room_${System.currentTimeMillis()}.png"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= 29) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BepNgon")
        }
    }
    val uri = ctx.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values,
    ) ?: return "Lưu ảnh lỗi 😿"
    return try {
        ctx.contentResolver.openOutputStream(uri)?.use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        } ?: throw java.io.IOException("no stream")
        "Đã lưu ảnh 📷"
    } catch (e: Exception) {
        ctx.contentResolver.delete(uri, null, null)
        "Lưu ảnh lỗi 😿"
    }
}

/** Bottom dock (Cornerlet's sticker sheet): category tabs on top, then a
 *  row of square item cards flanked by ‹ › page arrows — tap a card to
 *  drop the item into the room, or drag it to the exact spot. */
@Composable
private fun CatalogTray(
    game: GameState,
    roomId: String,
    tab: String,
    onTab: (String) -> Unit,
    pickedVariant: Map<String, String>,
    onPickVariant: (String, String) -> Unit,
    onPaint: (String, Int?) -> Unit,
    onAutoPlace: (DecorItem) -> Unit,
    onDragStart: (DecorItem, Offset) -> Unit,
    onDragPos: (Offset) -> Unit,
    onDrop: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Paper)
            .border(2.5.dp, LineC, RoundedCornerShape(24.dp))
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
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (sel) PinkSel else Color.White)
                        .border(
                            2.dp,
                            if (sel) Color(0xFFE58AA3) else LineC,
                            RoundedCornerShape(14.dp),
                        )
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
        val cardScroll = rememberScrollState()
        val scope = rememberCoroutineScope()
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Accent.copy(alpha = 0.8f),
                modifier = Modifier
                    .clickable { scope.launch { cardScroll.animateScrollBy(-420f) } }
                    .padding(horizontal = 4.dp),
            )
            Row(
                Modifier
                    .weight(1f)
                    .horizontalScroll(cardScroll),
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
                            onTapPlace = { onAutoPlace(item) },
                            onDragStart = onDragStart,
                            onDragPos = onDragPos,
                            onDrop = onDrop,
                        )
                    }
                }
            }
            Text(
                "›",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Accent.copy(alpha = 0.8f),
                modifier = Modifier
                    .clickable { scope.launch { cardScroll.animateScrollBy(420f) } }
                    .padding(horizontal = 4.dp),
            )
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
        Triple("Đất", DecorLayer.GROUND_PAINT, DecorEngine.groundVariant(room, def)),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .heightIn(max = 300.dp)
            .verticalScroll(rememberScrollState()),
    ) {
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
    onTapPlace: () -> Unit,
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
                    Modifier
                        // tap = drop it into the room near the center;
                        // drag = place it on the exact cell
                        .pointerInput(item.id) {
                            detectTapGestures { onTapPlace() }
                        }
                        .pointerInput(item.id) {
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
