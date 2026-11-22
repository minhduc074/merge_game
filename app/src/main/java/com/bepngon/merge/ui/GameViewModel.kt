package com.bepngon.merge.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bepngon.merge.BuildConfig
import com.bepngon.merge.data.SaveManager
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.GameEvent
import com.bepngon.merge.game.GameState
import com.bepngon.merge.game.ItemSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val ORDER_TICK_MS = 1_000L

data class GameUiState(
    val game: GameState,
    val loading: Boolean = true,
    val showRestartConfirm: Boolean = false,
    val showBag: Boolean = false,
    val showAchievements: Boolean = false,
    val showDaily: Boolean = false,
    val showGuide: Boolean = false,
    val showRoom: Boolean = false,
    val showMiniGame: Boolean = false,
    val nowMs: Long = 0,
) {
    val showOnboarding: Boolean get() = !loading && !game.onboardingDone
}

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val saveManager = SaveManager(app)
    private var startedAtMs = 0L

    private val _ui = MutableStateFlow(GameUiState(game = GameEngine.initialState()))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    init {
        startedAtMs = System.currentTimeMillis()
        viewModelScope.launch {
            saveManager.load(nowMs())?.let { restored ->
                _ui.value = _ui.value.copy(game = restored)
            }
            _ui.value = _ui.value.copy(loading = false)
        }
        viewModelScope.launch {
            while (true) {
                delay(GameEngine.energyRegenMs(_ui.value.game, nowMs()))
                update { s ->
                    if (s.energy < GameEngine.ENERGY_CAP) s.copy(energy = s.energy + 1) else s
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(ORDER_TICK_MS)
                applyEngine { GameEngine.tick(it, nowMs()) }
                _ui.value = _ui.value.copy(nowMs = nowMs())
            }
        }
    }

    fun moveOrMerge(from: Int, to: Int) = applyEngine { GameEngine.moveOrMerge(it, from, to) }

    fun inventoryToBoard(slot: Int, to: Int) =
        applyEngine { GameEngine.inventoryToBoard(it, slot, to) }

    fun boardToInventory(from: Int, slot: Int) =
        applyEngine { GameEngine.boardToInventory(it, from, slot) }

    fun moveInInventory(from: Int, to: Int) =
        applyEngine { GameEngine.moveInInventory(it, from, to) }

    fun tapGenerator(cellIndex: Int) = applyEngine { GameEngine.tapGenerator(it, cellIndex, nowMs()) }

    fun buyInventorySlot() = applyEngine { GameEngine.buyInventorySlot(it) }

    fun retrieveToBoard(slot: Int) = applyEngine { GameEngine.retrieveToBoard(it, slot) }

    fun openBag() {
        _ui.value = _ui.value.copy(showBag = true)
    }

    fun closeBag() {
        _ui.value = _ui.value.copy(showBag = false)
    }

    fun openAchievements() {
        _ui.value = _ui.value.copy(showAchievements = true)
    }

    fun closeAchievements() {
        _ui.value = _ui.value.copy(showAchievements = false)
    }

    fun openDaily() {
        _ui.value = _ui.value.copy(showDaily = true)
    }

    fun closeDaily() {
        _ui.value = _ui.value.copy(showDaily = false)
    }

    fun openGuide() {
        _ui.value = _ui.value.copy(showGuide = true)
    }

    fun closeGuide() {
        _ui.value = _ui.value.copy(showGuide = false)
    }

    fun claimAchievement(id: String) =
        applyEngine { GameEngine.claimAchievement(it, id) }

    fun claimDaily() =
        applyEngine { GameEngine.claimDaily(it, System.currentTimeMillis()) }

    fun dailyReady(): Boolean =
        GameEngine.dailyReady(_ui.value.game, System.currentTimeMillis())

    fun finishOnboarding() = applyEngine { GameEngine.finishOnboarding(it) }

    fun deliverToOrder(orderId: Long, source: ItemSource, index: Int) =
        applyEngine { GameEngine.deliverToOrder(it, orderId, source, index, nowMs()) }

    fun autoFulfillOrder(orderId: Long) =
        applyEngine { GameEngine.autoFulfillOrder(it, orderId, nowMs()) }

    fun claimQte() = applyEngine { GameEngine.claimQte(it, nowMs()) }

    fun openRoom() {
        _ui.value = _ui.value.copy(showRoom = true)
    }

    fun closeRoom() {
        _ui.value = _ui.value.copy(showRoom = false)
    }

    fun buyDecor(decorId: String) = applyEngine { GameEngine.buyDecor(it, decorId) }

    fun adoptPet(petId: String) = applyEngine { GameEngine.adoptPet(it, petId, nowMs()) }

    fun feedPetCoins() = applyEngine { GameEngine.feedPetCoins(it, nowMs()) }
    fun upgradeGenerator(uid: Long) = applyEngine { GameEngine.upgradeGenerator(it, uid) }

    fun openMiniGame() {
        // plays are unlimited — just show the game
        _ui.value = _ui.value.copy(showMiniGame = true)
    }

    fun finishMiniGame(score: Int) {
        applyEngine { GameEngine.finishMiniGame(it, score, nowMs()) }
        _ui.value = _ui.value.copy(showMiniGame = false)
    }

    fun closeMiniGame() {
        _ui.value = _ui.value.copy(showMiniGame = false)
    }

    /** Debug cheat — coin chip tapped 10×; no-op in release builds. */
    fun debugMaxOut() {
        if (BuildConfig.DEBUG) applyEngine { GameEngine.debugMaxOut(it) }
    }

    fun askRestart() {
        _ui.value = _ui.value.copy(showRestartConfirm = true)
    }

    fun dismissRestart() {
        _ui.value = _ui.value.copy(showRestartConfirm = false)
    }

    fun restart() {
        startedAtMs = System.currentTimeMillis()
        _ui.value = GameUiState(game = GameEngine.initialState(), loading = false)
        viewModelScope.launch { saveManager.clear() }
    }

    private fun nowMs(): Long = System.currentTimeMillis() - startedAtMs

    private fun applyEngine(op: (GameState) -> Pair<GameState, List<GameEvent>>) {
        val current = _ui.value.game
        val (next, events) = op(current)
        if (next === current && events.isEmpty()) return
        _ui.value = _ui.value.copy(game = next)
        events.forEach { _events.tryEmit(it) }
        if (next !== current) {
            viewModelScope.launch { saveManager.save(next, nowMs()) }
        }
    }

    private fun update(transform: (GameState) -> GameState) {
        _ui.value = _ui.value.copy(game = transform(_ui.value.game))
    }
}
