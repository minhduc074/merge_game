package com.bepngon.merge.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bepngon.merge.game.BoardItem
import com.bepngon.merge.game.Customers
import com.bepngon.merge.game.DecorEngine
import com.bepngon.merge.game.DecorState
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.GameState
import com.bepngon.merge.game.ItemCatalog
import com.bepngon.merge.game.Order
import com.bepngon.merge.game.PlacedDecor
import com.bepngon.merge.game.RoomSave
import com.bepngon.merge.game.Stats
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.saveStore by preferencesDataStore(name = "bepngon_save")

@Serializable
private data class OrderData(
    val id: Long,
    val customerId: String,
    val required: List<String>,
    val fulfilled: List<String>,
)

@Serializable
private data class CellData(
    val defId: String,
    val charges: Int = -1,
    val cooldownInMs: Long = 0,
    val rechargeInMs: Long = 0,
    val frozen: Boolean = false,
)

@Serializable
private data class StatsData(
    val merges: Int = 0,
    val ordersDone: Int = 0,
    val coinsEarned: Int = 0,
    val maxTier: Int = 0,
    val spawns: Int = 0,
    val raresFound: Int = 0,
    val qteClaimed: Int = 0,
    val maxCombo: Int = 0,
)

@Serializable
private data class PlacedDecorData(
    val uid: Long,
    val itemId: String,
    val variantId: String,
    val gx: Int,
    val gy: Int,
    val rot: Int = 0,
    val flip: Boolean = false,
    val scale: Float = 1f,
)

@Serializable
private data class RoomSaveData(
    val version: Int = 1,
    val placed: List<PlacedDecorData> = emptyList(),
    val floorPaint: String? = null,
    val wallPaint: String? = null,
    val wallPaintL: String? = null,
    val wallPaintR: String? = null,
    val skyPaint: String? = null,
    val groundPaint: String? = null,
)

@Serializable
private data class DecorSaveData(
    val rooms: Map<String, RoomSaveData> = emptyMap(),
    val ownedVariants: List<String> = emptyList(),
    val nextPlacedUid: Long = 1,
)

@Serializable
private data class SaveData(
    val cells: List<CellData?>,
    val inventory: List<String?>,
    val inventoryCap: Int,
    val orders: List<OrderData>,
    val nextOrderId: Long,
    val nextOrderInMs: Long,
    val energy: Int,
    val coins: Int,
    val score: Int,
    val xp: Int,
    val level: Int,
    val nextUid: Long,
    val lockedCells: List<Int>,
    val stats: StatsData = StatsData(),
    val reachedAchievements: List<String> = emptyList(),
    val claimedAchievements: List<String> = emptyList(),
    val lastDailyClaimEpochMs: Long = 0,
    val onboardingDone: Boolean = false,
    val frenzyInMs: Long = 0,
    val nextQteInMs: Long = 0,
    val dailyStreak: Int = 0,
    val miniPlaysLeft: Int = 3,
    val miniRefillInMs: Long = 0,
    val miniBest: Int = 0,
    val blockBest: Int = 0,
    val orderPaceMs: Long = 0,
    val orderDoneAgoMs: Long = 0,
    val decor: DecorSaveData? = null,
)

@Serializable
private data class SaveDataV2(
    val cells: List<String?>,
    val inventory: List<String?>,
    val inventoryCap: Int,
    val orders: List<OrderData>,
    val nextOrderId: Long,
    val nextOrderInMs: Long,
    val energy: Int,
    val coins: Int,
    val score: Int,
    val xp: Int,
    val level: Int,
    val nextUid: Long,
)

/** Saved decor blob -> state. Rooms written by a newer save version are
 *  dropped (fresh empty room) rather than half-loaded. */
private fun DecorSaveData.toState(): DecorState = DecorState(
    rooms = rooms.mapValues { (_, r) ->
        if (r.version != DecorEngine.DECOR_SAVE_VERSION) {
            RoomSave()
        } else {
            RoomSave(
                version = r.version,
                placed = r.placed.map {
                    PlacedDecor(it.uid, it.itemId, it.variantId, it.gx, it.gy, it.rot, it.flip, it.scale)
                },
                floorPaint = r.floorPaint,
                wallPaint = r.wallPaint,
                wallPaintL = r.wallPaintL,
                wallPaintR = r.wallPaintR,
                skyPaint = r.skyPaint,
                groundPaint = r.groundPaint,
            )
        }
    }.ifEmpty { mapOf(DecorEngine.DEFAULT_ROOM to RoomSave()) },
    ownedVariants = ownedVariants.toSet(),
    nextPlacedUid = maxOf(nextPlacedUid, 1),
)

