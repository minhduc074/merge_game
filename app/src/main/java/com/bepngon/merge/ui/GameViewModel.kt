package com.bepngon.merge.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bepngon.merge.data.SaveManager
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.GameEvent
import com.bepngon.merge.game.GameState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val ENERGY_REGEN_MS = 60_000L

data class GameUiState(
    val game: GameState,
    val loading: Boolean = true,
    val showRestartConfirm: Boolean = false,
)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val saveManager = SaveManager(app)

    private val _ui = MutableStateFlow(GameUiState(game = GameEngine.initialState()))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            saveManager.load()?.let { restored ->
                _ui.value = _ui.value.copy(game = restored)
            }
            _ui.value = _ui.value.copy(loading = false)
        }
        viewModelScope.launch {
            while (true) {
                delay(ENERGY_REGEN_MS)
                update { s ->
                    if (s.energy < GameEngine.ENERGY_CAP) s.copy(energy = s.energy + 1) else s
                }
            }
        }
    }

    fun moveOrMerge(from: Int, to: Int) = applyEngine { GameEngine.moveOrMerge(it, from, to) }

    fun tapGenerator(cellIndex: Int) = applyEngine { GameEngine.tapGenerator(it, cellIndex) }

    fun sell(cellIndex: Int) = applyEngine { GameEngine.sell(it, cellIndex) }

    fun askRestart() {
        _ui.value = _ui.value.copy(showRestartConfirm = true)
    }

    fun dismissRestart() {
        _ui.value = _ui.value.copy(showRestartConfirm = false)
    }

    fun restart() {
        _ui.value = GameUiState(game = GameEngine.initialState())
        viewModelScope.launch { saveManager.clear() }
    }

    private fun applyEngine(op: (GameState) -> Pair<GameState, List<GameEvent>>) {
        val current = _ui.value.game
        val (next, events) = op(current)
        if (next === current && events.isEmpty()) return
        _ui.value = _ui.value.copy(game = next)
        events.forEach { _events.tryEmit(it) }
        if (next !== current) {
            viewModelScope.launch { saveManager.save(next) }
        }
    }

    private fun update(transform: (GameState) -> GameState) {
        _ui.value = _ui.value.copy(game = transform(_ui.value.game))
    }
}
