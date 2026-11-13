package com.lin0721.game2048.core

import kotlin.math.abs
import kotlin.random.Random

/** 一块落脚平台，坐标是世界坐标（dp），中心对称。 */
enum class PlatformKind { NORMAL, BONUS, SPRING, NARROW, HEAVY }

/** 方块类型按序号决定：棋盘生成与渲染共用这一份规则，避免两边漂移。 */
fun platformKind(index: Int): PlatformKind = when {
    index < 5 -> PlatformKind.NORMAL
    index % 7 == 0 -> PlatformKind.BONUS
    index % 6 == 0 -> PlatformKind.SPRING
    index % 5 == 0 -> PlatformKind.NARROW
    index % 11 == 0 -> PlatformKind.HEAVY
    else -> PlatformKind.NORMAL
}

data class JumpPlatform(val center: Float, val width: Float) {
    val left: Float get() = center - width / 2f
    val right: Float get() = center + width / 2f
}

enum class JumpStatus { PLAYING, OVER }

/** 跳一跳的纯逻辑状态：平台、当前位置、得分与是否结束。 */
data class JumpGame(
    val platforms: List<JumpPlatform>,
    val current: Int,
    val playerX: Float,
    val score: Int = 0,
    val status: JumpStatus = JumpStatus.PLAYING,
) {
    val nextPlatform: JumpPlatform? get() = platforms.getOrNull(current + 1)
}

/**
 * 一次跳跃的结果。
 *
 * [next] 是落地后的状态；[landed] 为 false 时游戏结束，但 [landingX] 仍然有效，
 * 用来让 UI 把角色画到摔下去的位置。
 */
data class JumpResult(
    val next: JumpGame,
    val landingX: Float,
    val landed: Boolean,
    val centered: Boolean,
    val gained: Int,
)

/**
 * 跳一跳规则：按住蓄力，松手按距离跳向下一个平台；
 * 落在平台内 +1 分，落在中心附近再 +1。
 */
object Jump {
    const val PLATFORM_WIDTH = 56f

    /** 蓄力从最短到最长对应的跳跃距离（世界坐标 dp）。 */
    const val MIN_JUMP = 50f
    const val MAX_JUMP = 300f

    /** 平台中心的奖励判定半径。 */
    const val CENTER_TOLERANCE = 10f

    private const val MIN_GAP = 110f
    private const val MAX_GAP = 240f

    /** 新开一局：脚下 + 前面两块平台。 */
    fun newGame(rand: Random = Random): JumpGame {
        val first = JumpPlatform(90f, PLATFORM_WIDTH)
        val second = JumpPlatform(first.center + gap(rand), PLATFORM_WIDTH)
        val third = JumpPlatform(second.center + gap(rand), PLATFORM_WIDTH)
        return JumpGame(
            platforms = listOf(first, second, third),
            current = 0,
            playerX = first.center,
        )
    }

    fun jump(game: JumpGame, distance: Float, rand: Random = Random): JumpResult {
        val target = game.nextPlatform
        if (game.status != JumpStatus.PLAYING || target == null) {
            return JumpResult(game, game.playerX, landed = false, centered = false, gained = 0)
        }

        val moved = distance.coerceIn(MIN_JUMP, MAX_JUMP)
        val landing = game.playerX + moved
        if (landing < target.left || landing > target.right) {
            val next = game.copy(status = JumpStatus.OVER, playerX = landing)
            return JumpResult(next, landing, landed = false, centered = false, gained = 0)
        }

        val centered = abs(landing - target.center) <= CENTER_TOLERANCE
        val landedKind = platformKind(game.current + 1)
        val kindBonus = when (landedKind) {
            PlatformKind.BONUS -> 3
            PlatformKind.SPRING -> 2
            PlatformKind.NARROW -> 1
            else -> 0
        }
        val gained = (if (centered) 2 else 1) + kindBonus
        val nextIndex = game.current + 1

        // 每次落地补一块新平台，保证下一跳永远有目标
        val last = game.platforms.last()
        var platforms = game.platforms + JumpPlatform(last.center + gap(rand), PLATFORM_WIDTH)
        var current = nextIndex

        // 只留最近的几块，长局也不会让列表无限增长
        if (current > 2) {
            val drop = current - 1
            platforms = platforms.drop(drop)
            current -= drop
        }

        val next = game.copy(
            platforms = platforms,
            current = current,
            playerX = landing,
            score = game.score + gained,
            status = JumpStatus.PLAYING,
        )
        return JumpResult(next, landing, landed = true, centered = centered, gained = gained)
    }

    private fun gap(rand: Random): Float = MIN_GAP + rand.nextFloat() * (MAX_GAP - MIN_GAP)
}
