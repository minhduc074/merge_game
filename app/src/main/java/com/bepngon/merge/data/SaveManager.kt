package com.bepngon.merge.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bepngon.merge.game.BoardItem
import com.bepngon.merge.game.Customers
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.GameState
import com.bepngon.merge.game.ItemCatalog
import com.bepngon.merge.game.Order
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

class SaveManager(private val context: Context) {

    private val keyV3 = stringPreferencesKey("save_v3")
    private val keyV2 = stringPreferencesKey("save_v2")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(state: GameState, nowMs: Long) {
        val data = SaveData(
            cells = state.cells.map {
                it?.let { item ->
                    CellData(
                        defId = item.defId,
                        charges = item.charges,
                        cooldownInMs = (item.cooldownUntilMs - nowMs).coerceAtLeast(0),
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
        )
        context.saveStore.edit { it[keyV3] = json.encodeToString(data) }
    }

    suspend fun load(nowMs: Long): GameState? =
        loadV3(nowMs) ?: loadV2(nowMs)

    private suspend fun loadV3(nowMs: Long): GameState? = runCatching {
        val raw = context.saveStore.data.first()[keyV3] ?: return null
        val data = json.decodeFromString<SaveData>(raw)
        if (data.cells.size != GameEngine.CELLS) return null
        if (data.inventory.size != GameEngine.MAX_INVENTORY) return null
        GameState(
            cells = data.cells.mapIndexed { i, c ->
                c?.let {
                    BoardItem(
                        uid = 10_000L + i,
                        defId = it.defId,
                        charges = it.charges,
                        cooldownUntilMs = if (it.cooldownInMs > 0) nowMs + it.cooldownInMs else 0,
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
            lockedCells = data.lockedCells.toSet(),
        )
    }.getOrNull()

    private suspend fun loadV2(nowMs: Long): GameState? = runCatching {
        val raw = context.saveStore.data.first()[keyV2] ?: return null
        val data = json.decodeFromString<SaveDataV2>(raw)
        if (data.cells.size != GameEngine.CELLS) return null
        if (data.inventory.size != GameEngine.MAX_INVENTORY) return null
        GameState(
            cells = data.cells.mapIndexed { i, defId ->
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
