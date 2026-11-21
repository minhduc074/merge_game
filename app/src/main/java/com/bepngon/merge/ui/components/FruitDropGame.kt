package com.bepngon.merge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.GameState
import com.bepngon.merge.game.ItemCatalog
import com.bepngon.merge.ui.theme.CellWhite
import com.bepngon.merge.ui.theme.Cocoa
import com.bepngon.merge.ui.theme.CreamBg
import com.bepngon.merge.ui.theme.Gold
import com.bepngon.merge.ui.theme.MergeGreen
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/** Physics ball: a fruit tier bouncing inside the container.
 *  Position/velocity/deform are Compose state so every frame re-layouts. */
private class Ball(
    id: Int,
    tier: Int,
    x: Float,
    y: Float,
    vx: Float = 0f,
    vy: Float = 0f,
    r: Float,
    spawnMs: Long = 0L,
    popStart: Float = 0.2f,
) {
    var id by mutableIntStateOf(id)
    var tier by mutableIntStateOf(tier)
    var x by mutableFloatStateOf(x)
    var y by mutableFloatStateOf(y)
    var vx by mutableFloatStateOf(vx)
    var vy by mutableFloatStateOf(vy)
    var r by mutableFloatStateOf(r)
    var spawnMs by mutableLongStateOf(spawnMs)
    // Juice: spawn/merge pop-in (eases to 1) and squash&stretch deform.
    var pop by mutableFloatStateOf(popStart)
    var squashX by mutableFloatStateOf(1f)
    var squashY by mutableFloatStateOf(1f)
}

/** Merge/impact ring burst: expands and fades over ~0.4s. */
private class Puff(val x: Float, val y: Float, val r: Float) {
    var age by mutableFloatStateOf(0f)
}

private val TIERS = listOf("fruit_1", "fruit_2", "fruit_3", "fruit_4", "fruit_5", "fruit_6")
private const val GRAVITY = 2200f
private const val REST = 0.25f
private const val GAME_MS = 90_000L
private const val DROP_CD_MS = 600L
private const val OVERLINE_MS = 1_800L

private fun radiusFor(tier: Int, unit: Float): Float = unit * (0.62f + 0.22f * tier)

