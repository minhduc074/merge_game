package com.bepngon.merge.game

import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Block Blast!-style block puzzle: drag pieces from a 3-piece tray onto an
 * 8x8 grid, full rows/columns pop for points. Pure JVM — unit-testable.
 *
 * Scoring (per GDD):
 *   +10 per placed cell · line clear 100/300/600/1200/2500(+500/line past 5)
 *   clear multiplier x(1 + 0.5*combo) · combo survives while at least one of
 *   the last COMBO_WINDOW placements cleared · all-clear board +3000.
 */
object BlockBlast {
    const val SIZE = 8
    const val CELLS = SIZE * SIZE
    const val TRAY_SIZE = 3
    const val PLACE_POINTS = 10
    const val ALL_CLEAR_BONUS = 3000
    /** Consecutive clearless placements before the combo streak dies. */
    const val COMBO_WINDOW = 3
    /** Board fill fraction above which the RNG favors small pieces and the
     *  tray guarantees at least one fitting piece (when one exists). */
    const val CRAMPED_FILL = 0.65f
    /** Below this fill the RNG favors big pieces to ramp the board quickly. */
    const val EARLY_FILL = 0.35f
    const val COLORS = 8

    fun lineScore(lines: Int): Int = when {
        lines <= 0 -> 0
        lines == 1 -> 100
        lines == 2 -> 300
        lines == 3 -> 600
        lines == 4 -> 1200
        else -> 2500 + (lines - 5) * 500
    }

    /** A polyomino: [cells] are flat offsets inside a [w]x[h] bounding box. */
    class Piece(
        val id: String,
        val cells: IntArray,
        val w: Int,
        val h: Int,
        val weight: Float,
        val weird: Boolean,
    ) {
        val size: Int get() = cells.size
        fun cellRow(c: Int): Int = cells[c] / w
        fun cellCol(c: Int): Int = cells[c] % w
    }

    private fun shape(
        id: String,
        vararg rows: String,
        weight: Float,
        weird: Boolean = false,
    ): Piece {
        val w = rows[0].length
        val cells = rows.flatMapIndexed { r, row ->
            row.mapIndexedNotNull { c, ch -> if (ch == '#') r * w + c else null }
        }.toIntArray()
        return Piece(id, cells, w, rows.size, weight, weird)
    }

    val PIECES: List<Piece> = listOf(
        shape("dot", "#", weight = 9f),
        shape("bar2h", "##", weight = 8f),
        shape("bar2v", "#", "#", weight = 8f),
        shape("bar3h", "###", weight = 8f),
        shape("bar3v", "#", "#", "#", weight = 8f),
        shape("bar4h", "####", weight = 6f),
        shape("bar4v", "#", "#", "#", "#", weight = 6f),
        shape("bar5h", "#####", weight = 4f),
        shape("bar5v", "#", "#", "#", "#", "#", weight = 4f),
        shape("sq2", "##", "##", weight = 8f),
        shape("rect23", "###", "###", weight = 5f),
        shape("rect32", "##", "##", "##", weight = 5f),
        shape("sq3", "###", "###", "###", weight = 2f, weird = true),
        // L-trominoes
        shape("c3a", "##", "#.", weight = 7f),
        shape("c3b", "##", ".#", weight = 7f),
        shape("c3c", "#.", "##", weight = 7f),
        shape("c3d", ".#", "##", weight = 7f),
        // L-tetrominoes (3+2 legs)
        shape("l4a", "#.", "#.", "##", weight = 6f),
        shape("l4b", ".#", ".#", "##", weight = 6f),
        shape("l4c", "##", "#.", "#.", weight = 6f),
        shape("l4d", "##", ".#", ".#", weight = 6f),
        // V-pentominoes (3+3 legs)
        shape("v5a", "#..", "#..", "###", weight = 4f),
        shape("v5b", "..#", "..#", "###", weight = 4f),
        shape("v5c", "###", "#..", "#..", weight = 4f),
        shape("v5d", "###", "..#", "..#", weight = 4f),
        // T-tetrominoes
        shape("t4a", "###", ".#.", weight = 5f),
        shape("t4b", ".#.", "###", weight = 5f),
        shape("t4c", "#.", "##", "#.", weight = 5f),
        shape("t4d", ".#", "##", ".#", weight = 5f),
        // S/Z-tetrominoes
        shape("s4a", ".##", "##.", weight = 5f),
        shape("s4b", "##.", ".##", weight = 5f),
        shape("s4c", "#.", "##", ".#", weight = 5f),
        shape("s4d", ".#", "##", "#.", weight = 5f),
        // the "weird" set the GDD wants more of at high scores
        shape("plus", ".#.", "###", ".#.", weight = 2.5f, weird = true),
        shape("u5h", "#.#", "###", weight = 3.5f, weird = true),
        shape("u5v", "##", ".#", "##", weight = 3.5f, weird = true),
        shape("w5a", "#..", "##.", ".##", weight = 3f, weird = true),
        shape("w5b", "..#", ".##", "##.", weight = 3f, weird = true),
    )

