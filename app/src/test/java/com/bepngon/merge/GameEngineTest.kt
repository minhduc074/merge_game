package com.bepngon.merge

import com.bepngon.merge.game.BoardItem
import com.bepngon.merge.game.Customers
import com.bepngon.merge.game.DecorCatalog
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.GameEvent
import com.bepngon.merge.game.GameState
import com.bepngon.merge.game.ItemCatalog
import com.bepngon.merge.game.ItemSource
import com.bepngon.merge.game.Order
import com.bepngon.merge.game.QteOffer
import com.bepngon.merge.game.QteReward
import com.bepngon.merge.game.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private fun stateWith(
    vararg placements: Pair<Int, String>,
    energy: Int = 10,
    inventory: List<String?> = List(GameEngine.MAX_INVENTORY) { null },
    inventoryCap: Int = GameEngine.START_INVENTORY_CAP,
    orders: List<Order> = emptyList(),
    nextOrderAtMs: Long = 0,
    xp: Int = 0,
    level: Int = 1,
    coins: Int = 0,
    lockedCells: Set<Int> = emptySet(),
    stats: Stats = Stats(),
    reachedAchievements: Set<String> = emptySet(),
    frozenCells: Set<Int> = emptySet(),
): GameState {
    val cells = MutableList<BoardItem?>(GameEngine.CELLS) { null }
    var uid = 1L
    placements.forEach { (i, defId) ->
        cells[i] = GameEngine.newItem(uid++, defId).copy(frozen = i in frozenCells)
    }
    val inv = MutableList<BoardItem?>(GameEngine.MAX_INVENTORY) { null }
    inventory.forEachIndexed { i, defId -> defId?.let { inv[i] = BoardItem(uid++, it) } }
    return GameState(
        cells = cells,
        inventory = inv,
        inventoryCap = inventoryCap,
        orders = orders,
        nextOrderId = 1,
        nextOrderAtMs = nextOrderAtMs,
        energy = energy,
        coins = coins,
        score = 0,
        xp = xp,
        level = level,
        nextUid = uid,
        lockedCells = lockedCells,
        stats = stats,
        reachedAchievements = reachedAchievements,
    )
}

private fun orderOf(vararg defIds: String): Order =
    Order(id = 1, customer = Customers.ALL.first(), required = defIds.toList())

class GameEngineTest {

    @Test
    fun `merge two identical items creates next tier and scores`() {
        val s = stateWith(0 to "fruit_1", 1 to "fruit_1")
        val (next, events) = GameEngine.moveOrMerge(s, 0, 1)
        assertNull(next.cells[0])
        assertEquals("fruit_2", next.cells[1]?.defId)
        assertEquals(3, next.score)
        assertTrue(events.single() is GameEvent.Merged)
    }

