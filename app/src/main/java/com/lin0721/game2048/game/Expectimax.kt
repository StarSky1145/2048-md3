package com.lin0721.game2048.game

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max

/**
 * 自动游玩：expectimax + 时间盒。
 *
 * 2048 的随机性来自"新方块的位置与大小"，所以对手是期望节点而不是最小节点，
 * 用期望值搜索比 minimax 更贴近真实收益。
 *
 * 默认搜 4 层；4 层在低端机上可能吃满一帧，所以每步限制 [TIME_BUDGET_MS] 毫秒，
 * 超时就退回已经算完的 3 层结果。
 */
object Expectimax {

    /** 默认搜索深度（含根节点这一步）。 */
    const val SEARCH_DEPTH = 4

    /** 每步搜索的时间盒（毫秒）。 */
    const val TIME_BUDGET_MS = 60L

    /** 4 层超时后回退的深度。 */
    private const val FALLBACK_DEPTH = 3

    /**
     * 蛇形权重：左下角 16 最大，沿"底行向右 → 上 → 第二行向左 → 上 → 顶行向右"
     * 的蛇形路径递减。大块越贴近左下角蛇形排列分越高，挪到别的角落会明显丢分。
     */
    private val SNAKE_WEIGHTS = intArrayOf(
        1, 2, 3, 4,
        8, 7, 6, 5,
        9, 10, 11, 12,
        16, 15, 14, 13,
    )

    /** 蛇形项在总评估里的系数，和下面几项保持同一量级。 */
    private const val SNAKE_WEIGHT = 0.3

    /** 超时用异常打断递归，避免每层都传"是否超时"的标志。 */
    private object SearchTimeout : RuntimeException()

    /**
     * 选出一步。
     *
     * @param budgetMs 4 层搜索的时间盒；传 3 层深度时只用来兜底判断
     * @param maxDepth 目标深度，默认 4 层
     */
    fun bestMove(
        board: Board,
        budgetMs: Long = TIME_BUDGET_MS,
        maxDepth: Int = SEARCH_DEPTH,
    ): Dir? {
        if (!board.canMove()) return null
        val deadline = System.nanoTime() + budgetMs * 1_000_000L

        // 3 层通常只要几毫秒，先算出来当保底；4 层再在剩下的时间里跑
        val fallbackDepth = minOf(FALLBACK_DEPTH, maxDepth)
        val fallback = try {
            searchRoot(board, fallbackDepth, deadline)
        } catch (_: SearchTimeout) {
            null
        }
        if (maxDepth <= fallbackDepth) return fallback

        return try {
            searchRoot(board, maxDepth, deadline)
        } catch (_: SearchTimeout) {
            fallback
        }
    }

    /** 根节点：只挑方向，不做随机节点。 */
    private fun searchRoot(board: Board, depth: Int, deadline: Long): Dir? {
        var best: Dir? = null
        var bestScore = Double.NEGATIVE_INFINITY
        for (dir in Dir.entries) {
            checkTime(deadline)
            val (next, moved, gained) = board.move(dir)
            if (!moved) continue
            // 得分只作为同分时的微小偏好，不改变局面的主评估
            val score = gained * 0.01 + chance(next, depth - 1, deadline)
            if (score > bestScore) {
                bestScore = score
                best = dir
            }
        }
        return best
    }

    /** 随机节点：每个空格按 90% / 10% 的概率放 2 / 4。 */
    private fun chance(board: Board, depth: Int, deadline: Long): Double {
        if (depth <= 0) return evaluate(board)
        val empty = board.cells.indices.filter { board.cells[it] == 0 }
        if (empty.isEmpty()) return evaluate(board)

        var total = 0.0
        val probe = board.copy()
        for (idx in empty) {
            checkTime(deadline)
            probe.cells[idx] = 2
            total += 0.9 * maxNode(probe, depth - 1, deadline)
            probe.cells[idx] = 4
            total += 0.1 * maxNode(probe, depth - 1, deadline)
        }
        return total / empty.size
    }

    /** 玩家节点：取所有合法方向里的最大值。 */
    private fun maxNode(board: Board, depth: Int, deadline: Long): Double {
        if (depth <= 0) return evaluate(board)
        checkTime(deadline)

        var best = Double.NEGATIVE_INFINITY
        var any = false
        for (dir in Dir.entries) {
            val (next, moved, _) = board.move(dir)
            if (!moved) continue
            any = true
            val score = chance(next, depth - 1, deadline)
            if (score > best) best = score
        }
        return if (any) best else evaluate(board)
    }

    private fun checkTime(deadline: Long) {
        if (System.nanoTime() > deadline) throw SearchTimeout
    }

    /**
     * 局面评估：空位、平滑度、单调性、最大块，再加蛇形权重。
     *
     * 公开出来方便单独调用或以后写测试，不依赖 Android。
     */
    fun evaluate(board: Board): Double {
        val c = board.cells
        var empty = 0.0
        var smooth = 0.0
        var snake = 0.0
        for (i in 0 until 4) for (j in 0 until 4) {
            val idx = i * 4 + j
            val v = c[idx]
            if (v == 0) {
                empty += 1.0
                continue
            }
            val log = ln(v.toDouble())
            snake += log * SNAKE_WEIGHTS[idx]
            if (j < 3) {
                val r = c[idx + 1]
                if (r != 0) smooth -= abs(log - ln(r.toDouble()))
            }
            if (i < 3) {
                val d = c[idx + 4]
                if (d != 0) smooth -= abs(log - ln(d.toDouble()))
            }
        }

        // 逐列比较上行/下行两个方向的加权和，保留更大的那个
        var mono = 0.0
        for (i in 0 until 4) {
            var up = 0.0
            var down = 0.0
            for (j in 0 until 4) {
                val v = c[j * 4 + i]
                if (v == 0) continue
                val log = ln(v.toDouble())
                up += log * (j + 1)
                down += log * (4 - j)
            }
            mono += max(up, down)
        }

        return 2.7 * empty + smooth + mono +
            ln(board.maxTile.toDouble().coerceAtLeast(2.0)) +
            SNAKE_WEIGHT * snake
    }
}