@Composable
fun FruitDropGame(
    game: GameState,
    onFinish: (Int) -> Unit,
    onClose: () -> Unit,
) {
    var score by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var overlineSince by remember { mutableLongStateOf(0L) }
    var nextTier by remember { mutableIntStateOf(0) }
    var lastDrop by remember { mutableLongStateOf(0L) }
    var aimX by remember { mutableStateOf<Float?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val started = remember { System.currentTimeMillis() }
    val balls = remember { mutableStateListOf<Ball>() }
    val puffs = remember { mutableStateListOf<Puff>() }
    var uid by remember { mutableIntStateOf(0) }
    var previewPop by remember { mutableFloatStateOf(1f) }
    val pendingMerges = remember { mutableStateListOf<Triple<Float, Float, Int>>() }

    Box(
        Modifier
            .fillMaxSize()
            .background(CreamBg),
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "🍉 Thả trái",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Cocoa,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Điểm: $score",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cocoa,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    "✕",
                    fontSize = 22.sp,
                    color = Cocoa,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFFFE0B2))
                        .clickable(onClick = {
                            if (!finished) onFinish(score) else onClose()
                        })
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            val remain = (GAME_MS - (now - started)).coerceAtLeast(0) / 1000
            Text(
                "⏱ ${remain}s — thả trái trùng nhau để merge. " +
                    "Tràn lên quá vạch đỏ là thua!",
                fontSize = 12.sp,
                color = Cocoa,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFF4E0))
                    .border(3.dp, Color(0xFFA0522D), RoundedCornerShape(16.dp)),
            ) {
                val w = constraints.maxWidth.toFloat()
                val h = constraints.maxHeight.toFloat()
                val unit = w / 7f
                val dangerY = unit * 0.9f

                if (!finished) {
                    LaunchedEffect(w, h) {
                        var last = System.currentTimeMillis()
                        while (true) {
                            withFrameMillis {
                                // One clock everywhere: epoch ms (frame time is uptime-based).
                                val frame = System.currentTimeMillis()
                                now = frame
                                val dt = min((frame - last) / 1000f, 0.05f)
                                last = frame

                                // integrate
                                balls.forEach { b ->
                                    b.vy += GRAVITY * dt
                                    b.x += b.vx * dt
                                    b.y += b.vy * dt
                                    if (b.x < b.r) {
                                        b.x = b.r
                                        if (abs(b.vx) > 260f) { b.squashX = 0.72f; b.squashY = 1.22f }
                                        b.vx = -b.vx * REST
                                    }
                                    if (b.x > w - b.r) {
                                        b.x = w - b.r
                                        if (abs(b.vx) > 260f) { b.squashX = 0.72f; b.squashY = 1.22f }
                                        b.vx = -b.vx * REST
                                    }
                                    if (b.y > h - b.r) {
                                        b.y = h - b.r
                                        if (abs(b.vy) > 220f) { b.squashX = 1.35f; b.squashY = 0.6f }
                                        b.vy = -b.vy * REST
                                        b.vx *= 0.9f
                                        if (abs(b.vy) < 40f) b.vy = 0f
                                    }
                                    // squash & stretch: elongate while falling fast, spring back after.
                                    val flying = b.y < h - b.r - 2f && abs(b.vy) > 550f
                                    val tx = if (flying) 0.85f else 1f
                                    val ty = if (flying) 1.18f else 1f
                                    b.squashX += (tx - b.squashX) * min(1f, dt * 9)
                                    b.squashY += (ty - b.squashY) * min(1f, dt * 9)
                                    b.pop += (1f - b.pop) * min(1f, dt * 11)
                                }
                                puffs.forEach { it.age += dt }
                                puffs.removeAll { it.age > 0.4f }
                                previewPop += (1f - previewPop) * min(1f, dt * 12)

                                // pairwise collisions
                                for (i in balls.indices) {
                                    for (j in i + 1 until balls.size) {
                                        val a = balls[i]
                                        val c = balls[j]
                                        val dx = c.x - a.x
                                        val dy = c.y - a.y
                                        val d = sqrt(dx * dx + dy * dy)
                                        val minD = a.r + c.r
                                        if (d in 0.001f..minD) {
                                            if (a.tier == c.tier && a.tier < TIERS.lastIndex + 1) {
                                                pendingMerges += Triple(
                                                    (a.x + c.x) / 2f,
                                                    (a.y + c.y) / 2f,
                                                    a.tier,
                                                )
                                                a.id = -a.id - 1
                                                c.id = -c.id - 1
                                            } else {
                                                val nx = dx / d
                                                val ny = dy / d
                                                val overlap = (minD - d) / 2f
                                                a.x -= nx * overlap
                                                a.y -= ny * overlap
                                                c.x += nx * overlap
                                                c.y += ny * overlap
                                                val relV = abs(a.vx - c.vx) + abs(a.vy - c.vy)
                                                if (relV > 320f) {
                                                    a.squashX = 1.22f; a.squashY = 0.78f
                                                    c.squashX = 1.22f; c.squashY = 0.78f
                                                }
                                                val push = 40f
                                                a.vx -= nx * push * dt
                                                a.vy -= ny * push * dt
                                                c.vx += nx * push * dt
                                                c.vy += ny * push * dt
                                            }
                                        }
                                    }
                                }

                                // apply merges
                                if (pendingMerges.isNotEmpty()) {
                                    balls.removeAll { it.id < 0 }
                                    pendingMerges.forEach { (mx, my, t) ->
                                        val nt = t + 1
                                        puffs += Puff(mx, my, radiusFor(min(t, TIERS.lastIndex), unit))
                                        if (nt <= TIERS.lastIndex) {
                                            balls += Ball(
                                                id = uid++,
                                                tier = nt,
                                                x = mx.coerceIn(radiusFor(nt, unit), w - radiusFor(nt, unit)),
                                                y = my,
                                                vy = -120f,
                                                r = radiusFor(nt, unit),
                                                spawnMs = frame,
                                                popStart = 1.5f,
                                            )
                                        }
                                        score += (nt + 1) * 2
                                    }
                                    pendingMerges.clear()
                                }

                                // overflow check: settled ball above the danger line
                                val danger = balls.any {
                                    abs(it.vy) < 30f && it.y - it.r < dangerY &&
                                        frame - it.spawnMs > 600
                                }
                                if (danger) {
                                    if (overlineSince == 0L) overlineSince = frame
                                    if (frame - overlineSince > OVERLINE_MS) finished = true
                                } else {
                                    overlineSince = 0L
                                }
                                if (frame - started > GAME_MS) finished = true
                            }
                        }
                    }
                }

                // danger line
                Box(
                    Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, dangerY.roundToInt()) }
                        .height(2.dp)
                        .background(Color(0x88E53935)),
                )

                // balls (graphicsLayer last so squash warps the whole circle)
                val density = LocalDensity.current
                balls.forEach { b ->
                    Box(
                        Modifier
                            .offset { IntOffset((b.x - b.r).roundToInt(), (b.y - b.r).roundToInt()) }
                            .size(with(density) { (b.r * 2).toDp() })
                            .clip(CircleShape)
                            .background(CellWhite)
                            .graphicsLayer {
                                scaleX = b.pop * b.squashX
                                scaleY = b.pop * b.squashY
                                transformOrigin = TransformOrigin(0.5f, 1f)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        GameArt(TIERS[b.tier], ItemCatalog[TIERS[b.tier]].emoji,
                            with(density) { (b.r * 1.7f).toDp() })
                    }
                }

                // merge/impact ring bursts
                puffs.forEach { p ->
                    val t = p.age / 0.4f
                    val pr = p.r * (1f + t * 2.2f)
                    Box(
                        Modifier
                            .offset { IntOffset((p.x - pr).roundToInt(), (p.y - pr).roundToInt()) }
                            .size(with(density) { (pr * 2).toDp() })
                            .border(3.dp, Color(0xFFFFB74D).copy(alpha = 1f - t), CircleShape),
                    )
                }

                // aim guide + next fruit preview
                if (!finished) {
                    val ax = aimX ?: (w / 2)
                    val r = radiusFor(nextTier, unit)
                    Box(
                        Modifier
                            .offset { IntOffset((ax - r).roundToInt(), 6) }
                            .size(with(density) { (r * 2).toDp() })
                            .clip(CircleShape)
                            .background(CellWhite.copy(alpha = 0.8f))
                            .graphicsLayer {
                                scaleX = previewPop
                                scaleY = previewPop
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        GameArt(TIERS[nextTier], ItemCatalog[TIERS[nextTier]].emoji,
                            with(density) { (r * 1.7f).toDp() })
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(w, unit) {
                                detectDragGestures(
                                    onDrag = { change, _ ->
                                        change.consume()
                                        aimX = change.position.x.coerceIn(
                                            radiusFor(nextTier, unit),
                                            w - radiusFor(nextTier, unit),
                                        )
                                    },
                                    onDragEnd = {
                                        val nowT = System.currentTimeMillis()
                                        if (nowT - lastDrop > DROP_CD_MS) {
                                            lastDrop = nowT
                                            val dropX = (aimX ?: (w / 2)).coerceIn(
                                                radiusFor(nextTier, unit),
                                                w - radiusFor(nextTier, unit),
                                            )
                                            val r0 = radiusFor(nextTier, unit)
                                            balls += Ball(
                                                id = uid++,
                                                tier = nextTier,
                                                x = dropX,
                                                y = r0 + 12f,
                                                r = r0,
                                                spawnMs = nowT,
                                            )
                                            puffs += Puff(dropX, r0 + 12f, r0 * 0.8f)
                                            previewPop = 0.4f
                                            nextTier = nextDropTier()
                                        }
                                    },
                                )
                            }
                            .pointerInput(w, unit) {
                                detectTapGestures { pos ->
                                    val nowT = System.currentTimeMillis()
                                    if (nowT - lastDrop > DROP_CD_MS) {
                                        lastDrop = nowT
                                        val dropX = pos.x.coerceIn(
                                            radiusFor(nextTier, unit),
                                            w - radiusFor(nextTier, unit),
                                        )
                                        val r0 = radiusFor(nextTier, unit)
                                        aimX = dropX
                                        balls += Ball(
                                            id = uid++,
                                            tier = nextTier,
                                            x = dropX,
                                            y = r0 + 12f,
                                            r = r0,
                                            spawnMs = nowT,
                                        )
                                        puffs += Puff(dropX, r0 + 12f, r0 * 0.8f)
                                        previewPop = 0.4f
                                        nextTier = nextDropTier()
                                    }
                                }
                            },
                    )
                }

                if (finished) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xCCFFF8E7)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🍉 Hết giờ!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Cocoa)
                            Text("Điểm: $score", fontSize = 18.sp, color = Cocoa,
                                modifier = Modifier.padding(top = 6.dp))
                            val e = minOf(GameEngine.MINI_MAX_ENERGY, score / 2)
                            Text("+$e ⚡", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                color = MergeGreen, modifier = Modifier.padding(top = 4.dp))
                            if (score >= game.miniBest && score > 0) {
                                Text("🏅 Kỷ lục mới!", fontSize = 13.sp, color = Gold,
                                    modifier = Modifier.padding(top = 2.dp))
                            }
                            Text(
                                if (game.miniPlaysLeft > 0) "🍹 Nhận thưởng (${game.miniPlaysLeft} lượt còn)"
                                else "🍹 Nhận thưởng",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .padding(top = 14.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MergeGreen)
                                    .clickable { onFinish(score) }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Weighted next drop: mostly low tiers. */
private fun nextDropTier(random: Random = Random): Int = when (random.nextInt(100)) {
    in 0..54 -> 0
    in 55..84 -> 1
    else -> 2
}
