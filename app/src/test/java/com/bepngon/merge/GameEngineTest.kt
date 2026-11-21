package com.bepngon.merge

import com.bepngon.merge.game.BoardItem
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.GameEvent
import com.bepngon.merge.game.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private fun stateWith(vararg placements: Pair<Int, String>, energy: Int = 10): GameState {
    val cells = MutableList<BoardItem?>(GameEngine.CELLS) { null }
    var uid = 1L
    placements.forEach { (i, defId) -> cells[i] = BoardItem(uid++, defId) }
    return GameState(cells, energy, coins = 0, score = 0, nextUid = uid)
}

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
    fun `generator tap consumes energy and spawns item`() {
        val s = stateWith(0 to "gen_fruit_1")
        val (next, events) = GameEngine.tapGenerator(s, 0, Random(42))
        assertEquals(9, next.energy)
        assertEquals(1, next.cells.count { it != null && it.defId.startsWith("fruit_") })
        assertTrue(events.single() is GameEvent.Spawned)
    }

    @Test
    fun `generator tap with no energy emits NoEnergy`() {
        val s = stateWith(0 to "gen_fruit_1", energy = 0)
        val (_, events) = GameEngine.tapGenerator(s, 0)
        assertEquals(listOf(GameEvent.NoEnergy), events)
    }

    @Test
    fun `generator tap with full board emits BoardFull`() {
        val s = stateWith(*Array(GameEngine.CELLS) { it to "fruit_1" }.also {
            it[0] = 0 to "gen_fruit_1"
        })
        val (_, events) = GameEngine.tapGenerator(s, 0)
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
}
