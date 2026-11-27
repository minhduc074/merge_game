package com.bepngon.merge.game

data class BoardItem(
    val uid: Long,
    val defId: String,
    val charges: Int = -1,
    val cooldownUntilMs: Long = 0,
    /** Deadline for the next +1 charge while a generator is partially used. */
    val rechargeAtMs: Long = 0,
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
    val spawns: Int = 0,
    val raresFound: Int = 0,
    val qteClaimed: Int = 0,
    val maxCombo: Int = 0,
)

enum class Metric {
    MERGES,
    ORDERS,
    LEVEL,
    COINS_EARNED,
    HIGHEST_TIER,
    SPAWNS,
    RARES,
    QTES,
    MAX_COMBO,
    STREAK,
    MINI_BEST,
    BLOCK_BEST,
    SCORE,
    INVENTORY_CAP,
    DECOR_PLACED,
    GENERATOR_T2,
}

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
        Achievement("merge500", "Nghệ nhân merge", "Merge 500 lần", "🎖️", 500, Metric.MERGES, rewardCoins = 400),
        Achievement("merge1000", "Bậc thầy merge", "Merge 1000 lần", "🏅", 1000, Metric.MERGES, rewardCoins = 800),
        Achievement("order5", "Phục vụ tân thủ", "Hoàn thành 5 đơn", "📦", 5, Metric.ORDERS, rewardCoins = 30),
        Achievement("order20", "Đầu bếp chăm chỉ", "Hoàn thành 20 đơn", "🧑‍🍳", 20, Metric.ORDERS, rewardCoins = 100),
        Achievement("order50", "Chủ quán chăm chỉ", "Hoàn thành 50 đơn", "🏪", 50, Metric.ORDERS, rewardCoins = 250),
        Achievement("order100", "Ông trùm F&B", "Hoàn thành 100 đơn", "👑", 100, Metric.ORDERS, rewardCoins = 600),
        Achievement("tier3", "Món ngon", "Tạo item tier 3+", "🍪", 3, Metric.HIGHEST_TIER, rewardCoins = 30),
        Achievement("tier5", "Chuỗi xịn", "Tạo item tier 5+", "🍰", 5, Metric.HIGHEST_TIER, rewardCoins = 60),
        Achievement("tier7", "Siêu phẩm", "Tạo item tier 7+", "💎", 7, Metric.HIGHEST_TIER, rewardCoins = 150),
        Achievement("tier9", "Đỉnh cao ẩm thực", "Tạo item tier 9+", "🌟", 9, Metric.HIGHEST_TIER, rewardCoins = 400),
        Achievement("lv3", "Lên hạng", "Đạt level 3", "🌟", 3, Metric.LEVEL, rewardCoins = 50),
        Achievement("lv5", "Cao thủ", "Đạt level 5", "💫", 5, Metric.LEVEL, rewardCoins = 120),
        Achievement("lv10", "Lão làng", "Đạt level 10", "🎗️", 10, Metric.LEVEL, rewardCoins = 300),
        Achievement("lv15", "Đầu bếp cự phách", "Đạt level 15", "🏅", 15, Metric.LEVEL, rewardCoins = 500),
        Achievement("lv30", "Huyền thoại bếp", "Đạt level 30", "👑", 30, Metric.LEVEL, rewardCoins = 1500),
        Achievement("rich500", "Đại gia", "Kiếm tổng 500🪙", "💰", 500, Metric.COINS_EARNED, rewardEnergy = 20),
        Achievement("rich2000", "Phú ông", "Kiếm tổng 2.000🪙", "💎", 2000, Metric.COINS_EARNED, rewardEnergy = 40),
        Achievement("rich5000", "Triệu phú", "Kiếm tổng 5.000🪙", "🏦", 5000, Metric.COINS_EARNED, rewardEnergy = 100),
        Achievement("rich10000", "Tỷ phú", "Kiếm tổng 10.000🪙", "👑", 10000, Metric.COINS_EARNED, rewardEnergy = 250),
        Achievement("spawn50", "Khởi động máy", "Sinh 50 item từ máy", "🔋", 50, Metric.SPAWNS, rewardCoins = 40),
        Achievement("spawn200", "Dây chuyền sản xuất", "Sinh 200 item từ máy", "⚙️", 200, Metric.SPAWNS, rewardCoins = 120),
        Achievement("spawn500", "Nhà máy không nghỉ", "Sinh 500 item từ máy", "🏭", 500, Metric.SPAWNS, rewardCoins = 300),
        Achievement("rare1", "Đồ hiếm đầu tiên", "Sở hữu 1 item hiếm", "✨", 1, Metric.RARES, rewardCoins = 50),
        Achievement("rare10", "Thợ săn đồ hiếm", "Sở hữu 10 item hiếm", "🌟", 10, Metric.RARES, rewardCoins = 150),
        Achievement("rare30", "Bộ sưu tập hiếm", "Sở hữu 30 item hiếm", "💠", 30, Metric.RARES, rewardCoins = 350),
        Achievement("streak3", "Khách quen", "Điểm danh liên tiếp tới ngày 3", "📅", 2, Metric.STREAK, rewardCoins = 60),
        Achievement("streak7", "Ruột của quán", "Điểm danh liên tiếp tới ngày 7", "🗓️", 6, Metric.STREAK, rewardCoins = 150, rewardEnergy = 20),
        Achievement("score500", "Điểm khởi đầu", "Đạt 500 điểm", "⭐", 500, Metric.SCORE, rewardCoins = 30),
        Achievement("score2000", "Điểm cao thủ", "Đạt 2.000 điểm", "🌠", 2000, Metric.SCORE, rewardCoins = 100),
        Achievement("score8000", "Điểm số huyền thoại", "Đạt 8.000 điểm", "🏆", 8000, Metric.SCORE, rewardCoins = 300),
        Achievement("mini50", "Tập thả trái", "Đạt 50 điểm minigame", "🍉", 50, Metric.MINI_BEST, rewardCoins = 40),
        Achievement("mini150", "Cao thủ thả trái", "Đạt 150 điểm minigame", "🥝", 150, Metric.MINI_BEST, rewardCoins = 100),
        Achievement("mini300", "Vua thả trái", "Đạt 300 điểm minigame", "👑", 300, Metric.MINI_BEST, rewardCoins = 250),
        Achievement("block800", "Tập xếp khối", "Đạt 800 điểm Xếp khối", "🧱", 800, Metric.BLOCK_BEST, rewardCoins = 40),
        Achievement("block2500", "Cao thủ xếp khối", "Đạt 2.500 điểm Xếp khối", "🟦", 2500, Metric.BLOCK_BEST, rewardCoins = 100),
        Achievement("block7000", "Vua xếp khối", "Đạt 7.000 điểm Xếp khối", "👑", 7000, Metric.BLOCK_BEST, rewardCoins = 250),
        Achievement("bag6", "Kho rộng rãi", "Mở 6 ô kho", "🎒", 6, Metric.INVENTORY_CAP, rewardCoins = 60),
        Achievement("bag8", "Kho siêu cấp", "Mở hết 8 ô kho", "🧰", 8, Metric.INVENTORY_CAP, rewardCoins = 150),
        Achievement("decor5", "Tân trang nhà", "Đặt 5 món đồ trang trí", "🛋️", 5, Metric.DECOR_PLACED, rewardCoins = 50),
        Achievement("decor15", "Nhà thiết kế nội thất", "Đặt 15 món đồ trang trí", "🏡", 15, Metric.DECOR_PLACED, rewardCoins = 150),
        Achievement("genup1", "Máy cấp hai", "Sở hữu máy sinh tier 2+", "🧺", 1, Metric.GENERATOR_T2, rewardCoins = 80),
        Achievement("genup3", "Xưởng hiện đại", "Sở hữu 3 máy sinh tier 2+", "🏭", 3, Metric.GENERATOR_T2, rewardCoins = 200),
        Achievement("qte5", "Tay nhanh mắt tinh", "Nhận 5 quà bất ngờ", "🎁", 5, Metric.QTES, rewardCoins = 50),
        Achievement("qte20", "Thợ săn quà", "Nhận 20 quà bất ngờ", "🎉", 20, Metric.QTES, rewardCoins = 150),
        Achievement("combo5", "Combo bùng nổ", "Đạt combo 5 đơn", "🔥", 5, Metric.MAX_COMBO, rewardCoins = 80),
        Achievement("combo8", "Combo thần tốc", "Đạt combo 8 đơn", "⚡", 8, Metric.MAX_COMBO, rewardCoins = 200),
    )

    fun byId(id: String): Achievement = ALL.first { it.id == id }

    /** Tiered chains: claim tier N to reveal tier N+1 in the same row. */
    val CHAINS = listOf(
        listOf(byId("merge10"), byId("merge50"), byId("merge200"), byId("merge500"), byId("merge1000")),
        listOf(byId("order5"), byId("order20"), byId("order50"), byId("order100")),
        listOf(byId("tier3"), byId("tier5"), byId("tier7"), byId("tier9")),
        listOf(byId("lv3"), byId("lv5"), byId("lv10"), byId("lv15"), byId("lv30")),
        listOf(byId("rich500"), byId("rich2000"), byId("rich5000"), byId("rich10000")),
        listOf(byId("spawn50"), byId("spawn200"), byId("spawn500")),
        listOf(byId("rare1"), byId("rare10"), byId("rare30")),
        listOf(byId("streak3"), byId("streak7")),
        listOf(byId("score500"), byId("score2000"), byId("score8000")),
        listOf(byId("mini50"), byId("mini150"), byId("mini300")),
        listOf(byId("block800"), byId("block2500"), byId("block7000")),
        listOf(byId("bag6"), byId("bag8")),
        listOf(byId("decor5"), byId("decor15")),
        listOf(byId("genup1"), byId("genup3")),
        listOf(byId("qte5"), byId("qte20")),
        listOf(byId("combo5"), byId("combo8")),
    )
}