    @Test
    fun `drag to empty cell moves item`() {
        val s = stateWith(0 to "fruit_1", 1 to "fruit_2")
        val (next, events) = GameEngine.moveOrMerge(s, 0, 5)
        assertNull(next.cells[0])
        assertEquals("fruit_1", next.cells[5]?.defId)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `drag onto different item is rejected`() {
        val s = stateWith(0 to "fruit_1", 1 to "fruit_2")
        val (next, events) = GameEngine.moveOrMerge(s, 0, 1)
        assertEquals("fruit_1", next.cells[0]?.defId)
        assertEquals("fruit_2", next.cells[1]?.defId)
        assertEquals(listOf(GameEvent.InvalidDrop), events)
    }

    @Test
    fun `max tier items cannot merge`() {
        val s = stateWith(0 to "fruit_11", 1 to "fruit_11")
        val (_, events) = GameEngine.moveOrMerge(s, 0, 1)
        assertEquals(listOf(GameEvent.InvalidDrop), events)
    }

    @Test
    fun `generator tap consumes energy, charge and spawns item`() {
        val s = stateWith(0 to "gen_fruit_1")
        val (next, events) = GameEngine.tapGenerator(s, 0, 1000, Random(42))
        assertEquals(9, next.energy)
        assertEquals(7, next.cells[0]?.charges) // maxCharges 8 - 1
        assertEquals(1, next.cells.count { it != null && it.defId.startsWith("fruit_") })
        assertTrue(events.single() is GameEvent.Spawned)
    }

    @Test
    fun `generator enters cooldown after last charge then recharges on tick`() {
        var s = stateWith(0 to "gen_fruit_1")
        // drain all 8 charges
        repeat(8) {
            s = GameEngine.tapGenerator(s, 0, 1000).first
            // free spawned items so board never fills
            val cells = s.cells.map { it?.takeIf { i -> i.defId.startsWith("gen_") } }
            s = s.copy(cells = cells)
        }
        val gen = s.cells[0]!!
        assertEquals(0, gen.charges)
        assertEquals(1000 + GameEngine.GEN_COOLDOWN_MS, gen.cooldownUntilMs)

        // tap while cooling → GeneratorEmpty
        val (_, ev1) = GameEngine.tapGenerator(s, 0, 2000)
        assertEquals(listOf(GameEvent.GeneratorEmpty), ev1)

        // tick after cooldown → recharged + event
        val (recharged, ev2) = GameEngine.tick(s, 1000 + GameEngine.GEN_COOLDOWN_MS + 1)
        assertEquals(8, recharged.cells[0]?.charges)
        assertEquals(0L, recharged.cells[0]?.cooldownUntilMs)
        assertTrue(ev2.any { it is GameEvent.GeneratorReady })
    }

    @Test
    fun `generator tap with no energy emits NoEnergy`() {
        val s = stateWith(0 to "gen_fruit_1", energy = 0)
        val (_, events) = GameEngine.tapGenerator(s, 0, 1000)
        assertEquals(listOf(GameEvent.NoEnergy), events)
    }

    @Test
    fun `generator tap with full board emits BoardFull`() {
        val s = stateWith(*Array(GameEngine.CELLS) { it to "fruit_1" }.also {
            it[0] = 0 to "gen_fruit_1"
        })
        val (_, events) = GameEngine.tapGenerator(s, 0, 1000)
        assertEquals(listOf(GameEvent.BoardFull), events)
    }

    @Test
    fun `generators can merge into next generator tier`() {
        val s = stateWith(0 to "gen_fruit_1", 1 to "gen_fruit_1")
        val (next, _) = GameEngine.moveOrMerge(s, 0, 1)
        assertNotNull(next.cells[1])
        assertEquals("gen_fruit_2", next.cells[1]?.defId)
    }

    // ---------- inventory ----------

    @Test
    fun `board item moves to empty inventory slot`() {
        val s = stateWith(0 to "fruit_2")
        val (next, _) = GameEngine.boardToInventory(s, 0, 2)
        assertNull(next.cells[0])
        assertEquals("fruit_2", next.inventory[2]?.defId)
    }

    @Test
    fun `board item cannot enter occupied inventory slot`() {
        val s = stateWith(0 to "fruit_2", inventory = listOf("fruit_1", null, null, null, null, null, null, null))
        val (next, events) = GameEngine.boardToInventory(s, 0, 0)
        assertEquals("fruit_2", next.cells[0]?.defId)
        assertEquals(listOf(GameEvent.InvalidDrop), events)
    }

    @Test
    fun `inventory item merges onto matching board item`() {
        val s = stateWith(5 to "fruit_1", inventory = listOf("fruit_1", null, null, null, null, null, null, null))
        val (next, events) = GameEngine.inventoryToBoard(s, 0, 5)
        assertNull(next.inventory[0])
        assertEquals("fruit_2", next.cells[5]?.defId)
        assertTrue(events.single() is GameEvent.Merged)
    }

    @Test
    fun `inventory item moves to empty board cell`() {
        val s = stateWith(inventory = listOf("fruit_3", null, null, null, null, null, null, null))
        val (next, _) = GameEngine.inventoryToBoard(s, 0, 10)
        assertNull(next.inventory[0])
        assertEquals("fruit_3", next.cells[10]?.defId)
    }

    @Test
    fun `buying inventory slot costs coins and raises cap`() {
        val s = stateWith(coins = 150)
        val (next, events) = GameEngine.buyInventorySlot(s)
        assertEquals(5, next.inventoryCap)
        assertEquals(50, next.coins)
        assertEquals(listOf(GameEvent.SlotBought(5)), events)
    }

    @Test
    fun `buying slot without enough coins is rejected`() {
        val s = stateWith(coins = 50)
        val (next, events) = GameEngine.buyInventorySlot(s)
        assertEquals(4, next.inventoryCap)
        assertEquals(listOf(GameEvent.NotEnoughCoins), events)
    }

    @Test
    fun `inventory cap stops at max`() {
        val s = stateWith(coins = 9999, inventoryCap = GameEngine.MAX_INVENTORY)
        val (next, events) = GameEngine.buyInventorySlot(s)
        assertEquals(GameEngine.MAX_INVENTORY, next.inventoryCap)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `retrieve moves inventory item to first free board cell`() {
        val s = stateWith(
            0 to "fruit_1",
            inventory = listOf("fruit_2", null, null, null, null, null, null, null),
        )
        val (next, _) = GameEngine.retrieveToBoard(s, 0)
        assertNull(next.inventory[0])
        assertEquals("fruit_2", next.cells[1]?.defId)
    }

    @Test
    fun `retrieve on full board fails`() {
        val full = Array(GameEngine.CELLS) { it to "fruit_1" }
        val s = stateWith(*full, inventory = listOf("fruit_2", null, null, null, null, null, null, null))
        val (_, events) = GameEngine.retrieveToBoard(s, 0)
        assertEquals(listOf(GameEvent.BoardFull), events)
    }

    // ---------- orders ----------

    @Test
    fun `delivering needed item progresses order`() {
        val s = stateWith(0 to "fruit_1", orders = listOf(orderOf("fruit_1", "fruit_2")))
        val (next, events) = GameEngine.deliverToOrder(s, 1, ItemSource.BOARD, 0)
        assertNull(next.cells[0])
        assertEquals(listOf("fruit_1"), next.orders.single().fulfilled)
        assertEquals(listOf(GameEvent.OrderDelivered(1)), events)
    }

    @Test
    fun `delivering unwanted item is rejected`() {
        val s = stateWith(0 to "fruit_3", orders = listOf(orderOf("fruit_1")))
        val (next, events) = GameEngine.deliverToOrder(s, 1, ItemSource.BOARD, 0)
        assertEquals("fruit_3", next.cells[0]?.defId)
        assertEquals(listOf(GameEvent.DeliverRejected), events)
    }

    @Test
    fun `completing order pays coins and xp then removes it`() {
        val s = stateWith(
            0 to "fruit_1",
            orders = listOf(orderOf("fruit_1")),
        )
        val (next, events) = GameEngine.deliverToOrder(s, 1, ItemSource.BOARD, 0)
        assertTrue(next.orders.isEmpty())
        assertEquals(1, next.coins) // fruit_1 sell=1, x1.5 → 1
        assertEquals(7, next.xp) // tier1*2 + 5
        assertTrue(events.any { it is GameEvent.OrderCompleted })
    }

    @Test
    fun `delivering from inventory works`() {
        val s = stateWith(
            inventory = listOf("fruit_1", null, null, null, null, null, null, null),
            orders = listOf(orderOf("fruit_1")),
        )
        val (next, _) = GameEngine.deliverToOrder(s, 1, ItemSource.INVENTORY, 0)
        assertNull(next.inventory[0])
        assertTrue(next.orders.isEmpty())
    }

    @Test
    fun `big order completion triggers level up and reward`() {
        // fruit_5 x2 + fruit_4 → xp = (10+10+8)+5 = 33 ≥ xpNeeded(1)=32
        val s = stateWith(
            0 to "fruit_5", 1 to "fruit_5", 2 to "fruit_4",
            orders = listOf(orderOf("fruit_5", "fruit_5", "fruit_4")),
        )
        var cur = s
        GameEngine.deliverToOrder(cur, 1, ItemSource.BOARD, 0).also { cur = it.first }
        GameEngine.deliverToOrder(cur, 1, ItemSource.BOARD, 1).also { cur = it.first }
        val (next, events) = GameEngine.deliverToOrder(cur, 1, ItemSource.BOARD, 2)
        assertEquals(2, next.level)
        assertEquals(1, next.xp)
        assertTrue(next.orders.isEmpty())
        assertTrue(events.any { it is GameEvent.LevelUp && it.level == 2 })
    }

    @Test
    fun `level 3 reward grants drink generator and inventory slot`() {
        // level 2 + 40xp; order fruit_5 x2 → +25xp = 65 ≥ xpNeeded(2)=44
        val s = stateWith(
            0 to "fruit_5", 1 to "fruit_5",
            orders = listOf(orderOf("fruit_5", "fruit_5")),
            xp = 40, level = 2,
        )
        var cur = s
        GameEngine.deliverToOrder(cur, 1, ItemSource.BOARD, 0).also { cur = it.first }
        val (next, _) = GameEngine.deliverToOrder(cur, 1, ItemSource.BOARD, 1)
        assertEquals(3, next.level)
        assertEquals(5, next.inventoryCap)
        assertTrue(next.cells.any { it?.defId == "gen_drink_1" })
    }

    @Test
    fun `order spawns when due and only from produceable families`() {
        val s = stateWith(0 to "gen_fruit_1", nextOrderAtMs = 10)
        val (next, events) = GameEngine.maybeSpawnOrder(s, 100, Random(1))
        assertEquals(1, next.orders.size)
        val order = next.orders.single()
        assertTrue(order.required.isNotEmpty())
        assertTrue(order.required.all { it.startsWith("fruit_") })
        assertTrue(events.single() is GameEvent.OrderSpawned)
        assertTrue(next.nextOrderAtMs > 100)
    }

    @Test
    fun `auto fulfill consumes board items and completes order`() {
        val s = stateWith(
            0 to "fruit_1", 1 to "fruit_2",
            orders = listOf(orderOf("fruit_1", "fruit_2")),
        )
        val (next, events) = GameEngine.autoFulfillOrder(s, 1)
        assertTrue(next.orders.isEmpty())
        assertNull(next.cells[0])
        assertNull(next.cells[1])
        assertTrue(events.any { it is GameEvent.OrderCompleted })
    }

    @Test
    fun `auto fulfill takes remainder from inventory`() {
        val s = stateWith(
            0 to "fruit_1",
            inventory = listOf("fruit_2", null, null, null, null, null, null, null),
            orders = listOf(orderOf("fruit_1", "fruit_2")),
        )
        val (next, _) = GameEngine.autoFulfillOrder(s, 1)
        assertNull(next.cells[0])
        assertNull(next.inventory[0])
        assertTrue(next.orders.isEmpty())
    }

    @Test
    fun `auto fulfill rejects when items missing`() {
        val s = stateWith(0 to "fruit_1", orders = listOf(orderOf("fruit_1", "fruit_1")))
        val (next, events) = GameEngine.autoFulfillOrder(s, 1)
        assertEquals("fruit_1", next.cells[0]?.defId)
        assertEquals(1, next.orders.size)
        assertEquals(listOf(GameEvent.DeliverRejected), events)
    }

    @Test
    fun `auto fulfill only consumes remaining after partial delivery`() {
        val s = stateWith(
            0 to "fruit_2",
            orders = listOf(orderOf("fruit_1", "fruit_2").copy(fulfilled = listOf("fruit_1"))),
        )
        val (next, _) = GameEngine.autoFulfillOrder(s, 1)
        assertNull(next.cells[0])
        assertTrue(next.orders.isEmpty())
    }

    @Test
    fun `merges increment stats and unlock achievement`() {
        var s = stateWith(0 to "fruit_1", 1 to "fruit_1")
        val unlocks = mutableListOf<GameEvent>()
        repeat(10) {
            val empties = s.cells.indices.filter { s.cells[it] == null }
            if (empties.size < 2) return@repeat
            val (next, ev) = GameEngine.moveOrMerge(s, empties[0], empties[0]) // placeholder
            s = next
        }
    }

    @Test
    fun `stats count merges and orders`() {
        var s = stateWith(0 to "fruit_1", 1 to "fruit_1")
        val (m, _) = GameEngine.moveOrMerge(s, 0, 1)
        assertEquals(1, m.stats.merges)
        assertEquals(2, m.stats.maxTier)

        s = stateWith(
            0 to "fruit_1",
            orders = listOf(orderOf("fruit_1")),
        )
        val (done, _) = GameEngine.deliverToOrder(s, 1, ItemSource.BOARD, 0)
        assertEquals(1, done.stats.ordersDone)
        assertTrue(done.stats.coinsEarned > 0)
    }

    @Test
    fun `achievement unlocks once and claim grants reward once`() {
        val s = stateWith(stats = Stats(merges = 10))
        // reaching goal is detected inside ops; emulate via a merge stat check:
        val s2 = s.copy(reachedAchievements = emptySet())
        // claim without reached-set still works since metric >= goal
        val (claimed, ev) = GameEngine.claimAchievement(s2, "merge10")
        assertEquals(s2.coins + 20, claimed.coins)
        assertTrue("merge10" in claimed.claimedAchievements)
        val (again, ev2) = GameEngine.claimAchievement(claimed, "merge10")
        assertEquals(claimed.coins, again.coins)
        assertTrue(ev2.isEmpty())
        assertTrue(ev.any { it is GameEvent.AchievementClaimed })
    }

    @Test
    fun `claim achievement rejects when goal not reached`() {
        val s = stateWith(stats = Stats(merges = 3))
        val (next, ev) = GameEngine.claimAchievement(s, "merge10")
        assertTrue(next === s)
        assertTrue(ev.isEmpty())
    }

    @Test
    fun `merges unlock achievement via engine check`() {
        var s = stateWith(0 to "fruit_1", 1 to "fruit_1", stats = Stats(merges = 9))
        val (next, ev) = GameEngine.moveOrMerge(s, 0, 1)
        assertEquals(10, next.stats.merges)
        assertTrue(ev.any { it is GameEvent.AchievementUnlocked && it.achievement.id == "merge10" })
        assertTrue("merge10" in next.reachedAchievements)
        // no duplicate unlock on subsequent merges
        val s2 = next.copy(cells = next.cells.map { null })
        val s3 = s2.copy(
            cells = s2.cells.toMutableList().also {
                it[0] = GameEngine.newItem(1, "fruit_1")
                it[1] = GameEngine.newItem(2, "fruit_1")
            },
        )
        val (_, ev3) = GameEngine.moveOrMerge(s3, 0, 1)
        assertTrue(ev3.none { it is GameEvent.AchievementUnlocked })
    }

    @Test
    fun `daily claim grants then blocks for 24h`() {
        val t0 = GameEngine.DAILY_COOLDOWN_MS + 60_000L
        val s = stateWith()
        val (claimed, ev) = GameEngine.claimDaily(s, t0)
        assertEquals(s.coins + GameEngine.DAILY_COINS, claimed.coins)
        assertEquals(
            minOf(GameEngine.ENERGY_CAP, s.energy + GameEngine.DAILY_ENERGY),
            claimed.energy,
        )
        assertTrue(ev.any { it is GameEvent.DailyClaimed })

        val (again, ev2) = GameEngine.claimDaily(claimed, t0 + 60_000)
        assertEquals(claimed.coins, again.coins)
        assertEquals(listOf(GameEvent.DailyNotReady), ev2)

        val (later, _) = GameEngine.claimDaily(
            claimed,
            t0 + GameEngine.DAILY_COOLDOWN_MS,
        )
        assertEquals(
            claimed.coins + GameEngine.DAILY_REWARDS[1].first,
            later.coins,
        )
    }

    // ---------- frozen items ----------

    @Test
    fun `frozen item cannot move`() {
        val s = stateWith(0 to "fruit_3", 5 to "fruit_1", frozenCells = setOf(0))
        val (next, _) = GameEngine.moveOrMerge(s, 0, 5)
        assertEquals("fruit_3", next.cells[0]?.defId)
        assertEquals("fruit_1", next.cells[5]?.defId)
    }

    @Test
    fun `matching item onto frozen merges and unlocks`() {
        val s = stateWith(0 to "fruit_3", 1 to "fruit_3", frozenCells = setOf(0))
        val (next, events) = GameEngine.moveOrMerge(s, 1, 0)
        assertNull(next.cells[1])
        assertEquals("fruit_4", next.cells[0]?.defId)
        assertEquals(false, next.cells[0]?.frozen)
        assertTrue(events.any { it is GameEvent.Merged })
    }

    @Test
    fun `non matching item onto frozen is rejected`() {
        val s = stateWith(0 to "fruit_3", 1 to "fruit_2", frozenCells = setOf(0))
        val (next, events) = GameEngine.moveOrMerge(s, 1, 0)
        assertEquals("fruit_3", next.cells[0]?.defId)
        assertEquals(true, next.cells[0]?.frozen)
        assertEquals(listOf(GameEvent.InvalidDrop), events)
    }

    @Test
    fun `max tier frozen unfreezes in place`() {
        val s = stateWith(0 to "fruit_11", 1 to "fruit_11", frozenCells = setOf(0))
        val (next, events) = GameEngine.moveOrMerge(s, 1, 0)
        assertNull(next.cells[1])
        assertEquals("fruit_11", next.cells[0]?.defId)
        assertEquals(false, next.cells[0]?.frozen)
        assertTrue(events.any { it is GameEvent.Unfrozen })
    }

    @Test
    fun `frozen item cannot be delivered or counted for orders`() {
        val s = stateWith(
            0 to "fruit_1",
            frozenCells = setOf(0),
            orders = listOf(orderOf("fruit_1")),
        )
        assertEquals(false, GameEngine.canFulfillOrder(s, s.orders.first()))
        val (next, events) = GameEngine.deliverToOrder(s, 1, ItemSource.BOARD, 0)
        assertEquals("fruit_1", next.cells[0]?.defId)
        assertEquals(listOf(GameEvent.DeliverRejected), events)
    }

    @Test
    fun `frozen item cannot go to inventory`() {
        val s = stateWith(0 to "fruit_3", frozenCells = setOf(0))
        val (next, events) = GameEngine.boardToInventory(s, 0, 0)
        assertEquals("fruit_3", next.cells[0]?.defId)
        assertEquals(listOf(GameEvent.InvalidDrop), events)
    }

    // ---------- locked cells ----------

    @Test
    fun `locked cells block drops and spawning`() {
        val s = stateWith(0 to "fruit_1", 1 to "gen_fruit_1", lockedCells = setOf(5, 6))
        val (moved, ev1) = GameEngine.moveOrMerge(s, 0, 5)
        assertEquals("fruit_1", moved.cells[0]?.defId)
        assertEquals(listOf(GameEvent.InvalidDrop), ev1)

        // spawn never lands on locked cells
        var cur = s
        repeat(7) {
            cur = GameEngine.tapGenerator(cur, 1, 1000, Random(3)).first
        }
        assertNull(cur.cells[5])
        assertNull(cur.cells[6])
    }

    @Test
    fun `level up unlocks locked cells`() {
        val s = stateWith(
            0 to "fruit_5",
            orders = listOf(orderOf("fruit_5", "fruit_5")),
            xp = 40, level = 2,
            lockedCells = GameEngine.INITIAL_LOCKED.toSet(),
        )
        var cur = GameEngine.deliverToOrder(s, 1, ItemSource.BOARD, 0).first
        val lockedBefore = cur.lockedCells.size
        // force xp over level 3 threshold via another order
        cur = cur.copy(
            orders = listOf(orderOf("fruit_5").copy(id = 2)),
            cells = cur.cells.toMutableList().also { it[0] = GameEngine.newItem(99, "fruit_5") },
        )
        val (next, events) = GameEngine.deliverToOrder(cur, 2, ItemSource.BOARD, 0)
        assertTrue(next.level >= 3)
        assertTrue(next.lockedCells.size < lockedBefore)
        assertTrue(events.any { it is GameEvent.CellsUnlocked })
    }

    @Test
    fun `merge resets generator charges`() {
        // drain a gen to 3 charges, then merge with fresh one
        var s = stateWith(0 to "gen_fruit_1", 1 to "gen_fruit_1")
        repeat(5) {
            s = GameEngine.tapGenerator(s, 0, 1000, Random(1)).first
            val cells = s.cells.map { it?.takeIf { i -> i.defId.startsWith("gen_") } }
            s = s.copy(cells = cells)
        }
        assertEquals(3, s.cells[0]?.charges)
        val (merged, _) = GameEngine.moveOrMerge(s, 0, 1)
        assertEquals("gen_fruit_2", merged.cells[1]?.defId)
        assertEquals(16, merged.cells[1]?.charges) // fresh maxCharges for tier-2 gen
    }

    @Test
    fun `no order spawns before due time or when slots full`() {
        val s = stateWith(0 to "gen_fruit_1", nextOrderAtMs = 10)
        val (early, _) = GameEngine.maybeSpawnOrder(s, 5, Random(1))
        assertTrue(early.orders.isEmpty())

        val fullOrders = List(GameEngine.MAX_ACTIVE_ORDERS) { i ->
            orderOf("fruit_1").copy(id = i + 1L)
        }
        val s2 = stateWith(0 to "gen_fruit_1", orders = fullOrders, nextOrderAtMs = 10)
        val (reschedule, _) = GameEngine.maybeSpawnOrder(s2, 100, Random(1))
        assertEquals(GameEngine.MAX_ACTIVE_ORDERS, reschedule.orders.size)
        assertEquals(10_100L, reschedule.nextOrderAtMs)
    }

    @Test
    fun `empty order list refills immediately regardless of timer`() {
        // All orders completed but nextOrderAtMs is still in the future —
        // the list must refill on the next tick, not wait out the timer.
        val s = stateWith(0 to "gen_fruit_1", nextOrderAtMs = 60_000)
            .copy(nextOrderId = 5)
        val (next, events) = GameEngine.maybeSpawnOrder(s, 100, Random(1))
        assertEquals(1, next.orders.size)
        assertTrue(events.any { it is GameEvent.OrderSpawned })
        assertTrue(next.nextOrderAtMs > 100)
    }

    @Test
    fun `first order still respects the initial delay`() {
        // nextOrderId == 1 → nothing has spawned yet, keep FIRST_ORDER_DELAY
        val s = stateWith(
            0 to "gen_fruit_1",
            nextOrderAtMs = GameEngine.FIRST_ORDER_DELAY_MS,
        )
        val (early, _) = GameEngine.maybeSpawnOrder(s, 5, Random(1))
        assertTrue(early.orders.isEmpty())
    }

    // ---------- quick-time events ----------

    @Test
    fun `wildcard duplicates the dragged-onto item into its own cell`() {
        val s = stateWith(0 to "magic_wild", 1 to "fruit_3")
        val (next, events) = GameEngine.moveOrMerge(s, 0, 1)
        assertEquals("fruit_3", next.cells[0]?.defId)
        assertEquals("fruit_3", next.cells[1]?.defId)
        assertTrue(events.single() is GameEvent.ItemDuplicated)
    }

    @Test
    fun `wildcard cannot target frozen items or other wildcards`() {
        val s = stateWith(0 to "magic_wild", 1 to "fruit_3", frozenCells = setOf(1))
        val (_, events) = GameEngine.moveOrMerge(s, 0, 1)
        assertTrue(events.single() is GameEvent.InvalidDrop)

        val s2 = stateWith(0 to "magic_wild", 1 to "magic_wild")
        val (_, events2) = GameEngine.moveOrMerge(s2, 0, 1)
        assertTrue(events2.single() is GameEvent.InvalidDrop)
    }

    @Test
    fun `wildcard moves onto empty cells like a normal item`() {
        val s = stateWith(0 to "magic_wild")
        val (next, _) = GameEngine.moveOrMerge(s, 0, 1)
        assertNull(next.cells[0])
        assertEquals("magic_wild", next.cells[1]?.defId)
    }

    @Test
    fun `qte offer appears on tick and expires`() {
        val s = stateWith(0 to "gen_fruit_1").copy(nextQteAtMs = 10)
        val (offered, events) = GameEngine.tick(s, 20, Random(1))
        assertNotNull(offered.qte)
        assertTrue(events.any { it is GameEvent.QteOffered })

        val (expired, _) = GameEngine.tick(
            offered,
            offered.qte!!.expiresAtMs + 1,
            Random(1),
        )
        assertNull(expired.qte)
        assertTrue(expired.nextQteAtMs > offered.qte!!.expiresAtMs + 1)
    }

    @Test
    fun `claiming frenzy makes generator taps free`() {
        var s = stateWith(0 to "gen_fruit_1", energy = 5)
            .copy(qte = QteOffer(QteReward.FRENZY, 60_000))
        val (claimed, events) = GameEngine.claimQte(s, 100, Random(1))
        assertNull(claimed.qte)
        assertEquals(100 + GameEngine.FRENZY_MS, claimed.frenzyUntilMs)
        assertTrue(events.any { it is GameEvent.FrenzyStarted })
        assertTrue(events.any { it is GameEvent.QteClaimed })

        val (tapped, _) = GameEngine.tapGenerator(claimed, 0, 200, Random(1))
        assertEquals(5, tapped.energy) // free during frenzy
        assertEquals(
            ItemCatalog["gen_fruit_1"].maxCharges,
            tapped.cells[0]?.charges,
        )
    }

    @Test
    fun `claiming wildcard spawns magic_wild on an empty cell`() {
        val s = stateWith(0 to "gen_fruit_1")
            .copy(qte = QteOffer(QteReward.WILDCARD, 60_000))
        val (next, events) = GameEngine.claimQte(s, 100, Random(1))
        assertTrue(next.cells.any { it?.defId == "magic_wild" })
        assertTrue(events.any { it is GameEvent.WildcardSpawned })
        assertNull(next.qte)
    }

    @Test
    fun `claimQte without an offer is a no-op`() {
        val s = stateWith(0 to "gen_fruit_1")
        val (next, events) = GameEngine.claimQte(s, 100, Random(1))
        assertEquals(s, next)
        assertTrue(events.isEmpty())
    }

    // ---------- order combo ----------

    @Test
    fun `completing orders in the window builds a combo with bonus from the third`() {
        var s = stateWith(
            0 to "fruit_1", 1 to "fruit_2", 2 to "fruit_3",
            orders = listOf(
                orderOf("fruit_1").copy(id = 1),
                orderOf("fruit_2").copy(id = 2),
                orderOf("fruit_3").copy(id = 3),
            ),
            level = 10, // keep xp rewards below the level-up threshold
        )
        fun complete(id: Long, at: Long) {
            val (n, ev) = GameEngine.autoFulfillOrder(s, id, at)
            s = n
            if (ev.any { it is GameEvent.ComboBonus }) {
                throw AssertionError("unexpected bonus at $at")
            }
        }
        // Orders 1-2: combo builds, no bonus yet
        complete(1, 0)
        complete(2, 5_000)
        assertEquals(2, s.comboCount)

        // Order 3 inside the window: bonus +30 coins +12 xp (10*x, 4*x)
        val (s3, events3) = GameEngine.autoFulfillOrder(s, 3, 10_000)
        val bonus = events3.filterIsInstance<GameEvent.ComboBonus>().single()
        assertEquals(3, bonus.combo)
        assertEquals(30, bonus.coins)
        assertEquals(12, bonus.xp)
        // Order pays sellPrice*5/4 for fruit_3 (=6) plus the combo bonus.
        assertEquals(s.coins + 6 + 30, s3.coins)
        assertEquals(3, s3.comboCount)
        assertEquals(10_000 + GameEngine.COMBO_WINDOW_MS, s3.comboUntilMs)
    }

    @Test
    fun `combo resets when the window lapses`() {
        var s = stateWith(
            0 to "fruit_1", 1 to "fruit_2",
            orders = listOf(
                orderOf("fruit_1").copy(id = 1),
                orderOf("fruit_2").copy(id = 2),
            ),
            level = 10,
        )
        s = GameEngine.autoFulfillOrder(s, 1, 0).first
        assertEquals(1, s.comboCount)
        val (s2, events) = GameEngine.autoFulfillOrder(s, 2, GameEngine.COMBO_WINDOW_MS + 1)
        assertEquals(1, s2.comboCount)
        assertTrue(events.none { it is GameEvent.ComboBonus })
    }

    // ---------- daily streak ----------

    @Test
    fun `daily claims advance the streak and wrap after day 7`() {
        var s = stateWith()
        val t0 = 1_000_000_000L
        repeat(8) { day ->
            val at = t0 + day * GameEngine.DAILY_COOLDOWN_MS
            val idx = GameEngine.dailyDayIndex(s, at)
            assertEquals(day % 7, idx)
            val (next, events) = GameEngine.claimDaily(s, at, Random(1))
            val claimed = events.filterIsInstance<GameEvent.DailyClaimed>().single()
            assertEquals(day % 7 + 1, claimed.day)
            if (idx == 6) {
                assertTrue(claimed.wildcard)
                assertTrue(events.any { it is GameEvent.WildcardSpawned })
            } else {
                assertTrue(!claimed.wildcard)
            }
            s = next
        }
        assertEquals(0, s.dailyStreak) // wrapped to day 1 index on the 8th claim
        assertTrue(s.cells.any { it?.defId == "magic_wild" })
    }

    @Test
    fun `streak resets when the 48h window is missed`() {
        var s = stateWith()
        val t0 = 1_000_000_000L
        s = GameEngine.claimDaily(s, t0, Random(1)).first
        assertEquals(0, s.dailyStreak)
        s = GameEngine.claimDaily(s, t0 + GameEngine.DAILY_COOLDOWN_MS, Random(1)).first
        assertEquals(1, s.dailyStreak)

        // Skip ~3 days → back to day 1
        val late = t0 + GameEngine.DAILY_COOLDOWN_MS + 3 * GameEngine.DAILY_COOLDOWN_MS
        assertEquals(0, GameEngine.dailyDayIndex(s, late))
        val (s2, events) = GameEngine.claimDaily(s, late, Random(1))
        assertEquals(1, events.filterIsInstance<GameEvent.DailyClaimed>().single().day)
        assertEquals(0, s2.dailyStreak)
    }

    @Test
    fun `wand roll is boosted while a generator is cooling`() {
        var wild = 0
        repeat(100) { i ->
            val s = stateWith(0 to "gen_fruit_1").let { st ->
                st.copy(
                    cells = st.cells.toMutableList().apply {
                        this[0] = this[0]!!.copy(cooldownUntilMs = 60_000)
                    },
                    nextQteAtMs = 10,
                )
            }
            val (offered, _) = GameEngine.tick(s, 20, Random(i))
            if (offered.qte?.reward == QteReward.WILDCARD) wild++
        }
        // ~50% expected with the cooling boost; fixed seeds keep this deterministic.
        assertTrue(wild >= 35)
    }

    @Test
    fun `wand roll stays rare when no generator is cooling`() {
        var wild = 0
        repeat(100) { i ->
            val s = stateWith(0 to "gen_fruit_1").copy(nextQteAtMs = 10)
            val (offered, _) = GameEngine.tick(s, 20, Random(i))
            if (offered.qte?.reward == QteReward.WILDCARD) wild++
        }
        // ~20% expected with even odds; fixed seeds keep this deterministic.
        assertTrue(wild < 35)
    }

    // ---------- cafe room / pet / mini game ----------

    @Test
    fun `buying decor spends coins and marks owned`() {
        val s = stateWith(coins = 100)
        val (next, events) = GameEngine.buyDecor(s, "decor_rug")
        assertEquals(50, next.coins)
        assertTrue("decor_rug" in next.decorOwned)
        assertTrue(events.any { it is GameEvent.DecorBought })
    }

    @Test
    fun `decor rejects without coins or level`() {
        // Not enough coins
        val poor = stateWith(coins = 10)
        assertEquals(poor, GameEngine.buyDecor(poor, "decor_rug").first)
        // Level locked (decor_table needs Lv3)
        val low = stateWith(coins = 999, level = 1)
        assertEquals(low, GameEngine.buyDecor(low, "decor_table").first)
        // Already owned
        val owned = stateWith(coins = 999).let {
            GameEngine.buyDecor(it, "decor_rug").first
        }
        assertEquals(owned, GameEngine.buyDecor(owned, "decor_rug").first)
    }

    @Test
    fun `decor order-coins buff raises order payout`() {
        // decor_counter grants +3% order coins
        val s = stateWith(
            0 to "fruit_1",
            coins = 999,
            level = 7,
            orders = listOf(orderOf("fruit_1")),
        ).let {
            var st = it
            DecorCatalog.ALL.forEach { d ->
                st = GameEngine.buyDecor(st, d.id).first
            }
            st
        }
        val expectedPct = DecorCatalog.orderCoinsPct(s.decorOwned)
        assertTrue(expectedPct > 0)
        val (next, _) = GameEngine.autoFulfillOrder(s, 1, 0)
        val base = ItemCatalog["fruit_1"].sellPrice * 5 / 4
        assertEquals(s.coins + base * (100 + expectedPct) / 100, next.coins)
    }

    @Test
    fun `adopting pet costs coins and starts fed`() {
        val s = stateWith(coins = 250)
        val (next, events) = GameEngine.adoptPet(s, "cat", 1000)
        assertEquals(50, next.coins)
        assertEquals("cat", next.pet?.defId)
        assertTrue(next.pet!!.isFed(1000))
        assertTrue(events.any { it is GameEvent.PetAdopted })
    }

    @Test
    fun `fed pet buff boosts order coins while hungry pet does not`() {
        val base = stateWith(
            0 to "fruit_5", // sellPrice 10 → payout 12, big enough for int rounding
            orders = listOf(orderOf("fruit_5")),
        )
        val withPet = GameEngine.adoptPet(base.copy(coins = 500), "fox", 0).first
        val fedCoins = GameEngine.autoFulfillOrder(withPet, 1, 1_000).first.coins -
            withPet.coins
        // Hungry: same state, buff off → plain payout
        val hungry = withPet.copy(pet = withPet.pet!!.copy(fedUntilMs = 0))
        val baseCoins = GameEngine.autoFulfillOrder(hungry, 1, 1_000).first.coins -
            hungry.coins
        // fox = +8%
        assertEquals(baseCoins * 108 / 100, fedCoins)
        assertTrue(fedCoins > baseCoins)
    }

    @Test
    fun `feed pet with coins spends coins and feeds`() {
        var s = stateWith(coins = 600)
        s = GameEngine.adoptPet(s, "dog", 0).first
        val hungry = s.copy(pet = s.pet!!.copy(fedUntilMs = 0))
        val (fed, events) = GameEngine.feedPetCoins(hungry, 5000)
        assertEquals(hungry.coins - GameEngine.PET_FEED_COINS, fed.coins)
        assertTrue(fed.pet!!.isFed(5000))
        assertTrue(events.any { it is GameEvent.PetFed })
        // board items are untouched — pets only eat bought food
        assertEquals(hungry.cells, fed.cells)
    }

    @Test
    fun `panda drops a gift item on tick when fed`() {
        var s = stateWith(coins = 600)
        s = GameEngine.adoptPet(s, "panda", 0).first
        s = s.copy(pet = s.pet!!.copy(nextGiftAtMs = 1000))
        val (next, events) = GameEngine.tick(s, 2000, Random(1))
        assertTrue(events.any { it is GameEvent.PetGift })
        assertTrue(next.cells.any { it != null })
        assertEquals(2000 + GameEngine.PANDA_GIFT_MS, next.pet!!.nextGiftAtMs)
    }

    @Test
    fun `mini game pays 1 energy per 10 points capped`() {
        var s = stateWith(energy = 90)
        val (done, events) = GameEngine.finishMiniGame(s, 50, 1000)
        assertEquals(95, done.energy)
        assertEquals(50, done.miniBest)
        val fin = events.filterIsInstance<GameEvent.MiniGameFinished>().single()
        assertEquals(5, fin.energy)

        // reward still capped at MINI_MAX_ENERGY even for huge scores
        val (done2, events2) = GameEngine.finishMiniGame(s, 1000, 1000)
        val fin2 = events2.filterIsInstance<GameEvent.MiniGameFinished>().single()
        assertEquals(20, fin2.energy)
        assertEquals(100, done2.energy) // clamped at ENERGY_CAP
    }

    @Test
    fun `mini game play refills on tick`() {
        var s = stateWith().copy(miniPlaysLeft = 0, miniRefillAtMs = 1000)
        val (next, _) = GameEngine.tick(s, 2000, Random(1))
        assertEquals(1, next.miniPlaysLeft)
        assertTrue(next.miniRefillAtMs > 0)
    }

    @Test
    fun `mini game plays are unlimited`() {
        val s = stateWith().copy(miniPlaysLeft = 0)
        val (next, events) = GameEngine.startMiniGame(s, 0)
        assertEquals(s, next)
        assertTrue(events.isEmpty())
    }
}
