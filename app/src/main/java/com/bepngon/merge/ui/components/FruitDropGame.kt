package com.bepngon.merge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepngon.merge.game.GameState
import com.bepngon.merge.game.Subak
import com.bepngon.merge.game.SubakEngine
import com.bepngon.merge.ui.theme.CellBorder
import com.bepngon.merge.ui.theme.CellWhite
import com.bepngon.merge.ui.theme.Cocoa
import com.bepngon.merge.ui.theme.CreamBg
import com.bepngon.merge.ui.theme.Gold
import com.bepngon.merge.ui.theme.MergeGreen
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Fruit-drop minigame — Suika-style: drop fruits, identical ones merge
 * into a bigger fruit, overflow the box and it's game over.
 */

private val FRUIT_EMOJI = listOf(
    "🫐", "🍇", "🍋", "🍊", "🍎", "🍈", "🍐", "🍑", "🍍", "🍈", "🍉",
)
private fun fruitRes(i: Int) = "subak_${Subak.FRUIT_NAMES[i]}"

// board tokens — Bếp Ngon palette
private val CBorder = Color(0x1A6D4C41)       // cocoa 10%
private val CBorderLight = Color(0x136D4C41)  // cocoa ~7.5%
private val CText = Color(0xFF6D4C41)         // Cocoa
private val CDanger = Color(0xFFEF5350)       // WarnRed
private val CRing = Color(0xFFFFB300)         // Gold

@Composable
private fun FruitSprite(index: Int, sizeDp: androidx.compose.ui.unit.Dp, alpha: Float = 1f) {
    GameArt(fruitRes(index), FRUIT_EMOJI[index], sizeDp, alpha = alpha)
}

