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
 *  Position/velocity are Compose state so ball movement triggers re-layout. */
private class Ball(
    id: Int,
    tier: Int,
    x: Float,
    y: Float,
    vx: Float = 0f,
    vy: Float = 0f,
    r: Float,
    spawnMs: Long = 0L,
) {
    var id by mutableIntStateOf(id)
    var tier by mutableIntStateOf(tier)
    var x by mutableFloatStateOf(x)
    var y by mutableFloatStateOf(y)
    var vx by mutableFloatStateOf(vx)
    var vy by mutableFloatStateOf(vy)
    var r by mutableFloatStateOf(r)
    var spawnMs by mutableLongStateOf(spawnMs)
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
    var uid by remember { mutableIntStateOf(0) }
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
                                    if (b.x < b.r) { b.x = b.r; b.vx = -b.vx * REST }
                                    if (b.x > w - b.r) { b.x = w - b.r; b.vx = -b.vx * REST }
                                    if (b.y > h - b.r) {
                                        b.y = h - b.r
                                        b.vy = -b.vy * REST
                                        b.vx *= 0.9f
                                        if (abs(b.vy) < 40f) b.vy = 0f
                                    }
                                }

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
                                        if (nt <= TIERS.lastIndex) {
                                            balls += Ball(
                                                id = uid++,
                                                tier = nt,
                                                x = mx.coerceIn(radiusFor(nt, unit), w - radiusFor(nt, unit)),
                                                y = my,
                                                vy = -120f,
                                                r = radiusFor(nt, unit),
                                                spawnMs = frame,
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

                // balls
                val density = LocalDensity.current
                balls.forEach { b ->
                    Box(
                        Modifier
                            .offset { IntOffset((b.x - b.r).roundToInt(), (b.y - b.r).roundToInt()) }
                            .size(with(density) { (b.r * 2).toDp() })
                            .clip(CircleShape)
                            .background(CellWhite),
                        contentAlignment = Alignment.Center,
                    ) {
                        GameArt(TIERS[b.tier], ItemCatalog[TIERS[b.tier]].emoji,
                            with(density) { (b.r * 1.7f).toDp() })
                    }
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
                            .background(CellWhite.copy(alpha = 0.8f)),
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
                                            balls += Ball(
                                                id = uid++,
                                                tier = nextTier,
                                                x = dropX,
                                                y = radiusFor(nextTier, unit) + 12f,
                                                r = radiusFor(nextTier, unit),
                                                spawnMs = nowT,
                                            )
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
                                        balls += Ball(
                                            id = uid++,
                                            tier = nextTier,
                                            x = pos.x.coerceIn(
                                                radiusFor(nextTier, unit),
                                                w - radiusFor(nextTier, unit),
                                            ),
                                            y = radiusFor(nextTier, unit) + 12f,
                                            r = radiusFor(nextTier, unit),
                                            spawnMs = nowT,
                                        )
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
