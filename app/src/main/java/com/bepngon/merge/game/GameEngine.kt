package com.bepngon.merge.game

import kotlin.math.ceil
import kotlin.random.Random

object GameEngine {
    const val COLS = 5
    const val ROWS = 7
    const val CELLS = COLS * ROWS
    const val ENERGY_CAP = 100
    const val ENERGY_PER_SPAWN = 1
    const val START_COINS = 50
    const val MAX_INVENTORY = 8
    const val START_INVENTORY_CAP = 4
    const val MAX_ACTIVE_ORDERS = 5
    const val FIRST_ORDER_DELAY_MS = 8_000L
    const val ORDER_INTERVAL_MIN_MS = 6_000L
    const val ORDER_INTERVAL_MAX_MS = 28_000L
    const val GEN_COOLDOWN_MS = 45_000L
    const val DAILY_COINS = 50
    const val DAILY_ENERGY = 10
    const val DAILY_COOLDOWN_MS = 24 * 3_600_000L
    /** Claim within this window of the last claim to keep the streak alive. */
    const val DAILY_STREAK_WINDOW_MS = 48 * 3_600_000L
    /** (coins, energy) per streak day 1..7; day 7 also drops a magic wand. */
    val DAILY_REWARDS: List<Pair<Int, Int>> = listOf(
        50 to 10, 60 to 12, 75 to 15, 90 to 18, 110 to 22, 140 to 28, 200 to 40,
    )

    const val COMBO_WINDOW_MS = 30_000L
    const val COMBO_MIN = 3

    // Quick-time event: a claimable bonus appears for a short window.
    const val MAGIC_WILD_ID = "magic_wild"
    const val FRENZY_MS = 90_000L
    const val QTE_ENERGY = 15
    const val QTE_COINS = 30
    const val QTE_FIRST_MS = 90_000L
    const val QTE_OFFER_MS = 15_000L
    const val QTE_INTERVAL_MIN_MS = 150_000L
    const val QTE_INTERVAL_MAX_MS = 240_000L
    /** Magic-wand weight in the QTE roll while a generator is cooling. */
    const val QTE_WILD_WHEN_COOLING = 0.5f

    // Cafe room, pet and mini-game (Suika fruit drop).
    const val ENERGY_REGEN_BASE_MS = 60_000L
    const val PET_FEED_COINS = 15
    const val PET_FEED_MS = 30 * 60_000L
    const val PANDA_GIFT_MS = 4 * 3_600_000L
    const val MINI_MAX_PLAYS = 3
    const val MINI_REFILL_MS = 4 * 3_600_000L
    const val MINI_MAX_ENERGY = 20

    // Cells locked at game start, in unlock order (first entries unlock first).
    val INITIAL_LOCKED: List<Int> = listOf(
        idx(4, 0), idx(4, 4),
        idx(5, 0), idx(5, 2), idx(5, 4),
        idx(5, 1), idx(5, 3), idx(6, 0),
        idx(6, 1), idx(6, 2), idx(6, 3), idx(6, 4),
    )

    private val UNLOCK_SCHEDULE: Map<Int, Int> = mapOf(
        2 to 2, 3 to 3, 4 to 3, 5 to 4,
    )

    /** Frozen high-tier items hidden under locked cells (revealed on level-up)
     *  plus a few on the open board at start. */
    private val FROZEN_SEEDS: Map<Int, String> = mapOf(
        idx(4, 0) to "fruit_4",   // under web, unlocks Lv2
        idx(4, 4) to "bakery_4",  // under web, unlocks Lv2
        idx(5, 2) to "fruit_5",
        idx(5, 4) to "drink_3",
        idx(6, 1) to "viet_3",
        idx(6, 3) to "fruit_6",
        idx(6, 0) to "bakery_5",
    )

