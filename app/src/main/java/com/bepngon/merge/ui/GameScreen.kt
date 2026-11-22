package com.bepngon.merge.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bepngon.merge.BuildConfig
import com.bepngon.merge.game.BoardItem
import com.bepngon.merge.game.Customers
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.GameEvent
import com.bepngon.merge.game.ItemCatalog
import com.bepngon.merge.game.ItemSource
import com.bepngon.merge.game.LevelReward
import com.bepngon.merge.game.QteReward
import com.bepngon.merge.sound.SoundManager
import com.bepngon.merge.ui.components.AchievementsScreen
import com.bepngon.merge.ui.components.BagScreen
import com.bepngon.merge.ui.components.DailyRewardDialog
import com.bepngon.merge.ui.components.GameArt
import com.bepngon.merge.ui.components.FruitDropGame
import com.bepngon.merge.ui.components.OnboardingScreen
import com.bepngon.merge.ui.components.OrderBar
import com.bepngon.merge.ui.components.RoomScreen
import com.bepngon.merge.ui.components.TopBar
import com.bepngon.merge.ui.theme.CellWhite
import com.bepngon.merge.ui.theme.Cocoa
import com.bepngon.merge.ui.theme.CreamBg
import com.bepngon.merge.ui.theme.Gold
import com.bepngon.merge.ui.theme.MergeGreen
import com.bepngon.merge.ui.theme.WarnRed
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun GameScreen(vm: GameViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Box(
        Modifier
            .fillMaxSize()
            .background(CreamBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .displayCutoutPadding(),
    ) {
        if (ui.loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            GameContent(vm, ui)
        }

        if (ui.showBag) {
            BagScreen(
                inventory = ui.game.inventory,
                cap = ui.game.inventoryCap,
                coins = ui.game.coins,
                onBuySlot = vm::buyInventorySlot,
                onRetrieve = vm::retrieveToBoard,
                onClose = vm::closeBag,
            )
        }

        if (ui.showAchievements) {
            AchievementsScreen(
                game = ui.game,
                onClaim = vm::claimAchievement,
                onClose = vm::closeAchievements,
            )
        }

        if (ui.showRoom) {
            RoomScreen(
                game = ui.game,
                nowMs = ui.nowMs,
                onBuyDecor = vm::buyDecor,
                onAdoptPet = vm::adoptPet,
                onFeedPetCoins = vm::feedPetCoins,
                onOpenMiniGame = vm::openMiniGame,
                onClose = vm::closeRoom,
            )
        }

        if (ui.showMiniGame) {
            FruitDropGame(
                game = ui.game,
                onFinish = vm::finishMiniGame,
                onClose = vm::closeMiniGame,
            )
        }

        if (ui.showDaily) {
            val now = System.currentTimeMillis()
            DailyRewardDialog(
                ready = GameEngine.dailyReady(ui.game, now),
                msUntilReady = (GameEngine.DAILY_COOLDOWN_MS -
                    (now - ui.game.lastDailyClaimEpochMs)).coerceAtLeast(0),
                nextDay = GameEngine.dailyDayIndex(ui.game, now),
                onClaim = vm::claimDaily,
                onClose = vm::closeDaily,
            )
        }

        if (ui.showOnboarding || ui.showGuide) {
            OnboardingScreen(
                onDone = if (ui.showGuide) vm::closeGuide else vm::finishOnboarding,
            )
        }

        if (ui.showRestartConfirm) {
            AlertDialog(
                onDismissRequest = vm::dismissRestart,
                title = { Text("Chơi lại từ đầu?") },
                text = { Text("Toàn bộ tiến trình hiện tại sẽ bị xoá.") },
                confirmButton = {
                    TextButton(onClick = vm::restart) { Text("Chơi lại", color = WarnRed) }
                },
                dismissButton = {
                    TextButton(onClick = vm::dismissRestart) { Text("Huỷ") }
                },
            )
        }
    }
}

private data class DragState(
    val source: ItemSource,
    val index: Int,
    val uid: Long,
    val defId: String,
    val position: Offset,
)

private data class Floater(
    val id: Long,
    val text: String,
    val position: Offset,
    val color: Color,
)

private data class FlyAnim(
    val id: Long,
    val resName: String,
    val emoji: String,
    val from: Offset,
    val to: Offset,
)

@Composable
private fun GameContent(vm: GameViewModel, ui: GameUiState) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    val cellBounds = remember { mutableStateMapOf<Int, Rect>() }
    val invBounds = remember { mutableStateMapOf<Int, Rect>() }
    val orderBounds = remember { mutableStateMapOf<Long, Rect>() }

    var drag by remember { mutableStateOf<DragState?>(null) }
    var hoverCell by remember { mutableStateOf<Int?>(null) }
    var hoverInv by remember { mutableStateOf<Int?>(null) }
    var floaters by remember { mutableStateOf(listOf<Floater>()) }
    var floaterId by remember { mutableStateOf(0L) }
    var levelBanner by remember { mutableStateOf<String?>(null) }
    var infoItem by remember { mutableStateOf<BoardItem?>(null) }
    var genListItem by remember { mutableStateOf<BoardItem?>(null) }
    var statInfo by remember { mutableStateOf<String?>(null) }
    var levelTaps by remember { mutableIntStateOf(0) }
    var coinBounds by remember { mutableStateOf<Rect?>(null) }
    var contentOrigin by remember { mutableStateOf(Offset.Zero) }
    var flies by remember { mutableStateOf(listOf<FlyAnim>()) }
    var flyId by remember { mutableStateOf(0L) }
    var sparkles by remember { mutableStateOf(listOf<Offset>()) }

    fun pushFly(resName: String, emoji: String, from: Offset, to: Offset) {
        val f = FlyAnim(flyId++, resName, emoji, from, to)
        flies = flies + f
        scope.launch {
            kotlinx.coroutines.delay(600)
            flies = flies - f
        }
    }

    fun pushFloater(text: String, pos: Offset, color: Color) {
        val f = Floater(floaterId++, text, pos, color)
        floaters = floaters + f
        scope.launch {
            kotlinx.coroutines.delay(900)
            floaters = floaters - f
        }
    }

    val onDragStart: (ItemSource, Int, Long, String, Offset) -> Unit =
        { source, index, uid, defId, pos ->
            drag = DragState(source, index, uid, defId, pos)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }

    val onDrag: (Offset) -> Unit = { pos ->
        drag?.let { d ->
            drag = d.copy(position = pos)
            val overOrder = ui.game.orders.any { o ->
                orderBounds[o.id]?.contains(pos) == true
            }
            hoverInv = invBounds.entries.firstOrNull { it.value.contains(pos) }?.key
            hoverCell = if (overOrder || hoverInv != null) {
                null
            } else {
                cellBounds.entries.firstOrNull { it.value.contains(pos) }?.key
            }
        }
    }

    val onDragEnd: () -> Unit = {
        val d = drag
        if (d != null) {
            val orderHit = ui.game.orders.firstOrNull { o ->
                orderBounds[o.id]?.contains(d.position) == true
            }?.id
            val invHit = invBounds.entries.firstOrNull { it.value.contains(d.position) }?.key
            val cellHit = cellBounds.entries.firstOrNull { it.value.contains(d.position) }?.key
            when {
                orderHit != null -> vm.deliverToOrder(orderHit, d.source, d.index)
                invHit != null -> when (d.source) {
                    ItemSource.BOARD -> vm.boardToInventory(d.index, invHit)
                    ItemSource.INVENTORY -> vm.moveInInventory(d.index, invHit)
                }
                cellHit != null -> when (d.source) {
                    ItemSource.BOARD -> vm.moveOrMerge(d.index, cellHit)
                    ItemSource.INVENTORY -> vm.inventoryToBoard(d.index, cellHit)
                }
            }
        }
        drag = null
        hoverCell = null
        hoverInv = null
    }

    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                is GameEvent.Merged -> {
                    SoundManager.playMerge()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    cellBounds[e.cellIndex]?.let { r ->
                        pushFloater("+${e.scoreGained}", r.center, MergeGreen)
                        sparkles = sparkles + r.center
                        scope.launch {
                            kotlinx.coroutines.delay(650)
                            sparkles = sparkles.drop(1)
                        }
                    }
                }
                is GameEvent.Spawned -> {
                    SoundManager.play(SoundManager.Sfx.Spawn)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                is GameEvent.Unfrozen -> {
                    SoundManager.play(SoundManager.Sfx.Unlock)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    cellBounds[e.cellIndex]?.let { r ->
                        pushFloater("❄️ Mở băng!", r.center, Color(0xFF4FC3F7))
                    }
                }
                is GameEvent.OrderSpawned -> {
                    SoundManager.play(SoundManager.Sfx.Tap)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                is GameEvent.OrderDelivered -> {
                    SoundManager.play(SoundManager.Sfx.Deliver)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                is GameEvent.OrderCompleted -> {
                    SoundManager.play(SoundManager.Sfx.Complete)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val pos = orderBounds[e.order.id]?.center
                        ?: Offset(300f, 200f)
                    pushFloater("🎉 +${e.coinsGained}🪙 +${e.xpGained}XP", pos, MergeGreen)
                    coinBounds?.center?.let { wallet ->
                        pushFly("icon_coin", "🪙", pos, wallet)
                    }
                }
                is GameEvent.AutoFulfillFly -> {
                    val dest = orderBounds[e.orderId]?.center ?: Offset(300f, 200f)
                    e.flown.forEach { (defId, fromIdx) ->
                        val from = if (fromIdx >= 0) {
                            cellBounds[fromIdx]?.center
                        } else {
                            invBounds[-1 - fromIdx]?.center
                        } ?: return@forEach
                        pushFly(defId, ItemCatalog[defId].emoji, from, dest)
                    }
                }
                is GameEvent.LevelUp -> {
                    SoundManager.play(SoundManager.Sfx.LevelUp)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    levelBanner = "🎉 Lên cấp ${e.level}! ${rewardText(e.reward)}"
                    scope.launch {
                        kotlinx.coroutines.delay(2800)
                        levelBanner = null
                    }
                }
                is GameEvent.CellsUnlocked -> {
                    SoundManager.play(SoundManager.Sfx.Unlock)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    levelBanner = "🔓 Mở khóa ${e.count} ô mới!"
                    scope.launch {
                        kotlinx.coroutines.delay(2500)
                        levelBanner = null
                    }
                }
                is GameEvent.GeneratorReady -> {
                    SoundManager.play(SoundManager.Sfx.Tap)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    cellBounds[e.cellIndex]?.let { r ->
                        pushFloater("⚡ xong!", r.center, MergeGreen)
                    }
                }
                is GameEvent.SlotBought -> {
                    SoundManager.play(SoundManager.Sfx.Coin)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    pushFloater("+1 ô kho! 🎒", Offset(300f, 1400f), MergeGreen)
                }
                is GameEvent.AchievementUnlocked -> {
                    SoundManager.play(SoundManager.Sfx.Achieve)
                    levelBanner = "${e.achievement.emoji} Thành tựu: ${e.achievement.title}!"
                    scope.launch {
                        kotlinx.coroutines.delay(2200)
                        levelBanner = null
                    }
                }
                is GameEvent.AchievementClaimed -> {
                    SoundManager.play(SoundManager.Sfx.Achieve)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    pushFloater(
                        "+${e.achievement.rewardCoins}🪙 +${e.achievement.rewardEnergy}⚡",
                        Offset(540f, 300f),
                        Gold,
                    )
                }
                is GameEvent.DailyClaimed -> {
                    SoundManager.play(SoundManager.Sfx.Daily)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    levelBanner = "🎁 Ngày ${e.day}: +${e.coins}🪙 +${e.energy}⚡" +
                        if (e.wildcard) " +🪄" else ""
                    scope.launch {
                        kotlinx.coroutines.delay(1800)
                        levelBanner = null
                    }
                    vm.closeDaily()
                }
                is GameEvent.ComboBonus -> {
                    SoundManager.play(SoundManager.Sfx.LevelUp)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    levelBanner = "🔥 COMBO x${e.combo}! +${e.coins}🪙 +${e.xp}XP"
                    scope.launch {
                        kotlinx.coroutines.delay(2000)
                        levelBanner = null
                    }
                }
                GameEvent.DailyNotReady -> {
                    SoundManager.play(SoundManager.Sfx.Error)
                }
                is GameEvent.QteOffered -> {
                    SoundManager.play(SoundManager.Sfx.Achieve)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                is GameEvent.QteClaimed -> {
                    SoundManager.play(SoundManager.Sfx.Daily)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    levelBanner = when (e.reward) {
                        QteReward.WILDCARD -> "🪄 Đũa phép! Kéo lên item để nhân bản"
                        QteReward.FRENZY -> "🔥 Điên cuồng! Gen miễn phí 90 giây!"
                        QteReward.ENERGY -> "⚡ +${GameEngine.QTE_ENERGY} năng lượng!"
                        QteReward.COINS -> "🪙 +${GameEngine.QTE_COINS} vàng!"
                        QteReward.RECHARGE -> "🔋 Mọi generator đã sạc đầy!"
                    }
                    scope.launch {
                        kotlinx.coroutines.delay(2500)
                        levelBanner = null
                    }
                }
                is GameEvent.WildcardSpawned -> {
                    SoundManager.play(SoundManager.Sfx.Achieve)
                    val r = if (e.index >= 0) {
                        cellBounds[e.index]
                    } else {
                        invBounds[-1 - e.index]
                    }
                    r?.let {
                        sparkles = sparkles + it.center
                        scope.launch {
                            kotlinx.coroutines.delay(650)
                            sparkles = sparkles.drop(1)
                        }
                    }
                }
                is GameEvent.ItemDuplicated -> {
                    SoundManager.playMerge()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val r = if (e.index >= 0) {
                        cellBounds[e.index]
                    } else {
                        invBounds[-1 - e.index]
                    }
                    r?.let {
                        pushFloater("🪄 Nhân bản!", it.center, MergeGreen)
                        sparkles = sparkles + it.center
                        scope.launch {
                            kotlinx.coroutines.delay(650)
                            sparkles = sparkles.drop(1)
                        }
                    }
                }
                is GameEvent.FrenzyStarted -> {
                    SoundManager.play(SoundManager.Sfx.LevelUp)
                }
                is GameEvent.DecorBought -> {
                    SoundManager.play(SoundManager.Sfx.Coin)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                is GameEvent.PetAdopted -> {
                    SoundManager.play(SoundManager.Sfx.Achieve)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    levelBanner = "🐾 Bé ${Customers.byId(e.defId).name} về nhà!"
                    scope.launch {
                        kotlinx.coroutines.delay(2200)
                        levelBanner = null
                    }
                }
                is GameEvent.PetFed -> {
                    SoundManager.play(SoundManager.Sfx.Complete)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                is GameEvent.PetGift -> {
                    SoundManager.play(SoundManager.Sfx.Daily)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    cellBounds[e.cellIndex]?.let { r ->
                        pushFloater("🐼 tặng quà!", r.center, MergeGreen)
                        sparkles = sparkles + r.center
                        scope.launch {
                            kotlinx.coroutines.delay(650)
                            sparkles = sparkles.drop(1)
                        }
                    }
                }
                is GameEvent.MiniGameFinished -> {
                    SoundManager.play(SoundManager.Sfx.Daily)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    levelBanner = "🍉 +${e.energy}⚡ từ mini game!"
                    scope.launch {
                        kotlinx.coroutines.delay(2000)
                        levelBanner = null
                    }
                }
                GameEvent.MiniGameNoPlays -> {
                    SoundManager.play(SoundManager.Sfx.Error)
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    pushFloater("Hết lượt chơi! ⏳", Offset(300f, 1400f), WarnRed)
                }
                GameEvent.OnboardingFinished -> {
                    SoundManager.play(SoundManager.Sfx.Complete)
                }
                is GameEvent.GeneratorUpgraded -> {
                    SoundManager.playMerge()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val pos = cellBounds[e.cellIndex]?.center ?: Offset(540f, 1500f)
                    pushFloater("⬆️ -${e.cost}🪙", pos, Gold)
                    infoItem = infoItem?.copy(
                        defId = e.newDefId,
                        charges = ItemCatalog[e.newDefId].maxCharges,
                    )
                    genListItem = genListItem?.copy(
                        defId = e.newDefId,
                        charges = ItemCatalog[e.newDefId].maxCharges,
                    )
                }
                GameEvent.NotEnoughCoins -> {
                    SoundManager.play(SoundManager.Sfx.Error)
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    pushFloater("Không đủ vàng! 🪙", Offset(300f, 1400f), WarnRed)
                }
                GameEvent.GeneratorEmpty,
                GameEvent.DeliverRejected,
                GameEvent.NoEnergy,
                GameEvent.BoardFull,
                GameEvent.InventoryFull,
                GameEvent.InvalidDrop -> {
                    SoundManager.play(SoundManager.Sfx.Error)
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { contentOrigin = it.positionInWindow() },
    ) {
        TopBar(
            score = ui.game.score,
            coins = ui.game.coins,
            energy = ui.game.energy,
            energyCap = GameEngine.ENERGY_CAP,
            level = ui.game.level,
            xp = ui.game.xp,
            xpNeeded = GameEngine.xpNeededForLevel(ui.game.level),
            dailyReady = vm.dailyReady(),
            onRestart = vm::askRestart,
            onAchievements = vm::openAchievements,
            onDaily = vm::openDaily,
            onGuide = vm::openGuide,
            onRoom = vm::openRoom,
            onCoinBounds = { coinBounds = it },
            onStatTap = { kind -> statInfo = kind },
            onLevelTap = {
                // Debug cheat: 10 silent taps on the Lv badge -> max out.
                if (BuildConfig.DEBUG) {
                    levelTaps++
                    if (levelTaps >= 10) {
                        levelTaps = 0
                        vm.debugMaxOut()
                    }
                }
            },
        )

        OrderBar(
            orders = ui.game.orders,
            dragDefId = drag?.defId,
            canFulfill = { GameEngine.canFulfillOrder(ui.game, it) },
            onFulfill = vm::autoFulfillOrder,
            onItemTap = { infoItem = BoardItem(-1, it) },
            onPositioned = { id, r -> orderBounds[id] = r },
            modifier = Modifier.padding(bottom = 6.dp),
        )

        val frenzyLeft = ui.game.frenzyUntilMs - ui.nowMs
        if (frenzyLeft > 0) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFF7043)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "🔥 GEN MIỄN PHÍ — còn ${frenzyLeft / 1000 + 1}s",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
            }
        }

        if (ui.game.boardFull) {
            Text(
                "Bàn đầy rồi! Merge hoặc giao đơn để dọn chỗ nhé 😅",
                color = WarnRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                textAlign = TextAlign.Center,
            )
        }

        Box(Modifier.weight(1f)) {
            BoardGrid(
                cells = ui.game.cells,
                lockedCells = ui.game.lockedCells,
                nowMs = ui.nowMs,
                drag = drag,
                hoverCell = hoverCell,
                cellBounds = cellBounds,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onTapGenerator = vm::tapGenerator,
                onTapInfo = { infoItem = it },
                onLongPressGen = { genListItem = it },
                modifier = Modifier,
            )
        }

        InventoryBar(
            inventory = ui.game.inventory,
            cap = ui.game.inventoryCap,
            nowMs = ui.nowMs,
            drag = drag,
            hoverInv = hoverInv,
            invBounds = invBounds,
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            onTapInfo = { infoItem = it },
            onLongPressGen = { genListItem = it },
            onOpenBag = vm::openBag,
        )
    }

    // Floaters + banners (window-coordinate space, same as drag overlay)
    floaters.forEach { f ->
        key(f.id) { ScoreFloater(f.copy(position = f.position - contentOrigin)) }
    }
    levelBanner?.let { text ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LevelBanner(text)
        }
    }
    infoItem?.let { item ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { infoItem = null },
            )
            ItemInfoCard(
                item = item,
                nowMs = ui.nowMs,
                onDismiss = { infoItem = null },
                onUpgrade = { vm.upgradeGenerator(item.uid) },
            )
        }
    }
    genListItem?.let { item ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { genListItem = null },
            )
            GenSpawnListCard(
                item = item,
                nowMs = ui.nowMs,
                onDismiss = { genListItem = null },
                onUpgrade = { vm.upgradeGenerator(item.uid) },
            )
        }
    }
    statInfo?.let { kind ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { statInfo = null },
            )
            StatInfoCard(kind = kind, onDismiss = { statInfo = null })
        }
    }

    // Drag overlay
    drag?.let { d ->
        val emoji = ItemCatalog[d.defId].emoji
        val tileSize = 52.dp
        Box(
            Modifier
                .offset {
                    IntOffset(
                        (d.position.x - contentOrigin.x - tileSize.toPx() / 2).roundToInt(),
                        (d.position.y - contentOrigin.y - tileSize.toPx() / 2).roundToInt(),
                    )
                }
                .size(tileSize)
                .zIndex(10f)
                .shadow(8.dp, RoundedCornerShape(14.dp))
                .background(CellWhite, RoundedCornerShape(14.dp))
                .scale(1.1f),
            contentAlignment = Alignment.Center,
        ) {
            GameArt(d.defId, emoji, 42.dp, emojiSize = 30.sp)
        }
    }

    // Juice layer: sparkle bursts + flying items
    sparkles.forEachIndexed { i, center ->
        key(i, center) { SparkleBurst(center - contentOrigin) }
    }
    flies.forEach { f ->
        key(f.id) {
            FlyingEmoji(
                f.copy(from = f.from - contentOrigin, to = f.to - contentOrigin),
            )
        }
    }

    // Quick-time event bubble floating above the inventory bar
    ui.game.qte?.let { offer ->
        val leftSec = ((offer.expiresAtMs - ui.nowMs) / 1000).coerceAtLeast(0)
        if (leftSec > 0) {
            Box(
                Modifier.fillMaxSize().padding(bottom = 78.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                QteBubble(offer = offer, leftSec = leftSec, onClaim = vm::claimQte)
            }
        }
    }
}

