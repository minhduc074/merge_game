package com.bepngon.merge.game

import kotlin.math.ceil
import kotlin.random.Random

object GameEngine {
    const val COLS = 7
    const val ROWS = 9
    const val CELLS = COLS * ROWS
    const val ENERGY_CAP = 100
    const val ENERGY_PER_SPAWN = 1
    const val START_COINS = 50
    const val MAX_INVENTORY = 8
    const val START_INVENTORY_CAP = 4
    const val MAX_ACTIVE_ORDERS = 3
    const val FIRST_ORDER_DELAY_MS = 8_000L
    const val ORDER_INTERVAL_MIN_MS = 20_000L
    const val ORDER_INTERVAL_MAX_MS = 35_000L

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
            inventory = List(MAX_INVENTORY) { null },
            inventoryCap = START_INVENTORY_CAP,
            orders = emptyList(),
            nextOrderId = 1,
            nextOrderAtMs = FIRST_ORDER_DELAY_MS,
            energy = ENERGY_CAP,
            coins = START_COINS,
            score = 0,
            xp = 0,
            level = 1,
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

    // ---------- board moves / merges ----------

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

        return doMerge(state, dragged, target, sourceIndex = from, targetIndex = to)
    }

    private fun doMerge(
        state: GameState,
        dragged: BoardItem,
        target: BoardItem,
        sourceIndex: Int?,
        targetIndex: Int,
        sourceInventorySlot: Int? = null,
    ): Pair<GameState, List<GameEvent>> {
        val result = ItemCatalog.mergeResultOf(dragged) ?: return state to emptyList()
        val newItem = BoardItem(state.nextUid, result.id)
        val cells = state.cells.toMutableList()
        val inventory = state.inventory.toMutableList()
        cells[targetIndex] = newItem
        if (sourceIndex != null) cells[sourceIndex] = null
        if (sourceInventorySlot != null) inventory[sourceInventorySlot] = null
        return state.copy(
            cells = cells,
            inventory = inventory,
            score = state.score + result.score,
            nextUid = state.nextUid + 1,
        ) to listOf(GameEvent.Merged(targetIndex, newItem, result.score))
    }

    fun inventoryToBoard(state: GameState, slot: Int, to: Int): Pair<GameState, List<GameEvent>> {
        if (to !in 0 until CELLS || slot !in state.inventory.indices) return state to emptyList()
        val dragged = state.inventory[slot] ?: return state to emptyList()
        val target = state.cells[to]

        if (target == null) {
            val cells = state.cells.toMutableList()
            val inventory = state.inventory.toMutableList()
            cells[to] = dragged
            inventory[slot] = null
            return state.copy(cells = cells, inventory = inventory) to emptyList()
        }

        if (!canMerge(dragged, target)) {
            return state to listOf(GameEvent.InvalidDrop)
        }
        return doMerge(state, dragged, target, sourceIndex = null, targetIndex = to, sourceInventorySlot = slot)
    }

    fun boardToInventory(state: GameState, from: Int, slot: Int): Pair<GameState, List<GameEvent>> {
        if (from !in 0 until CELLS || slot !in 0 until state.inventoryCap) {
            return state to listOf(GameEvent.InvalidDrop)
        }
        if (state.inventory[slot] != null) return state to listOf(GameEvent.InvalidDrop)
        val dragged = state.cells[from] ?: return state to emptyList()

        val cells = state.cells.toMutableList()
        val inventory = state.inventory.toMutableList()
        inventory[slot] = dragged
        cells[from] = null
        return state.copy(cells = cells, inventory = inventory) to emptyList()
    }

    fun moveInInventory(state: GameState, from: Int, to: Int): Pair<GameState, List<GameEvent>> {
        if (from == to || from !in state.inventory.indices || to !in 0 until state.inventoryCap) {
            return state to emptyList()
        }
        val item = state.inventory[from] ?: return state to emptyList()
        val inventory = state.inventory.toMutableList()
        inventory[to] = item.also { inventory[from] = inventory[to] }
        return state.copy(inventory = inventory) to emptyList()
    }

    // ---------- generator ----------

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

    // ---------- selling ----------

    fun sell(state: GameState, cellIndex: Int): Pair<GameState, List<GameEvent>> {
        val item = state.cells.getOrNull(cellIndex) ?: return state to emptyList()
        val price = ItemCatalog[item.defId].sellPrice
        val cells = state.cells.toMutableList()
        cells[cellIndex] = null
        return state.copy(cells = cells, coins = state.coins + price) to
            listOf(GameEvent.Sold(cellIndex, price))
    }

    fun sellFromInventory(state: GameState, slot: Int): Pair<GameState, List<GameEvent>> {
        val item = state.inventory.getOrNull(slot) ?: return state to emptyList()
        val price = ItemCatalog[item.defId].sellPrice
        val inventory = state.inventory.toMutableList()
        inventory[slot] = null
        return state.copy(inventory = inventory, coins = state.coins + price) to
            listOf(GameEvent.Sold(-1, price))
    }

    // ---------- orders ----------

    fun maybeSpawnOrder(
        state: GameState,
        nowMs: Long,
        random: Random = Random,
    ): Pair<GameState, List<GameEvent>> {
        if (state.nextOrderAtMs <= 0 || nowMs < state.nextOrderAtMs) return state to emptyList()
        if (state.orders.size >= MAX_ACTIVE_ORDERS) {
            return state.copy(nextOrderAtMs = nowMs + 10_000L) to emptyList()
        }
        val order = generateOrder(state, random)
        return state.copy(
            orders = state.orders + order,
            nextOrderId = state.nextOrderId + 1,
            nextOrderAtMs = nowMs + nextOrderDelay(random),
        ) to listOf(GameEvent.OrderSpawned(order))
    }

    private fun nextOrderDelay(random: Random): Long =
        ORDER_INTERVAL_MIN_MS + random.nextLong(ORDER_INTERVAL_MAX_MS - ORDER_INTERVAL_MIN_MS)

    private fun generateOrder(state: GameState, random: Random): Order {
        val produceable = produceableFamilies(state)
        val candidates = ItemCatalog.allInFamilies(produceable)
            .filter { it.tier <= 5 }

        val count = when (random.nextInt(100)) {
            in 0 until 50 -> 1
            in 50 until 85 -> 2
            else -> 3
        }
        val picked = mutableListOf<String>()
        repeat(count) {
            val def = pickOrderItem(candidates, random)
            picked += def.id
        }
        val customer = Customers.ALL.random(random)
        return Order(
            id = state.nextOrderId,
            customer = customer,
            required = picked,
        )
    }

    private fun pickOrderItem(candidates: List<ItemDef>, random: Random): ItemDef {
        val weights = candidates.map { def ->
            when (def.tier) {
                1 -> 40
                2 -> 30
                3 -> 18
                4 -> 9
                else -> 3
            }
        }
        val total = weights.sum()
        var roll = random.nextInt(total)
        candidates.forEachIndexed { i, def ->
            roll -= weights[i]
            if (roll < 0) return def
        }
        return candidates.first()
    }

    fun produceableFamilies(state: GameState): Set<Family> {
        val families = state.cells.mapNotNull { it?.defId }
            .map { ItemCatalog[it] }
            .filter { it.isGenerator }
            .map { gen -> ItemCatalog[gen.spawnTable.first().itemId].family }
            .toMutableSet()
        if (families.isEmpty()) families += Family.FRUIT
        return families
    }

    fun deliverToOrder(
        state: GameState,
        orderId: Long,
        source: ItemSource,
        index: Int,
    ): Pair<GameState, List<GameEvent>> {
        val order = state.orders.firstOrNull { it.id == orderId } ?: return state to emptyList()
        val item = when (source) {
            ItemSource.BOARD -> state.cells.getOrNull(index)
            ItemSource.INVENTORY -> state.inventory.getOrNull(index)
        } ?: return state to emptyList()

        if (item.defId !in order.remaining) {
            return state to listOf(GameEvent.DeliverRejected)
        }

        val cells = state.cells.toMutableList()
        val inventory = state.inventory.toMutableList()
        when (source) {
            ItemSource.BOARD -> cells[index] = null
            ItemSource.INVENTORY -> inventory[index] = null
        }

        val updated = order.copy(fulfilled = order.fulfilled + item.defId)
        var next = state.copy(
            cells = cells,
            inventory = inventory,
            orders = state.orders.map { if (it.id == orderId) updated else it },
        )
        val events = mutableListOf<GameEvent>(GameEvent.OrderDelivered(orderId))

        if (updated.isComplete) {
            val (rewarded, rewardEvents) = completeOrder(next, updated)
            next = rewarded
            events += rewardEvents
        }
        return next to events
    }

    private fun completeOrder(state: GameState, order: Order): Pair<GameState, List<GameEvent>> {
        val coinsGained = order.required.sumOf { ItemCatalog[it].sellPrice } * 3 / 2
        val xpGained = order.required.sumOf { ItemCatalog[it].tier * 2 } + 5
        val events = mutableListOf<GameEvent>(
            GameEvent.OrderCompleted(order, coinsGained, xpGained)
        )

        var next = state.copy(
            orders = state.orders.filterNot { it.id == order.id },
            coins = state.coins + coinsGained,
            xp = state.xp + xpGained,
        )

        while (next.xp >= xpNeededForLevel(next.level)) {
            next = next.copy(
                xp = next.xp - xpNeededForLevel(next.level),
                level = next.level + 1,
            )
            val reward = rewardForLevel(next.level)
            next = applyLevelReward(next, reward)
            events += GameEvent.LevelUp(next.level, reward)
        }
        return next to events
    }

    fun xpNeededForLevel(level: Int): Int = 20 + level * 12

    fun rewardForLevel(level: Int): LevelReward = when (level) {
        2 -> LevelReward(coins = 30)
        3 -> LevelReward(generatorDefId = "gen_drink_1", inventoryCapBonus = 1)
        4 -> LevelReward(coins = 50)
        5 -> LevelReward(generatorDefId = "gen_viet_1", inventoryCapBonus = 1)
        else -> LevelReward(coins = 20 + level * 10)
    }

    private fun applyLevelReward(state: GameState, reward: LevelReward): GameState {
        var next = state.copy(
            coins = state.coins + reward.coins,
            inventoryCap = minOf(MAX_INVENTORY, state.inventoryCap + reward.inventoryCapBonus),
        )
        val genDefId = reward.generatorDefId ?: return next
        val emptyBoard = emptyCells(next).firstOrNull()
        if (emptyBoard != null) {
            val cells = next.cells.toMutableList()
            cells[emptyBoard] = BoardItem(next.nextUid, genDefId)
            next = next.copy(cells = cells, nextUid = next.nextUid + 1)
        } else {
            val emptySlot = (0 until next.inventoryCap).firstOrNull { next.inventory[it] == null }
            if (emptySlot != null) {
                val inventory = next.inventory.toMutableList()
                inventory[emptySlot] = BoardItem(next.nextUid, genDefId)
                next = next.copy(inventory = inventory, nextUid = next.nextUid + 1)
            } else {
                next = next.copy(coins = next.coins + 100)
            }
        }
        return next
    }

    // ---------- helpers ----------

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
