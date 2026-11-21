package com.bepngon.merge.game

import kotlin.random.Random

object GameEngine {
    const val COLS = 7
    const val ROWS = 9
    const val CELLS = COLS * ROWS
    const val ENERGY_CAP = 100
    const val ENERGY_PER_SPAWN = 1
    const val START_COINS = 50

    fun initialState(): GameState {
        val cells = MutableList<BoardItem?>(CELLS) { null }
        var uid = 1L
        fun place(defId: String, index: Int) {
            cells[index] = BoardItem(uid++, defId)
        }
        place("gen_fruit_1", idx(3, 3))
        place("gen_fruit_1", idx(3, 5))
        place("gen_bakery_1", idx(5, 4))
        place("fruit_1", idx(1, 2))
        place("fruit_1", idx(1, 4))
        place("bakery_1", idx(7, 3))
        return GameState(
            cells = cells,
            energy = ENERGY_CAP,
            coins = START_COINS,
            score = 0,
            nextUid = uid,
        )
    }

    fun idx(row: Int, col: Int): Int = row * COLS + col
    fun rowOf(index: Int): Int = index / COLS
    fun colOf(index: Int): Int = index % COLS

    fun canMerge(a: BoardItem?, b: BoardItem?): Boolean =
        a != null && b != null &&
            a.defId == b.defId &&
            ItemCatalog[a.defId].mergeResult != null

    fun moveOrMerge(state: GameState, from: Int, to: Int): Pair<GameState, List<GameEvent>> {
        if (from == to || from !in 0 until CELLS || to !in 0 until CELLS) {
            return state to emptyList()
        }
        val dragged = state.cells[from] ?: return state to emptyList()
        val target = state.cells[to]

        if (target == null) {
            val cells = state.cells.toMutableList()
            cells[to] = dragged
            cells[from] = null
            return state.copy(cells = cells) to emptyList()
        }

        if (!canMerge(dragged, target)) {
            return state to listOf(GameEvent.InvalidDrop)
        }

        val result = ItemCatalog.mergeResultOf(dragged)!!
        val newItem = BoardItem(state.nextUid, result.id)
        val cells = state.cells.toMutableList()
        cells[to] = newItem
        cells[from] = null
        return state.copy(
            cells = cells,
            score = state.score + result.score,
            nextUid = state.nextUid + 1,
        ) to listOf(GameEvent.Merged(to, newItem, result.score))
    }

    fun tapGenerator(
        state: GameState,
        cellIndex: Int,
        random: Random = Random,
    ): Pair<GameState, List<GameEvent>> {
        val item = state.cells.getOrNull(cellIndex) ?: return state to emptyList()
        val def = ItemCatalog[item.defId]
        if (!def.isGenerator || def.spawnTable.isEmpty()) return state to emptyList()
        if (state.energy < ENERGY_PER_SPAWN) return state to listOf(GameEvent.NoEnergy)

        val empty = emptyCells(state)
        if (empty.isEmpty()) return state to listOf(GameEvent.BoardFull)

        val adjacent = empty.filter { isAdjacent(it, cellIndex) }
        val target = (adjacent.ifEmpty { empty }).random(random)
        val spawnedDefId = pickWeighted(def.spawnTable, random)
        val newItem = BoardItem(state.nextUid, spawnedDefId)

        val cells = state.cells.toMutableList()
        cells[target] = newItem
        return state.copy(
            cells = cells,
            energy = state.energy - ENERGY_PER_SPAWN,
            nextUid = state.nextUid + 1,
        ) to listOf(GameEvent.Spawned(target, newItem))
    }

    fun sell(state: GameState, cellIndex: Int): Pair<GameState, List<GameEvent>> {
        val item = state.cells.getOrNull(cellIndex) ?: return state to emptyList()
        val price = ItemCatalog[item.defId].sellPrice
        val cells = state.cells.toMutableList()
        cells[cellIndex] = null
        return state.copy(cells = cells, coins = state.coins + price) to
            listOf(GameEvent.Sold(cellIndex, price))
    }

    fun emptyCells(state: GameState): List<Int> =
        state.cells.indices.filter { state.cells[it] == null }

    private fun isAdjacent(a: Int, b: Int): Boolean {
        val dr = kotlin.math.abs(rowOf(a) - rowOf(b))
        val dc = kotlin.math.abs(colOf(a) - colOf(b))
        return dr <= 1 && dc <= 1 && (dr + dc) > 0
    }

    private fun pickWeighted(table: List<SpawnEntry>, random: Random): String {
        val total = table.sumOf { it.weight }
        var roll = random.nextInt(total)
        for (entry in table) {
            roll -= entry.weight
            if (roll < 0) return entry.itemId
        }
        return table.last().itemId
    }
}
