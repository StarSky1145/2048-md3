package com.lin0721.game2048.core

import kotlin.random.Random

enum class Dir { UP, DOWN, LEFT, RIGHT }

/**
 * 一条线向左滑动并合并后的完整结果。
 *
 * [values] 是合并后的四个值；[fromA] / [fromB] 记录每个输出格在原始线里的来源下标
 * （-1 表示该格为空；[fromB] 只在合并时有值）。带上来源信息后，[TileBoard]
 * 才能知道某个方块的 id 从哪一格搬到了哪一格，UI 也才能做滑动动画。
 */
class LineSlide(
    val values: IntArray,
    val fromA: IntArray,
    val fromB: IntArray,
    val merged: BooleanArray,
    val score: Int,
)

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
            val line = IntArray(4) { j -> cells[index(dir, i, j)] }
            val result = slideLine(line)
            gained += result.score
            for (j in 0 until 4) {
                if (result.values[j] != line[j]) moved = true
                out[index(dir, i, j)] = result.values[j]
            }
        }
        return Triple(Board(out), moved, gained)
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

        /**
         * 把"方向 + 第 i 条线 + 线内第 j 格"映射成 16 格数组下标。
         * 四个方向的坐标系都从"移动的最前方"开始编号，这样滑动方向永远是向 0。
         */
        fun index(dir: Dir, i: Int, j: Int): Int = when (dir) {
            Dir.LEFT -> i * 4 + j
            Dir.RIGHT -> i * 4 + (3 - j)
            Dir.UP -> j * 4 + i
            Dir.DOWN -> (3 - j) * 4 + i
        }

        /** 一条线向左滑动并合并；棋盘移动与瓦片动画共用这一套规则。 */
        fun slideLine(line: IntArray): LineSlide {
            val values = IntArray(4)
            val fromA = IntArray(4) { -1 }
            val fromB = IntArray(4) { -1 }
            val merged = BooleanArray(4)
            var score = 0
            var out = 0

            val filled = (0 until 4).filter { line[it] != 0 }
            var k = 0
            while (k < filled.size) {
                val a = filled[k]
                if (k + 1 < filled.size && line[filled[k + 1]] == line[a]) {
                    val b = filled[k + 1]
                    values[out] = line[a] * 2
                    fromA[out] = a
                    fromB[out] = b
                    merged[out] = true
                    score += values[out]
                    k += 2
                } else {
                    values[out] = line[a]
                    fromA[out] = a
                    k += 1
                }
                out++
            }
            return LineSlide(values, fromA, fromB, merged, score)
        }
    }
}
