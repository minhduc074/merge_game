package com.bepngon.merge.game

data class BoardItem(
    val uid: Long,
    val defId: String,
    val charges: Int = -1,
    val cooldownUntilMs: Long = 0,
) {
    val isCoolingDown: Boolean get() = cooldownUntilMs > 0
}

data class Customer(val id: String, val name: String, val emoji: String)

object Customers {
    val ALL = listOf(
        Customer("cat", "Mèo Mun", "🐱"),
        Customer("dog", "Cún Vàng", "🐶"),
        Customer("rabbit", "Thỏ Bông", "🐰"),
        Customer("panda", "Gấu Trúc", "🐼"),
        Customer("fox", "Cáo Cam", "🦊"),
        Customer("frog", "Ếch Con", "🐸"),
        Customer("pig", "Heo Hồng", "🐷"),
        Customer("koala", "Koala", "🐨"),
    )

    fun byId(id: String): Customer = ALL.first { it.id == id }
}

data class Order(
    val id: Long,
    val customer: Customer,
    val required: List<String>,
    val fulfilled: List<String> = emptyList(),
) {
    val remaining: List<String>
        get() = required.toMutableList().apply { fulfilled.forEach { remove(it) } }

    val isComplete: Boolean get() = remaining.isEmpty()
}

data class GameState(
    val cells: List<BoardItem?>,
    val inventory: List<BoardItem?>,
    val inventoryCap: Int,
    val orders: List<Order>,
    val nextOrderId: Long,
    val nextOrderAtMs: Long,
    val energy: Int,
    val coins: Int,
    val score: Int,
    val xp: Int,
    val level: Int,
    val nextUid: Long,
    val lockedCells: Set<Int> = emptySet(),
) {
    val boardFull: Boolean
        get() = cells.indices.all { cells[it] != null || it in lockedCells }
}

data class LevelReward(
    val coins: Int = 0,
    val generatorDefId: String? = null,
    val inventoryCapBonus: Int = 0,
    val unlockCells: Int = 0,
)

enum class ItemSource { BOARD, INVENTORY }

sealed interface GameEvent {
    data class Merged(val cellIndex: Int, val newItem: BoardItem, val scoreGained: Int) : GameEvent
    data class Spawned(val cellIndex: Int, val item: BoardItem) : GameEvent
    data class Sold(val cellIndex: Int, val coinsGained: Int) : GameEvent
    data class OrderSpawned(val order: Order) : GameEvent
    data class OrderDelivered(val orderId: Long) : GameEvent
    data class OrderCompleted(val order: Order, val coinsGained: Int, val xpGained: Int) : GameEvent
    data class LevelUp(val level: Int, val reward: LevelReward) : GameEvent
    data class CellsUnlocked(val count: Int) : GameEvent
    data class GeneratorReady(val cellIndex: Int) : GameEvent
    data object GeneratorEmpty : GameEvent
    data object DeliverRejected : GameEvent
    data object InventoryFull : GameEvent
    data object NoEnergy : GameEvent
    data object BoardFull : GameEvent
    data object InvalidDrop : GameEvent
}
