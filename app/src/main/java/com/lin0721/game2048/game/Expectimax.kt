package com.lin0721.game2048.game

import kotlin.math.ln
import kotlin.random.Random

/**
 * 自动游玩：expectimax 搜 3 层。
 *
 * 2048 的随机性来自"新方块的位置与大小"，所以对手是期望节点而不是最小节点，
 * 用期望值剪枝比 minimax 更贴近真实收益。
 */
object Expectimax {

    fun bestMove(board: Board, depth: Int = 3): Dir? {
        var best: Dir? = null
        var bestScore = Double.NEGATIVE_INFINITY
        for (dir in Dir.entries) {
            val (next, moved, _) = board.move(dir)
            if (!moved) continue
            val score = chance(next, depth - 1)
            if (score > bestScore) {
                bestScore = score
                best = dir
            }
        }
        return best
    }

    private fun chance(board: Board, depth: Int): Double {
        if (depth <= 0) return evaluate(board)
        val empty = board.cells.indices.filter { board.cells[it] == 0 }
        if (empty.isEmpty()) return evaluate(board)
        var total = 0.0
        for (idx in empty) {
            for (value in intArrayOf(2, 4)) {
                val p = if (value == 2) 0.9 else 0.1
                val b = board.copy()
                b.cells[idx] = value
                total += p * maxNode(b, depth - 1)
            }
        }
        return total / empty.size
    }

    private fun maxNode(board: Board, depth: Int): Double {
        if (depth <= 0) return evaluate(board)
        var best = Double.NEGATIVE_INFINITY
        var any = false
        for (dir in Dir.entries) {
            val (next, moved, _) = board.move(dir)
            if (!moved) continue
            any = true
            val v = chance(next, depth - 1)
            if (v > best) best = v
        }
        return if (any) best else evaluate(board)
    }

    /** 空格、单调性、平滑度、最大块，权重按经验值。 */
    private fun evaluate(board: Board): Double {
        val c = board.cells
        var empty = 0.0
        var smooth = 0.0
        var mono = 0.0
        for (i in 0 until 4) for (j in 0 until 4) {
            val v = c[i * 4 + j]
            if (v == 0) { empty += 1.0; continue }
            if (j < 3) {
                val r = c[i * 4 + j + 1]
                if (r != 0) smooth -= kotlin.math.abs(ln(v.toDouble()) - ln(r.toDouble()))
            }
            if (i < 3) {
                val d = c[(i + 1) * 4 + j]
                if (d != 0) smooth -= kotlin.math.abs(ln(v.toDouble()) - ln(d.toDouble()))
            }
        }
        for (i in 0 until 4) {
            var up = 0.0; var down = 0.0
            for (j in 0 until 4) {
                val v = c[j * 4 + i]
                if (v == 0) continue
                up += ln(v.toDouble()) * (j + 1)
                down += ln(v.toDouble()) * (4 - j)
            }
            mono += kotlin.math.max(up, down)
        }
        return 2.7 * empty + 1.0 * smooth + 1.0 * mono + 1.0 * ln(board.maxTile.toDouble().coerceAtLeast(2.0))
    }
}