    fun byId(id: String): Piece = PIECES.first { it.id == id }
}

/** A tray slot: which piece and which palette index it renders with. */
data class TrayPiece(val piece: BlockBlast.Piece, val color: Int)

/** What one successful placement did — the UI animates from this. */
data class BlastPlacement(
    val slot: Int,
    val row: Int,
    val col: Int,
    val color: Int,
    val pieceW: Int,
    val pieceH: Int,
    val placedCells: List<Int>,
    /** Cells that popped, deduped, mapped to their pre-clear color index. */
    val clearedCells: Map<Int, Int>,
    val lines: Int,
    val combo: Int,
    val gained: Int,
    val allClear: Boolean,
)

class BlockBlastEngine(private val random: Random = Random) {

    /** 0 = empty, otherwise palette index + 1. */
    val grid = IntArray(BlockBlast.CELLS)
    val tray = arrayOfNulls<TrayPiece>(BlockBlast.TRAY_SIZE)

    var score = 0
        private set
    var combo = 0
        private set
    var bestCombo = 0
        private set
    var moves = 0
        private set
    var gameOver = false
        private set

    /** Placements since the last clear; combo dies at COMBO_WINDOW. */
    private var dry = 0

    val filled: Int get() = grid.count { it != 0 }
    private val fillFraction: Float get() = filled.toFloat() / BlockBlast.CELLS

    init {
        refillTray()
    }

    fun canPlace(piece: BlockBlast.Piece, row: Int, col: Int): Boolean {
        for (i in piece.cells.indices) {
            val r = row + piece.cellRow(i)
            val c = col + piece.cellCol(i)
            if (r !in 0 until BlockBlast.SIZE || c !in 0 until BlockBlast.SIZE) return false
            if (grid[r * BlockBlast.SIZE + c] != 0) return false
        }
        return true
    }

    fun anyPlacement(piece: BlockBlast.Piece): Boolean {
        for (r in 0..BlockBlast.SIZE - piece.h) {
            for (c in 0..BlockBlast.SIZE - piece.w) {
                if (canPlace(piece, r, c)) return true
            }
        }
        return false
    }

    /**
     * Cells that would pop if [piece] landed at (row,col) — pure preview for
     * the drag highlight; empty when the placement clears nothing or is
     * invalid.
     */
    fun previewClear(piece: BlockBlast.Piece, row: Int, col: Int): List<Int> {
        if (!canPlace(piece, row, col)) return emptyList()
        val s = BlockBlast.SIZE
        val mine = BooleanArray(BlockBlast.CELLS)
        for (i in piece.cells.indices) {
            mine[(row + piece.cellRow(i)) * s + col + piece.cellCol(i)] = true
        }
        fun occ(i: Int) = grid[i] != 0 || mine[i]
        val out = LinkedHashSet<Int>()
        for (r in 0 until s) {
            if ((0 until s).all { c -> occ(r * s + c) }) {
                for (c in 0 until s) out += r * s + c
            }
        }
        for (c in 0 until s) {
            if ((0 until s).all { r -> occ(r * s + c) }) {
                for (r in 0 until s) out += r * s + c
            }
        }
        return out.toList()
    }

