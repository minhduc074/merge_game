package com.bepngon.merge

import com.bepngon.merge.game.BlockBlast
import com.bepngon.merge.game.BlockBlastEngine
import com.bepngon.merge.game.TrayPiece
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private fun engine(seed: Int = 1) = BlockBlastEngine(Random(seed))

private fun fill(e: BlockBlastEngine, vararg cells: Int, color: Int = 1) {
    cells.forEach { e.grid[it] = color }
}

private fun fillRow(e: BlockBlastEngine, r: Int, exceptCol: Int = -1) {
    for (c in 0 until BlockBlast.SIZE) if (c != exceptCol) e.grid[r * BlockBlast.SIZE + c] = 1
}

private fun fillCol(e: BlockBlastEngine, c: Int, exceptRow: Int = -1) {
    for (r in 0 until BlockBlast.SIZE) if (r != exceptRow) e.grid[r * BlockBlast.SIZE + c] = 1
}

private fun setTray(e: BlockBlastEngine, vararg ids: String?) {
    ids.forEachIndexed { i, id ->
        e.tray[i] = id?.let { TrayPiece(BlockBlast.byId(it), 0) }
    }
}

class BlockBlastEngineTest {

    @Test
    fun `placing a piece scores 10 per cell and occupies the grid`() {
        val e = engine()
        setTray(e, "sq2", null, null)
        val res = e.placePiece(0, 3, 4)
        assertNotNull(res)
        assertEquals(40, res!!.gained)
        assertEquals(40, e.score)
        assertEquals(1, e.grid[3 * 8 + 4])
        assertEquals(1, e.grid[4 * 8 + 5])
        // tray emptied -> auto-refilled
        assertTrue(e.tray.all { it != null })
        assertEquals(1, e.moves)
    }

    @Test
    fun `illegal drops return null and change nothing`() {
        val e = engine()
        setTray(e, "sq2", null, null)
        assertNull(e.placePiece(0, 7, 7))          // overflows bottom-right
        assertNull(e.placePiece(0, -1, 0))         // off grid
        assertNull(e.placePiece(1, 0, 0))          // empty slot
        assertEquals(0, e.score)
        assertEquals(0, e.moves)
        e.grid[0] = 1
        assertNull(e.placePiece(0, 0, 0))          // overlap
    }

    @Test
    fun `full row clears and pays 100 plus placement`() {
        val e = engine()
        e.grid[0] = 1                                // stray cell so it's not all-clear
        fillRow(e, 7, exceptCol = 3)
        setTray(e, "dot", null, null)
        val res = e.placePiece(0, 7, 3)!!
        assertEquals(1, res.lines)
        assertEquals(8, res.clearedCells.size)
        assertEquals(110, res.gained)               // 10 + 100, combo was 0
        assertTrue((0 until 8).all { c -> e.grid[7 * 8 + c] == 0 })
        assertEquals(1, e.combo)
    }

    @Test
    fun `crossing row and column counts as two lines`() {
        val e = engine()
        e.grid[1] = 1                                // stray cell so it's not all-clear
        fillRow(e, 3, exceptCol = 3)
        fillCol(e, 3, exceptRow = 3)
        setTray(e, "dot", null, null)
        val res = e.placePiece(0, 3, 3)!!
        assertEquals(2, res.lines)
        // deduped: 8 + 8 - 1 shared cell
        assertEquals(15, res.clearedCells.size)
        assertEquals(310, res.gained)               // 10 + 300
    }

    @Test
    fun `multi-line score table and combo multiplier`() {
        val e = engine()
        e.grid[4] = 1                                // stray cell survives both clears
        // first clear -> combo becomes 1, multiplier was x1
        fillRow(e, 7, exceptCol = 0)
        setTray(e, "dot", null, null)
        e.placePiece(0, 7, 0)
        assertEquals(1, e.combo)
        // second clear -> multiplier x1.5 on base 100 -> 150 + 10
        fillRow(e, 6, exceptCol = 0)
        setTray(e, "dot", null, null)
        val res = e.placePiece(0, 6, 0)!!
        assertEquals(160, res.gained)
        assertEquals(2, e.combo)
    }

