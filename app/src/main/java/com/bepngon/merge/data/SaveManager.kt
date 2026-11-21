package com.bepngon.merge.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bepngon.merge.game.BoardItem
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.GameState
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.saveStore by preferencesDataStore(name = "bepngon_save")

@Serializable
private data class SaveData(
    val cells: List<String?>,
    val energy: Int,
    val coins: Int,
    val score: Int,
    val nextUid: Long,
)

class SaveManager(private val context: Context) {

    private val key = stringPreferencesKey("save_v1")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(state: GameState) {
        val data = SaveData(
            cells = state.cells.map { it?.defId },
            energy = state.energy,
            coins = state.coins,
            score = state.score,
            nextUid = state.nextUid,
        )
        context.saveStore.edit { it[key] = json.encodeToString(data) }
    }

    suspend fun load(): GameState? = runCatching {
        val raw = context.saveStore.data.first()[key] ?: return null
        val data = json.decodeFromString<SaveData>(raw)
        if (data.cells.size != GameEngine.CELLS) return null
        GameState(
            cells = data.cells.mapIndexed { i, defId ->
                defId?.let { BoardItem(uid = 10_000L + i, defId = it) }
            },
            energy = data.energy,
            coins = data.coins,
            score = data.score,
            nextUid = maxOf(data.nextUid, 10_000L + GameEngine.CELLS),
        )
    }.getOrNull()

    suspend fun clear() {
        context.saveStore.edit { it.remove(key) }
    }
}
