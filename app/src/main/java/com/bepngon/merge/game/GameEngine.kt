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
    const val GEN_COOLDOWN_MS = 90_000L

    // Cells locked at game start, in unlock order (first entries unlock first).
    val INITIAL_LOCKED: List<Int> = listOf(
        idx(5, 0), idx(5, 6), idx(6, 0), idx(6, 1), idx(6, 5), idx(6, 6),
        idx(7, 0), idx(7, 1), idx(7, 2), idx(7, 3), idx(7, 4), idx(7, 5), idx(7, 6),
        idx(8, 0), idx(8, 1), idx(8, 2), idx(8, 3), idx(8, 4), idx(8, 5), idx(8, 6),
    )

    private val UNLOCK_SCHEDULE: Map<Int, Int> = mapOf(
        2 to 2, 3 to 3, 4 to 3, 5 to 4, 6 to 4, 7 to 4,
    )

    fun initialState(): GameState {
        val cells = MutableList<BoardItem?>(CELLS) { null }
        var uid = 1L
        fun place(defId: String, index: Int) {
            cells[index] = newItem(uid++, defId)
        }
        place("gen_fruit_1", idx(3, 3))
        place("gen_fruit_1", idx(3, 5))
        place("gen_bakery_1", idx(5, 4))
        place("fruit_1", idx(1, 2))
        place("fruit_1", idx(1, 4))
        place("bakery_1", idx(4, 3))
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
            lockedCells = INITIAL_LOCKED.toSet(),
        )
    }

    fun newItem(uid: Long, defId: String): BoardItem {
        val def = ItemCatalog[defId]
        return BoardItem(uid, defId, charges = if (def.isGenerator) def.maxCharges else -1)
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
        if (to in state.lockedCells) return state to listOf(GameEvent.InvalidDrop)
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
        val newItem = newItem(state.nextUid, result.id)
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
        if (to in state.lockedCells) return state to listOf(GameEvent.InvalidDrop)
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
        nowMs: Long,
        random: Random = Random,
    ): Pair<GameState, List<GameEvent>> {
        val item = state.cells.getOrNull(cellIndex) ?: return state to emptyList()
        val def = ItemCatalog[item.defId]
        if (!def.isGenerator || def.spawnTable.isEmpty()) return state to emptyList()

        val cooling = item.cooldownUntilMs > nowMs
        if (cooling || item.charges == 0) return state to listOf(GameEvent.GeneratorEmpty)
        if (state.energy < ENERGY_PER_SPAWN) return state to listOf(GameEvent.NoEnergy)

        val empty = emptyCells(state)
        if (empty.isEmpty()) return state to listOf(GameEvent.BoardFull)

        // Gen that finished cooling (or old save without charges) is fully charged.
        var genItem = item
        if (item.cooldownUntilMs in 1..nowMs || item.charges < 0) {
            genItem = item.copy(charges = def.maxCharges, cooldownUntilMs = 0)
        }

        val adjacent = empty.filter { isAdjacent(it, cellIndex) }
        val target = (adjacent.ifEmpty { empty }).random(random)
        val spawnedDefId = pickWeighted(def.spawnTable, random)
        val spawned = newItem(state.nextUid, spawnedDefId)

        val charges = genItem.charges - 1
        genItem = genItem.copy(
            charges = charges,
            cooldownUntilMs = if (charges == 0) nowMs + GEN_COOLDOWN_MS else 0,
        )

        val cells = state.cells.toMutableList()
        cells[cellIndex] = genItem
        cells[target] = spawned
        return state.copy(
            cells = cells,
            energy = state.energy - ENERGY_PER_SPAWN,
            nextUid = state.nextUid + 1,
        ) to listOf(GameEvent.Spawned(target, spawned))
    }

    // ---------- periodic tick: recharge gens + spawn orders ----------

    fun tick(
        state: GameState,
        nowMs: Long,
        random: Random = Random,
    ): Pair<GameState, List<GameEvent>> {
        val events = mutableListOf<GameEvent>()
        var next = state

        val cells = next.cells.toMutableList()
        var recharged = false
        cells.forEachIndexed { i, it ->
            if (it != null && it.cooldownUntilMs in 1..nowMs) {
                cells[i] = it.copy(
                    charges = ItemCatalog[it.defId].maxCharges,
                    cooldownUntilMs = 0,
                )
                events += GameEvent.GeneratorReady(i)
                recharged = true
            }
        }
        if (recharged) next = next.copy(cells = cells)

        val (afterOrders, orderEvents) = maybeSpawnOrder(next, nowMs, random)
        return afterOrders to (events + orderEvents)
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
            .filter { it.isGenerator && it.spawnTable.isNotEmpty() }
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
            val unlocked = minOf(
                reward.unlockCells,
                next.lockedCells.size,
            )
            next = applyLevelReward(next, reward)
            events += GameEvent.LevelUp(next.level, reward)
            if (unlocked > 0) events += GameEvent.CellsUnlocked(unlocked)
        }
        return next to events
    }

    fun xpNeededForLevel(level: Int): Int = 20 + level * 12

    fun rewardForLevel(level: Int): LevelReward {
        val unlock = UNLOCK_SCHEDULE[level] ?: 0
        return when (level) {
            2 -> LevelReward(coins = 30, unlockCells = unlock)
            3 -> LevelReward(
                generatorDefId = "gen_drink_1",
                inventoryCapBonus = 1,
                unlockCells = unlock,
            )
            4 -> LevelReward(coins = 50, unlockCells = unlock)
            5 -> LevelReward(
                generatorDefId = "gen_viet_1",
                inventoryCapBonus = 1,
                unlockCells = unlock,
            )
            else -> LevelReward(coins = 20 + level * 10, unlockCells = unlock)
        }
    }

    /** Level at which a still-locked cell opens, or null if it never locks/unlocks. */
    fun unlockLevelForCell(cellIndex: Int): Int? {
        val pos = INITIAL_LOCKED.indexOf(cellIndex)
        if (pos < 0) return null
        var need = pos + 1
        var level = 2
        while (level <= 20) {
            need -= UNLOCK_SCHEDULE[level] ?: 0
            if (need <= 0) return level
            level++
        }
        return null
    }

    private fun applyLevelReward(state: GameState, reward: LevelReward): GameState {
        val toUnlock = INITIAL_LOCKED
            .filter { it in state.lockedCells }
            .take(reward.unlockCells)
        var next = state.copy(
            coins = state.coins + reward.coins,
            inventoryCap = minOf(MAX_INVENTORY, state.inventoryCap + reward.inventoryCapBonus),
            lockedCells = state.lockedCells - toUnlock.toSet(),
        )
        val genDefId = reward.generatorDefId ?: return next
        val emptyBoard = emptyCells(next).firstOrNull()
        if (emptyBoard != null) {
            val cells = next.cells.toMutableList()
            cells[emptyBoard] = newItem(next.nextUid, genDefId)
            next = next.copy(cells = cells, nextUid = next.nextUid + 1)
        } else {
            val emptySlot = (0 until next.inventoryCap).firstOrNull { next.inventory[it] == null }
            if (emptySlot != null) {
                val inventory = next.inventory.toMutableList()
                inventory[emptySlot] = newItem(next.nextUid, genDefId)
                next = next.copy(inventory = inventory, nextUid = next.nextUid + 1)
            } else {
                next = next.copy(coins = next.coins + 100)
            }
        }
        return next
    }

    // ---------- helpers ----------

    fun emptyCells(state: GameState): List<Int> =
        state.cells.indices.filter { state.cells[it] == null && it !in state.lockedCells }

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