class SaveManager(private val context: Context) {

    private val keyV3 = stringPreferencesKey("save_v3")
    private val keyV2 = stringPreferencesKey("save_v2")
    private val json = Json { ignoreUnknownKeys = true }

    /** Old saves stored a 7-wide board; anything still inside the current
     *  5×7 grid keeps its row/col position, cells outside are dropped. */
    private fun <T> remapCells(cells: List<T>): List<T> {
        if (cells.size == GameEngine.CELLS) return cells
        val legacyCols = 7
        val out = MutableList<T?>(GameEngine.CELLS) { null }
        cells.forEachIndexed { i, c ->
            val r = i / legacyCols
            val col = i % legacyCols
            if (c != null && r < GameEngine.ROWS && col < GameEngine.COLS) {
                out[r * GameEngine.COLS + col] = c
            }
        }
        @Suppress("UNCHECKED_CAST")
        return out as List<T>
    }

    suspend fun save(state: GameState, nowMs: Long) {
        val data = SaveData(
            cells = state.cells.map {
                it?.let { item ->
                    CellData(
                        defId = item.defId,
                        charges = item.charges,
                        cooldownInMs = (item.cooldownUntilMs - nowMs).coerceAtLeast(0),
                        rechargeInMs = (item.rechargeAtMs - nowMs).coerceAtLeast(0),
                        frozen = item.frozen,
                    )
                }
            },
            inventory = state.inventory.map { it?.defId },
            inventoryCap = state.inventoryCap,
            orders = state.orders.map {
                OrderData(it.id, it.customer.id, it.required, it.fulfilled)
            },
            nextOrderId = state.nextOrderId,
            nextOrderInMs = (state.nextOrderAtMs - nowMs).coerceAtLeast(0),
            energy = state.energy,
            coins = state.coins,
            score = state.score,
            xp = state.xp,
            level = state.level,
            nextUid = state.nextUid,
            lockedCells = state.lockedCells.toList(),
            stats = state.stats.let {
                StatsData(
                    it.merges, it.ordersDone, it.coinsEarned, it.maxTier,
                    it.spawns, it.raresFound, it.qteClaimed, it.maxCombo,
                )
            },
            reachedAchievements = state.reachedAchievements.toList(),
            claimedAchievements = state.claimedAchievements.toList(),
            lastDailyClaimEpochMs = state.lastDailyClaimEpochMs,
            onboardingDone = state.onboardingDone,
            frenzyInMs = (state.frenzyUntilMs - nowMs).coerceAtLeast(0),
            nextQteInMs = (state.nextQteAtMs - nowMs).coerceAtLeast(0),
            dailyStreak = state.dailyStreak,
            miniPlaysLeft = state.miniPlaysLeft,
            miniRefillInMs = (state.miniRefillAtMs - nowMs).coerceAtLeast(0),
            miniBest = state.miniBest,
            blockBest = state.blockBest,
            orderPaceMs = state.orderPaceMs,
            orderDoneAgoMs = (nowMs - state.lastOrderDoneAtMs).coerceAtLeast(0),
            decor = DecorSaveData(
                rooms = state.decor.rooms.mapValues { (_, r) ->
                    RoomSaveData(
                        version = DecorEngine.DECOR_SAVE_VERSION,
                        placed = r.placed.map {
                            PlacedDecorData(it.uid, it.itemId, it.variantId, it.gx, it.gy, it.rot, it.flip, it.scale)
                        },
                        floorPaint = r.floorPaint,
                        wallPaint = r.wallPaint,
                        wallPaintL = r.wallPaintL,
                        wallPaintR = r.wallPaintR,
                        skyPaint = r.skyPaint,
                        groundPaint = r.groundPaint,
                    )
                },
                ownedVariants = state.decor.ownedVariants.toList(),
                nextPlacedUid = state.decor.nextPlacedUid,
            ),
        )
        context.saveStore.edit { it[keyV3] = json.encodeToString(data) }
    }

    suspend fun load(nowMs: Long): GameState? =
        loadV3(nowMs) ?: loadV2(nowMs)

