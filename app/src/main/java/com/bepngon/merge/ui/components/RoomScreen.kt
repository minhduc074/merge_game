package com.bepngon.merge.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepngon.merge.game.DecorCatalog
import com.bepngon.merge.game.DecorDef
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.GameState
import com.bepngon.merge.game.ItemCatalog
import com.bepngon.merge.game.ItemSource
import com.bepngon.merge.game.PetCatalog
import com.bepngon.merge.game.PetDef
import com.bepngon.merge.ui.theme.Cocoa
import com.bepngon.merge.ui.theme.CreamBg
import com.bepngon.merge.ui.theme.Gold
import com.bepngon.merge.ui.theme.MergeGreen
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun RoomScreen(
    game: GameState,
    nowMs: Long,
    onBuyDecor: (String) -> Unit,
    onAdoptPet: (String) -> Unit,
    onFeedPetCoins: () -> Unit,
    onFeedPetItem: (ItemSource, Int) -> Unit,
    onOpenMiniGame: () -> Unit,
    onClose: () -> Unit,
) {
    var panel by remember { mutableStateOf(RoomPanel.NONE) }

    Box(Modifier.fillMaxSize().background(CreamBg)) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "🏠 Tiệm của bé",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Cocoa,
                    modifier = Modifier.weight(1f),
                )
                Chip("🪙 ${game.coins}") {}
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

            // ---- the room scene ----
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFE9C9), Color(0xFFF7C9A0)),
                        ),
                    ),
            ) {
                val rw = constraints.maxWidth.toFloat()
                val rh = constraints.maxHeight.toFloat()
                val density = LocalDensity.current
                val ctx = LocalContext.current
                val bgRes = remember { artRes(ctx, "bg_cafe") }
                if (bgRes != 0) {
                    Image(
                        painter = painterResource(bgRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                DecorCatalog.ALL.forEach { d ->
                    if (d.id in game.decorOwned) {
                        GameArt(
                            name = d.id,
                            emoji = d.emoji,
                            size = with(density) { (rw * d.w).toDp() },
                            modifier = Modifier.offset {
                                IntOffset((rw * d.x).roundToInt(), (rh * d.y).roundToInt())
                            },
                        )
                    }
                }
                game.pet?.let { pet ->
                    PetSprite(
                        petId = pet.defId,
                        fed = pet.isFed(nowMs),
                        roomW = rw,
                        roomH = rh,
                        onClick = { panel = RoomPanel.PET },
                    )
                }
            }

            // ---- action bar ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip("🛋️ Decor") { panel = RoomPanel.DECOR }
                Chip("🐾 Thú cưng") { panel = RoomPanel.PET }
                val miniNote = if (game.miniPlaysLeft > 0) {
                    "${game.miniPlaysLeft} lượt"
                } else {
                    "đang hồi"
                }
                Chip("🍉 Thả trái ($miniNote)") { onOpenMiniGame() }
            }

            // ---- bottom panels ----
            when (panel) {
                RoomPanel.DECOR -> DecorShop(game, onBuyDecor)
                RoomPanel.PET -> PetPanel(
                    game = game,
                    nowMs = nowMs,
                    onAdoptPet = onAdoptPet,
                    onFeedPetCoins = onFeedPetCoins,
                    onFeedPetItem = onFeedPetItem,
                )
                RoomPanel.NONE -> {}
            }
        }
    }
}

private enum class RoomPanel { NONE, DECOR, PET }

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Cocoa,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, Color(0x33A0522D), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun PetSprite(
    petId: String,
    fed: Boolean,
    roomW: Float,
    roomH: Float,
    onClick: () -> Unit,
) {
    val t by rememberInfiniteTransition(label = "petwalk").animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(14_000)),
        label = "t",
    )
    // Wander left-right across the floor, hopping a little while moving.
    val px = (0.5f + 0.32f * sin(t)) * roomW
    val py = roomH * 0.82f - (8f * kotlin.math.abs(sin(t * 4)))
    val dir = if (kotlin.math.cos(t) >= 0f) 1f else -1f
    Column(
        Modifier
            .offset { IntOffset(px.roundToInt(), py.roundToInt()) }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!fed) {
            Text("😢 đói...", fontSize = 12.sp, color = Cocoa)
        }
        GameArt(
            name = "cust_$petId",
            emoji = "🐾",
            size = 54.dp,
            modifier = Modifier.scale(scaleX = dir, scaleY = 1f),
        )
    }
}