    @Test
    fun `combo survives sparse windows and dies after 3 dry placements`() {
        val e = engine()
        setTray(e, "dot", null, null)
        fillRow(e, 7, exceptCol = 0)
        e.placePiece(0, 7, 0)
        assertEquals(1, e.combo)
        // two non-clearing placements keep the streak alive
        setTray(e, "dot", null, null)
        e.placePiece(0, 0, 0)
        setTray(e, "dot", null, null)
        e.placePiece(0, 0, 1)
        assertEquals(1, e.combo)
        // a third dry placement kills it
        setTray(e, "dot", null, null)
        e.placePiece(0, 0, 2)
        assertEquals(0, e.combo)
    }

    @Test
    fun `clearing within the 3-placement window still grows the combo`() {
        val e = engine()
        setTray(e, "dot", null, null)
        fillRow(e, 7, exceptCol = 0)
        e.placePiece(0, 7, 0)
        // 2 dry + 1 clear inside the window -> combo grows instead of resetting
        setTray(e, "dot", null, null)
        e.placePiece(0, 0, 0)
        setTray(e, "dot", null, null)
        e.placePiece(0, 0, 1)
        fillRow(e, 6, exceptCol = 0)
        setTray(e, "dot", null, null)
        e.placePiece(0, 6, 0)
        assertEquals(2, e.combo)
    }

    @Test
    fun `emptying the whole board pays the all-clear bonus`() {
        val e = engine()
        fillRow(e, 7, exceptCol = 0)
        setTray(e, "dot", null, null)
        val res = e.placePiece(0, 7, 0)!!
        assertTrue(res.allClear)
        assertEquals(10 + 100 + BlockBlast.ALL_CLEAR_BONUS, res.gained)
    }

    @Test
    fun `tray refills after the third placement`() {
        val e = engine()
        setTray(e, "dot", "dot", "dot")
        e.placePiece(0, 0, 0)
        e.placePiece(1, 0, 1)
        e.placePiece(2, 0, 2)
        assertTrue(e.tray.all { it != null })
    }

    @Test
    fun `previewClear reports pop cells without touching the board`() {
        val e = engine()
        fillRow(e, 7, exceptCol = 3)
        val piece = BlockBlast.byId("dot")
        val preview = e.previewClear(piece, 7, 3)
        assertEquals(8, preview.size)
        // board unchanged, and the last hole is still empty
        assertEquals(7, e.grid.count { it != 0 })
        assertEquals(0, e.grid[7 * 8 + 3])
        // invalid placement previews nothing
        assertTrue(e.previewClear(BlockBlast.byId("sq2"), 7, 7).isEmpty())
    }

    @Test
    fun `game over when no tray piece fits anywhere`() {
        val e = engine()
        // holes cover every row AND column (main diagonal + (0,1),(1,0)), so a
        // dot at (0,0) pops nothing and the leftovers are all non-adjacent —
        // bar2h/sq2 can never fit -> game over
        for (i in 0 until BlockBlast.CELLS) e.grid[i] = 1
        listOf(0, 1, 8, 18, 27, 36, 45, 54, 63).forEach { e.grid[it] = 0 }
        setTray(e, "dot", "bar2h", "sq2")
        e.placePiece(0, 0, 0)
        assertTrue(e.gameOver)
        // no further placements allowed
        assertNull(e.placePiece(1, 0, 0))
    }

    @Test
    fun `game continues while at least one piece fits`() {
        val e = engine()
        // only (7,6)-(7,7) open; placing bar2h there clears row 7 and saves us
        for (i in 0 until BlockBlast.CELLS - 2) e.grid[i] = 1
        setTray(e, "bar2h", "sq3", "bar3v")
        e.placePiece(0, 7, 6)
        assertFalse(e.gameOver)
    }

    @Test
    fun `cramped refills guarantee at least one fitting piece`() {
        for (seed in 0 until 40) {
            val e = engine(seed)
            // ~72% filled, scattered holes
            for (i in 0 until BlockBlast.CELLS) {
                if (i % 7 != 3 || i / 7 == 0) e.grid[i] = 1
            }
            var guard = 0
            while (guard++ < 6) {
                e.refillTray()
                assertTrue(
                    "seed=$seed tray had no fitting piece",
                    e.tray.any { it != null && e.anyPlacement(it.piece) },
                )
            }
        }
    }

    @Test
    fun `line score table matches the GDD`() {
        assertEquals(100, BlockBlast.lineScore(1))
        assertEquals(300, BlockBlast.lineScore(2))
        assertEquals(600, BlockBlast.lineScore(3))
        assertEquals(1200, BlockBlast.lineScore(4))
        assertEquals(2500, BlockBlast.lineScore(5))
        assertEquals(3000, BlockBlast.lineScore(6))
    }
}