private fun Color.darken(f: Float): Color = Color(red * f, green * f, blue * f, alpha)

private fun rewardText(r: LevelReward): String = buildList {
    if (r.coins > 0) add("+${r.coins}🪙")
    r.generatorDefId?.let { add("+${ItemCatalog[it].emoji} ${ItemCatalog[it].name}") }
    if (r.inventoryCapBonus > 0) add("+${r.inventoryCapBonus} ô kho")
}.joinToString("  ")

@Composable
private fun BoardGrid(
    cells: List<BoardItem?>,
    lockedCells: Set<Int>,
    nowMs: Long,
    drag: DragState?,
    hoverCell: Int?,
    cellBounds: MutableMap<Int, Rect>,
    onDragStart: (ItemSource, Int, Long, String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onTapGenerator: (Int) -> Unit,
    onTapInfo: (BoardItem) -> Unit,
    onLongPressGen: (BoardItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        for (row in 0 until GameEngine.ROWS) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (col in 0 until GameEngine.COLS) {
                    val cell = GameEngine.idx(row, col)
                    val item = cells[cell]
                    CellBox(
                        index = cell,
                        item = item,
                        locked = cell in lockedCells,
                        unlockLevel = GameEngine.unlockLevelForCell(cell),
                        nowMs = nowMs,
                        hidden = drag?.source == ItemSource.BOARD && drag.index == cell,
                        hover = hoverCell == cell,
                        canDrop = if (drag != null && item != null && hoverCell == cell) {
                            when {
                                item.frozen -> drag.defId == item.defId
                                drag.defId == GameEngine.MAGIC_WILD_ID ->
                                    item.defId != GameEngine.MAGIC_WILD_ID
                                else -> GameEngine.canMerge(
                                    BoardItem(drag.uid, drag.defId),
                                    item,
                                )
                            }
                        } else {
                            null
                        },
                        cellBounds = cellBounds,
                        onDragStart = onDragStart,
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onTapGenerator = onTapGenerator,
                        onTapInfo = onTapInfo,
                        onLongPressGen = onLongPressGen,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CellBox(
    index: Int,
    item: BoardItem?,
    locked: Boolean,
    unlockLevel: Int?,
    nowMs: Long,
    hidden: Boolean,
    hover: Boolean,
    canDrop: Boolean?,
    cellBounds: MutableMap<Int, Rect>,
    onDragStart: (ItemSource, Int, Long, String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onTapGenerator: (Int) -> Unit,
    onTapInfo: (BoardItem) -> Unit,
    onLongPressGen: (BoardItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val bg = when {
        locked -> Color(0xFFD7CCC8)
        canDrop == true -> Color(0xFFA5D6A7)
        canDrop == false -> Color(0xFFEF9A9A)
        hover -> Color(0xFFFFF9C4)
        else -> CellWhite
    }
    Box(
        modifier
            .aspectRatio(1f)
            .onGloballyPositioned {
                bounds = it.boundsInWindow()
                cellBounds[index] = bounds
            }
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, Color(0x1A5D4037), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (locked) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GameArt("fx_webtile", "🕸️", 34.dp, alpha = 0.75f, emojiSize = 18.sp)
                if (unlockLevel != null) {
                    Text("Lv$unlockLevel", fontSize = 8.sp, color = Cocoa.copy(alpha = 0.7f))
                }
            }
        } else if (item != null) {
            ItemTile(
                index = index,
                item = item,
                nowMs = nowMs,
                hidden = hidden,
                cellBoundsInWindow = bounds,
                source = ItemSource.BOARD,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onTapGenerator = onTapGenerator,
                onTapInfo = onTapInfo,
                onLongPressGen = onLongPressGen,
            )
        }
    }
}

@Composable
private fun InventoryBar(
    inventory: List<BoardItem?>,
    cap: Int,
    nowMs: Long,
    drag: DragState?,
    hoverInv: Int?,
    invBounds: MutableMap<Int, Rect>,
    onDragStart: (ItemSource, Int, Long, String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onTapInfo: (BoardItem) -> Unit,
    onLongPressGen: (BoardItem) -> Unit,
    onOpenBag: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 60.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameArt(
            "icon_bag",
            "🎒",
            30.dp,
            emojiSize = 20.sp,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onOpenBag)
                .padding(4.dp),
        )
        for (slot in 0 until GameEngine.MAX_INVENTORY) {
            val locked = slot >= cap
            var bounds by remember { mutableStateOf(Rect.Zero) }
            val item = inventory[slot]
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .onGloballyPositioned {
                        bounds = it.boundsInWindow()
                        if (!locked) invBounds[slot] = bounds
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            locked -> Color(0xFFDCCFC2)
                            hoverInv == slot && item == null && drag != null ->
                                Color(0xFFA5D6A7)
                            hoverInv == slot -> Color(0xFFFFF9C4)
                            else -> Color(0xFFEDE0CF)
                        },
                    )
                    .border(
                        1.dp,
                        if (drag != null && item == null && !locked) Gold else Color(0x335D4037),
                        RoundedCornerShape(10.dp),
                    )
                    .then(if (locked) Modifier.clickable(onClick = onOpenBag) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    locked -> Text("🔒", fontSize = 12.sp)
                    item != null -> ItemTile(
                        index = slot,
                        item = item,
                        nowMs = nowMs,
                        hidden = drag?.source == ItemSource.INVENTORY && drag.index == slot,
                        cellBoundsInWindow = bounds,
                        source = ItemSource.INVENTORY,
                        onDragStart = onDragStart,
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onTapGenerator = {},
                        onTapInfo = onTapInfo,
                        onLongPressGen = onLongPressGen,
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemTile(
    index: Int,
    item: BoardItem,
    nowMs: Long,
    hidden: Boolean,
    cellBoundsInWindow: Rect,
    source: ItemSource,
    onDragStart: (ItemSource, Int, Long, String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onTapGenerator: (Int) -> Unit,
    onTapInfo: (BoardItem) -> Unit,
    onLongPressGen: (BoardItem) -> Unit,
) {
    val def = ItemCatalog[item.defId]
    val scope = rememberCoroutineScope()

    val scale = remember { Animatable(1f) }
    val pop = remember { Animatable(0f) }
    val currentOnDragStart by androidx.compose.runtime.rememberUpdatedState(onDragStart)
    val currentOnDrag by androidx.compose.runtime.rememberUpdatedState(onDrag)
    val currentOnDragEnd by androidx.compose.runtime.rememberUpdatedState(onDragEnd)
    val currentBounds by androidx.compose.runtime.rememberUpdatedState(cellBoundsInWindow)
    val currentOnTapInfo by androidx.compose.runtime.rememberUpdatedState(onTapInfo)
    val currentOnLongPressGen by androidx.compose.runtime.rememberUpdatedState(onLongPressGen)
    val currentItem by androidx.compose.runtime.rememberUpdatedState(item)
    val currentNowMs by androidx.compose.runtime.rememberUpdatedState(nowMs)

    val cooling = item.cooldownUntilMs > nowMs
    val depleted = def.isGenerator && (cooling || item.charges == 0)

    LaunchedEffect(item.uid) {
        pop.snapTo(0f)
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = if (hidden) 0.25f else if (cooling) 0.55f else 1f
            }
            .scale(scale.value * (0.6f + 0.4f * pop.value))
            .pointerInput(item.uid, source, item.frozen) {
                detectTapGestures(
                    onLongPress = {
                        val it = currentItem
                        if (ItemCatalog[it.defId].isGenerator) {
                            currentOnLongPressGen(it)
                        }
                    },
                ) {
                    val it = currentItem
                    val d = ItemCatalog[it.defId]
                    val dep = d.isGenerator &&
                        (it.cooldownUntilMs > currentNowMs || it.charges == 0)
                    if (d.isGenerator && !dep && !it.frozen) {
                        onTapGenerator(index)
                        scope.launch {
                            scale.animateTo(0.85f, tween(70))
                            scale.animateTo(
                                1f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            )
                        }
                    } else {
                        currentOnTapInfo(it)
                    }
                }
            }
            .then(
                if (item.frozen) {
                    Modifier // frozen items can't be dragged at all
                } else {
                    Modifier.pointerInput(item.uid, source) {
                        detectDragGestures(
                            onDragStart = {
                                currentOnDragStart(
                                    source,
                                    index,
                                    item.uid,
                                    item.defId,
                                    currentBounds.center,
                                )
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentOnDrag(currentBounds.topLeft + change.position)
                            },
                            onDragEnd = { currentOnDragEnd() },
                            onDragCancel = { currentOnDragEnd() },
                        )
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val boxSize = maxWidth * 0.66f
        Box(
            Modifier
                .size(boxSize)
                .shadow(1.dp, RoundedCornerShape(12.dp))
                .background(
                    if (item.frozen) {
                        Brush.verticalGradient(
                            listOf(Color(0xFFE1F5FE), Color(0xFFB3E5FC)),
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFFFFF), Color(def.family.tint)),
                        )
                    },
                    RoundedCornerShape(12.dp),
                )
                .border(
                    1.dp,
                    if (item.frozen) Color(0xFF4FC3F7) else Color(def.family.tint).darken(0.82f),
                    RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            GameArt(
                item.defId,
                def.emoji,
                boxSize * 0.95f,
                alpha = if (item.frozen) 0.6f else 1f,
                emojiSize = (boxSize.value * 0.62f).sp,
            )
        }
        if (item.frozen) {
            GameArt(
                "icon_snow",
                "❄️",
                boxSize * 0.34f,
                emojiSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(1.dp),
            )
        }
        if (def.isGenerator) {
            if (cooling) {
                Text(
                    "💤 ${(item.cooldownUntilMs - nowMs) / 1000 + 1}s",
                    fontSize = (boxSize.value * 0.21f).sp,
                    fontWeight = FontWeight.Bold,
                    color = Cocoa,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Color(0xCCFFFFFF), RoundedCornerShape(4.dp))
                        .padding(horizontal = 3.dp),
                )
            } else {
                Text(
                    "x${item.charges}",
                    fontSize = (boxSize.value * 0.21f).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(1.dp)
                        .background(MergeGreen, CircleShape)
                        .padding(horizontal = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun FlyingEmoji(f: FlyAnim) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(f.id) {
        anim.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
    }
    val t = anim.value
    val x = f.from.x + (f.to.x - f.from.x) * t
    // parabolic arc
    val lift = kotlin.math.sin(t * Math.PI.toFloat()) * 110f
    val y = f.from.y + (f.to.y - f.from.y) * t - lift
    GameArt(
        f.resName,
        f.emoji,
        44.dp,
        emojiSize = 28.sp,
        modifier = Modifier
            .offset { IntOffset((x - 24.dp.toPx()).roundToInt(), (y - 24.dp.toPx()).roundToInt()) }
            .scale(1.3f - 0.5f * t)
            .alpha(1f - t * 0.3f)
            .zIndex(20f),
    )
}

@Composable
private fun SparkleBurst(center: Offset) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(center) {
        anim.animateTo(1f, tween(550))
    }
    val dirs = remember(center) {
        List(10) { i ->
            val a = i * Math.PI * 2 / 10 + 0.3
            Offset(kotlin.math.cos(a).toFloat(), kotlin.math.sin(a).toFloat())
        }
    }
    val colors = listOf(Gold, MergeGreen, Color(0xFFFF8A65), Color(0xFFBA68C8))
    val t = anim.value
    dirs.forEachIndexed { i, dir ->
        val dist = 14f + t * 95f
        val p = center + dir * dist * 2.2f
        Text(
            if (i % 3 == 0) "⭐" else "✨",
            fontSize = (13 - 6 * t).sp,
            modifier = Modifier
                .offset { IntOffset(p.x.roundToInt(), p.y.roundToInt()) }
                .alpha(1f - t)
                .scale(1f - t * 0.4f),
        )
    }
    Box(
        Modifier
            .offset {
                IntOffset(center.x.roundToInt() - (30 * (1 + t)).roundToInt(), center.y.roundToInt() - (30 * (1 + t)).roundToInt())
            }
            .size((60 * (1 + t * 1.4f)).dp)
            .border(3.dp, colors[0].copy(alpha = 0.6f * (1f - t)), CircleShape),
    )
}

@Composable
private fun ScoreFloater(f: Floater) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(f.id) {
        anim.animateTo(1f, tween(850))
    }
    Text(
        f.text,
        color = f.color,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        modifier = Modifier
            .offset {
                IntOffset(
                    (f.position.x - 40.dp.toPx()).roundToInt(),
                    (f.position.y - 30.dp.toPx() - anim.value * 70.dp.toPx()).roundToInt(),
                )
            }
            .alpha(1f - anim.value),
    )
}

@Composable
private fun LevelBanner(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xEE3E2723))
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ItemInfoCard(
    item: BoardItem,
    nowMs: Long,
    onDismiss: () -> Unit,
    onUpgrade: (() -> Unit)? = null,
) {
    val def = ItemCatalog[item.defId]
    LaunchedEffect(item.uid) {
        kotlinx.coroutines.delay(6_000)
        onDismiss()
    }

    Column(
        Modifier
            .padding(top = 150.dp, start = 24.dp, end = 24.dp)
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(16.dp))
            .background(CellWhite, RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFFE8D5B7), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {}
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .background(Color(def.family.tint), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                GameArt(item.defId, def.emoji, 44.dp, emojiSize = 30.sp)
            }
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text(def.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Cocoa)
                Text(
                    "Tier ${def.tier}  •  🪙 ${def.sellPrice}" +
                        if (def.isRare) "  •  ✨ Hiếm" else "",
                    fontSize = 12.sp,
                    color = Cocoa.copy(alpha = 0.75f),
                )
            }
            Text(
                "✕",
                fontSize = 18.sp,
                color = Cocoa.copy(alpha = 0.5f),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .padding(6.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        if (def.isGenerator) {
            val cooling = item.cooldownUntilMs > nowMs
            Text(
                if (cooling) {
                    "💤 Hết lượt — hồi sau ${(item.cooldownUntilMs - nowMs) / 1000 + 1}s"
                } else {
                    "🔋 Lượt sinh: ${if (item.charges < 0) def.maxCharges else item.charges}/${def.maxCharges}"
                },
                fontSize = 12.sp,
                color = if (cooling) WarnRed else MergeGreen,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text("Sinh ra:", fontSize = 11.sp, color = Cocoa.copy(alpha = 0.7f))
            val total = def.spawnTable.sumOf { it.weight }
            Row(
                Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                def.spawnTable.forEach { entry ->
                    val spawnDef = ItemCatalog[entry.itemId]
                    Text(
                        "${if (spawnDef.isRare) "✨" else ""}${spawnDef.emoji} " +
                            "${entry.weight * 100 / total}%",
                        fontSize = 12.sp,
                        color = if (spawnDef.isRare) Gold else Cocoa,
                    )
                }
            }
            val nextGen = def.mergeResult?.let { ItemCatalog[it] }
            if (nextGen != null && !item.frozen) {
                Text(
                    "⬆️ Nâng lên ${nextGen.emoji} ${nextGen.name} — 🪙${GameEngine.genUpgradeCost(nextGen)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MergeGreen,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F5E9))
                        .clickable { onUpgrade?.invoke() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        } else {
            val next = def.mergeResult?.let { ItemCatalog[it] }
            if (item.frozen) {
                Text(
                    "❄️ Đóng băng — kéo ${def.emoji} khác vào để mở khóa",
                    fontSize = 12.sp,
                    color = Color(0xFF0288D1),
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
            }
            if (next == null) {
                Text(
                    "🏆 Đã max tier!",
                    fontSize = 12.sp,
                    color = Cocoa,
                )
            }
            if (item.defId == GameEngine.MAGIC_WILD_ID) {
                Text(
                    "🪄 Kéo lên item bất kì để nhân bản nó!",
                    fontSize = 12.sp,
                    color = Color(0xFF7B1FA2),
                    fontWeight = FontWeight.SemiBold,
                )
            }

            val gens = ItemCatalog.generatorsFor(item.defId)
            Spacer(Modifier.height(4.dp))
            Text(
                if (gens.isNotEmpty()) {
                    "🧺 Sinh từ: " + gens.joinToString(" / ") { "${it.emoji} ${it.name}" }
                } else {
                    "🧺 Sinh từ: không có (item đặc biệt)"
                },
                fontSize = 12.sp,
                color = Cocoa.copy(alpha = 0.8f),
            )
        }
        MergeChainStrip(item.defId)
    }
}

/** Horizontal merge chain tier-1 -> max; current item highlighted, past tiers dimmed. */
@Composable
private fun MergeChainStrip(currentDefId: String) {
    val chain = ItemCatalog.chainOf(currentDefId)
    if (chain.size <= 1) return
    val curIdx = chain.indexOfFirst { it.id == currentDefId }
    Spacer(Modifier.height(6.dp))
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chain.forEachIndexed { i, chainDef ->
            val isCur = i == curIdx
            Box(
                Modifier
                    .background(
                        if (isCur) Color(0xFFFFF3C9) else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .border(
                        1.5.dp,
                        if (isCur) Gold else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .padding(3.dp)
                    .alpha(if (i < curIdx) 0.35f else 1f),
            ) {
                GameArt(chainDef.id, chainDef.emoji, 24.dp, emojiSize = 15.sp)
            }
            if (i < chain.size - 1) {
                Text(
                    "›",
                    fontSize = 11.sp,
                    color = Cocoa.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun GenSpawnListCard(
    item: BoardItem,
    nowMs: Long,
    onDismiss: () -> Unit,
    onUpgrade: (() -> Unit)? = null,
) {
    val def = ItemCatalog[item.defId]
    LaunchedEffect(item.uid) {
        kotlinx.coroutines.delay(10_000)
        onDismiss()
    }

    Column(
        Modifier
            .padding(top = 150.dp, start = 24.dp, end = 24.dp)
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(16.dp))
            .background(CellWhite, RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFFE8D5B7), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {}
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .background(Color(def.family.tint), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                GameArt(item.defId, def.emoji, 44.dp, emojiSize = 30.sp)
            }
            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                Text(def.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Cocoa)
                val cooling = item.cooldownUntilMs > nowMs
                Text(
                    if (cooling) {
                        "💤 Hồi sau ${(item.cooldownUntilMs - nowMs) / 1000 + 1}s"
                    } else {
                        "🔋 Lượt: ${if (item.charges < 0) def.maxCharges else item.charges}/${def.maxCharges}"
                    },
                    fontSize = 12.sp,
                    color = if (cooling) WarnRed else MergeGreen,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                "✕",
                fontSize = 18.sp,
                color = Cocoa.copy(alpha = 0.5f),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .padding(6.dp),
            )
        }

        Spacer(Modifier.height(8.dp))
        val totalWeight = def.spawnTable.sumOf { it.weight }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Sinh ra:", fontSize = 11.sp, color = Cocoa.copy(alpha = 0.7f))
            def.spawnTable.forEach { entry ->
                val spawnDef = ItemCatalog[entry.itemId]
                Text(
                    "${if (spawnDef.isRare) "✨" else ""}${spawnDef.emoji} " +
                        "${entry.weight * 100 / totalWeight}%",
                    fontSize = 12.sp,
                    color = if (spawnDef.isRare) Gold else Cocoa,
                )
            }
        }
        val chain = def.spawnTable.firstOrNull()?.let { ItemCatalog.chainOf(it.itemId) }
        if (chain != null && chain.size > 1) {
            Text("🔗 Chuỗi merge:", fontSize = 12.sp, color = Cocoa.copy(alpha = 0.7f))
            chain.chunked(6).forEach { rowItems ->
                Row(Modifier.padding(top = 6.dp)) {
                    rowItems.forEachIndexed { i, cd ->
                        val gi = chain.indexOf(cd)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .background(Color(cd.family.tint), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                GameArt(cd.id, cd.emoji, 32.dp, emojiSize = 19.sp)
                            }
                            Text(
                                cd.name,
                                fontSize = 8.sp,
                                color = Cocoa.copy(alpha = 0.7f),
                                maxLines = 1,
                            )
                        }
                        if (gi < chain.lastIndex) {
                            Text(
                                "→",
                                fontSize = 13.sp,
                                color = Cocoa.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .align(Alignment.Top)
                                    .padding(top = 10.dp, start = 2.dp, end = 4.dp),
                            )
                        }
                    }
                }
            }
        }
        val nextGen = def.mergeResult?.let { ItemCatalog[it] }
        if (nextGen != null && !item.frozen) {
            Text(
                "⬆️ Nâng lên ${nextGen.emoji} ${nextGen.name} — 🪙${GameEngine.genUpgradeCost(nextGen)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MergeGreen,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE8F5E9))
                    .clickable { onUpgrade?.invoke() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

private data class StatInfo(
    val resName: String,
    val emoji: String,
    val title: String,
    val usage: String,
    val earn: String,
)

@Composable
private fun StatInfoCard(kind: String, onDismiss: () -> Unit) {
    LaunchedEffect(kind) {
        kotlinx.coroutines.delay(8_000)
        onDismiss()
    }
    val info = when (kind) {
        "score" -> StatInfo(
            "icon_star", "⭐", "Điểm số",
            "Thước đo tiến trình — điểm càng cao chứng tỏ bạn ghép đồ càng giỏi.",
            "Ghép 2 item giống nhau để cộng điểm. Item tier càng cao cho điểm càng nhiều.",
        )
        "coins" -> StatInfo(
            "icon_coin", "🪙", "Vàng",
            "Mua thêm ô Kho đồ (bấm túi 🎒 dưới bàn chơi).",
            "Hoàn thành đơn của khách, nhận thưởng Thành tựu 🏆, quà hằng ngày 🎁.",
        )
        else -> StatInfo(
            "icon_energy", "⚡", "Năng lượng",
            "Mỗi lần bấm generator tốn 1⚡ để sinh item mới.",
            "Tự hồi +1⚡ mỗi 60 giây (tối đa ${GameEngine.ENERGY_CAP}). Quà hằng ngày +10⚡.",
        )
    }

    Column(
        Modifier
            .padding(top = 150.dp, start = 24.dp, end = 24.dp)
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(16.dp))
            .background(CellWhite, RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFFE8D5B7), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {}
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .background(Color(0xFFFFF3D9), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                GameArt(info.resName, info.emoji, 40.dp, emojiSize = 26.sp)
            }
            Text(
                info.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Cocoa,
                modifier = Modifier.padding(start = 10.dp).weight(1f),
            )
            Text(
                "✕",
                fontSize = 18.sp,
                color = Cocoa.copy(alpha = 0.5f),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .padding(6.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("💡 Công dụng:", fontSize = 12.sp, color = Cocoa.copy(alpha = 0.7f))
        Text(info.usage, fontSize = 13.sp, color = Cocoa)
        Spacer(Modifier.height(8.dp))
        Text("🎯 Cách kiếm:", fontSize = 12.sp, color = Cocoa.copy(alpha = 0.7f))
        Text(info.earn, fontSize = 13.sp, color = Cocoa)
    }
}

private fun qteLabel(r: QteReward): Pair<String, String> = when (r) {
    QteReward.WILDCARD -> "🪄" to "Đũa phép nhân bản"
    QteReward.FRENZY -> "🔥" to "Gen miễn phí 90s"
    QteReward.ENERGY -> "⚡" to "+${GameEngine.QTE_ENERGY} năng lượng"
    QteReward.COINS -> "🪙" to "+${GameEngine.QTE_COINS} vàng"
    QteReward.RECHARGE -> "🔋" to "Sạc đầy mọi gen"
}

@Composable
private fun QteBubble(
    offer: com.bepngon.merge.game.QteOffer,
    leftSec: Long,
    onClaim: () -> Unit,
) {
    val pulse by rememberInfiniteTransition(label = "qte").animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(380), RepeatMode.Reverse),
        label = "pulse",
    )
    val (emoji, label) = qteLabel(offer.reward)
    Row(
        Modifier
            .scale(pulse)
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .background(Color(0xFFFFF3D9), RoundedCornerShape(20.dp))
            .border(2.dp, Gold, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClaim)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 26.sp)
        Column(Modifier.padding(start = 8.dp)) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Cocoa,
            )
            Text(
                "còn ${leftSec}s",
                fontSize = 11.sp,
                color = Cocoa.copy(alpha = 0.7f),
            )
        }
    }
}

