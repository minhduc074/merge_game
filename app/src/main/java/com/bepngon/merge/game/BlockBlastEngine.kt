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
    const val CLEAR_HELPER_BASE_CHANCE = 0.05f
    const val CLEAR_HELPER_PITY_STEP = 0.09f
    const val CLEAR_HELPER_MAX_CHANCE = 0.78f
    const val SETUP_HELPER_BASE_CHANCE = 0.10f
    const val SETUP_HELPER_PITY_STEP = 0.06f
    const val SETUP_HELPER_MAX_CHANCE = 0.72f
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
        val solid: Boolean get() = cells.size == w * h
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
    private var clearlessMoves = 0

    private data class PieceOption(
        val piece: BlockBlast.Piece,
        val fits: Int,
        val clearSpots: Int,
        val bestLines: Int,
        val clearQuality: Float,
        val setupQuality: Float,
    )

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
            clearlessMoves = 0
            for (i in cleared) grid[i] = 0
            if (grid.all { it == 0 }) {
                gained += BlockBlast.ALL_CLEAR_BONUS
                allClear = true
            }
        } else {
            dry += 1
            clearlessMoves += 1
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
        val options = analyzePieces()
        val byPiece = options.associateBy { it.piece }
        val fitting = options.filter { it.fits > 0 }
        val clearing = fitting.filter { it.bestLines > 0 }
        val smart = when {
            fitting.isEmpty() -> null
            clearing.isNotEmpty() && random.nextFloat() < clearOfferChance(fill) -> pickClearPiece(clearing)
            random.nextFloat() < setupOfferChance() -> pickSetupPiece(fitting, fill)
            else -> null
        }
        for (i in 0 until BlockBlast.TRAY_SIZE) {
            tray[i] = if (i == 0 && smart != null) {
                TrayPiece(smart, random.nextInt(BlockBlast.COLORS))
            } else {
                drawPiece(fill, byPiece)
            }
        }
        ensureFittingPieces(fitting, fill)
        val shuffled = tray.map { it!! }.shuffled(random)
        for (i in shuffled.indices) tray[i] = shuffled[i]
    }

    private fun setupOfferChance(): Float =
        (BlockBlast.SETUP_HELPER_BASE_CHANCE + clearlessMoves * BlockBlast.SETUP_HELPER_PITY_STEP)
            .coerceAtMost(BlockBlast.SETUP_HELPER_MAX_CHANCE)

    private fun clearOfferChance(fill: Float): Float {
        var chance = BlockBlast.CLEAR_HELPER_BASE_CHANCE +
            clearlessMoves * BlockBlast.CLEAR_HELPER_PITY_STEP
        if (fill > 0.50f) chance += 0.06f
        if (fill > BlockBlast.CRAMPED_FILL) chance += 0.06f
        return chance.coerceAtMost(BlockBlast.CLEAR_HELPER_MAX_CHANCE)
    }

    private fun analyzePieces(): List<PieceOption> {
        val rows = IntArray(BlockBlast.SIZE)
        val cols = IntArray(BlockBlast.SIZE)
        for (i in 0 until BlockBlast.CELLS) {
            if (grid[i] != 0) {
                rows[i / BlockBlast.SIZE]++
                cols[i % BlockBlast.SIZE]++
            }
        }
        return BlockBlast.PIECES.map { analyzePiece(it, rows, cols) }
    }

    private fun analyzePiece(piece: BlockBlast.Piece, baseRows: IntArray, baseCols: IntArray): PieceOption {
        var fits = 0
        var clearSpots = 0
        var bestLines = 0
        var clearQuality = 0f
        var setupQuality = 0f
        for (row in 0..BlockBlast.SIZE - piece.h) {
            for (col in 0..BlockBlast.SIZE - piece.w) {
                if (!canPlace(piece, row, col)) continue
                fits++
                val rows = baseRows.clone()
                val cols = baseCols.clone()
                for (rel in piece.cells) {
                    rows[row + rel / piece.w]++
                    cols[col + rel % piece.w]++
                }
                var lines = 0
                var bestLine = 0
                var total = 0f
                for (n in rows) {
                    if (n == BlockBlast.SIZE) lines++ else {
                        bestLine = maxOf(bestLine, n)
                        total += n * n * n / 5f
                    }
                }
                for (n in cols) {
                    if (n == BlockBlast.SIZE) lines++ else {
                        bestLine = maxOf(bestLine, n)
                        total += n * n * n / 5f
                    }
                }
                val progress = bestLine * bestLine * bestLine / 2f + total / 4f
                if (lines > 0) {
                    clearSpots++
                    bestLines = maxOf(bestLines, lines)
                    clearQuality = maxOf(clearQuality, lines * 1000f + progress)
                } else {
                    setupQuality = maxOf(setupQuality, progress)
                }
            }
        }
        return PieceOption(piece, fits, clearSpots, bestLines, clearQuality, setupQuality)
    }

    private fun pickWeighted(candidates: List<PieceOption>, weightOf: (PieceOption) -> Float): BlockBlast.Piece {
        var total = 0f
        for (op in candidates) total += weightOf(op).coerceAtLeast(0.01f)
        var r = random.nextFloat() * total
        var pick = candidates.last().piece
        for (op in candidates) {
            r -= weightOf(op).coerceAtLeast(0.01f)
            if (r <= 0f) {
                pick = op.piece
                break
            }
        }
        return pick
    }

    private fun pickClearPiece(candidates: List<PieceOption>): BlockBlast.Piece =
        pickWeighted(candidates) { op ->
            var w = 12f + op.bestLines * 32f + op.clearSpots * 10f + op.clearQuality / 250f
            if (op.piece.size <= 3) w *= 1.15f
            if (op.piece.weird) w *= 0.75f
            w
        }

    private fun pickSetupPiece(candidates: List<PieceOption>, fill: Float): BlockBlast.Piece =
        pickWeighted(candidates) { op ->
            var w = 6f + op.setupQuality + op.fits * 0.55f
            if (op.piece.solid) w *= if (fill < 0.50f) 1.18f else 1.05f
            if (op.piece.weird) w *= if (fill > BlockBlast.CRAMPED_FILL) 0.35f else 0.70f
            if (fill < BlockBlast.EARLY_FILL) {
                if (op.piece.size >= 4) w *= 1.35f
                if (op.piece.size == 1) w *= 0.55f
            } else if (fill > BlockBlast.CRAMPED_FILL) {
                if (op.piece.size <= 3) w *= 2.2f
                if (op.piece.size >= 5) w *= 0.45f
            }
            w
        }

    private fun ensureFittingPieces(fitting: List<PieceOption>, fill: Float) {
        if (fitting.isEmpty()) return
        val target = when {
            fill > BlockBlast.CRAMPED_FILL -> 2
            fill > 0.45f || clearlessMoves >= 3 -> 1
            else -> 0
        }
        var guard = 0
        while (tray.count { it != null && anyPlacement(it.piece) } < target && guard++ < 8) {
            val replaceable = (0 until BlockBlast.TRAY_SIZE).filter {
                tray[it]?.piece?.let { p -> !anyPlacement(p) } ?: true
            }
            if (replaceable.isEmpty()) break
            val slot = replaceable[random.nextInt(replaceable.size)]
            tray[slot] = TrayPiece(pickSetupPiece(fitting, fill), random.nextInt(BlockBlast.COLORS))
        }
    }

    private fun drawPiece(fill: Float, options: Map<BlockBlast.Piece, PieceOption>): TrayPiece {
        val weirdBoost = (0.65f + score / 7000f).coerceAtMost(1.80f)
        val ws = FloatArray(BlockBlast.PIECES.size)
        var total = 0f
        for (i in BlockBlast.PIECES.indices) {
            val p = BlockBlast.PIECES[i]
            var w = p.weight
            if (fill < BlockBlast.EARLY_FILL) {
                if (p.size >= 4) w *= 1.45f else if (p.size == 1) w *= 0.55f
                if (p.solid) w *= 1.12f
            } else if (fill > BlockBlast.CRAMPED_FILL) {
                if (p.size <= 3) w *= 2.8f else if (p.size >= 5) w *= 0.35f
                if (p.weird) w *= 0.60f
            }
            val op = options[p]
            if (op != null) {
                if (op.fits == 0) {
                    w *= if (fill > 0.45f) 0.04f else 0.15f
                } else {
                    w *= 0.80f + (op.fits.coerceAtMost(20) / 20f) * 0.40f
                    if (op.bestLines > 0) {
                        w *= (1.00f + clearlessMoves * 0.08f).coerceAtMost(1.45f)
                    }
                }
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