data class GameState(
    val cells: List<BoardItem?>,
    val inventory: List<BoardItem?>,
    val inventoryCap: Int,
    val orders: List<Order>,
    val nextOrderId: Long,
    val nextOrderAtMs: Long,
    val energy: Int,
    val nextEnergyAtMs: Long = 0,
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
    val frenzyUntilMs: Long = 0,
    val qte: QteOffer? = null,
    val nextQteAtMs: Long = 0,
    val dailyStreak: Int = 0,
    val comboCount: Int = 0,
    val comboUntilMs: Long = 0,
    val miniPlaysLeft: Int = 3,
    val miniRefillAtMs: Long = 0,
    val miniBest: Int = 0,
    val blockBest: Int = 0,
    /** EMA of ms between completed orders — drives adaptive spawn pacing. */
    val orderPaceMs: Long = 0,
    val lastOrderDoneAtMs: Long = 0,
    val decor: DecorState = DecorState(),
) {
    fun metricOf(metric: Metric): Int = when (metric) {
        Metric.MERGES -> stats.merges
        Metric.ORDERS -> stats.ordersDone
        Metric.LEVEL -> level
        Metric.COINS_EARNED -> stats.coinsEarned
        Metric.HIGHEST_TIER -> stats.maxTier
        Metric.SPAWNS -> stats.spawns
        Metric.RARES -> stats.raresFound
        Metric.QTES -> stats.qteClaimed
        Metric.MAX_COMBO -> stats.maxCombo
        Metric.STREAK -> dailyStreak
        Metric.MINI_BEST -> miniBest
        Metric.BLOCK_BEST -> blockBest
        Metric.SCORE -> score
        Metric.INVENTORY_CAP -> inventoryCap
        Metric.DECOR_PLACED -> decor.rooms.values.sumOf { it.placed.size }
        Metric.GENERATOR_T2 -> (cells + inventory).count {
            it != null && ItemCatalog[it.defId].let { d -> d.isGenerator && d.tier >= 2 }
        }
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

enum class QteReward { WILDCARD, FRENZY, ENERGY, COINS, RECHARGE }

/** A timed bonus the player taps to claim before it expires. */
data class QteOffer(val reward: QteReward, val expiresAtMs: Long)

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
    data class GeneratorUpgraded(val cellIndex: Int, val cost: Int, val newDefId: String) : GameEvent
    data class SlotBought(val newCap: Int) : GameEvent
    data class AchievementUnlocked(val achievement: Achievement) : GameEvent
    data class AchievementClaimed(val achievement: Achievement) : GameEvent
    data class DailyClaimed(
        val day: Int,
        val coins: Int,
        val energy: Int,
        val wildcard: Boolean = false,
    ) : GameEvent
    data object DailyNotReady : GameEvent
    /** Bonus for completing [combo] orders inside the combo window. */
    data class ComboBonus(val combo: Int, val coins: Int, val xp: Int) : GameEvent
    data object OnboardingFinished : GameEvent
    data object GeneratorEmpty : GameEvent
    data object NotEnoughCoins : GameEvent
    data object DeliverRejected : GameEvent
    data object InventoryFull : GameEvent
    data object NoEnergy : GameEvent
    data object BoardFull : GameEvent
    data object InvalidDrop : GameEvent
    data class QteOffered(val offer: QteOffer) : GameEvent
    data class QteClaimed(val reward: QteReward) : GameEvent
    /** Wildcard item placed; index >= 0 is a board cell, < 0 encodes -1 - inventory slot. */
    data class WildcardSpawned(val index: Int) : GameEvent
    /** Duplicate created by the magic wand; index uses the same encoding as [WildcardSpawned]. */
    data class ItemDuplicated(val index: Int) : GameEvent
    data class FrenzyStarted(val untilMs: Long) : GameEvent
    data class MiniGameFinished(val score: Int, val energy: Int) : GameEvent
    data class BlockGameFinished(val score: Int, val energy: Int) : GameEvent
    data object MiniGameNoPlays : GameEvent
    data class VariantBought(val variantId: String, val cost: Int) : GameEvent
    data class DecorPlaced(val roomId: String, val uid: Long) : GameEvent
    data class DecorRemoved(val roomId: String, val uid: Long) : GameEvent
    data class PaintApplied(val roomId: String) : GameEvent
}
