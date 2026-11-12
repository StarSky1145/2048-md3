package com.lin0721.game2048.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.game2048.game.MoveResult
import com.lin0721.game2048.game.Slide
import com.lin0721.game2048.game.TileBoard
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val TileGap = 8.dp
private val TileCorner = 10.dp
private val BoardCorner = 16.dp
private val EmptyCell = Color(0xFF2B272E)

/**
 * 棋盘视图：16 格底板 + 覆盖其上的带 id 方块。
 *
 * 每次移动由 [move] / [seq] 驱动一段滑动动画：[seq] 变化时按 [MoveResult]
 * 重新给每个方块设起点，再缓动到终点；合并的两个来源先滑进同一格，
 * 动画结束后换成面值翻倍的新方块并弹一下。关闭 [animate] 就是直接瞬移，
 * 动画时长由设置页的 [animationMs] 决定（快 80 / 标准 140 / 慢 240）。
 */
@Composable
fun BoardView(
    board: TileBoard,
    move: MoveResult?,
    seq: Int,
    animate: Boolean,
    animationMs: Int,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(BoardCorner))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(TileGap),
    ) {
        val cell = (maxWidth - TileGap * 3) / 4
        val density = LocalDensity.current
        val cellPx = with(density) { cell.toPx() }
        val stepPx = with(density) { (cell + TileGap).toPx() }

        val slides = remember(move) { move?.slides?.associateBy { it.id }.orEmpty() }
        val mergedIds = remember(move) { move?.merges?.map { it.intoId }?.toSet().orEmpty() }

        // 只有"本实例内发生的新移动"才播动画：从设置页返回时 composition 会重建，
        // 这时上一步的滑动数据还在，但不能再放一遍。
        val seenSeq = remember { intArrayOf(seq) }
        val freshMove = seenSeq[0] != seq
        if (freshMove) seenSeq[0] = seq

        val slideMs = if (animate) animationMs else 0

        // 合并时先让两个来源方块滑进去，滑完这一组"替身"就退场
        var ghostsVisible by remember(seq) { mutableStateOf(animate && freshMove) }
        LaunchedEffect(seq) {
            if (ghostsVisible) {
                delay(slideMs.toLong())
                ghostsVisible = false
            }
        }

        Canvas(Modifier.matchParentSize()) {
            val radius = CornerRadius(TileCorner.toPx())
            for (row in 0 until 4) for (col in 0 until 4) {
                drawRoundRect(
                    color = EmptyCell,
                    topLeft = Offset(col * stepPx, row * stepPx),
                    size = Size(cellPx, cellPx),
                    cornerRadius = radius,
                )
            }
        }

        board.tiles.forEach { tile ->
            val slide = if (animate && freshMove) slides[tile.id] else null
            val merged = animate && freshMove && tile.id in mergedIds
            key(tile.id, seq) {
                AnimatedTile(
                    value = tile.value,
                    fromX = (slide?.fromCol ?: tile.col) * stepPx,
                    fromY = (slide?.fromRow ?: tile.row) * stepPx,
                    toX = tile.col * stepPx,
                    toY = tile.row * stepPx,
                    size = cell,
                    slide = slide != null,
                    slideMs = slideMs,
                    mergeDelay = if (merged) slideMs else 0,
                    pulse = merged,
                    fadeIn = animate && freshMove && tile.id == move?.spawned?.id,
                )
            }
        }

        if (ghostsVisible) {
            move?.merges?.forEach { merge ->
                GhostTile(merge.a, stepPx, cell, seq, slideMs)
                GhostTile(merge.b, stepPx, cell, seq, slideMs)
            }
        }
    }
}

/**
 * 一个存活方块：从 ([fromX], [fromY]) 缓动到 ([toX], [toY])（单位 px）。
 *
 * [mergeDelay] > 0 表示这是合并出来的新方块，先隐藏等来源滑到位，再出现并弹一下；
 * [fadeIn] 是新生成方块的一次淡入，避免它凭空出现得太硬。
 */
