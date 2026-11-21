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
    val targetR: Float = r,
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
    // s of near-zero speed → past SLEEP_MS it stops integrating (physics-only)
    var still = 0f
}

/** Merge/impact ring burst: expands and fades over ~0.4s. */
private class Puff(val x: Float, val y: Float, val r: Float, val alpha: Float = 1f) {
    var age by mutableFloatStateOf(0f)
}

private val TIERS = listOf("fruit_1", "fruit_2", "fruit_3", "fruit_4", "fruit_5", "fruit_6")
private const val GRAVITY = 2200f
private const val STEP = 1f / 60f     // fixed physics step
private const val MAX_STEPS = 4       // per frame; backlog beyond this is dropped
private const val MAX_VY = 2600f      // terminal velocity; also prevents tunneling
private const val REST = 0.12f        // wall / floor restitution
private const val REST_BALL = 0.25f   // fruit-fruit restitution (subak/Rapier value)
private const val FRICTION = 0.35f    // tangential impulse cap (subak/Rapier value)
private const val REST_VEL = 160f     // px/s closing speed below which contacts are
                                      // inelastic → no rest jitter in the pile
private const val LINEAR_DAMP = 0.2f  // /s exponential damping (Rapier linearDamping)
private const val RISE_V = 400f       // px/s upward-speed cap — a shove may hop a
                                      // fruit ~35px, never launch it out of the pile
private const val WAKE_V = 140f       // px/s normal impact that wakes a settled
                                      // fruit; gentler nudges leave it asleep
private const val FLOOR_FRICTION = 6f // /s velocity scrub while touching the floor
private const val SLEEP_V = 18f       // px/s below which a fruit counts as still
private const val SLEEP_MS = 0.4f     // s of stillness → asleep (no integrate)
private const val GAME_MS = 90_000L
private const val DROP_CD_MS = 600L
private const val OVERLINE_MS = 1_800L
private const val WALL_PAD = 10f      // fruit circles never touch the brown border
private const val BALL_GAP = 2f       // and never overlap each other's circle
private const val SOLVER_PASSES = 4   // relaxation iterations per substep
private const val SUBSTEPS = 2        // physics substeps per fixed step — halved
                                      // penetration so drops don't visibly sink in

