package com.bepngon.merge.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bepngon.merge.game.BoardItem
import com.bepngon.merge.game.Customers
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.GameState
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
private data class SaveData(
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

    private val key = stringPreferencesKey("save_v2")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(state: GameState, nowMs: Long) {
        val data = SaveData(
            cells = state.cells.map { it?.defId },
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
        )
        context.saveStore.edit { it[key] = json.encodeToString(data) }
    }

    suspend fun load(nowMs: Long): GameState? = runCatching {
        val raw = context.saveStore.data.first()[key] ?: return null
        val data = json.decodeFromString<SaveData>(raw)
        if (data.cells.size != GameEngine.CELLS) return null
        if (data.inventory.size != GameEngine.MAX_INVENTORY) return null
        GameState(
            cells = data.cells.mapIndexed { i, defId ->
                defId?.let { BoardItem(uid = 10_000L + i, defId = it) }
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
        )
    }.getOrNull()

    suspend fun clear() {
        context.saveStore.edit { it.remove(key) }
    }
}
