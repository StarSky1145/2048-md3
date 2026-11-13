package com.lin0721.game2048.core

import kotlin.random.Random

/** 扫雷的一格：是否有雷、是否已翻开、是否插旗，以及周围 8 格的雷数。 */
data class MineCell(
    val mine: Boolean = false,
    val revealed: Boolean = false,
    val flagged: Boolean = false,
    val adjacent: Int = 0,
)

enum class MinesweeperStatus { PLAYING, WON, LOST }

/** 三档难度；size 为边长，mines 为雷数。 */
enum class MineDifficulty(val size: Int, val mines: Int) {
    EASY(9, 10),
    MEDIUM(12, 25),
    HARD(16, 50);

    companion object {
        /** 从持久化的名字恢复，旧数据/坏数据回退到简单。 */
        fun of(value: String?): MineDifficulty =
            entries.firstOrNull { it.name == value } ?: EASY
    }
}

/** 一局扫雷的完整状态；所有操作都返回新状态，UI 只负责渲染。 */
data class MinesweeperState(
    val size: Int = Minesweeper.DEFAULT_SIZE,
    val mineCount: Int = Minesweeper.DEFAULT_MINES,
    val cells: List<MineCell> = List(size * size) { MineCell() },
    val status: MinesweeperStatus = MinesweeperStatus.PLAYING,
    val minesPlaced: Boolean = false,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
) {
    val flagCount: Int get() = cells.count { it.flagged }

    /** 剩余雷数 = 总雷数 - 已插旗数，插多了显示 0。 */
    val remainingMines: Int get() = (mineCount - flagCount).coerceAtLeast(0)

    /** 从第一次挖开开始计时；结束后冻结在完成时间。 */
    fun elapsedSeconds(now: Long): Int {
        val start = startedAt ?: return 0
        val end = finishedAt ?: now
        return ((end - start).coerceAtLeast(0L) / 1000L).toInt()
    }
}

/**
 * 扫雷逻辑，纯 Kotlin，不依赖 Compose；支持 9×9 / 12×12 / 16×16 三档。
 *
 * 第一次挖开时才放雷：被点的格子和它周围 8 格一定没有雷，
 * 所以第一下必然安全，而且会挖开一小片；插旗不会触发放雷。
 */
object Minesweeper {

    const val DEFAULT_SIZE = 9
    const val DEFAULT_MINES = 10

    fun newGame(difficulty: MineDifficulty): MinesweeperState =
        newGame(difficulty.size, difficulty.mines)

    fun newGame(
        size: Int = DEFAULT_SIZE,
        mineCount: Int = DEFAULT_MINES,
    ): MinesweeperState {
        val safeSize = size.coerceAtLeast(4)
        // 第一次点击会排除 3×3，给后续留出足够的放雷空间
        val safeMines = mineCount.coerceIn(1, safeSize * safeSize - 9)
        return MinesweeperState(
            size = safeSize,
            mineCount = safeMines,
            cells = List(safeSize * safeSize) { MineCell() },
        )
    }

    /**
     * 提示：翻一张必安全的格子。
     * 优先挑「周围已有数字、且这个数字的邻雷已经插满」的格子——那种格子在逻辑上可推出安全，
     * 玩起来像真的在推理，而不是白送一格。
     */
    fun hint(state: MinesweeperState, now: Long, rand: Random = Random): MinesweeperState {
        if (state.status != MinesweeperStatus.PLAYING) return state
        val size = state.size
        val safe = state.cells.indices.filter {
            val c = state.cells[it]
            !c.revealed && !c.flagged && !c.mine
        }
        if (safe.isEmpty()) return state
        val deduced = safe.firstOrNull { i ->
            val r = i / size
            val col = i % size
            val neighbours = buildList {
                for (dr in -1..1) for (dc in -1..1) {
                    if (dr == 0 && dc == 0) continue
                    val nr = r + dr
                    val nc = col + dc
                    if (nr in 0 until size && nc in 0 until size) add(nr * size + nc)
                }
            }
            neighbours.any { n ->
                val cell = state.cells[n]
                cell.revealed && cell.adjacent > 0 &&
                    neighbours.count { state.cells[it].flagged } >= cell.adjacent
            }
        }
        val pick = deduced ?: safe.random(rand)
        return reveal(state, pick / size, pick % size, now, rand)
    }

