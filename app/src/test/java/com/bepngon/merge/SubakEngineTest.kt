package com.bepngon.merge

import com.bepngon.merge.game.Subak
import com.bepngon.merge.game.SubakEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt
import kotlin.random.Random

private fun stepSeconds(e: SubakEngine, s: Float) {
    var acc = 0f
    while (acc < s * 1000f) {
        e.step(16.7f)
        acc += 16.7f
    }
}

class SubakEngineTest {

    @Test
    fun `drop uses current index, advances to next, counts drops`() {
        val e = SubakEngine(Random(1))
        val cur = e.currentFruitIndex
        val nxt = e.nextFruitIndex
        val f = e.dropFruit(0.3f)
        assertEquals(cur, f.fruitIndex)
        assertEquals(nxt, e.currentFruitIndex)
        assertEquals(1, e.dropCount)
        assertTrue(f.body.position.y <= Subak.GAME_OVER_HEIGHT)
    }

    @Test
    fun `drop indices stay within the droppable tiers`() {
        val e = SubakEngine(Random(7))
        repeat(50) {
            e.dropFruit(0.3f)
            assertTrue(e.currentFruitIndex in 0 until Subak.DROPPABLE_TIERS)
            assertTrue(e.nextFruitIndex in 0 until Subak.DROPPABLE_TIERS)
        }
    }

    @Test
    fun `same-tier contact merges into next tier at midpoint and scores`() {
        val e = SubakEngine(Random(1))
        val r = Subak.FRUITS[0].radius
        e.addFruit(0, 0.3f, 0.8f)
        e.addFruit(0, 0.3f + r * 1.5f, 0.8f)   // overlapping → contact this step
        stepSeconds(e, 1f)
        assertEquals(1, e.fruits.size)
        assertEquals(1, e.fruits[0].fruitIndex)
        assertEquals(Subak.FRUITS[1].points, e.score)
        val p = e.fruits[0].body.position
        assertTrue(p.x > 0.3f && p.x < 0.3f + r * 1.5f)
    }

    @Test
    fun `different tiers do not merge`() {
        val e = SubakEngine(Random(1))
        e.addFruit(0, 0.3f, 0.8f)
        e.addFruit(1, 0.32f, 0.8f)
        stepSeconds(e, 0.5f)
        assertEquals(2, e.fruits.size)
        assertEquals(0, e.score)
    }

    @Test
    fun `two watermelons vanish and award flat bonus`() {
        val e = SubakEngine(Random(1))
        val r = Subak.FRUITS.last().radius
        e.addFruit(Subak.FRUITS.lastIndex, 0.3f, 0.8f)
        e.addFruit(Subak.FRUITS.lastIndex, 0.3f + r, 0.8f)
        stepSeconds(e, 1f)
        assertEquals(0, e.fruits.size)
        assertEquals(Subak.WATERMELON_MERGE_POINTS, e.score)
    }

    @Test
    fun `merge emits an expanding effect at the midpoint`() {
        val e = SubakEngine(Random(1))
        val r = Subak.FRUITS[0].radius
        e.addFruit(0, 0.3f, 0.8f)
        e.addFruit(0, 0.3f + r * 1.5f, 0.8f)
        stepSeconds(e, 0.5f)
        assertFalse(e.mergeEffects.isEmpty())
    }

    @Test
    fun `fruits stay inside walls and floor after settling`() {
        val e = SubakEngine(Random(3))
        // dense drop across the width
        val xs = listOf(0.05f, 0.15f, 0.25f, 0.35f, 0.45f, 0.55f)
        repeat(30) { i ->
            e.dropFruit(xs[i % xs.size])
            stepSeconds(e, 0.15f)
        }
        stepSeconds(e, 5f)
        assertFalse(e.fruits.isEmpty())
        for (f in e.fruits) {
            val p = f.body.position
            assertFalse(p.x.isNaN() || p.y.isNaN())
            assertTrue("x=${p.x}", p.x - f.radius > -0.02f)
            assertTrue("x=${p.x}", p.x + f.radius < Subak.GAME_WIDTH + 0.02f)
            assertTrue("y=${p.y}", p.y + f.radius < Subak.GAME_HEIGHT + 0.02f)
        }
    }

    @Test
    fun `settled pile has no deep interpenetration`() {
        val e = SubakEngine(Random(5))
        val xs = listOf(0.1f, 0.3f, 0.5f, 0.2f, 0.4f)
        repeat(25) { i ->
            e.dropFruit(xs[i % xs.size])
            stepSeconds(e, 0.15f)
        }
        stepSeconds(e, 5f)
        for (a in e.fruits) for (b in e.fruits) {
            if (a === b) continue
            val pa = a.body.position
            val pb = b.body.position
            val dx = pb.x - pa.x
            val dy = pb.y - pa.y
            val d = sqrt(dx * dx + dy * dy)
            val minD = a.radius + b.radius
            assertTrue(
                "overlap ${minD - d}m between ${a.fruitIndex} and ${b.fruitIndex}",
                d > minD - 0.012f,   // Box2D slop is ~0.005m; allow margin
            )
        }
    }

    @Test
    fun `fruit resting above the line for over a second ends the game`() {
        val e = SubakEngine(Random(9))
        // fill the container until the pile crosses the danger line
        var guard = 0
        while (!e.gameOver && guard < 220) {
            val x = 0.04f + (guard % 11) * 0.052f
            e.dropFruit(x)
            stepSeconds(e, 0.45f)
            guard++
        }
        assertTrue("game never ended after $guard drops", e.gameOver)
        assertNotNull(e.gameOverFruitId)
    }

    @Test
    fun `a fruit briefly crossing the line does not end the game`() {
        val e = SubakEngine(Random(2))
        // one fruit falling past the line must not trigger gameover
        e.dropFruit(0.3f)
        stepSeconds(e, 1.5f)
        assertFalse(e.gameOver)
        assertNull(e.gameOverFruitId)
    }
}
