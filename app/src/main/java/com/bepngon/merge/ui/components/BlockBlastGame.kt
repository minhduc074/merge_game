package com.bepngon.merge.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepngon.merge.game.BlockBlast
import com.bepngon.merge.game.BlockBlastEngine
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.GameState
import com.bepngon.merge.sound.SoundManager
import com.bepngon.merge.ui.theme.Gold
import com.bepngon.merge.ui.theme.MergeGreen
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Block Blast!-style minigame — dark navy board, glossy neon pieces.
 * Drag a tray piece onto the 6x6 grid; full rows/columns pop in a wave.
 */

private val ScreenBg = Color(0xFF0E1526)
private val BoardBg = Color(0xFF1B243B)
private val CellBg = Color(0xFF28324F)
private val SlotBg = Color(0xFF1B243B)
private val SlotBorder = Color(0xFF334066)
private val InkWhite = Color(0xFFF5F7FF)
private val InkDim = Color(0xFF9AA7C7)

private val PieceColors = listOf(
    Color(0xFFFF6B6B),  // coral
    Color(0xFFFFA94D),  // honey orange
    Color(0xFFFFD93D),  // lemon
    Color(0xFF6BCB77),  // green
    Color(0xFF4ECDC4),  // teal
    Color(0xFF4D96FF),  // blue
    Color(0xFF9B5DE5),  // violet
    Color(0xFFF15BB5),  // pink
)

private const val POP_MS = 180L
private const val CLEAR_CELL_MS = 230L
private const val WAVE_STEP_MS = 45L
private const val FLOATER_MS = 780L
private const val BIG_FLOATER_MS = 1200L
private const val SHAKE_MS = 280L
private const val LIFT_CELLS = 1.75f

private class ClearCell(val index: Int, val colorIdx: Int, val delayMs: Long)
private class ClearFx(val cells: List<ClearCell>, val start: Long)
private class PopFx(val cells: Set<Int>, val start: Long)
private class Floater(
    val text: String,
    val color: Color,
    val x: Float,
    val y: Float,
    val big: Boolean,
    val start: Long,
)

private class Particle(
    var x: Float,
    var y: Float,
    val vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    var rot: Float,
    val vr: Float,
    val born: Long,
    val life: Long,
)

private fun Color.darken(f: Float) = Color(red * f, green * f, blue * f, alpha)

/** Glossy "jelly" block: base + top gloss + bottom shade + specular + edge. */
private fun DrawScope.jelly(
    left: Float,
    top: Float,
    s: Float,
    color: Color,
    alpha: Float = 1f,
    scale: Float = 1f,
) {
    if (alpha <= 0f || scale <= 0f) return
    val bs = (s - s * 0.09f) * scale
    val tl = Offset(left + (s - bs) / 2f, top + (s - bs) / 2f)
    val rad = CornerRadius(bs * 0.24f)
    drawRoundRect(color.copy(alpha = alpha), tl, Size(bs, bs), rad)
    drawRoundRect(
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.40f * alpha),
            1f to Color.White.copy(alpha = 0f),
        ),
        Offset(tl.x + bs * 0.10f, tl.y + bs * 0.05f),
        Size(bs * 0.80f, bs * 0.44f),
        CornerRadius(bs * 0.16f),
    )
    drawRoundRect(
        color.darken(0.72f).copy(alpha = 0.5f * alpha),
        Offset(tl.x, tl.y + bs * 0.78f),
        Size(bs, bs * 0.22f),
        CornerRadius(bs * 0.18f),
    )
    drawCircle(
        Color.White.copy(alpha = 0.55f * alpha),
        radius = bs * 0.07f,
        center = Offset(tl.x + bs * 0.26f, tl.y + bs * 0.20f),
    )
    drawRoundRect(
        color.darken(0.62f).copy(alpha = 0.45f * alpha),
        tl,
        Size(bs, bs),
        rad,
        style = Stroke(bs * 0.045f),
    )
}

private fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val u = t.coerceIn(0f, 1f) - 1f
    return 1f + c3 * u * u * u + c1 * u * u
}