@Composable
private fun AnimatedTile(
    value: Int,
    fromX: Float,
    fromY: Float,
    toX: Float,
    toY: Float,
    size: Dp,
    slide: Boolean,
    slideMs: Int,
    mergeDelay: Int,
    pulse: Boolean,
    fadeIn: Boolean,
) {
    val x = remember { Animatable(fromX) }
    val y = remember { Animatable(fromY) }
    val scaleAnim = remember { Animatable(1f) }
    val fadeAnim = remember { Animatable(if (mergeDelay > 0 || fadeIn) 0f else 1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            if (mergeDelay > 0) {
                delay(mergeDelay.toLong())
                fadeAnim.snapTo(1f)
                if (pulse) {
                    val upMs = (slideMs * 0.5f).roundToInt().coerceAtLeast(1)
                    val downMs = (slideMs * 0.65f).roundToInt().coerceAtLeast(1)
                    scaleAnim.animateTo(1.15f, tween(upMs, easing = LinearOutSlowInEasing))
                    scaleAnim.animateTo(1f, tween(downMs, easing = FastOutSlowInEasing))
                }
            }
            if (fadeIn) {
                val fadeMs = (slideMs * 0.65f).roundToInt().coerceAtLeast(1)
                launch { fadeAnim.animateTo(1f, tween(fadeMs, easing = LinearOutSlowInEasing)) }
            }
            if (slide) {
                val spec = tween<Float>(slideMs, easing = FastOutSlowInEasing)
                launch { x.animateTo(toX, spec) }
                launch { y.animateTo(toY, spec) }
            }
        }
    }

    val (bg, fg) = tileColors(value)
    Box(
        Modifier
            .offset { IntOffset(x.value.roundToInt(), y.value.roundToInt()) }
            .size(size)
            .graphicsLayer {
                alpha = fadeAnim.value
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            }
            .clip(RoundedCornerShape(TileCorner))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        ValueText(value, fg)
    }
}

/** 合并时滑进目标的"替身"方块：只负责把两个来源画到同一格，滑完就消失。 */
@Composable
private fun GhostTile(slide: Slide, stepPx: Float, size: Dp, seq: Int, slideMs: Int) {
    key(slide.id, seq) {
        val x = remember { Animatable(slide.fromCol * stepPx) }
        val y = remember { Animatable(slide.fromRow * stepPx) }
        LaunchedEffect(Unit) {
            coroutineScope {
                val spec = tween<Float>(slideMs, easing = FastOutSlowInEasing)
                launch { x.animateTo(slide.toCol * stepPx, spec) }
                launch { y.animateTo(slide.toRow * stepPx, spec) }
            }
        }
        val (bg, fg) = tileColors(slide.value)
        Box(
            Modifier
                .offset { IntOffset(x.value.roundToInt(), y.value.roundToInt()) }
                .size(size)
                .clip(RoundedCornerShape(TileCorner))
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            ValueText(slide.value, fg)
        }
    }
}

@Composable
private fun ValueText(value: Int, color: Color) {
    Text(
        value.toString(),
        color = color,
        fontSize = when {
            value < 100 -> 24.sp
            value < 1000 -> 20.sp
            else -> 16.sp
        },
        fontWeight = FontWeight.Bold,
    )
}

/** 方块配色：数值越大越亮，2048 以上用紫色收尾。 */
private fun tileColors(value: Int): Pair<Color, Color> = when (value) {
    0 -> EmptyCell to Color.Transparent
    2 -> Color(0xFF3A3540) to Color(0xFFEDE0E3)
    4 -> Color(0xFF4A4350) to Color(0xFFEDE0E3)
    8 -> Color(0xFFB4643C) to Color.White
    16 -> Color(0xFFC97545) to Color.White
    32 -> Color(0xFFD3794A) to Color.White
    64 -> Color(0xFFE08A4E) to Color.White
    128 -> Color(0xFFE6C35C) to Color(0xFF3A2E10)
    256 -> Color(0xFFEAD06A) to Color(0xFF3A2E10)
    512 -> Color(0xFFEFDB79) to Color(0xFF3A2E10)
    1024 -> Color(0xFFD8C7F0) to Color(0xFF2A2038)
    else -> Color(0xFFC9B6F2) to Color(0xFF231A33)
}
