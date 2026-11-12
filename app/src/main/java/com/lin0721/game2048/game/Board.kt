package com.lin0721.game2048.game

import kotlin.random.Random

enum class Dir { UP, DOWN, LEFT, RIGHT }

/**
 * 2048 棋盘：16 个格子，0 表示空。
 * 移动逻辑与 AI 共用同一套"把一维线合并"的实现，避免两边规则漂移。
 */
class Board(val cells: IntArray = IntArray(16)) {

    val maxTile: Int get() = cells.maxOrNull() ?: 0

    val emptyCount: Int get() = cells.count { it == 0 }

    fun copy() = Board(cells.copyOf())

    /** 在随机空格放一个 2 或 4，返回是否成功。 */
    fun spawn(rand: Random = Random): Boolean {
        val empty = cells.indices.filter { cells[it] == 0 }
        if (empty.isEmpty()) return false
        cells[empty.random(rand)] = if (rand.nextInt(10) == 0) 4 else 2
        return true
    }

    /** 返回 (新棋盘, 是否发生移动, 本步得分)。 */
    fun move(dir: Dir): Triple<Board, Boolean, Int> {
        val out = IntArray(16)
        var moved = false
        var gained = 0
        for (i in 0 until 4) {
            val line = IntArray(4) { j -> valueAt(dir, i, j) }
            val (merged, score) = slide(line)
            gained += score
            for (j in 0 until 4) {
                if (merged[j] != line[j]) moved = true
                setLine(dir, i, j, merged[j], out)
            }
        }
        return Triple(Board(out), moved, gained)
    }

    private fun valueAt(dir: Dir, i: Int, j: Int): Int = when (dir) {
        Dir.LEFT -> cells[i * 4 + j]
        Dir.RIGHT -> cells[i * 4 + (3 - j)]
        Dir.UP -> cells[j * 4 + i]
        Dir.DOWN -> cells[(3 - j) * 4 + i]
    }

    private fun setLine(dir: Dir, i: Int, j: Int, value: Int, out: IntArray) {
        when (dir) {
            Dir.LEFT -> out[i * 4 + j] = value
            Dir.RIGHT -> out[i * 4 + (3 - j)] = value
            Dir.UP -> out[j * 4 + i] = value
            Dir.DOWN -> out[(3 - j) * 4 + i] = value
        }
    }

    fun canMove(): Boolean {
        if (emptyCount > 0) return true
        for (i in 0 until 4) for (j in 0 until 4) {
            val v = cells[i * 4 + j]
            if (j < 3 && cells[i * 4 + j + 1] == v) return true
            if (i < 3 && cells[(i + 1) * 4 + j] == v) return true
        }
        return false
    }

    companion object {
        fun newGame(rand: Random = Random): Board {
            val b = Board()
            b.spawn(rand); b.spawn(rand)
            return b
        }

        /** 一条线向左滑动并合并，返回合并后的线与得分。 */
        fun slide(line: IntArray): Pair<IntArray, Int> {
            val compact = line.filter { it != 0 }.toMutableList()
            val merged = mutableListOf<Int>()
            var score = 0
            var i = 0
            while (i < compact.size) {
                if (i + 1 < compact.size && compact[i] == compact[i + 1]) {
                    val v = compact[i] * 2
                    merged += v
                    score += v
                    i += 2
                } else {
                    merged += compact[i]
                    i++
                }
            }
            while (merged.size < 4) merged += 0
            return merged.toIntArray() to score
        }
    }
}
