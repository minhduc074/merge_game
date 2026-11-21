package com.bepngon.merge.game

data class BoardItem(
    val uid: Long,
    val defId: String,
)

data class GameState(
    val cells: List<BoardItem?>,
    val energy: Int,
    val coins: Int,
    val score: Int,
    val nextUid: Long,
) {
    val boardFull: Boolean get() = cells.all { it != null }

    fun hasMergeablePair(): Boolean {
        val counts = mutableMapOf<String, Int>()
        for (item in cells) {
            if (item == null) continue
            if (ItemCatalog[item.defId].mergeResult == null) continue
            val n = (counts[item.defId] ?: 0) + 1
            if (n >= 2) return true
            counts[item.defId] = n
        }
        return false
    }
}

sealed interface GameEvent {
    data class Merged(val cellIndex: Int, val newItem: BoardItem, val scoreGained: Int) : GameEvent
    data class Spawned(val cellIndex: Int, val item: BoardItem) : GameEvent
    data class Sold(val cellIndex: Int, val coinsGained: Int) : GameEvent
    data object NoEnergy : GameEvent
    data object BoardFull : GameEvent
    data object InvalidDrop : GameEvent
}