    private suspend fun loadV3(nowMs: Long): GameState? = runCatching {
        val raw = context.saveStore.data.first()[keyV3] ?: return null
        val data = json.decodeFromString<SaveData>(raw)
        if (data.inventory.size != GameEngine.MAX_INVENTORY) return null
        val cells = remapCells(data.cells)
        GameState(
            cells = cells.mapIndexed { i, c ->
                c?.let {
                    BoardItem(
                        uid = 10_000L + i,
                        defId = it.defId,
                        charges = it.charges,
                        cooldownUntilMs = if (it.cooldownInMs > 0) nowMs + it.cooldownInMs else 0,
                        rechargeAtMs = if (it.rechargeInMs > 0) nowMs + it.rechargeInMs else 0,
                        frozen = it.frozen,
                    )
                }
            },
            inventory = data.inventory.mapIndexed { i, defId ->
                defId?.let { BoardItem(uid = 20_000L + i, defId = it) }
            },
            inventoryCap = data.inventoryCap,
            orders = data.orders.map {
                Order(it.id, Customers.byId(it.customerId), it.required, it.fulfilled)
            },
            nextOrderId = data.nextOrderId,
            nextOrderAtMs = nowMs + data.nextOrderInMs,
            energy = data.energy,
            coins = data.coins,
            score = data.score,
            xp = data.xp,
            level = data.level,
            nextUid = maxOf(data.nextUid, 30_000L),
            // The locked set is derived from level, never stored positions —
            // remapping saved indices mangled them and unlocked bottom cells.
            lockedCells = GameEngine.lockedCellsForLevel(
                data.level,
                legacy = data.cells.size != GameEngine.CELLS,
            ),
            stats = data.stats.let {
                Stats(
                    it.merges, it.ordersDone, it.coinsEarned, it.maxTier,
                    it.spawns, it.raresFound, it.qteClaimed, it.maxCombo,
                )
            },
            reachedAchievements = data.reachedAchievements.toSet(),
            claimedAchievements = data.claimedAchievements.toSet(),
            lastDailyClaimEpochMs = data.lastDailyClaimEpochMs,
            onboardingDone = data.onboardingDone,
            frenzyUntilMs = if (data.frenzyInMs > 0) nowMs + data.frenzyInMs else 0,
            nextQteAtMs = if (data.nextQteInMs > 0) {
                nowMs + data.nextQteInMs
            } else {
                nowMs + GameEngine.QTE_FIRST_MS
            },
            dailyStreak = data.dailyStreak,
            miniPlaysLeft = data.miniPlaysLeft,
            miniRefillAtMs = if (data.miniRefillInMs > 0) nowMs + data.miniRefillInMs else 0,
            miniBest = data.miniBest,
            blockBest = data.blockBest,
            orderPaceMs = data.orderPaceMs,
            lastOrderDoneAtMs = if (data.orderDoneAgoMs > 0) {
                nowMs - data.orderDoneAgoMs
            } else {
                0
            },
            decor = data.decor?.toState() ?: DecorState(),
        )
    }.getOrNull()

    private suspend fun loadV2(nowMs: Long): GameState? = runCatching {
        val raw = context.saveStore.data.first()[keyV2] ?: return null
        val data = json.decodeFromString<SaveDataV2>(raw)
        if (data.inventory.size != GameEngine.MAX_INVENTORY) return null
        val cells = remapCells(data.cells)
        GameState(
            cells = cells.mapIndexed { i, defId ->
                defId?.let { id ->
                    val def = ItemCatalog[id]
                    BoardItem(
                        uid = 10_000L + i,
                        defId = id,
                        charges = if (def.isGenerator) def.maxCharges else -1,
                    )
                }
            },
            inventory = data.inventory.mapIndexed { i, defId ->
                defId?.let { BoardItem(uid = 20_000L + i, defId = it) }
            },
            inventoryCap = data.inventoryCap,
            orders = data.orders.map {
                Order(it.id, Customers.byId(it.customerId), it.required, it.fulfilled)
            },
            nextOrderId = data.nextOrderId,
            nextOrderAtMs = nowMs + data.nextOrderInMs,
            energy = data.energy,
            coins = data.coins,
            score = data.score,
            xp = data.xp,
            level = data.level,
            nextUid = maxOf(data.nextUid, 30_000L),
            lockedCells = emptySet(),
        )
    }.getOrNull()

    suspend fun clear() {
        context.saveStore.edit {
            it.remove(keyV3)
            it.remove(keyV2)
        }
    }
}