@Composable
fun FruitDropGame(
    game: GameState,
    onFinish: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val engine = remember { SubakEngine() }
    var finished by remember { mutableStateOf(false) }
    var overAt by remember { mutableLongStateOf(0L) }
    var lastDrop by remember { mutableLongStateOf(0L) }
    var aimX by remember { mutableStateOf<Float?>(null) }
    var tick by remember { mutableIntStateOf(0) }

    Box(
        Modifier
            .fillMaxSize()
            .background(CreamBg),
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            // header
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
                    "✕",
                    fontSize = 22.sp,
                    color = Cocoa,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFFFE0B2))
                        .clickable(onClick = {
                            if (!finished) onFinish(engine.score) else onClose()
                        })
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }

            // sidebar row: NEXT | SCORE | CYCLE
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 14.dp),
                ) {
                    Text("NEXT", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = CText, letterSpacing = 1.sp)
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CellWhite)
                            .border(2.dp, CellBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        FruitSprite(engine.nextFruitIndex, 30.dp)
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 14.dp),
                ) {
                    Text("SCORE", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = CText, letterSpacing = 1.sp)
                    Text(
                        "${engine.score}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CText,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CellWhite)
                            .border(2.dp, CellBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                // circle of evolution: all fruits in a 2-row snake inside a pill
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CellWhite)
                        .border(2.dp, CellBorder, RoundedCornerShape(20.dp))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column {
                        Row {
                            for (i in 0..5) {
                                FruitSprite(i, 22.dp)
                            }
                        }
                        Row {
                            for (i in 10 downTo 6) {
                                FruitSprite(i, 22.dp)
                            }
                        }
                    }
                }
            }

            // gameplay area — warm cream tub with a cocoa border, like the
            // merge board: 2:3 field in the game's palette
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CellWhite)
                    .border(3.dp, Cocoa, RoundedCornerShape(16.dp)),
            ) {
                val w = constraints.maxWidth.toFloat()
                val h = constraints.maxHeight.toFloat()
                val pxScale = w / Subak.GAME_WIDTH   // meters → px
                fun m(v: Float) = v * pxScale
                // fit the physics world to the real field aspect so the
                // floor sits at the container's visible bottom edge
                SideEffect { engine.configure(h / pxScale) }
                val dangerPx = m(engine.gameOverHeight)

                if (!finished) {
                    LaunchedEffect(w, h) {
                        var last = System.currentTimeMillis()
                        while (true) {
                            withFrameMillis {
                                val frame = System.currentTimeMillis()
                                engine.step((frame - last).toFloat())
                                last = frame
                                tick++
                                if (engine.gameOver) {
                                    finished = true
                                    overAt = frame
                                }
                            }
                        }
                    }
                }

                // restricted zone hatch + game-over line + drop line + rings
                Canvas(Modifier.fillMaxSize()) {
                    tick // subscribe to frames
                    // 45° hatch, 15px spacing, clipped to the top band
                    val step = 15f * sqrt(2f)
                    var c = 0f
                    while (c <= w + dangerPx) {
                        drawLine(
                            CBorderLight,
                            Offset(c, 0f),
                            Offset(c - dangerPx, dangerPx),
                            strokeWidth = 1f,
                        )
                        c += step
                    }
                    // danger line — the game's warn-red, dashed
                    val dash = 14f
                    var dx = 0f
                    while (dx < w) {
                        drawLine(
                            CDanger.copy(alpha = 0.65f),
                            Offset(dx, dangerPx),
                            Offset(min(dx + dash * 0.6f, w), dangerPx),
                            strokeWidth = 4f,
                        )
                        dx += dash
                    }

                    // drop guide line at aim x
                    if (!finished) {
                        val ax = aimX
                        if (ax != null) {
                            drawLine(
                                CBorder,
                                Offset(ax, 0f),
                                Offset(ax, h),
                                strokeWidth = 2f,
                            )
                        }
                    }

                    // merge effects: gold ring expands r → 5r, fades out over 1s
                    engine.mergeEffects.forEach { e ->
                        val t = ((engine.engineMs - e.startMs).toFloat() /
                            Subak.MERGE_EFFECT_MS).coerceIn(0f, 1f)
                        val ease = 1f - (1f - t).let { it * it * it * it * it }
                        drawCircle(
                            CRing.copy(alpha = (1f - ease) * 0.55f),
                            radius = m(e.radius) * (1f + ease * 4f),
                            center = Offset(m(e.x), m(e.y)),
                            style = Stroke(width = 3f),
                        )
                    }

                    // danger ring around the fruit that ended the game
                    if (finished) {
                        val gf = engine.fruits.firstOrNull { it.id == engine.gameOverFruitId }
                        if (gf != null) {
                            drawCircle(
                                CDanger,
                                radius = m(gf.radius) + 10f,
                                center = Offset(
                                    m(gf.body.position.x),
                                    m(gf.body.position.y),
                                ),
                                style = Stroke(width = 4f),
                            )
                        }
                    }
                }

                // fruits — interpolated transforms + real rotation
                val density = LocalDensity.current
                val t = engine.interpolationT()
                engine.fruits.forEach { f ->
                    tick
                    val p = f.body.position
                    val ix = f.prevX * (1f - t) + p.x * t
                    val iy = f.prevY * (1f - t) + p.y * t
                    val rot = (f.prevRot * (1f - t) + f.body.angle * t) * 180f / PI.toFloat()
                    val r = m(f.radius)
                    Box(
                        Modifier
                            .offset { IntOffset((m(ix) - r).roundToInt(), (m(iy) - r).roundToInt()) }
                            .size(with(density) { (r * 2).toDp() })
                            .graphicsLayer { rotationZ = rot },
                        contentAlignment = Alignment.Center,
                    ) {
                        FruitSprite(f.fruitIndex, with(density) { (r * 2).toDp() })
                    }
                }

                // preview fruit at drop height following the pointer
                if (!finished) {
                    val cur = Subak.FRUITS[engine.currentFruitIndex]
                    val r = m(cur.radius)
                    val ax = (aimX ?: (w / 2f)).coerceIn(r, w - r)
                    val ay = m(engine.gameOverHeight / 2f)
                    Box(
                        Modifier
                            .offset { IntOffset((ax - r).roundToInt(), (ay - r).roundToInt()) }
                            .size(with(density) { (r * 2).toDp() })
                            .graphicsLayer { this.alpha = 0.9f },
                        contentAlignment = Alignment.Center,
                    ) {
                        FruitSprite(engine.currentFruitIndex,
                            with(density) { (r * 2).toDp() })
                    }

                    // input: drag to aim + release drops, tap drops immediately
                    Box(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(w) {
                                detectDragGestures(
                                    onDrag = { change, _ ->
                                        change.consume()
                                        aimX = change.position.x.coerceIn(r, w - r)
                                    },
                                    onDragEnd = {
                                        val nowT = System.currentTimeMillis()
                                        if (nowT - lastDrop > Subak.DROP_COOLDOWN_MS) {
                                            lastDrop = nowT
                                            engine.dropFruit(
                                                (aimX ?: (w / 2f)).coerceIn(r, w - r) / pxScale
                                            )
                                        }
                                    },
                                )
                            }
                            .pointerInput(w) {
                                detectTapGestures { pos ->
                                    val nowT = System.currentTimeMillis()
                                    if (nowT - lastDrop > Subak.DROP_COOLDOWN_MS) {
                                        lastDrop = nowT
                                        aimX = pos.x.coerceIn(r, w - r)
                                        engine.dropFruit(aimX!! / pxScale)
                                    }
                                }
                            },
                    )
                }

                // game-over overlay — appears after 1.5s like the original
                if (finished && System.currentTimeMillis() - overAt > 1500) {
                    tick
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xCCFFF8E7)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🍉 Tràn rồi!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Cocoa)
                            Text("Điểm: ${engine.score}", fontSize = 18.sp, color = Cocoa,
                                modifier = Modifier.padding(top = 6.dp))
                            val e = minOf(
                                com.bepngon.merge.game.GameEngine.MINI_MAX_ENERGY,
                                engine.score / 10,
                            )
                            Text("+$e ⚡", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                color = MergeGreen, modifier = Modifier.padding(top = 4.dp))
                            if (engine.score >= game.miniBest && engine.score > 0) {
                                Text("🏅 Kỷ lục mới!", fontSize = 13.sp, color = Gold,
                                    modifier = Modifier.padding(top = 2.dp))
                            }
                            Text(
                                "🍹 Nhận thưởng",
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
            Text(
                "Thả trái trùng nhau để merge. Tràn quá vạch là thua!",
                fontSize = 11.sp,
                color = Cocoa.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