@Composable
fun BlockBlastGame(
    game: GameState,
    onFinish: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val engine = remember { BlockBlastEngine() }
    var tick by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var showOver by remember { mutableStateOf(false) }
    var shakeUntil by remember { mutableLongStateOf(0L) }

    var boardBounds by remember { mutableStateOf(Rect.Zero) }
    var rootBounds by remember { mutableStateOf(Rect.Zero) }
    val slotRects = remember { Array(BlockBlast.TRAY_SIZE) { Rect.Zero } }

    var dragSlot by remember { mutableIntStateOf(-1) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }

    var pop by remember { mutableStateOf<PopFx?>(null) }
    val clears = remember { mutableStateListOf<ClearFx>() }
    val floaters = remember { mutableStateListOf<Floater>() }
    val particles = remember { mutableStateListOf<Particle>() }

    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    val cellPx = boardBounds.width / BlockBlast.SIZE

    /** Lift: piece bottom rides ~1.75 cells above the fingertip. */
    fun snapOf(piece: BlockBlast.Piece): Pair<Int, Int> {
        if (cellPx <= 0f) return -1 to -1
        val fx = (dragPos.x - boardBounds.left) / cellPx
        val fy = (dragPos.y - boardBounds.top) / cellPx
        val col = (fx - piece.w / 2f).roundToInt()
        val row = (fy - piece.h - LIFT_CELLS).roundToInt()
        return row to col
    }

    fun drop() {
        val slot = dragSlot
        dragSlot = -1
        if (slot < 0 || finished) return
        val tp = engine.tray[slot] ?: return
        val (row, col) = snapOf(tp.piece)
        val res = engine.placePiece(slot, row, col)
        tick++
        if (res == null) {
            SoundManager.play(SoundManager.Sfx.Error)
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            return
        }
        val now = System.currentTimeMillis()
        val cell = cellPx
        pop = PopFx((res.placedCells - res.clearedCells.keys).toSet(), now)
        // floater anchor = piece bounding-box center, clamped inside the board
        val ax = ((res.col + res.pieceW / 2f) * cell).coerceIn(cell * 1.4f, cellPx * BlockBlast.SIZE - cell * 1.4f)
        val ay = ((res.row + res.pieceH / 2f) * cell).coerceIn(cell, cellPx * BlockBlast.SIZE - cell)
        if (res.gained > 0) {
            floaters += Floater("+${res.gained}", PieceColors[res.color], ax, ay, big = false, start = now)
        }

        if (res.clearedCells.isNotEmpty()) {
            // wave radiates from the piece center
            val or = res.row + res.pieceH / 2f - 0.5f
            val oc = res.col + res.pieceW / 2f - 0.5f
            clears += ClearFx(
                res.clearedCells.map { (i, c) ->
                    val d = abs(i / BlockBlast.SIZE - or) + abs(i % BlockBlast.SIZE - oc)
                    ClearCell(i, c, (d * WAVE_STEP_MS).toLong())
                },
                now,
            )
            // shatter particles — 3 shards per popped cell
            if (cell > 0f) {
                res.clearedCells.forEach { (i, ci) ->
                    if (particles.size > 220) return@forEach
                    val cx = (i % BlockBlast.SIZE + 0.5f) * cell
                    val cy = (i / BlockBlast.SIZE + 0.5f) * cell
                    repeat(3) {
                        particles += Particle(
                            x = cx,
                            y = cy,
                            vx = (Random.nextFloat() - 0.5f) * 560f,
                            vy = -90f - Random.nextFloat() * 380f,
                            color = PieceColors[ci],
                            size = cell * (0.12f + Random.nextFloat() * 0.10f),
                            rot = Random.nextFloat() * 90f,
                            vr = (Random.nextFloat() - 0.5f) * 720f,
                            born = now,
                            life = 420L + Random.nextInt(230).toLong(),
                        )
                    }
                }
            }
            val word = when {
                res.allClear -> "SẠCH BÀN!"
                res.lines >= 5 -> "KHÔNG THỂ TIN!"
                res.lines == 4 -> "PHI THƯỜNG!"
                res.lines == 3 -> "TUYỆT VỜI!"
                res.lines == 2 -> "ĐỈNH!"
                else -> null
            }
            if (word != null) {
                floaters += Floater(
                    word, if (res.allClear) Gold else PieceColors[res.color],
                    boardBounds.width / 2f, ay - cell * 0.9f, big = true, start = now,
                )
            }
            if (res.combo >= 3) {
                floaters += Floater(
                    "🔥 Combo x${res.combo}", Color(0xFFFF8A3D),
                    boardBounds.width / 2f, cell * 1.1f, big = true, start = now,
                )
            }
            if (res.lines >= 3 || res.allClear || res.combo >= 5) {
                shakeUntil = now + SHAKE_MS
            }
            SoundManager.playMergePitch(res.lines, res.combo)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            SoundManager.play(SoundManager.Sfx.Spawn)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        if (engine.gameOver) {
            finished = true
            floaters += Floater(
                "Hết nước đi!", Color(0xFFFF8A80),
                boardBounds.width / 2f, boardBounds.height / 2f, big = true, start = now,
            )
        }
    }

    // animate + prune fx while any are alive
    val fxActive = pop != null || clears.isNotEmpty() || floaters.isNotEmpty() || particles.isNotEmpty()
    LaunchedEffect(fxActive) {
        if (!fxActive) return@LaunchedEffect
        var last = -1L
        while (true) {
            withFrameMillis { frame ->
                val now = System.currentTimeMillis()
                val dt = if (last < 0) 0.016f else ((frame - last) / 1000f).coerceAtMost(0.05f)
                last = frame
                particles.forEach {
                    it.x += it.vx * dt
                    it.vy += 2400f * dt
                    it.y += it.vy * dt
                    it.rot += it.vr * dt
                }
                particles.removeAll { now - it.born > it.life }
                clears.removeAll { fx -> fx.cells.all { now - fx.start - it.delayMs > CLEAR_CELL_MS } }
                floaters.removeAll { now - it.start > if (it.big) BIG_FLOATER_MS else FLOATER_MS }
                pop?.let { if (now - it.start > POP_MS) pop = null }
                if (now >= shakeUntil) shakeUntil = 0
                tick++
            }
        }
    }

    LaunchedEffect(finished) {
        if (finished) {
            delay(1400)
            showOver = true
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootBounds = it.boundsInRoot() }
            .background(ScreenBg),
    ) {
        // generated backdrop when present — flat navy otherwise
        val ctx = LocalContext.current
        val bgRes = remember { artRes(ctx, "bg_blockblast") }
        if (bgRes != 0) {
            Image(
                painter = painterResource(bgRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            // header
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "🧱 Xếp khối",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkWhite,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "✕",
                    fontSize = 22.sp,
                    color = InkWhite,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF2A3659))
                        .clickable(onClick = {
                            if (!finished) onFinish(engine.score) else onClose()
                        })
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }

            // score row: SCORE | BEST | combo flame
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    tick // engine vars are plain Kotlin — subscribe to frames
                    Text("SCORE", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = InkDim, letterSpacing = 1.sp)
                    Text(
                        "${engine.score}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = InkWhite,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BoardBg)
                            .border(2.dp, SlotBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(start = 14.dp),
                ) {
                    tick
                    Text("BEST", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = InkDim, letterSpacing = 1.sp)
                    Text(
                        "${maxOf(game.blockBest, engine.score)}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Gold,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BoardBg)
                            .border(2.dp, SlotBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    tick
                    if (engine.combo >= 2) {
                        Text(
                            "🔥 x${engine.combo}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF8A3D),
                        )
                    }
                }
            }

            // board — square, navy, soft inner border
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                val side = min(maxWidth.value, maxHeight.value).dp
                val nowShake = System.currentTimeMillis()
                Box(
                    Modifier
                        .size(side)
                        .graphicsLayer {
                            tick
                            if (nowShake < shakeUntil) {
                                val p = (shakeUntil - nowShake) / SHAKE_MS.toFloat()
                                translationX = kotlin.math.sin(nowShake * 0.11f) * 9f * p
                                translationY = kotlin.math.cos(nowShake * 0.13f) * 7f * p
                            }
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .background(BoardBg)
                        .border(3.dp, SlotBorder, RoundedCornerShape(18.dp))
                        .padding(7.dp),
                ) {
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { boardBounds = it.boundsInRoot() },
                    ) {
                        tick // engine vars are plain Kotlin — subscribe to frames
                        val cell = size.width / BlockBlast.SIZE
                        val now = System.currentTimeMillis()
                        val gap = cell * 0.06f
                        val cellRad = CornerRadius(cell * 0.20f)

                        // empty cells
                        for (r in 0 until BlockBlast.SIZE) {
                            for (c in 0 until BlockBlast.SIZE) {
                                if (engine.grid[r * BlockBlast.SIZE + c] == 0) {
                                    drawRoundRect(
                                        CellBg,
                                        Offset(c * cell + gap, r * cell + gap),
                                        Size(cell - gap * 2, cell - gap * 2),
                                        cellRad,
                                    )
                                }
                            }
                        }

                        // line-clear highlight under everything when dragging
                        val tp = engine.tray.getOrNull(dragSlot)
                        if (tp != null && cellPx > 0f) {
                            val (sr, sc) = snapOf(tp.piece)
                            val ok = engine.canPlace(tp.piece, sr, sc)
                            val preview = engine.previewClear(tp.piece, sr, sc)
                            val pc = PieceColors[tp.color]
                            preview.forEach { i ->
                                val r = i / BlockBlast.SIZE
                                val c = i % BlockBlast.SIZE
                                drawRoundRect(
                                    pc.copy(alpha = 0.40f),
                                    Offset(c * cell + gap, r * cell + gap),
                                    Size(cell - gap * 2, cell - gap * 2),
                                    cellRad,
                                )
                            }
                            if (ok) {
                                tp.piece.cells.forEach { rel ->
                                    val r = sr + rel / tp.piece.w
                                    val c = sc + rel % tp.piece.w
                                    if (r in 0 until BlockBlast.SIZE && c in 0 until BlockBlast.SIZE) {
                                        jelly(c * cell, r * cell, cell, pc, alpha = 0.55f)
                                    }
                                }
                            }
                        }

                        // placed blocks (with pop-in on the newest placement)
                        val popFx = pop
                        for (i in 0 until BlockBlast.CELLS) {
                            val v = engine.grid[i]
                            if (v == 0) continue
                            var scale = 1f
                            if (popFx != null && i in popFx.cells) {
                                scale = easeOutBack((now - popFx.start) / POP_MS.toFloat())
                            }
                            jelly(
                                (i % BlockBlast.SIZE) * cell,
                                (i / BlockBlast.SIZE) * cell,
                                cell, PieceColors[(v - 1) % PieceColors.size], scale = scale,
                            )
                        }

                        // wave clear ghosts: flash then shrink+fade
                        clears.forEach { fx ->
                            fx.cells.forEach { cc ->
                                val t = (now - fx.start - cc.delayMs) / CLEAR_CELL_MS.toFloat()
                                if (t < 0f || t > 1f) return@forEach
                                val r = cc.index / BlockBlast.SIZE
                                val c = cc.index % BlockBlast.SIZE
                                jelly(
                                    c * cell, r * cell, cell,
                                    PieceColors[cc.colorIdx],
                                    alpha = 1f - t,
                                    scale = 1f - 0.55f * t * t,
                                )
                                if (t < 0.4f) {
                                    drawRoundRect(
                                        Color.White.copy(alpha = (1f - t / 0.4f) * 0.7f),
                                        Offset(c * cell + gap, r * cell + gap),
                                        Size(cell - gap * 2, cell - gap * 2),
                                        cellRad,
                                    )
                                }
                            }
                        }

                        // shatter particles
                        particles.forEach { p ->
                            val t = (now - p.born) / p.life.toFloat()
                            if (t < 0f || t > 1f) return@forEach
                            rotate(p.rot, Offset(p.x, p.y)) {
                                drawRect(
                                    p.color.copy(alpha = 1f - t),
                                    Offset(p.x - p.size / 2f, p.y - p.size / 2f),
                                    Size(p.size, p.size),
                                )
                            }
                        }

                        // floating score text (native canvas = easy emoji/stroke)
                        floaters.forEach { f ->
                            val dur = if (f.big) BIG_FLOATER_MS else FLOATER_MS
                            val t = ((now - f.start) / dur.toFloat()).coerceIn(0f, 1f)
                            val rise = cell * (if (f.big) 1.5f else 0.9f) * t
                            val alpha = when {
                                t < 0.12f -> t / 0.12f
                                t > 0.7f -> 1f - (t - 0.7f) / 0.3f
                                else -> 1f
                            }
                            val textSize = cell * (if (f.big) 0.72f else 0.44f)
                            val y = f.y - rise
                            val fillPaint = Paint().apply {
                                this.color = f.color.toArgb()
                                this.textSize = textSize
                                this.alpha = (alpha * 255).roundToInt()
                                this.isFakeBoldText = true
                                this.textAlign = Paint.Align.CENTER
                            }
                            val strokePaint = Paint().apply {
                                this.color = 0xFF101627.toInt()
                                this.textSize = textSize
                                this.alpha = (alpha * 255).roundToInt()
                                this.isFakeBoldText = true
                                this.textAlign = Paint.Align.CENTER
                                this.style = Paint.Style.STROKE
                                this.strokeWidth = textSize * 0.16f
                                this.strokeJoin = Paint.Join.ROUND
                            }
                            drawContext.canvas.nativeCanvas.drawText(f.text, f.x, y, strokePaint)
                            drawContext.canvas.nativeCanvas.drawText(f.text, f.x, y, fillPaint)
                        }
                    }
                }
            }

            // tray — 3 slots; dead pieces dim, dragging slot shows empty
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(96.dp),
            ) {
                for (i in 0 until BlockBlast.TRAY_SIZE) {
                    val tp = engine.tray[i]
                    tick // engine vars are plain Kotlin — subscribe to frames
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(horizontal = 5.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SlotBg)
                            .border(2.dp, SlotBorder, RoundedCornerShape(14.dp))
                            .onGloballyPositioned { slotRects[i] = it.boundsInRoot() }
                            .pointerInput(tp, finished) {
                                if (tp == null || finished) return@pointerInput
                                detectDragGestures(
                                    onDragStart = { off ->
                                        dragSlot = i
                                        dragPos = slotRects[i].topLeft + off
                                        SoundManager.play(SoundManager.Sfx.Tap)
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    onDrag = { ch, amt ->
                                        ch.consume()
                                        dragPos += amt
                                    },
                                    onDragEnd = { drop() },
                                    onDragCancel = { dragSlot = -1 },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        tick // engine vars are plain Kotlin — subscribe to frames
                        if (tp != null && i != dragSlot) {
                            val fits = engine.anyPlacement(tp.piece)
                            Canvas(
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = if (fits || finished) 1f else 0.32f },
                            ) {
                                val s = min(
                                    size.width / (tp.piece.w + 0.6f),
                                    size.height / (tp.piece.h + 0.6f),
                                )
                                val ox = (size.width - tp.piece.w * s) / 2f
                                val oy = (size.height - tp.piece.h * s) / 2f
                                tp.piece.cells.forEach { rel ->
                                    jelly(
                                        ox + (rel % tp.piece.w) * s,
                                        oy + (rel / tp.piece.w) * s,
                                        s, PieceColors[tp.color],
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                "Kéo khối vào lưới — đầy hàng/cột là nổ! Đặt hết 3 khối để nhận khay mới.\n⚡ mốc điểm: 800→20 · 2500→46 · 7000→~91",
                fontSize = 11.sp,
                color = InkDim,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        // dragged piece — board-scale, lifted above the finger
        val dragTp = engine.tray.getOrNull(dragSlot)
        if (dragTp != null && cellPx > 0f) {
            val liftPx = cellPx * LIFT_CELLS
            val wPx = dragTp.piece.w * cellPx
            val hPx = dragTp.piece.h * cellPx
            Canvas(
                Modifier
                    .offset {
                        // dragPos is in root coords; offset() is parent-local —
                        // subtract the root box origin (status-bar inset etc.)
                        IntOffset(
                            (dragPos.x - rootBounds.left - wPx / 2f).roundToInt(),
                            (dragPos.y - rootBounds.top - liftPx - hPx).roundToInt(),
                        )
                    }
                    .size(
                        width = with(density) { wPx.toDp() },
                        height = with(density) { hPx.toDp() },
                    ),
            ) {
                dragTp.piece.cells.forEach { rel ->
                    jelly(
                        (rel % dragTp.piece.w) * cellPx,
                        (rel / dragTp.piece.w) * cellPx,
                        cellPx, PieceColors[dragTp.color],
                    )
                }
            }
        }

        // game-over overlay
        if (showOver) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xE60E1526)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🧱 Hết chỗ rồi!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = InkWhite)
                    Text("Điểm: ${engine.score}", fontSize = 18.sp, color = InkWhite,
                        modifier = Modifier.padding(top = 6.dp))
                    val e = GameEngine.blockEnergyFor(engine.score)
                    Text("+$e ⚡", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = MergeGreen, modifier = Modifier.padding(top = 4.dp))
                    if (engine.score > game.blockBest && engine.score > 0) {
                        Text("🏅 Kỷ lục mới!", fontSize = 13.sp, color = Gold,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                    Text(
                        "🧃 Nhận thưởng",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .padding(top = 14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MergeGreen)
                            .clickable { onFinish(engine.score) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
