package com.bepngon.merge

import com.bepngon.merge.game.BoardItem
import com.bepngon.merge.game.Customers
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.GameEvent
import com.bepngon.merge.game.GameState
import com.bepngon.merge.game.ItemSource
import com.bepngon.merge.game.Order
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
): GameState {
    val cells = MutableList<BoardItem?>(GameEngine.CELLS) { null }
    var uid = 1L
    placements.forEach { (i, defId) ->
        cells[i] = GameEngine.newItem(uid++, defId)
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
    fun `selling item pays coins and frees cell`() {
        val s = stateWith(4 to "fruit_3")
        val (next, events) = GameEngine.sell(s, 4)
        assertNull(next.cells[4])
        assertEquals(5, next.coins)
        assertTrue(events.single() is GameEvent.Sold)
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
    fun `selling from inventory pays coins`() {
        val s = stateWith(inventory = listOf("fruit_3", null, null, null, null, null, null, null))
        val (next, _) = GameEngine.sellFromInventory(s, 0)
        assertNull(next.inventory[0])
        assertEquals(5, next.coins)
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

        val fullOrders = List(3) { i -> orderOf("fruit_1").copy(id = i + 1L) }
        val s2 = stateWith(0 to "gen_fruit_1", orders = fullOrders, nextOrderAtMs = 10)
        val (reschedule, _) = GameEngine.maybeSpawnOrder(s2, 100, Random(1))
        assertEquals(3, reschedule.orders.size)
        assertEquals(10_100L, reschedule.nextOrderAtMs)
    }
}
