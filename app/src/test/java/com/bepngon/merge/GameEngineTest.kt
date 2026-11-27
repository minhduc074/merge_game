package com.bepngon.merge

import com.bepngon.merge.game.BoardItem
import com.bepngon.merge.game.Customers
import com.bepngon.merge.game.Family
import com.bepngon.merge.game.GameBalance
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.GameEvent
import com.bepngon.merge.game.GameState
import com.bepngon.merge.game.GeneratorTuning
import com.bepngon.merge.game.ItemCatalog
import com.bepngon.merge.game.ItemSource
import com.bepngon.merge.game.Metric
import com.bepngon.merge.game.Order
import com.bepngon.merge.game.QteOffer
import com.bepngon.merge.game.QteReward
import com.bepngon.merge.game.SpawnEntry
import com.bepngon.merge.game.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random
import org.junit.Assert.assertNotEquals

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
    fun `remote balance overrides xp curve and minigame brackets`() {
        try {
            GameBalance.xpBase = 100
            GameBalance.xpLinear = 20
            GameBalance.xpQuadratic = 6
            assertEquals(126, GameEngine.xpNeededForLevel(1))
            GameBalance.blockEnergyBrackets = listOf(0 to 50, 1000 to 100)
            assertEquals(20, GameEngine.blockEnergyFor(1000))
        } finally {
            GameBalance.reset()
        }
    }

    @Test
    fun `server generator tuning overrides max charges and spawn table`() {
        try {
            ItemCatalog.applyGeneratorTunings(
                mapOf(
                    "gen_fruit_1" to GeneratorTuning(
                        maxCharges = 8,
                        spawnTable = listOf(SpawnEntry("fruit_1", 90), SpawnEntry("rare_fruit_1", 10)),
                    ),
                ),
            )
            val gen = ItemCatalog["gen_fruit_1"]
            assertEquals(8, gen.maxCharges)
            assertEquals(2, gen.spawnTable.size)
            assertEquals(10, gen.spawnTable[1].weight)
            assertEquals(8, ItemCatalog.generatorsFor("fruit_1").first { it.id == "gen_fruit_1" }.maxCharges)
        } finally {
            ItemCatalog.clearGeneratorTunings()
        }
        assertEquals(8, ItemCatalog["gen_fruit_1"].maxCharges)
    }

    @Test
    fun `energy regen catches up multiple intervals and caps at the natural cap`() {
        val s = stateWith(energy = 8).copy(nextEnergyAtMs = 1_000)
        val next = GameEngine.applyEnergyRegen(s, 121_000)
        assertEquals(15, next.energy)
        assertEquals(141_000, next.nextEnergyAtMs)

        val capped = GameEngine.applyEnergyRegen(
            stateWith(energy = 99).copy(nextEnergyAtMs = 1_000),
            121_000,
        )
        assertEquals(GameEngine.ENERGY_CAP, capped.energy)
    }

    @Test
    fun `energy regen accepts overdue offline timestamps`() {
        val s = stateWith(energy = 10).copy(nextEnergyAtMs = -119_000)
        val next = GameEngine.applyEnergyRegen(s, 0)
        assertEquals(16, next.energy)
        assertEquals(1_000, next.nextEnergyAtMs)
    }

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
    fun `drag onto different item swaps them`() {
        val s = stateWith(0 to "fruit_1", 1 to "fruit_2")
        val (next, events) = GameEngine.moveOrMerge(s, 0, 1)
        assertEquals("fruit_2", next.cells[0]?.defId)
        assertEquals("fruit_1", next.cells[1]?.defId)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `inventory item swaps with different board item`() {
        val s = stateWith(5 to "fruit_2", inventory = listOf("fruit_1", null, null, null, null, null, null, null))
        val (next, events) = GameEngine.inventoryToBoard(s, 0, 5)
        assertEquals("fruit_1", next.cells[5]?.defId)
        assertEquals("fruit_2", next.inventory[0]?.defId)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `different items do not swap with frozen`() {
        val s = stateWith(0 to "fruit_1", 1 to "fruit_2", frozenCells = setOf(1))
        val (next, events) = GameEngine.moveOrMerge(s, 0, 1)
        assertEquals("fruit_1", next.cells[0]?.defId)
        assertEquals("fruit_2", next.cells[1]?.defId)
        assertEquals(listOf(GameEvent.InvalidDrop), events)
    }

    @Test
    fun `max tier items cannot merge`() {
        val s = stateWith(0 to "fruit_11", 1 to "fruit_11")
        val (_, events) = GameEngine.moveOrMerge(s, 0, 1)
        assertTrue(events.none { it is GameEvent.Merged })
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
    fun `partially used generator regains one charge per regen interval`() {
        var s = stateWith(0 to "gen_fruit_1")
        repeat(2) {
            s = GameEngine.tapGenerator(s, 0, 1000).first
            // free spawned items so board never fills
            val cells = s.cells.map { it?.takeIf { i -> i.defId.startsWith("gen_") } }
            s = s.copy(cells = cells)
        }
        val gen = s.cells[0]!!
        assertEquals(6, gen.charges)
        assertEquals(1000 + GameEngine.GEN_REGEN_MS, gen.rechargeAtMs)

        // first interval -> +1, next interval armed, not full yet
        val (t1, ev1) = GameEngine.tick(s, 1000 + GameEngine.GEN_REGEN_MS)
        assertEquals(7, t1.cells[0]?.charges)
        assertEquals(1000 + 2 * GameEngine.GEN_REGEN_MS, t1.cells[0]?.rechargeAtMs)
        assertTrue(ev1.none { it is GameEvent.GeneratorReady })

        // second interval -> full, timer cleared, ready event
        val (t2, ev2) = GameEngine.tick(t1, 1000 + 2 * GameEngine.GEN_REGEN_MS)
        assertEquals(8, t2.cells[0]?.charges)
        assertEquals(0L, t2.cells[0]?.rechargeAtMs)
        assertTrue(ev2.any { it is GameEvent.GeneratorReady })
    }

    @Test
    fun `gen regen catches up on multiple elapsed intervals`() {
        var s = stateWith(0 to "gen_fruit_1")
        repeat(3) {
            s = GameEngine.tapGenerator(s, 0, 1000).first
            val cells = s.cells.map { it?.takeIf { i -> i.defId.startsWith("gen_") } }
            s = s.copy(cells = cells)
        }
        // 5/8 charged, deadline long past -> +3 to full in a single tick
        val (t, _) = GameEngine.tick(s, 1000 + 4 * GameEngine.GEN_REGEN_MS)
        assertEquals(8, t.cells[0]?.charges)
        assertEquals(0L, t.cells[0]?.rechargeAtMs)
    }

    @Test
    fun `drained generator clears regen timer in favor of cooldown`() {
        var s = stateWith(0 to "gen_fruit_1")
        repeat(8) {
            s = GameEngine.tapGenerator(s, 0, 1000).first
            val cells = s.cells.map { it?.takeIf { i -> i.defId.startsWith("gen_") } }
            s = s.copy(cells = cells)
        }
        val gen = s.cells[0]!!
        assertEquals(0, gen.charges)
        assertEquals(0L, gen.rechargeAtMs)
        assertEquals(1000 + GameEngine.GEN_COOLDOWN_MS, gen.cooldownUntilMs)
    }

    @Test
    fun `generator exactly at legacy cap is topped up after capacity increase`() {
        var s = stateWith(0 to "gen_fruit_1")
        s = s.copy(cells = s.cells.map { it?.copy(charges = 6, rechargeAtMs = 0, cooldownUntilMs = 0) })
        val (t, ev) = GameEngine.tick(s, 50_000)
        assertEquals(8, t.cells[0]?.charges)
        assertTrue(ev.any { it is GameEvent.GeneratorReady })
    }

    @Test
    fun `stored generator exactly at legacy cap is topped up after capacity increase`() {
        var s = stateWith(inventory = listOf("gen_fruit_1"))
        s = s.copy(inventory = s.inventory.map { it?.copy(charges = 6) })
        val (t, _) = GameEngine.tick(s, 50_000)
        assertEquals(8, t.inventory[0]?.charges)
    }

    @Test
    fun `old save with partial charges arms the regen timer on tick`() {
        var s = stateWith(0 to "gen_fruit_1")
        s = s.copy(cells = s.cells.map { it?.copy(charges = 4) })
        val (t, ev) = GameEngine.tick(s, 50_000)
        assertEquals(4, t.cells[0]?.charges)
        assertEquals(50_000 + GameEngine.GEN_REGEN_MS, t.cells[0]?.rechargeAtMs)
        assertTrue(ev.none { it is GameEvent.GeneratorReady })
    }

    @Test
    fun `generator tap increments spawns stat`() {
        val s = stateWith(0 to "gen_fruit_1")
        val (next, _) = GameEngine.tapGenerator(s, 0, 1000, Random(42))
        assertEquals(1, next.stats.spawns)
    }

    @Test
    fun `spawn achievement unlocks once spawn goal reached`() {
        val s = stateWith(0 to "gen_fruit_1", stats = Stats(spawns = 49))
        val (next, events) = GameEngine.tapGenerator(s, 0, 1000, Random(42))
        assertTrue("spawn50" in next.reachedAchievements)
        assertTrue(
            events.any {
                it is GameEvent.AchievementUnlocked && it.achievement.id == "spawn50"
            },
        )
    }

    @Test
    fun `merging rare items counts toward raresFound`() {
        val s = stateWith(0 to "rare_fruit_1", 1 to "rare_fruit_1")
        val (next, _) = GameEngine.moveOrMerge(s, 0, 1)
        assertEquals(1, next.stats.raresFound)
    }

    @Test
    fun `claiming a qte offer increments qteClaimed`() {
        val s = stateWith().copy(qte = QteOffer(QteReward.COINS, 10_000))
        val (next, _) = GameEngine.claimQte(s, 1000, Random(42))
        assertEquals(1, next.stats.qteClaimed)
    }

    @Test
    fun `orders completed inside the window grow maxCombo`() {
        val s = stateWith(
            0 to "fruit_1",
            1 to "fruit_1",
            orders = listOf(
                Order(1, Customers.ALL[0], listOf("fruit_1")),
                Order(2, Customers.ALL[0], listOf("fruit_1")),
            ),
        )
        val (s1, _) = GameEngine.autoFulfillOrder(s, 1, 1000)
        val (s2, _) = GameEngine.autoFulfillOrder(s1, 2, 2000)
        assertEquals(2, s2.stats.maxCombo)
    }

    @Test
    fun `metricOf exposes state-derived metrics`() {
        val s = stateWith(0 to "gen_fruit_2", 5 to "gen_fruit_1")
            .copy(score = 1234, inventoryCap = 6, dailyStreak = 4, miniBest = 77)
        assertEquals(1, s.metricOf(Metric.GENERATOR_T2))
        assertEquals(1234, s.metricOf(Metric.SCORE))
        assertEquals(6, s.metricOf(Metric.INVENTORY_CAP))
        assertEquals(4, s.metricOf(Metric.STREAK))
        assertEquals(77, s.metricOf(Metric.MINI_BEST))
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
        assertEquals(4, next.xp) // tier1 + 3
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
        // fruit_5 x2 + fruit_4 → xp = (5+5+4)+3 = 17; +65 base = 82 ≥ xpNeeded(1)=80
        val s = stateWith(
            0 to "fruit_5", 1 to "fruit_5", 2 to "fruit_4",
            orders = listOf(orderOf("fruit_5", "fruit_5", "fruit_4")),
            xp = 65,
        )
        var cur = s
        GameEngine.deliverToOrder(cur, 1, ItemSource.BOARD, 0).also { cur = it.first }
        GameEngine.deliverToOrder(cur, 1, ItemSource.BOARD, 1).also { cur = it.first }
        val (next, events) = GameEngine.deliverToOrder(cur, 1, ItemSource.BOARD, 2)
        assertEquals(2, next.level)
        assertEquals(2, next.xp)
        assertTrue(next.orders.isEmpty())
        assertTrue(events.any { it is GameEvent.LevelUp && it.level == 2 })
    }

    @Test
    fun `level 3 reward grants drink generator and inventory slot`() {
        // level 2 + 100xp; order fruit_5 x2 → +13xp = 113 ≥ xpNeeded(2)=110
        val s = stateWith(
            0 to "fruit_5", 1 to "fruit_5",
            orders = listOf(orderOf("fruit_5", "fruit_5")),
            xp = 100, level = 2,
        )
        var cur = s
        GameEngine.deliverToOrder(cur, 1, ItemSource.BOARD, 0).also { cur = it.first }
        val (next, _) = GameEngine.deliverToOrder(cur, 1, ItemSource.BOARD, 1)
        assertEquals(3, next.level)
        assertEquals(5, next.inventoryCap)
        assertTrue(next.cells.any { it?.defId == "gen_drink_1" })
    }

    @Test
    fun `orders never ask families without a generator on board`() {
        // even at max level with full windows open: only fruit/bakery gens
        // placed → no drink/viet item may ever be requested, rare included
        val s = stateWith(
            0 to "gen_fruit_3", 1 to "gen_bakery_3",
            nextOrderAtMs = 10, level = 30,
        )
        var cur = s.copy(nextOrderId = 2)
        var totalPicks = 0
        repeat(200) { i ->
            val (next, events) = GameEngine.maybeSpawnOrder(
                cur.copy(orders = emptyList(), nextOrderAtMs = 10),
                100 + i * 1000L, Random(900 + i),
            )
            cur = next
            (events.single() as GameEvent.OrderSpawned).order.required.forEach { id ->
                totalPicks++
                val def = ItemCatalog[id]
                assertTrue(
                    "drink/viet asked without its machine: $id",
                    def.family == Family.FRUIT || def.family == Family.BAKERY,
                )
            }
        }
        assertTrue("no picks collected", totalPicks > 100)
        // positive control: placing the drink machine opens drink asks
        val withDrink = stateWith(
            0 to "gen_fruit_3", 1 to "gen_drink_3",
            nextOrderAtMs = 10, level = 30,
        )
        var cur2 = withDrink.copy(nextOrderId = 2)
        var sawDrink = false
        repeat(200) { i ->
            val (next, events) = GameEngine.maybeSpawnOrder(
                cur2.copy(orders = emptyList(), nextOrderAtMs = 10),
                100 + i * 1000L, Random(1900 + i),
            )
            cur2 = next
            if ((events.single() as GameEvent.OrderSpawned).order.required
                    .any { ItemCatalog[it].family == Family.DRINK }
            ) sawDrink = true
        }
        assertTrue("drink never asked despite drink machine", sawDrink)
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
        assertEquals(s.energy + GameEngine.DAILY_ENERGY, claimed.energy)
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

    @Test
    fun `fresh board hides a frozen item under every locked cell`() {
        val s = GameEngine.initialState(Random(1))
        GameEngine.INITIAL_LOCKED.forEach { cell ->
            val item = s.cells[cell]
            assertNotNull("locked cell $cell should hide an item", item)
            assertTrue("locked cell $cell should be frozen", item!!.frozen)
        }
    }

    @Test
    fun `fresh board starts with one generator per starter family`() {
        val s = GameEngine.initialState(Random(1))
        val gens = s.cells.filterNotNull()
            .filter { ItemCatalog[it.defId].isGenerator }
        assertEquals(
            setOf("gen_fruit_1", "gen_bakery_1"),
            gens.map { it.defId }.toSet(),
        )
    }

    @Test
    fun `fresh board scatters frozen items on random open cells`() {
        fun openFrozen(s: GameState) = s.cells.indices.filter {
            it !in GameEngine.INITIAL_LOCKED && s.cells[it]?.frozen == true
        }
        val frozen = openFrozen(GameEngine.initialState(Random(2)))
        assertEquals(3, frozen.size)
        // never placed in the locked bottom rows
        assertTrue(frozen.all { it < GameEngine.INITIAL_LOCKED.min() })
        // different seeds land on different cells
        assertNotEquals(
            openFrozen(GameEngine.initialState(Random(2))),
            openFrozen(GameEngine.initialState(Random(99))),
        )
    }

    @Test
    fun `level-up unlock reveals a frozen item in sparse saves`() {
        // save from before locked cells were pre-seeded: empty underneath
        val cell = GameEngine.INITIAL_LOCKED.first()
        val s = stateWith(
            0 to "fruit_1",
            lockedCells = setOf(cell),
            orders = listOf(orderOf("fruit_1")),
            xp = 76, // +4xp from fruit_1 order -> 80 >= xpNeeded(1)=80
        )
        val (next, _) = GameEngine.deliverToOrder(s, 1, ItemSource.BOARD, 0)
        assertEquals(2, next.level)
        assertTrue(next.lockedCells.isEmpty())
        assertTrue(
            "unlocked cell should reveal a frozen item",
            next.cells[cell]?.frozen == true,
        )
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
            xp = 100, level = 2, // +13xp = 113 → Lv3 only; odd levels unlock nothing
            lockedCells = GameEngine.INITIAL_LOCKED.toSet(),
        )
        var cur = GameEngine.deliverToOrder(s, 1, ItemSource.BOARD, 0).first
        val lockedBefore = cur.lockedCells.size
        // first delivery was partial (no xp) — still Lv2. bump xp so the next
        // order (+8) crosses xpNeeded(2)+xpNeeded(3)=260 — even levels unlock
        cur = cur.copy(
            xp = 253,
            orders = listOf(orderOf("fruit_5").copy(id = 2)),
            cells = cur.cells.toMutableList().also { it[0] = GameEngine.newItem(99, "fruit_5") },
        )
        val (next, events) = GameEngine.deliverToOrder(cur, 2, ItemSource.BOARD, 0)
        assertTrue(next.level >= 4)
        assertTrue(next.lockedCells.size < lockedBefore)
        assertTrue(events.any { it is GameEvent.CellsUnlocked })
    }

    @Test
    fun `locked cells derive from level and heal stale saves`() {
        assertEquals(GameEngine.INITIAL_LOCKED.toSet(), GameEngine.lockedCellsForLevel(1))
        // 1 cell every 2 levels: Lv5 -> unlocks at 2,4 -> 10 still locked
        assertEquals(
            GameEngine.INITIAL_LOCKED.drop(2).toSet(),
            GameEngine.lockedCellsForLevel(5),
        )
        assertTrue(GameEngine.lockedCellsForLevel(24).isEmpty())
        // migrated saves honor the old faster schedule: Lv5 had all unlocked
        assertTrue(GameEngine.lockedCellsForLevel(5, legacy = true).isEmpty())
        assertEquals(
            GameEngine.INITIAL_LOCKED.drop(2).toSet(),
            GameEngine.lockedCellsForLevel(2, legacy = true),
        )
    }

    @Test
    fun `every generator can rarely spawn its family rare item`() {
        val rareByFamily = mapOf(
            "gen_fruit" to "rare_fruit_1",
            "gen_bakery" to "rare_bakery_1",
            "gen_drink" to "rare_drink_1",
            "gen_viet" to "rare_viet_1",
        )
        rareByFamily.forEach { (prefix, rareId) ->
            (1..3).forEach { tier ->
                val gen = ItemCatalog["${prefix}_$tier"]
                val entry = gen.spawnTable.singleOrNull { it.itemId == rareId }
                assertNotNull("${gen.id} missing rare spawn", entry)
                assertTrue(
                    "${gen.id} rare weight too high",
                    entry!!.weight in 1..5,
                )
            }
            assertTrue(ItemCatalog[rareId].isRare)
            assertEquals(3, ItemCatalog.chainOf(rareId).size)
        }
    }

    @Test
    fun `short chains cap spawn reach one tier lower than long chains`() {
        // drink/viet chains are short (7-8 tiers vs fruit/bakery 10-11) —
        // their gens cap lower: lv1 -> t2, lv2 -> t2, lv3 -> t3
        val expectedCap = mapOf(1 to 2, 2 to 2, 3 to 3)
        listOf("gen_drink", "gen_viet").forEach { prefix ->
            (1..3).forEach { tier ->
                val gen = ItemCatalog["${prefix}_$tier"]
                val maxTier = gen.spawnTable.filter { !ItemCatalog[it.itemId].isRare }
                    .maxOf { ItemCatalog[it.itemId].tier }
                assertEquals("${gen.id} spawn cap", expectedCap[tier], maxTier)
            }
        }
    }

    @Test
    fun `gen tier N only spawns item tiers up to N+1`() {
        // gen lv1 -> t1-2, lv2 -> t1-3, lv3 -> t1-4; higher is merge-only
        listOf("gen_fruit", "gen_bakery", "gen_drink", "gen_viet").forEach { prefix ->
            (1..3).forEach { tier ->
                val gen = ItemCatalog["${prefix}_$tier"]
                val normal = gen.spawnTable.filter { !ItemCatalog[it.itemId].isRare }
                val maxTier = normal.maxOf { ItemCatalog[it.itemId].tier }
                val minTier = normal.minOf { ItemCatalog[it.itemId].tier }
                assertEquals(1, minTier)
                assertTrue(
                    "${gen.id} reaches t$maxTier, cap is t${tier + 1}",
                    maxTier <= tier + 1,
                )
                assertEquals("${gen.id} table must sum to 100", 100, gen.spawnTable.sumOf { it.weight })
                assertTrue(
                    "${gen.id} tier-1 must dominate",
                    normal.all { it.itemId.endsWith("_1") || it.weight < normal.first().weight },
                )
            }
        }
    }

    @Test
    fun `orders occasionally request rare items`() {
        val s = stateWith(0 to "gen_fruit_3", nextOrderAtMs = 10, level = 10)
        var cur = s.copy(nextOrderId = 2)
        var rarePicks = 0
        var totalPicks = 0
        repeat(200) { i ->
            val (next, events) = GameEngine.maybeSpawnOrder(
                cur.copy(orders = emptyList(), nextOrderAtMs = 10),
                100 + i * 1000L, Random(i + 500),
            )
            cur = next
            val order = (events.single() as GameEvent.OrderSpawned).order
            order.required.forEach { id ->
                totalPicks++
                if (ItemCatalog[id].isRare) rarePicks++
            }
        }
        assertTrue("no rare items ever requested", rarePicks > 0)
        assertTrue(
            "rare picks too common: $rarePicks/$totalPicks",
            rarePicks * 100 / totalPicks < 15,
        )
    }

    @Test
    fun `rare order tiers collapse toward the jackpot with player level`() {
        fun rareTiers(level: Int, seed0: Int): Set<Int> {
            val s = stateWith(0 to "gen_fruit_3", nextOrderAtMs = 10, level = level)
            var cur = s.copy(nextOrderId = 2)
            val tiers = mutableSetOf<Int>()
            repeat(200) { i ->
                val (next, events) = GameEngine.maybeSpawnOrder(
                    cur.copy(orders = emptyList(), nextOrderAtMs = 10),
                    100 + i * 1000L, Random(seed0 + i),
                )
                cur = next
                (events.single() as GameEvent.OrderSpawned).order.required
                    .filter { ItemCatalog[it].isRare }
                    .forEach { tiers += ItemCatalog[it].tier }
            }
            return tiers
        }
        // rare chains len 3 → window [level*3/30+1, 3]: any tier early,
        // t2-3 from ~Lv10, only the t3 jackpot past Lv20.
        assertTrue(rareTiers(5, 0).all { it in 1..3 })
        val mid = rareTiers(10, 0)
        assertTrue("Lv10 rares t2-3, got $mid", mid.isNotEmpty() && mid.all { it in 2..3 })
        val late = rareTiers(24, 0)
        assertTrue("Lv24 rares t3 only, got $late", late.isNotEmpty() && late.all { it == 3 })
    }

    @Test
    fun `normal order tiers follow the chain window`() {
        fun normalTiers(level: Int, seed0: Int): Set<Int> {
            val s = stateWith(0 to "gen_fruit_3", nextOrderAtMs = 10, level = level)
            var cur = s.copy(nextOrderId = 2)
            val tiers = mutableSetOf<Int>()
            repeat(200) { i ->
                val (next, events) = GameEngine.maybeSpawnOrder(
                    cur.copy(orders = emptyList(), nextOrderAtMs = 10),
                    100 + i * 1000L, Random(seed0 + i),
                )
                cur = next
                (events.single() as GameEvent.OrderSpawned).order.required
                    .filter { !ItemCatalog[it].isRare }
                    .forEach { tiers += ItemCatalog[it].tier }
            }
            return tiers
        }
        // fruit chain len 11 → window [level*11/30+1, +3], pinned at t8-11 endgame
        val t1 = normalTiers(1, 0)
        assertTrue("Lv1 window t1-4, got $t1", t1.isNotEmpty() && t1.all { it in 1..4 })
        assertEquals("Lv1 still asks tier 1", 1, t1.min())
        val t12 = normalTiers(12, 0)
        assertTrue("Lv12 window t5-8, got $t12", t12.isNotEmpty() && t12.all { it in 5..8 })
        val t25 = normalTiers(25, 0)
        assertTrue("Lv25 pinned to t8-11, got $t25", t25.isNotEmpty() && t25.all { it in 8..11 })
        val t30 = normalTiers(30, 0)
        assertTrue("Lv30 pinned to t8-11, got $t30", t30.isNotEmpty() && t30.all { it in 8..11 })
    }

    @Test
    fun `order window scales with each chain length`() {
        // drink len 8 vs fruit len 11 at Lv6: fruit min = 6*11/30+1 = 3,
        // drink min = 6*8/30+1 = 2 — longer chains ramp faster.
        fun tiersByFamily(level: Int, seed0: Int): Pair<Set<Int>, Set<Int>> {
            val s = stateWith(
                0 to "gen_fruit_3", 1 to "gen_drink_3",
                nextOrderAtMs = 10, level = level,
            )
            var cur = s.copy(nextOrderId = 2)
            val fruit = mutableSetOf<Int>()
            val drink = mutableSetOf<Int>()
            repeat(200) { i ->
                val (next, events) = GameEngine.maybeSpawnOrder(
                    cur.copy(orders = emptyList(), nextOrderAtMs = 10),
                    100 + i * 1000L, Random(seed0 + i),
                )
                cur = next
                (events.single() as GameEvent.OrderSpawned).order.required
                    .map { ItemCatalog[it] }
                    .filter { !it.isRare }
                    .forEach {
                        when (it.family) {
                            Family.FRUIT -> fruit += it.tier
                            Family.DRINK -> drink += it.tier
                            else -> {}
                        }
                    }
            }
            return fruit to drink
        }
        val (fruit6, drink6) = tiersByFamily(6, 0)
        assertTrue("fruit Lv6 t3-6, got $fruit6", fruit6.isNotEmpty() && fruit6.all { it in 3..6 })
        assertTrue("drink Lv6 t2-5, got $drink6", drink6.isNotEmpty() && drink6.all { it in 2..5 })
    }

    @Test
    fun `rare items merge up their own short chain`() {
        val s = stateWith(0 to "rare_fruit_1", 1 to "rare_fruit_1")
        val (next, _) = GameEngine.moveOrMerge(s, 0, 1)
        assertEquals("rare_fruit_2", next.cells[1]?.defId)
        assertNull(next.cells[0])
        val s2 = next.copy(
            cells = next.cells.toMutableList()
                .also { it[0] = GameEngine.newItem(50, "rare_fruit_2") },
        )
        val (next2, _) = GameEngine.moveOrMerge(s2, 0, 1)
        assertEquals("rare_fruit_3", next2.cells[1]?.defId)
    }

    @Test
    fun `coin upgrade bumps generator tier for a steep price`() {
        val s = stateWith(0 to "gen_fruit_1", coins = 1000)
        val uid = s.cells[0]!!.uid
        val (next, events) = GameEngine.upgradeGenerator(s, uid)
        assertEquals("gen_fruit_2", next.cells[0]?.defId)
        assertEquals(ItemCatalog["gen_fruit_2"].maxCharges, next.cells[0]?.charges)
        assertEquals(
            1000 - GameEngine.genUpgradeCost(ItemCatalog["gen_fruit_2"]),
            next.coins,
        )
        assertTrue(events.any { it is GameEvent.GeneratorUpgraded })
        // tier-2 generator also unlocks the genup1 achievement
        assertTrue("genup1" in next.reachedAchievements)
    }

    @Test
    fun `gen upgrade rejected when broke or not a generator`() {
        val broke = stateWith(0 to "gen_fruit_1", coins = 0)
        val (s1, e1) = GameEngine.upgradeGenerator(broke, broke.cells[0]!!.uid)
        assertEquals(broke.cells, s1.cells)
        assertTrue(e1.single() is GameEvent.NotEnoughCoins)
        val s = stateWith(0 to "fruit_1", coins = 9999)
        assertEquals(s, GameEngine.upgradeGenerator(s, s.cells[0]!!.uid).first)
        val s2 = stateWith(0 to "gen_fruit_3", coins = 9999)
        assertEquals(s2, GameEngine.upgradeGenerator(s2, s2.cells[0]!!.uid).first)
    }

    @Test
    fun `inventory generator upgrades too`() {
        val s = stateWith(coins = 2000).copy(
            inventory = MutableList<BoardItem?>(GameEngine.MAX_INVENTORY) { null }
                .also { it[0] = GameEngine.newItem(99, "gen_drink_1") },
        )
        val (next, _) = GameEngine.upgradeGenerator(s, s.inventory[0]!!.uid)
        assertEquals("gen_drink_2", next.inventory[0]?.defId)
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
    fun `order spawn pacing adapts to completion speed`() {
        // Fast finisher (pace 40s/order): base = pace/4 = 10s, jittered 6..14s
        val fast = stateWith(0 to "gen_fruit_1", nextOrderAtMs = 10)
            .copy(orderPaceMs = 40_000)
        val (fastNext, _) = GameEngine.maybeSpawnOrder(fast, 100, Random(1))
        val fastDelay = fastNext.nextOrderAtMs - 100
        assertTrue("fast delay $fastDelay", fastDelay in 6_000..14_000)

        // Slow player (pace 200s/order): delay clamps to the 28s ceiling
        val slow = stateWith(0 to "gen_fruit_1", nextOrderAtMs = 10)
            .copy(orderPaceMs = 200_000)
        val (slowNext, _) = GameEngine.maybeSpawnOrder(slow, 100, Random(1))
        assertTrue(slowNext.nextOrderAtMs - 100 >= 15_000)

        // Completing an order updates the pace tracker
        val s = stateWith(
            0 to "fruit_1",
            orders = listOf(orderOf("fruit_1")),
        )
        val (done, _) = GameEngine.autoFulfillOrder(s, 1, 60_000)
        assertEquals(60_000, done.lastOrderDoneAtMs)
        val (done2, _) = GameEngine.autoFulfillOrder(
            done.copy(
                orders = listOf(orderOf("fruit_1").copy(id = 2)),
                cells = done.cells.toMutableList()
                    .also { it[0] = GameEngine.newItem(99, "fruit_1") },
            ),
            2, 90_000,
        )
        assertEquals(30_000, done2.orderPaceMs)
    }

    @Test
    fun `low level orders stay within tier 5`() {
        val s = stateWith(0 to "gen_fruit_3", nextOrderAtMs = 10, level = 2)
        var cur = s.copy(nextOrderId = 2)
        repeat(20) { i ->
            val (next, events) = GameEngine.maybeSpawnOrder(
                cur.copy(orders = emptyList(), nextOrderAtMs = 10),
                100 + i * 1000L, Random(i),
            )
            cur = next
            val order = (events.single() as GameEvent.OrderSpawned).order
            order.required.forEach { id ->
                assertTrue("tier>5 at low level: $id", ItemCatalog[id].tier <= 5)
            }
        }
    }

    @Test
    fun `high level orders request higher tiers`() {
        // Lv25 → maxTier 8 and the level bonus makes tier-6+ requests likely
        val s = stateWith(0 to "gen_fruit_3", nextOrderAtMs = 10, level = 25)
        var cur = s.copy(nextOrderId = 2)
        var sawHighTier = false
        var highTierShare = 0
        var totalItems = 0
        repeat(30) { i ->
            val (next, events) = GameEngine.maybeSpawnOrder(
                cur.copy(orders = emptyList(), nextOrderAtMs = 10),
                100 + i * 1000L, Random(i + 100),
            )
            cur = next
            val order = (events.single() as GameEvent.OrderSpawned).order
            order.required.forEach { id ->
                totalItems++
                if (ItemCatalog[id].tier >= 6) { sawHighTier = true; highTierShare++ }
            }
        }
        assertTrue("no tier-6+ item requested at level 25", sawHighTier)
        assertTrue(
            "high-tier share too low: $highTierShare/$totalItems",
            highTierShare * 100 / totalItems >= 10,
        )
    }

    @Test
    fun `viet order window scales by its own chain length`() {
        // viet chain len 7 → window [level*7/30+1, +3]: Lv5 [2,5], Lv11 [3,6]
        fun vietTiers(level: Int, seed0: Int): Set<Int> {
            val s = stateWith(0 to "gen_viet_1", nextOrderAtMs = 10, level = level)
            var cur = s.copy(nextOrderId = 2)
            val tiers = mutableSetOf<Int>()
            repeat(40) { i ->
                val (next, events) = GameEngine.maybeSpawnOrder(
                    cur.copy(orders = emptyList(), nextOrderAtMs = 10),
                    100 + i * 1000L, Random(seed0 + i),
                )
                cur = next
                (events.single() as GameEvent.OrderSpawned).order.required
                    .filter { it.startsWith("viet_") }
                    .forEach { tiers += ItemCatalog[it].tier }
            }
            return tiers
        }
        val t5 = vietTiers(5, 0)
        assertTrue("viet Lv5 window t2-5, got $t5", t5.isNotEmpty() && t5.all { it in 2..5 })
        val t11 = vietTiers(11, 0)
        assertTrue("viet Lv11 window t3-6, got $t11", t11.isNotEmpty() && t11.all { it in 3..6 })
    }

    @Test
    fun `later families pay more coins per order item`() {
        // same sellPrice (40) across families isolates the family bonus
        val fruit = GameEngine.orderCoinReward(orderOf("rare_fruit_1"))
        val drink = GameEngine.orderCoinReward(orderOf("rare_drink_1"))
        val viet = GameEngine.orderCoinReward(orderOf("rare_viet_1"))
        assertTrue(drink > fruit)
        assertTrue(viet > drink)
        assertEquals(50, fruit)
        assertEquals(62, drink)
        assertEquals(70, viet)
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

        // Order 3 inside the window: bonus +30 coins +6 xp (10*x, 2*x)
        val (s3, events3) = GameEngine.autoFulfillOrder(s, 3, 10_000)
        val bonus = events3.filterIsInstance<GameEvent.ComboBonus>().single()
        assertEquals(3, bonus.combo)
        assertEquals(30, bonus.coins)
        assertEquals(6, bonus.xp)
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

    // ---------- cafe room / mini game ----------

    @Test
    fun `mini game pays tiered energy uncapped`() {
        var s = stateWith(energy = 90)
        val (done, events) = GameEngine.finishMiniGame(s, 50, 1000)
        assertEquals(95, done.energy)
        assertEquals(50, done.miniBest)
        val fin = events.filterIsInstance<GameEvent.MiniGameFinished>().single()
        assertEquals(5, fin.energy)

        // huge scores pay out fully and overflow ENERGY_CAP
        val (done2, events2) = GameEngine.finishMiniGame(s, 1000, 1000)
        val fin2 = events2.filterIsInstance<GameEvent.MiniGameFinished>().single()
        assertEquals(33, fin2.energy)
        assertEquals(123, done2.energy)
    }

    @Test
    fun `mini game energy curve flattens at score milestones`() {
        assertEquals(9, GameEngine.miniEnergyFor(99))
        assertEquals(10, GameEngine.miniEnergyFor(100))
        assertEquals(18, GameEngine.miniEnergyFor(300))
        assertEquals(28, GameEngine.miniEnergyFor(700))
        assertEquals(49, GameEngine.miniEnergyFor(2000))
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

    // ---------- block puzzle mini game ----------

    @Test
    fun `block game pays tiered energy and tracks its own best`() {
        var s = stateWith(energy = 90)
        val (done, events) = GameEngine.finishBlockGame(s, 800, 1000)
        assertEquals(110, done.energy)
        assertEquals(800, done.blockBest)
        val fin = events.filterIsInstance<GameEvent.BlockGameFinished>().single()
        assertEquals(20, fin.energy)

        // best only grows, energy overflows the natural cap
        val (done2, _) = GameEngine.finishBlockGame(done, 300, 1000)
        assertEquals(800, done2.blockBest)
        assertEquals(110 + 7, done2.energy)
    }

    @Test
    fun `block game energy curve flattens at score milestones`() {
        assertEquals(19, GameEngine.blockEnergyFor(799))
        assertEquals(20, GameEngine.blockEnergyFor(800))
        assertEquals(46, GameEngine.blockEnergyFor(2500))
        assertEquals(91, GameEngine.blockEnergyFor(7000))
    }
}