    fun toggleFlag(state: MinesweeperState, row: Int, col: Int): MinesweeperState {
        if (state.status != MinesweeperStatus.PLAYING) return state
        val index = indexOf(state, row, col) ?: return state
        val cell = state.cells[index]
        if (cell.revealed) return state
        return state.copy(
            cells = state.cells.toMutableList().also {
                it[index] = cell.copy(flagged = !cell.flagged)
            },
        )
    }

    /**
     * 挖开一格。第一次挖开会先放雷，再按 0 连锁展开。
     *
     * @param now 用于计时，测试时可注入固定时间
     */
    fun reveal(
        state: MinesweeperState,
        row: Int,
        col: Int,
        now: Long,
        rand: Random = Random,
    ): MinesweeperState {
        if (state.status != MinesweeperStatus.PLAYING) return state
        val index = indexOf(state, row, col) ?: return state
        val target = state.cells[index]
        if (target.revealed || target.flagged) return state

        var cells = state.cells
        var startedAt = state.startedAt
        if (!state.minesPlaced) {
            cells = placeMines(state.size, state.mineCount, index, state.cells, rand)
            startedAt = now
        }

        val safeCells = cells.toMutableList()
        if (safeCells[index].mine) {
            for (i in safeCells.indices) {
                if (safeCells[i].mine) safeCells[i] = safeCells[i].copy(revealed = true)
            }
            return state.copy(
                cells = safeCells,
                status = MinesweeperStatus.LOST,
                minesPlaced = true,
                startedAt = startedAt,
                finishedAt = now,
            )
        }

        revealFrom(safeCells, state.size, index)
        val won = safeCells.none { !it.mine && !it.revealed }
        return state.copy(
            cells = safeCells,
            status = if (won) MinesweeperStatus.WON else MinesweeperStatus.PLAYING,
            minesPlaced = true,
            startedAt = startedAt,
            finishedAt = if (won) now else null,
        )
    }

    private fun indexOf(state: MinesweeperState, row: Int, col: Int): Int? {
        if (row !in 0 until state.size || col !in 0 until state.size) return null
        return row * state.size + col
    }

    /** 排除首点及其周围 8 格后随机放雷，并算好每格的相邻雷数。 */
    private fun placeMines(
        size: Int,
        mineCount: Int,
        first: Int,
        previous: List<MineCell>,
        rand: Random,
    ): List<MineCell> {
        val forbidden = (neighborsOf(size, first) + first).toSet()
        val candidates = (0 until size * size).filter { it !in forbidden }
        val mines = candidates.shuffled(rand).take(mineCount).toSet()

        val adjacent = IntArray(size * size)
        for (mine in mines) {
            for (near in neighborsOf(size, mine)) adjacent[near]++
        }
        // 首点之前插的旗要保留下来，不能因为放雷被清掉
        return List(size * size) { i ->
            MineCell(
                mine = i in mines,
                flagged = previous[i].flagged,
                adjacent = adjacent[i],
            )
        }
    }

    /** 从 [start] 开始翻开；周围没有雷的格子继续向 8 邻扩散。 */
    private fun revealFrom(cells: MutableList<MineCell>, size: Int, start: Int) {
        val queue = ArrayDeque<Int>()
        queue.addLast(start)
        while (queue.isNotEmpty()) {
            val i = queue.removeFirst()
            val cell = cells[i]
            if (cell.revealed || cell.flagged || cell.mine) continue
            cells[i] = cell.copy(revealed = true)
            if (cell.adjacent != 0) continue
            for (near in neighborsOf(size, i)) {
                val next = cells[near]
                if (!next.revealed && !next.flagged && !next.mine) queue.addLast(near)
            }
        }
    }

    private fun neighborsOf(size: Int, index: Int): List<Int> {
        val row = index / size
        val col = index % size
        val result = ArrayList<Int>(8)
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val r = row + dr
            val c = col + dc
            if (r in 0 until size && c in 0 until size) result += r * size + c
        }
        return result
    }
}