private fun radiusFor(tier: Int, unit: Float): Float = unit * (0.56f + 0.19f * tier)

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
                        var acc = 0f   // fixed-step accumulator
                        while (true) {
                            withFrameMillis {
                                // One clock everywhere: epoch ms (frame time is uptime-based).
                                val frame = System.currentTimeMillis()
                                now = frame
                                val dt = min((frame - last) / 1000f, 0.05f)
                                last = frame

                                fun clampIn(b: Ball) {
                                    if (b.x < WALL_PAD + b.r) b.x = WALL_PAD + b.r
                                    if (b.x > w - WALL_PAD - b.r) b.x = w - WALL_PAD - b.r
                                    if (b.y > h - WALL_PAD - b.r) {
                                        b.y = h - WALL_PAD - b.r
                                        if (b.vy > 0f) b.vy = 0f
                                    }
                                }

                                // One fixed physics step. Restitution only fires above
                                // REST_VEL — slow contacts are inelastic so resting piles
                                // don't micro-bounce (Box2D b2_velocityThreshold).
                                fun step(sdt: Float) {
                                    // integrate (asleep fruit skip gravity & motion)
                                    balls.forEach { b ->
                                        if (b.still >= SLEEP_MS) return@forEach
                                        b.vy = min(b.vy + GRAVITY * sdt, MAX_VY)
                                        // linear damping like Rapier linearDamping
                                        b.vx *= (1f - LINEAR_DAMP * sdt)
                                        b.vy *= (1f - LINEAR_DAMP * sdt)
                                        b.x += b.vx * sdt
                                        b.y += b.vy * sdt
                                        if (b.x < WALL_PAD + b.r) {
                                            b.x = WALL_PAD + b.r
                                            val impact = abs(b.vx)
                                            if (impact > 350f) {
                                                val dep = (impact / 2200f).coerceIn(0.05f, 0.22f)
                                                b.squashX = 1f - dep; b.squashY = 1f + dep * 0.8f
                                            }
                                            b.vx = if (impact > REST_VEL) -b.vx * REST else 0f
                                        }
                                        if (b.x > w - WALL_PAD - b.r) {
                                            b.x = w - WALL_PAD - b.r
                                            val impact = abs(b.vx)
                                            if (impact > 350f) {
                                                val dep = (impact / 2200f).coerceIn(0.05f, 0.22f)
                                                b.squashX = 1f - dep; b.squashY = 1f + dep * 0.8f
                                            }
                                            b.vx = if (impact > REST_VEL) -b.vx * REST else 0f
                                        }
                                        if (b.y > h - WALL_PAD - b.r) {
                                            b.y = h - WALL_PAD - b.r
                                            val impact = abs(b.vy)
                                            if (impact > 320f) {
                                                val dep = (impact / 2200f).coerceIn(0.06f, 0.28f)
                                                b.squashX = 1f + dep * 1.2f; b.squashY = 1f - dep
                                                if (impact > 850f) {
                                                    puffs += Puff(b.x, h - WALL_PAD - 2f, b.r * 0.55f, 0.3f)
                                                }
                                            }
                                            b.vy = if (impact > REST_VEL) -b.vy * REST else 0f
                                        }
                                        // ground friction while resting/sliding on the floor
                                        if (b.y + b.r >= h - WALL_PAD - 1f) {
                                            b.vx *= (1f - FLOOR_FRICTION * sdt).coerceAtLeast(0f)
                                        }
                                    }

                                    // Pairwise contacts, several relaxation passes so a pile
                                    // settles without overlapping; each pass re-clamps.
                                    repeat(SOLVER_PASSES) {
                                        for (i in balls.indices) {
                                            for (j in i + 1 until balls.size) {
                                                val a = balls[i]
                                                val c = balls[j]
                                                if (a.id < 0 || c.id < 0) continue
                                                val dx = c.x - a.x
                                                val dy = c.y - a.y
                                                val d = sqrt(dx * dx + dy * dy)
                                                val minD = a.r + c.r + BALL_GAP
                                                if (d !in 0.001f..minD) continue
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
                                                    // mass ∝ r³ (volume): the biggest fruit
                                                    // is ~20× a cherry → it gets the small
                                                    // share of every separation
                                                    val ma = a.r * a.r * a.r
                                                    val mc = c.r * c.r * c.r
                                                    val tot = ma + mc
                                                    val rvx = a.vx - c.vx
                                                    val rvy = a.vy - c.vy
                                                    val vn = rvx * nx + rvy * ny
                                                    // anti-compression (ref: CryptoDragonLady/
                                                    // fruit): undo the FULL overlap each pass
                                                    // so fruits are never left interpenetrating
                                                    val need = minD - d
                                                    if (need > 0f) {
                                                        var wa = mc / tot
                                                        var wc = ma / tot
                                                        val aSleep = a.still >= SLEEP_MS
                                                        val cSleep = c.still >= SLEEP_MS
                                                        // a settled fruit is a solid anchor —
                                                        // the mover takes all the separation
                                                        if (aSleep != cSleep) {
                                                            wa = if (aSleep) 0f else 1f
                                                            wc = if (cSleep) 0f else 1f
                                                        }
                                                        var ax = -nx * need * wa
                                                        var ay = -ny * need * wa
                                                        var cx = nx * need * wc
                                                        var cy = ny * need * wc
                                                        // a fruit pinned by wall/floor can't
                                                        // be pushed through — its share goes
                                                        // to the other fruit
                                                        if (a.x + ax - a.r < WALL_PAD ||
                                                            a.x + ax + a.r > w - WALL_PAD) {
                                                            cx += ax; ax = 0f
                                                        }
                                                        if (c.x + cx - c.r < WALL_PAD ||
                                                            c.x + cx + c.r > w - WALL_PAD) {
                                                            ax += cx; cx = 0f
                                                        }
                                                        if (a.y + ay + a.r > h - WALL_PAD) {
                                                            cy += ay; ay = 0f
                                                        }
                                                        if (c.y + cy + c.r > h - WALL_PAD) {
                                                            ay += cy; cy = 0f
                                                        }
                                                        a.x += ax; a.y += ay
                                                        c.x += cx; c.y += cy
                                                        // impulse along the contact normal —
                                                        // real momentum exchange like Rapier;
                                                        // mass ∝ r³ so a small fruit barely
                                                        // moves a big one
                                                        if (vn < 0f) {
                                                            val e = if (-vn > REST_VEL) REST_BALL else 0f
                                                            val inv = 1f / ma + 1f / mc
                                                            val jn = -(1f + e) * vn / inv
                                                            a.vx -= nx * jn / ma
                                                            a.vy -= ny * jn / ma
                                                            c.vx += nx * jn / mc
                                                            c.vy += ny * jn / mc
                                                            // friction capped by μ·jn
                                                            val tx = -ny
                                                            val ty = nx
                                                            val vt = rvx * tx + rvy * ty
                                                            val jt = (-vt / inv).coerceIn(-jn * FRICTION, jn * FRICTION)
                                                            a.vx -= tx * jt / ma
                                                            a.vy -= ty * jt / ma
                                                            c.vx += tx * jt / mc
                                                            c.vy += ty * jt / mc
                                                            // a sleeper that only got grazed
                                                            // stays anchored despite impulses
                                                            if (aSleep && -vn <= WAKE_V) {
                                                                a.vx = 0f; a.vy = 0f
                                                            }
                                                            if (cSleep && -vn <= WAKE_V) {
                                                                c.vx = 0f; c.vy = 0f
                                                            }
                                                        }
                                                        // only a real hit wakes a settled
                                                        // fruit — grazes/rolls leave it asleep
                                                        if (-vn > WAKE_V) {
                                                            a.still = 0f; c.still = 0f
                                                        }
                                                        // squash on hard hits; the lighter
                                                        // fruit deforms more
                                                        val impact = -vn
                                                        if (impact > 300f) {
                                                            val dep = (impact / 2200f).coerceIn(0.04f, 0.25f)
                                                            val dA = dep * (2f * mc / tot)
                                                            val dC = dep * (2f * ma / tot)
                                                            if (abs(ny) > abs(nx)) {
                                                                a.squashX = 1f + dA; a.squashY = 1f - dA
                                                                c.squashX = 1f + dC; c.squashY = 1f - dC
                                                            } else {
                                                                a.squashX = 1f - dA; a.squashY = 1f + dA
                                                                c.squashX = 1f - dC; c.squashY = 1f + dC
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        balls.forEach(::clampIn)
                                    }
                                    // upward-speed cap: impulse chains on a packed
                                    // pile can launch the last fruit sky-high
                                    balls.forEach { if (it.vy < -RISE_V) it.vy = -RISE_V }
                                }

                                // fixed timestep: identical step sizes keep friction and
                                // restitution consistent regardless of frame hitches
                                acc += dt
                                var steps = 0
                                while (acc >= STEP && steps < MAX_STEPS) {
                                    repeat(SUBSTEPS) { step(STEP / SUBSTEPS) }
                                    acc -= STEP
                                    steps++
                                }
                                if (steps == MAX_STEPS) acc = 0f

                                // once per frame: sleep bookkeeping + support check + juice
                                balls.forEach { b ->
                                    if (b.vx * b.vx + b.vy * b.vy < SLEEP_V * SLEEP_V) {
                                        b.still += dt
                                    } else {
                                        b.still = 0f
                                    }
                                    if (b.still >= SLEEP_MS) { b.vx = 0f; b.vy = 0f }
                                    // squash & stretch: elongate while falling fast, spring back
                                    val flying = b.y < h - WALL_PAD - b.r - 2f && abs(b.vy) > 550f
                                    val tx = if (flying) 0.92f else 1f
                                    val ty = if (flying) 1.10f else 1f
                                    b.squashX += (tx - b.squashX) * min(1f, dt * 9)
                                    b.squashY += (ty - b.squashY) * min(1f, dt * 9)
                                    b.pop += (1f - b.pop) * min(1f, dt * 11)
                                    // merged fruit grows into its new radius instead
                                    // of instantly overlapping neighbors — top cause
                                    // of pop-ups on a full pile
                                    if (b.r < b.targetR) {
                                        b.r += (b.targetR - b.r) * min(1f, dt * 10)
                                    }
                                }
                                // wake sleepers whose support disappeared (fruit below merged)
                                balls.forEach { b ->
                                    if (b.still >= SLEEP_MS && b.y + b.r < h - WALL_PAD - 6f &&
                                        balls.none { o ->
                                            o !== b &&
                                                abs(o.x - b.x) < b.r + o.r + 8f &&
                                                abs(o.y - b.y) < b.r + o.r + 8f
                                        }
                                    ) b.still = 0f
                                }
                                puffs.forEach { it.age += dt }
                                puffs.removeAll { it.age > 0.4f }
                                previewPop += (1f - previewPop) * min(1f, dt * 12)

                                // apply merges
                                if (pendingMerges.isNotEmpty()) {
                                    balls.removeAll { it.id < 0 }
                                    pendingMerges.forEach { (mx, my, t) ->
                                        // a merge removes a support point — wake
                                        // fruits near it, not the whole pile
                                        val wakeR = unit * 3f
                                        balls.forEach { b ->
                                            val dx = b.x - mx
                                            val dy = b.y - my
                                            if (dx * dx + dy * dy < wakeR * wakeR) {
                                                b.still = 0f
                                            }
                                        }
                                        val nt = t + 1
                                        puffs += Puff(mx, my, radiusFor(min(t, TIERS.lastIndex), unit) * 0.8f, 0.6f)
                                        if (nt <= TIERS.lastIndex) {
                                            balls += Ball(
                                                id = uid++,
                                                tier = nt,
                                                x = mx.coerceIn(radiusFor(nt, unit), w - radiusFor(nt, unit)),
                                                y = my,
                                                vy = -80f,
                                                r = radiusFor(t, unit),
                                                targetR = radiusFor(nt, unit),
                                                spawnMs = frame,
                                                popStart = 1.3f,
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
                    val pr = p.r * (1f + t * 1.5f)
                    Box(
                        Modifier
                            .offset { IntOffset((p.x - pr).roundToInt(), (p.y - pr).roundToInt()) }
                            .size(with(density) { (pr * 2).toDp() })
                            .border(2.dp, Color(0xFFFFB74D).copy(alpha = (1f - t) * p.alpha), CircleShape),
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
                                            WALL_PAD + radiusFor(nextTier, unit),
                                            w - WALL_PAD - radiusFor(nextTier, unit),
                                        )
                                    },
                                    onDragEnd = {
                                        val nowT = System.currentTimeMillis()
                                        if (nowT - lastDrop > DROP_CD_MS) {
                                            lastDrop = nowT
                                            val dropX = (aimX ?: (w / 2)).coerceIn(
                                                WALL_PAD + radiusFor(nextTier, unit),
                                                w - WALL_PAD - radiusFor(nextTier, unit),
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
                                            puffs += Puff(dropX, r0 + 12f, r0 * 0.8f, 0.5f)
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
                                            WALL_PAD + radiusFor(nextTier, unit),
                                            w - WALL_PAD - radiusFor(nextTier, unit),
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
                                        puffs += Puff(dropX, r0 + 12f, r0 * 0.8f, 0.5f)
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