@Composable
private fun DecorShop(game: GameState, onBuyDecor: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(10.dp)
            .height(190.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("🛋️ Cửa hàng decor", fontWeight = FontWeight.ExtraBold, color = Cocoa)
        DecorCatalog.ALL.forEach { d -> DecorRow(d, game, onBuyDecor) }
    }
}

@Composable
private fun DecorRow(d: DecorDef, game: GameState, onBuyDecor: (String) -> Unit) {
    val owned = d.id in game.decorOwned
    val locked = game.level < d.unlockLevel
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameArt(d.id, d.emoji, 34.dp)
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(d.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Cocoa)
            val buff = buildList {
                if (d.orderCoinsPct > 0) add("+${d.orderCoinsPct}% vàng order")
                if (d.genCooldownPct > 0) add("gen nhanh hơn ${d.genCooldownPct}%")
                if (d.energyRegenPct > 0) add("⚡ nhanh hơn ${d.energyRegenPct}%")
            }.joinToString(" · ")
            if (buff.isNotEmpty()) {
                Text(buff, fontSize = 11.sp, color = MergeGreen)
            }
        }
        when {
            owned -> Text("✅", fontSize = 16.sp)
            locked -> Text("Lv${d.unlockLevel} 🔒", fontSize = 12.sp, color = Cocoa)
            else -> Text(
                "🪙${d.cost}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (game.coins >= d.cost) Cocoa else Color.Gray,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (game.coins >= d.cost) Gold else Color(0xFFDDDDDD))
                    .clickable(enabled = game.coins >= d.cost) { onBuyDecor(d.id) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun PetPanel(
    game: GameState,
    nowMs: Long,
    onAdoptPet: (String) -> Unit,
    onFeedPetCoins: () -> Unit,
    onFeedPetItem: (ItemSource, Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(10.dp)
            .height(200.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val pet = game.pet
        if (pet == null) {
            Text("🐾 Chọn 1 bé để nuôi", fontWeight = FontWeight.ExtraBold, color = Cocoa)
            PetCatalog.ALL.forEach { p -> PetRow(p, game, onAdoptPet) }
        } else {
            val def = PetCatalog.byId(pet.defId)
            val fed = pet.isFed(nowMs)
            Row(verticalAlignment = Alignment.CenterVertically) {
                GameArt("cust_${pet.defId}", def.emoji, 40.dp)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(def.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Cocoa)
                    Text(def.buffDesc, fontSize = 11.sp, color = MergeGreen)
                    if (fed) {
                        val left = (pet.fedUntilMs - nowMs) / 60_000
                        Text("No — còn ${left}p buff", fontSize = 11.sp, color = Cocoa)
                        LinearProgressIndicator(
                            progress = {
                                ((pet.fedUntilMs - nowMs).toFloat() /
                                    GameEngine.PET_FEED_MS).coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            color = MergeGreen,
                        )
                    } else {
                        Text("😢 Đang đói — buff tạm tắt", fontSize = 11.sp, color = Color(0xFFE65100))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("🍽️ Cho ăn 🪙${GameEngine.PET_FEED_COINS}") { onFeedPetCoins() }
                Text(
                    "hoặc bấm món bên dưới để cho ăn",
                    fontSize = 11.sp,
                    color = Cocoa,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
            // Item picker: any item on board or in the bag can be fed to the pet.
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                game.cells.forEachIndexed { i, it ->
                    if (it != null) FeedIcon(it.defId) { onFeedPetItem(ItemSource.BOARD, i) }
                }
                game.inventory.forEachIndexed { i, it ->
                    if (it != null) FeedIcon(it.defId) { onFeedPetItem(ItemSource.INVENTORY, i) }
                }
            }
            Text(
                "🔄 Đổi thú khác:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Cocoa,
                modifier = Modifier.padding(top = 4.dp),
            )
            PetCatalog.ALL.filter { it.id != pet.defId }
                .forEach { p -> PetRow(p, game, onAdoptPet) }
        }
    }
}

@Composable
private fun FeedIcon(defId: String, onClick: () -> Unit) {
    val def = ItemCatalog[defId]
    Box(
        Modifier
            .padding(3.dp)
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CreamBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GameArt(defId, def.emoji, 30.dp)
    }
}

@Composable
private fun PetRow(p: PetDef, game: GameState, onAdoptPet: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameArt("cust_${p.id}", p.emoji, 32.dp)
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Cocoa)
            Text(p.buffDesc, fontSize = 11.sp, color = MergeGreen)
        }
        Text(
            "🪙${p.cost}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (game.coins >= p.cost) Cocoa else Color.Gray,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (game.coins >= p.cost) Gold else Color(0xFFDDDDDD))
                .clickable(enabled = game.coins >= p.cost) { onAdoptPet(p.id) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