    fun initialState(): GameState {
        val cells = MutableList<BoardItem?>(CELLS) { null }
        var uid = 1L
        fun place(defId: String, index: Int, frozen: Boolean = false) {
            cells[index] = newItem(uid++, defId).copy(frozen = frozen)
        }
        place("gen_fruit_1", idx(2, 1))
        place("gen_fruit_1", idx(2, 3))
        place("gen_bakery_1", idx(3, 2))
        place("fruit_1", idx(1, 1))
        place("fruit_1", idx(1, 3))
        place("bakery_1", idx(3, 3))
        place("fruit_3", idx(3, 0), frozen = true)
        place("bakery_3", idx(4, 2), frozen = true)
        FROZEN_SEEDS.forEach { (cell, defId) -> place(defId, cell, frozen = true) }
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
            nextQteAtMs = QTE_FIRST_MS,
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
        if (dragged.frozen) return state to emptyList() // frozen items can't move
        val target = state.cells[to]

        if (target == null) {
            val cells = state.cells.toMutableList()
            cells[to] = dragged
            cells[from] = null
            return state.copy(cells = cells) to emptyList()
        }

        // Magic wand: drop onto any unfrozen item to duplicate it into the
        // wand's own cell (the wand is consumed).
        if (dragged.defId == MAGIC_WILD_ID) {
            if (target.frozen || target.defId == MAGIC_WILD_ID) {
                return state to listOf(GameEvent.InvalidDrop)
            }
            val cells = state.cells.toMutableList()
            cells[from] = newItem(state.nextUid, target.defId)
            return state.copy(cells = cells, nextUid = state.nextUid + 1) to
                listOf(GameEvent.ItemDuplicated(from))
        }

        // Dropping a matching item onto a frozen one merges and unlocks it.
        val okToMerge = if (target.frozen) {
            dragged.defId == target.defId
        } else {
            canMerge(dragged, target)
        }
        if (!okToMerge) {
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
        val cells = state.cells.toMutableList()
        val inventory = state.inventory.toMutableList()
        if (sourceIndex != null) cells[sourceIndex] = null
        if (sourceInventorySlot != null) inventory[sourceInventorySlot] = null

        val result = ItemCatalog.mergeResultOf(dragged)
        if (result == null) {
            // Max-tier frozen item: consume the dragged match to unfreeze in place.
            if (!target.frozen) return state to emptyList()
            cells[targetIndex] = target.copy(frozen = false)
            return state.copy(cells = cells, inventory = inventory) to
                listOf(GameEvent.Unfrozen(targetIndex))
        }
        val newItem = newItem(state.nextUid, result.id)
        cells[targetIndex] = newItem
        val events = mutableListOf<GameEvent>(
            GameEvent.Merged(targetIndex, newItem, result.score)
        )
        val next = state.copy(
            cells = cells,
            inventory = inventory,
            score = state.score + result.score,
            nextUid = state.nextUid + 1,
            stats = state.stats.copy(
                merges = state.stats.merges + 1,
                maxTier = maxOf(state.stats.maxTier, result.tier),
            ),
        )
        return checkAchievements(next, events) to events
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

        // Magic wand from the bag: duplicate the target into the wand's slot.
        if (dragged.defId == MAGIC_WILD_ID) {
            if (target.frozen || target.defId == MAGIC_WILD_ID) {
                return state to listOf(GameEvent.InvalidDrop)
            }
            val inventory = state.inventory.toMutableList()
            inventory[slot] = newItem(state.nextUid, target.defId)
            return state.copy(inventory = inventory, nextUid = state.nextUid + 1) to
                listOf(GameEvent.ItemDuplicated(-1 - slot))
        }

        val okToMerge = if (target.frozen) {
            dragged.defId == target.defId
        } else {
            canMerge(dragged, target)
        }
        if (!okToMerge) {
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
        if (dragged.frozen) return state to listOf(GameEvent.InvalidDrop)

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

    /** Coin cost to unlock the inventory slot at index [cap]; null if no more slots. */
    fun inventorySlotCost(cap: Int): Int? = when (cap) {
        4 -> 100
        5 -> 200
        6 -> 350
        7 -> 500
        else -> null
    }

    fun buyInventorySlot(state: GameState): Pair<GameState, List<GameEvent>> {
        val cost = inventorySlotCost(state.inventoryCap) ?: return state to emptyList()
        if (state.coins < cost) return state to listOf(GameEvent.NotEnoughCoins)
        return state.copy(
            coins = state.coins - cost,
            inventoryCap = state.inventoryCap + 1,
        ) to listOf(GameEvent.SlotBought(state.inventoryCap + 1))
    }

    /** Move an inventory item to the first free board cell. */
    fun retrieveToBoard(state: GameState, slot: Int): Pair<GameState, List<GameEvent>> {
        val item = state.inventory.getOrNull(slot) ?: return state to emptyList()
        val target = emptyCells(state).firstOrNull()
            ?: return state to listOf(GameEvent.BoardFull)
        val cells = state.cells.toMutableList()
        val inventory = state.inventory.toMutableList()
        cells[target] = item
        inventory[slot] = null
        return state.copy(cells = cells, inventory = inventory) to emptyList()
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

        val frenzy = nowMs < state.frenzyUntilMs
        val cooling = item.cooldownUntilMs > nowMs
        if (cooling || item.charges == 0) return state to listOf(GameEvent.GeneratorEmpty)
        if (!frenzy && state.energy < ENERGY_PER_SPAWN) {
            return state to listOf(GameEvent.NoEnergy)
        }

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

        val charges = if (frenzy) genItem.charges else genItem.charges - 1
        genItem = genItem.copy(
            charges = charges,
            cooldownUntilMs =
                if (!frenzy && charges == 0) nowMs + genCooldownMs(state, nowMs) else 0,
        )

        val cells = state.cells.toMutableList()
        cells[cellIndex] = genItem
        cells[target] = spawned
        return state.copy(
            cells = cells,
            energy = if (frenzy) state.energy else state.energy - ENERGY_PER_SPAWN,
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
        val (afterQte, qteEvents) = qteStep(afterOrders, nowMs, random)
        val (afterPet, petEvents) = petStep(afterQte, nowMs, random)

        // Mini-game play refill: one play per refill window while below cap.
        var afterMini = afterPet
        val miniEvents = mutableListOf<GameEvent>()
        if (afterMini.miniRefillAtMs in 1..nowMs && afterMini.miniPlaysLeft < MINI_MAX_PLAYS) {
            val left = afterMini.miniPlaysLeft + 1
            afterMini = afterMini.copy(
                miniPlaysLeft = left,
                miniRefillAtMs = if (left < MINI_MAX_PLAYS) nowMs + MINI_REFILL_MS else 0,
            )
        }

        return afterMini to (events + orderEvents + qteEvents + petEvents + miniEvents)
    }

    // ---------- quick-time events ----------

    private fun nextQteDelay(random: Random): Long =
        QTE_INTERVAL_MIN_MS + random.nextLong(QTE_INTERVAL_MAX_MS - QTE_INTERVAL_MIN_MS)

    private fun qteStep(
        state: GameState,
        nowMs: Long,
        random: Random,
    ): Pair<GameState, List<GameEvent>> {
        val offer = state.qte
        if (offer != null) {
            return if (nowMs > offer.expiresAtMs) {
                state.copy(qte = null, nextQteAtMs = nowMs + nextQteDelay(random)) to emptyList()
            } else {
                state to emptyList()
            }
        }
        if (state.nextQteAtMs <= 0 || nowMs < state.nextQteAtMs) return state to emptyList()
        val newOffer = QteOffer(rollQteReward(state, nowMs, random), nowMs + QTE_OFFER_MS)
        return state.copy(qte = newOffer) to listOf(GameEvent.QteOffered(newOffer))
    }

    /** Even odds normally; the wand jumps to [QTE_WILD_WHEN_COOLING] while a gen cools. */
    private fun rollQteReward(state: GameState, nowMs: Long, random: Random): QteReward {
        val cooling = (state.cells + state.inventory).any {
            it != null && ItemCatalog[it.defId].isGenerator && it.cooldownUntilMs > nowMs
        }
        if (!cooling) return QteReward.entries.random(random)
        if (random.nextFloat() < QTE_WILD_WHEN_COOLING) return QteReward.WILDCARD
        return QteReward.entries.filter { it != QteReward.WILDCARD }.random(random)
    }

    fun claimQte(
        state: GameState,
        nowMs: Long,
        random: Random = Random,
    ): Pair<GameState, List<GameEvent>> {
        val offer = state.qte ?: return state to emptyList()
        var next = state.copy(qte = null, nextQteAtMs = nowMs + nextQteDelay(random))
        val events = mutableListOf<GameEvent>(GameEvent.QteClaimed(offer.reward))
        when (offer.reward) {
            QteReward.FRENZY -> {
                next = next.copy(frenzyUntilMs = nowMs + FRENZY_MS)
                events += GameEvent.FrenzyStarted(next.frenzyUntilMs)
            }
            QteReward.ENERGY ->
                next = next.copy(energy = minOf(ENERGY_CAP, next.energy + QTE_ENERGY))
            QteReward.COINS -> next = next.copy(
                coins = next.coins + QTE_COINS,
                stats = next.stats.copy(coinsEarned = next.stats.coinsEarned + QTE_COINS),
            )
            QteReward.RECHARGE -> {
                val cells = next.cells.toMutableList()
                cells.forEachIndexed { i, it ->
                    if (it != null && ItemCatalog[it.defId].isGenerator) {
                        cells[i] = it.copy(
                            charges = ItemCatalog[it.defId].maxCharges,
                            cooldownUntilMs = 0,
                        )
                    }
                }
                next = next.copy(cells = cells)
            }
            QteReward.WILDCARD -> {
                val (placed, cellIndex) = placeWildcard(next, random)
                next = placed
                if (cellIndex != null) events += GameEvent.WildcardSpawned(cellIndex)
            }
        }
        return next to events
    }

    /** Drop a magic wand on a random empty cell (or free bag slot, or +coins
     *  when everything is full). Returns the new state + where it landed. */
    private fun placeWildcard(
        state: GameState,
        random: Random,
    ): Pair<GameState, Int?> {
        val empty = emptyCells(state)
        if (empty.isNotEmpty()) {
            val cell = empty.random(random)
            val cells = state.cells.toMutableList()
            cells[cell] = newItem(state.nextUid, MAGIC_WILD_ID)
            return state.copy(cells = cells, nextUid = state.nextUid + 1) to cell
        }
        val slot = state.inventory.indices.firstOrNull { i ->
            i < state.inventoryCap && state.inventory[i] == null
        }
        if (slot != null) {
            val inventory = state.inventory.toMutableList()
            inventory[slot] = newItem(state.nextUid, MAGIC_WILD_ID)
            return state.copy(inventory = inventory, nextUid = state.nextUid + 1) to (-1 - slot)
        }
        return state.copy(
            coins = state.coins + QTE_COINS,
            stats = state.stats.copy(coinsEarned = state.stats.coinsEarned + QTE_COINS),
        ) to null
    }

    // ---------- orders ----------

    fun maybeSpawnOrder(
        state: GameState,
        nowMs: Long,
        random: Random = Random,
    ): Pair<GameState, List<GameEvent>> {
        if (state.nextOrderAtMs <= 0) return state to emptyList()
        // Never leave the order list empty once spawning has started —
        // bypass the timer so the next tick refills it immediately.
        val emptyAfterStart = state.orders.isEmpty() && state.nextOrderId > 1
        if (nowMs < state.nextOrderAtMs && !emptyAfterStart) return state to emptyList()
        if (state.orders.size >= MAX_ACTIVE_ORDERS) {
            return state.copy(nextOrderAtMs = nowMs + 10_000L) to emptyList()
        }
        val order = generateOrder(state, random)
        return state.copy(
            orders = state.orders + order,
            nextOrderId = state.nextOrderId + 1,
            nextOrderAtMs = nowMs + nextOrderDelay(state, random),
        ) to listOf(GameEvent.OrderSpawned(order))
    }

    /**
     * Adaptive spawn pacing: orders arrive ~4x faster than the player's
     * measured completion pace (EMA in GameState.orderPaceMs), with ±40%
     * jitter so arrivals don't feel metronomic. Fast finishers get a new
     * order every ~6-10s; slow players stretch toward the 28s ceiling.
     */
    private fun nextOrderDelay(state: GameState, random: Random): Long {
        val base = if (state.orderPaceMs > 0) {
            state.orderPaceMs / 4
        } else {
            (ORDER_INTERVAL_MIN_MS + ORDER_INTERVAL_MAX_MS) / 2
        }
        val jittered = base * (0.6f + random.nextFloat() * 0.8f)
        return jittered.toLong().coerceIn(ORDER_INTERVAL_MIN_MS, ORDER_INTERVAL_MAX_MS)
    }

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

    /** True when every remaining required item exists on board + inventory.
     *  Frozen items don't count — they can't be delivered. */
    fun canFulfillOrder(state: GameState, order: Order): Boolean {
        if (order.remaining.isEmpty()) return false
        val counts = mutableMapOf<String, Int>()
        state.cells.forEach { it?.takeUnless { i -> i.frozen }?.let { i -> counts.merge(i.defId, 1, Int::plus) } }
        state.inventory.forEach { it?.let { i -> counts.merge(i.defId, 1, Int::plus) } }
        return order.remaining.groupingBy { it }.eachCount()
            .all { (defId, need) -> (counts[defId] ?: 0) >= need }
    }

    /** Consume all remaining items for an order (board first, then inventory). */
    fun autoFulfillOrder(
        state: GameState,
        orderId: Long,
        nowMs: Long = 0,
    ): Pair<GameState, List<GameEvent>> {
        val order = state.orders.firstOrNull { it.id == orderId } ?: return state to emptyList()
        if (!canFulfillOrder(state, order)) {
            return state to listOf(GameEvent.DeliverRejected)
        }
        val cells = state.cells.toMutableList()
        val inventory = state.inventory.toMutableList()
        val flown = mutableListOf<Pair<String, Int>>()
        for (defId in order.remaining) {
            val boardIdx = cells.indexOfFirst { it?.defId == defId && it.frozen == false }
            if (boardIdx >= 0) {
                cells[boardIdx] = null
                flown += defId to boardIdx
                continue
            }
            val invIdx = inventory.indexOfFirst { it?.defId == defId }
            if (invIdx >= 0) {
                inventory[invIdx] = null
                flown += defId to (-1 - invIdx)
            }
        }
        val next = state.copy(cells = cells, inventory = inventory)
        val (done, events) = completeOrder(next, order, nowMs)
        return done to (listOf<GameEvent>(
            GameEvent.OrderDelivered(orderId),
            GameEvent.AutoFulfillFly(orderId, flown),
        ) + events)
    }

    fun deliverToOrder(
        state: GameState,
        orderId: Long,
        source: ItemSource,
        index: Int,
        nowMs: Long = 0,
    ): Pair<GameState, List<GameEvent>> {
        val order = state.orders.firstOrNull { it.id == orderId } ?: return state to emptyList()
        val item = when (source) {
            ItemSource.BOARD -> state.cells.getOrNull(index)
            ItemSource.INVENTORY -> state.inventory.getOrNull(index)
        } ?: return state to emptyList()

        if (item.frozen || item.defId !in order.remaining) {
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
            val (rewarded, rewardEvents) = completeOrder(next, updated, nowMs)
            next = rewarded
            events += rewardEvents
        }
        return next to events
    }

    private fun completeOrder(
        state: GameState,
        order: Order,
        nowMs: Long = 0,
    ): Pair<GameState, List<GameEvent>> {
        val baseCoins = order.required.sumOf { ItemCatalog[it].sellPrice } * 5 / 4
        val coinsGained = baseCoins * (100 + orderCoinPct(state, nowMs)) / 100
        val xpGained =
            (order.required.sumOf { ItemCatalog[it].tier * 2 } + 5) *
                (100 + xpPct(state, nowMs)) / 100

        // Combo: consecutive completed orders inside the window build a chain;
        // from the 3rd onward each pays a growing bonus.
        val combo = if (nowMs < state.comboUntilMs) state.comboCount + 1 else 1
        val comboCoins = if (combo >= COMBO_MIN) 10 * combo else 0
        val comboXp = if (combo >= COMBO_MIN) 4 * combo else 0
        val events = mutableListOf<GameEvent>(
            GameEvent.OrderCompleted(order, coinsGained, xpGained)
        )
        if (combo >= COMBO_MIN) {
            events += GameEvent.ComboBonus(combo, comboCoins, comboXp)
        }

        // completion pace (EMA α=0.25) — feeds adaptive order spawn timing
        val dt = if (state.lastOrderDoneAtMs > 0) {
            (nowMs - state.lastOrderDoneAtMs).coerceAtLeast(0)
        } else {
            0L
        }
        val pace = when {
            dt <= 0L -> state.orderPaceMs
            state.orderPaceMs <= 0L -> dt
            else -> (state.orderPaceMs * 3 + dt) / 4
        }

        var next = state.copy(
            orders = state.orders.filterNot { it.id == order.id },
            coins = state.coins + coinsGained + comboCoins,
            xp = state.xp + xpGained + comboXp,
            comboCount = combo,
            comboUntilMs = nowMs + comboWindowMs(state, nowMs),
            orderPaceMs = pace,
            lastOrderDoneAtMs = nowMs,
            stats = state.stats.copy(
                ordersDone = state.stats.ordersDone + 1,
                coinsEarned = state.stats.coinsEarned + coinsGained + comboCoins,
            ),
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
        return checkAchievements(next, events) to events
    }

    // ---------- achievements / daily / onboarding ----------

    private fun checkAchievements(
        state: GameState,
        events: MutableList<GameEvent>,
    ): GameState {
        var next = state
        for (a in Achievements.ALL) {
            if (a.id !in next.reachedAchievements && next.metricOf(a.metric) >= a.goal) {
                next = next.copy(reachedAchievements = next.reachedAchievements + a.id)
                events += GameEvent.AchievementUnlocked(a)
            }
        }
        return next
    }

    fun claimAchievement(state: GameState, id: String): Pair<GameState, List<GameEvent>> {
        val a = Achievements.ALL.firstOrNull { it.id == id } ?: return state to emptyList()
        if (id in state.claimedAchievements || state.metricOf(a.metric) < a.goal) {
            return state to emptyList()
        }
        return state.copy(
            coins = state.coins + a.rewardCoins,
            energy = minOf(ENERGY_CAP, state.energy + a.rewardEnergy),
            claimedAchievements = state.claimedAchievements + id,
        ) to listOf(GameEvent.AchievementClaimed(a))
    }

    fun dailyReady(state: GameState, epochMs: Long): Boolean =
        epochMs - state.lastDailyClaimEpochMs >= DAILY_COOLDOWN_MS

    /** 0-based index into [DAILY_REWARDS] the next claim would grant. */
    fun dailyDayIndex(state: GameState, epochMs: Long): Int = when {
        state.lastDailyClaimEpochMs == 0L -> 0
        epochMs - state.lastDailyClaimEpochMs < DAILY_STREAK_WINDOW_MS ->
            (state.dailyStreak + 1) % DAILY_REWARDS.size
        else -> 0
    }

    fun claimDaily(
        state: GameState,
        epochMs: Long,
        random: Random = Random,
    ): Pair<GameState, List<GameEvent>> {
        if (!dailyReady(state, epochMs)) return state to listOf(GameEvent.DailyNotReady)
        val day = dailyDayIndex(state, epochMs)
        val (coins, energy) = DAILY_REWARDS[day]
        var next = state.copy(
            coins = state.coins + coins,
            energy = minOf(ENERGY_CAP, state.energy + energy),
            lastDailyClaimEpochMs = epochMs,
            dailyStreak = day,
            stats = state.stats.copy(coinsEarned = state.stats.coinsEarned + coins),
        )
        val events = mutableListOf<GameEvent>(
            GameEvent.DailyClaimed(day + 1, coins, energy, wildcard = day == DAILY_REWARDS.lastIndex)
        )
        if (day == DAILY_REWARDS.lastIndex) {
            val (placed, cellIndex) = placeWildcard(next, random)
            next = placed
            if (cellIndex != null) events += GameEvent.WildcardSpawned(cellIndex)
        }
        return next to events
    }

    fun finishOnboarding(state: GameState): Pair<GameState, List<GameEvent>> =
        state.copy(onboardingDone = true) to listOf(GameEvent.OnboardingFinished)

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

    // ---------- cafe room, pet & mini game ----------

    private fun fedPetDef(state: GameState, nowMs: Long): PetDef? =
        state.pet
            ?.takeIf { it.isFed(nowMs) }
            ?.let { PetCatalog.byId(it.defId) }

    fun orderCoinPct(state: GameState, nowMs: Long): Int =
        DecorCatalog.orderCoinsPct(state.decorOwned) +
            ((fedPetDef(state, nowMs)?.buff as? PetBuff.OrderCoinsPct)?.pct ?: 0)

    fun xpPct(state: GameState, nowMs: Long): Int =
        (fedPetDef(state, nowMs)?.buff as? PetBuff.XpPct)?.pct ?: 0

    fun genCooldownMs(state: GameState, nowMs: Long): Long {
        val pct = DecorCatalog.genCooldownPct(state.decorOwned) +
            ((fedPetDef(state, nowMs)?.buff as? PetBuff.GenCooldownPct)?.pct ?: 0)
        // Recharge can at most be halved so cooldowns still matter.
        return GEN_COOLDOWN_MS * (100 - pct).coerceAtLeast(50) / 100
    }

    fun energyRegenMs(state: GameState, nowMs: Long): Long {
        val pct = DecorCatalog.energyRegenPct(state.decorOwned) +
            ((fedPetDef(state, nowMs)?.buff as? PetBuff.EnergyRegenPct)?.pct ?: 0)
        return ENERGY_REGEN_BASE_MS * (100 - pct).coerceAtLeast(60) / 100
    }

    fun comboWindowMs(state: GameState, nowMs: Long): Long =
        COMBO_WINDOW_MS +
            ((fedPetDef(state, nowMs)?.buff as? PetBuff.ComboBonusMs)?.ms ?: 0L)

    private fun feedMs(state: GameState, nowMs: Long): Long {
        // The feed-duration bonus is intrinsic to the pet, not gated on being fed.
        val bonus = state.pet?.let {
            (PetCatalog.byId(it.defId).buff as? PetBuff.FeedBonusMs)?.ms
        } ?: 0L
        return PET_FEED_MS + bonus
    }

    fun buyDecor(state: GameState, decorId: String): Pair<GameState, List<GameEvent>> {
        val def = DecorCatalog.byId(decorId)
        if (def.id in state.decorOwned ||
            state.level < def.unlockLevel ||
            state.coins < def.cost
        ) {
            return state to listOf(GameEvent.NotEnoughCoins)
        }
        return state.copy(
            coins = state.coins - def.cost,
            decorOwned = state.decorOwned + def.id,
        ) to listOf(GameEvent.DecorBought(def.id))
    }

    fun adoptPet(state: GameState, petId: String, nowMs: Long): Pair<GameState, List<GameEvent>> {
        val def = PetCatalog.byId(petId)
        if (state.coins < def.cost) return state to listOf(GameEvent.NotEnoughCoins)
        // Adoption comes with a first free meal so the buff kicks in right away.
        val pet = PetState(
            defId = def.id,
            fedUntilMs = nowMs + feedMs(state, nowMs),
            nextGiftAtMs = if (def.buff is PetBuff.PeriodicGift) nowMs + PANDA_GIFT_MS else 0,
        )
        return state.copy(coins = state.coins - def.cost, pet = pet) to
            listOf(GameEvent.PetAdopted(def.id))
    }

    fun feedPetCoins(state: GameState, nowMs: Long): Pair<GameState, List<GameEvent>> {
        val pet = state.pet ?: return state to emptyList()
        if (state.coins < PET_FEED_COINS) return state to listOf(GameEvent.NotEnoughCoins)
        return state.copy(
            coins = state.coins - PET_FEED_COINS,
            pet = pet.copy(fedUntilMs = nowMs + feedMs(state, nowMs)),
        ) to listOf(GameEvent.PetFed(nowMs + feedMs(state, nowMs)))
    }

    private fun petStep(
        state: GameState,
        nowMs: Long,
        random: Random,
    ): Pair<GameState, List<GameEvent>> {
        val pet = state.pet ?: return state to emptyList()
        val def = PetCatalog.byId(pet.defId)
        if (def.buff !is PetBuff.PeriodicGift || pet.nextGiftAtMs <= 0) {
            return state to emptyList()
        }
        if (nowMs < pet.nextGiftAtMs) return state to emptyList()
        if (!pet.isFed(nowMs)) {
            // Gift waits while the pet is hungry — nudge the timer a little.
            return state.copy(pet = pet.copy(nextGiftAtMs = nowMs + 10 * 60_000L)) to emptyList()
        }
        val empty = emptyCells(state)
        val next = state.copy(pet = pet.copy(nextGiftAtMs = nowMs + PANDA_GIFT_MS))
        if (empty.isEmpty()) return next to emptyList()
        val giftTiers = ItemCatalog.allInFamilies(
            setOf(Family.FRUIT, Family.BAKERY, Family.DRINK, Family.VIET),
        ).filter { it.tier in 1..3 }
        val cell = empty.random(random)
        val cells = next.cells.toMutableList()
        cells[cell] = newItem(next.nextUid, giftTiers.random(random).id)
        return next.copy(cells = cells, nextUid = next.nextUid + 1) to
            listOf(GameEvent.PetGift(cell))
    }

    fun startMiniGame(state: GameState, nowMs: Long): Pair<GameState, List<GameEvent>> {
        // unlimited plays — kept as a hook so the caller stays unchanged
        return state to emptyList()
    }

    fun finishMiniGame(
        state: GameState,
        score: Int,
        nowMs: Long,
    ): Pair<GameState, List<GameEvent>> {
        val energy = minOf(MINI_MAX_ENERGY, score / 10)
        return state.copy(
            energy = minOf(ENERGY_CAP, state.energy + energy),
            miniBest = maxOf(state.miniBest, score),
        ) to listOf(GameEvent.MiniGameFinished(score, energy))
    }

    // ---------- debug ----------

    /** Debug cheat (tap coin chip 10×): jump to max level — every level reward
     *  applies, so all cells unlock and reward generators get granted — plus a
     *  fat wallet, full energy and max inventory for testing. */
    const val DEBUG_LEVEL = 20
    const val DEBUG_COINS = 99_999

    fun debugMaxOut(state: GameState): Pair<GameState, List<GameEvent>> {
        var next = state
        var unlocked = 0
        while (next.level < DEBUG_LEVEL) {
            next = next.copy(level = next.level + 1)
            val reward = rewardForLevel(next.level)
            unlocked += minOf(reward.unlockCells, next.lockedCells.size)
            next = applyLevelReward(next, reward)
        }
        next = next.copy(
            xp = 0,
            coins = next.coins + DEBUG_COINS,
            energy = ENERGY_CAP,
            inventoryCap = MAX_INVENTORY,
            onboardingDone = true,
            miniPlaysLeft = MINI_MAX_PLAYS,
            miniRefillAtMs = 0L,
            stats = next.stats.copy(coinsEarned = next.stats.coinsEarned + DEBUG_COINS),
        )
        val events = mutableListOf<GameEvent>(
            GameEvent.LevelUp(next.level, rewardForLevel(next.level)),
        )
        if (unlocked > 0) events += GameEvent.CellsUnlocked(unlocked)
        return checkAchievements(next, events) to events
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
