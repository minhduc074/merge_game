package com.bepngon.merge.game

data class BoardItem(
    val uid: Long,
    val defId: String,
    val charges: Int = -1,
    val cooldownUntilMs: Long = 0,
    val frozen: Boolean = false,
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

data class Stats(
    val merges: Int = 0,
    val ordersDone: Int = 0,
    val coinsEarned: Int = 0,
    val maxTier: Int = 0,
)

enum class Metric { MERGES, ORDERS, LEVEL, COINS_EARNED, HIGHEST_TIER }

data class Achievement(
    val id: String,
    val title: String,
    val desc: String,
    val emoji: String,
    val goal: Int,
    val metric: Metric,
    val rewardCoins: Int = 0,
    val rewardEnergy: Int = 0,
)

object Achievements {
    val ALL = listOf(
        Achievement("merge10", "Tập merge", "Merge 10 lần", "🔗", 10, Metric.MERGES, rewardCoins = 20),
        Achievement("merge50", "Tay merge vàng", "Merge 50 lần", "⚡", 50, Metric.MERGES, rewardCoins = 60),
        Achievement("merge200", "Merge huyền thoại", "Merge 200 lần", "👑", 200, Metric.MERGES, rewardCoins = 200),
        Achievement("order5", "Phục vụ tân thủ", "Hoàn thành 5 đơn", "📦", 5, Metric.ORDERS, rewardCoins = 30),
        Achievement("order20", "Đầu bếp chăm chỉ", "Hoàn thành 20 đơn", "🧑‍🍳", 20, Metric.ORDERS, rewardCoins = 100),
        Achievement("tier3", "Món ngon", "Tạo item tier 3+", "🍪", 3, Metric.HIGHEST_TIER, rewardCoins = 30),
        Achievement("lv3", "Lên hạng", "Đạt level 3", "🌟", 3, Metric.LEVEL, rewardCoins = 50),
        Achievement("lv5", "Cao thủ", "Đạt level 5", "💫", 5, Metric.LEVEL, rewardCoins = 120),
        Achievement("tier5", "Chuỗi xịn", "Tạo item tier 5+", "🍰", 5, Metric.HIGHEST_TIER, rewardCoins = 60),
        Achievement("rich500", "Đại gia", "Kiếm tổng 500🪙", "💰", 500, Metric.COINS_EARNED, rewardEnergy = 20),
    )

    fun byId(id: String): Achievement = ALL.first { it.id == id }
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
    val stats: Stats = Stats(),
    val reachedAchievements: Set<String> = emptySet(),
    val claimedAchievements: Set<String> = emptySet(),
    val lastDailyClaimEpochMs: Long = 0,
    val onboardingDone: Boolean = false,
) {
    fun metricOf(metric: Metric): Int = when (metric) {
        Metric.MERGES -> stats.merges
        Metric.ORDERS -> stats.ordersDone
        Metric.LEVEL -> level
        Metric.COINS_EARNED -> stats.coinsEarned
        Metric.HIGHEST_TIER -> stats.maxTier
    }
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
    data class Unfrozen(val cellIndex: Int) : GameEvent
    data class OrderSpawned(val order: Order) : GameEvent
    data class OrderDelivered(val orderId: Long) : GameEvent
    /** Items flown to the customer on auto-fulfill. fromIndex >= 0 is a board cell,
     *  < 0 encodes inventory slot as (-1 - slot). */
    data class AutoFulfillFly(
        val orderId: Long,
        val flown: List<Pair<String, Int>>,
    ) : GameEvent
    data class OrderCompleted(val order: Order, val coinsGained: Int, val xpGained: Int) : GameEvent
    data class LevelUp(val level: Int, val reward: LevelReward) : GameEvent
    data class CellsUnlocked(val count: Int) : GameEvent
    data class GeneratorReady(val cellIndex: Int) : GameEvent
    data class SlotBought(val newCap: Int) : GameEvent
    data class AchievementUnlocked(val achievement: Achievement) : GameEvent
    data class AchievementClaimed(val achievement: Achievement) : GameEvent
    data class DailyClaimed(val coins: Int, val energy: Int) : GameEvent
    data object DailyNotReady : GameEvent
    data object OnboardingFinished : GameEvent
    data object GeneratorEmpty : GameEvent
    data object NotEnoughCoins : GameEvent
    data object DeliverRejected : GameEvent
    data object InventoryFull : GameEvent
    data object NoEnergy : GameEvent
    data object BoardFull : GameEvent
    data object InvalidDrop : GameEvent
}