    /** @return the placement outcome, or null when the drop is illegal. */
    fun placePiece(slot: Int, row: Int, col: Int): BlastPlacement? {
        val tp = tray[slot] ?: return null
        if (gameOver || !canPlace(tp.piece, row, col)) return null
        val s = BlockBlast.SIZE
        val piece = tp.piece
        val placed = piece.cells.map { (row + it / piece.w) * s + col + it % piece.w }
        for (i in placed) grid[i] = tp.color + 1
        var gained = piece.size * BlockBlast.PLACE_POINTS

        val fullRows = (0 until s).filter { r -> (0 until s).all { c -> grid[r * s + c] != 0 } }
        val fullCols = (0 until s).filter { c -> (0 until s).all { r -> grid[r * s + c] != 0 } }
        val cleared = LinkedHashSet<Int>()
        fullRows.forEach { r -> for (c in 0 until s) cleared += r * s + c }
        fullCols.forEach { c -> for (r in 0 until s) cleared += r * s + c }
        val clearedCells = cleared.associateWith { grid[it] - 1 }
        val lines = fullRows.size + fullCols.size
        var allClear = false

        if (lines > 0) {
            // multiplier uses the streak BEFORE this clear: 1st = x1, then grows
            gained += (BlockBlast.lineScore(lines) * (1f + 0.5f * combo)).roundToInt()
            combo += 1
            bestCombo = maxOf(bestCombo, combo)
            dry = 0
            for (i in cleared) grid[i] = 0
            if (grid.all { it == 0 }) {
                gained += BlockBlast.ALL_CLEAR_BONUS
                allClear = true
            }
        } else {
            dry += 1
            if (dry >= BlockBlast.COMBO_WINDOW) {
                combo = 0
                dry = 0
            }
        }

        score += gained
        moves += 1
        tray[slot] = null
        if (tray.all { it == null }) refillTray()
        checkGameOver()
        return BlastPlacement(
            slot, row, col, tp.color, piece.w, piece.h,
            placed, clearedCells, lines, combo, gained, allClear,
        )
    }

    /** Draw a fresh 3-piece tray. Public so tests can exercise the pity RNG. */
    fun refillTray() {
        val fill = fillFraction
        for (i in 0 until BlockBlast.TRAY_SIZE) tray[i] = drawPiece(fill)
        // pity: on a cramped board at least one piece must fit somewhere
        if (fill > BlockBlast.CRAMPED_FILL && tray.none { it != null && anyPlacement(it.piece) }) {
            val fitting = BlockBlast.PIECES.filter { anyPlacement(it) }
            if (fitting.isNotEmpty()) {
                tray[random.nextInt(BlockBlast.TRAY_SIZE)] =
                    TrayPiece(fitting[random.nextInt(fitting.size)], random.nextInt(BlockBlast.COLORS))
            }
        }
    }

    private fun drawPiece(fill: Float): TrayPiece {
        val weirdBoost = (0.5f + score / 5000f).coerceAtMost(1.8f)
        val ws = FloatArray(BlockBlast.PIECES.size)
        var total = 0f
        for (i in BlockBlast.PIECES.indices) {
            val p = BlockBlast.PIECES[i]
            var w = p.weight
            if (fill < BlockBlast.EARLY_FILL) {
                if (p.size >= 4) w *= 1.5f else if (p.size == 1) w *= 0.5f
            } else if (fill > BlockBlast.CRAMPED_FILL) {
                if (p.size <= 3) w *= 3f else if (p.size >= 5) w *= 0.3f
            }
            if (p.weird) w *= weirdBoost
            ws[i] = w
            total += w
        }
        var r = random.nextFloat() * total
        var pick = BlockBlast.PIECES.last()
        for (i in ws.indices) {
            r -= ws[i]
            if (r <= 0f) {
                pick = BlockBlast.PIECES[i]
                break
            }
        }
        return TrayPiece(pick, random.nextInt(BlockBlast.COLORS))
    }

    private fun checkGameOver() {
        gameOver = tray.filterNotNull().none { anyPlacement(it.piece) }
    }
}
