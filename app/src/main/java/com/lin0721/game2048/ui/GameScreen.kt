package com.lin0721.game2048.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.game2048.game.Board
import com.lin0721.game2048.game.Dir
import com.lin0721.game2048.game.Expectimax
import kotlinx.coroutines.delay

@Composable
fun GameScreen() {
    var board by remember { mutableStateOf(Board.newGame()) }
    var score by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var auto by remember { mutableStateOf(false) }
    var over by remember { mutableStateOf(false) }

    fun play(dir: Dir) {
        if (over) return
        val (next, moved, gained) = board.move(dir)
        if (!moved) return
        next.spawn()
        board = next
        score += gained
        if (score > best) best = score
        if (!next.canMove()) { over = true; auto = false }
    }

    LaunchedEffect(auto) {
        while (auto && !over) {
            delay(90)
            val dir = Expectimax.bestMove(board) ?: break
            play(dir)
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("2048", color = MaterialTheme.colorScheme.onBackground, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("合并方块，凑出 2048", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScoreCard("得分", score)
                    ScoreCard("最高", best)
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { board = Board.newGame(); score = 0; over = false; auto = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) { Text("新游戏") }
                OutlinedButton(
                    onClick = { auto = !auto },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (auto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    ),
                ) { Text(if (auto) "停止自动" else "自动游玩") }
            }

            Spacer(Modifier.height(10.dp))

            Box(
                Modifier.fillMaxWidth().aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
                    .pointerInput(over) {
                        var dx = 0f
                        var dy = 0f
                        detectDragGestures(
                            onDragStart = { dx = 0f; dy = 0f },
                            onDragEnd = {
                                if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                                    if (dx > 40f) play(Dir.RIGHT) else if (dx < -40f) play(Dir.LEFT)
                                } else {
                                    if (dy > 40f) play(Dir.DOWN) else if (dy < -40f) play(Dir.UP)
                                }
                            },
                        ) { _, drag -> dx += drag.x; dy += drag.y }
                    },
            ) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (r in 0 until 4) {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (c in 0 until 4) Tile(board.cells[r * 4 + c], Modifier.weight(1f).fillMaxSize())
                        }
                    }
                }
                if (over) {
                    Box(
                        Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("结束", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("得分 $score", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                if (auto) "AI 正在走：expectimax 3 层" else "滑动方块，或点自动游玩看 AI 下",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ScoreCard(label: String, value: Int) {
    Column(
        Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        Text(value.toString(), color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Tile(value: Int, modifier: Modifier = Modifier) {
    val (bg, fg) = tileColors(value)
    val scale = remember { Animatable(1f) }
    LaunchedEffect(value) {
        if (value != 0) {
            scale.snapTo(1.12f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }
    Box(modifier.scale(scale.value).clip(RoundedCornerShape(10.dp)).background(bg), contentAlignment = Alignment.Center) {
        if (value != 0) {
            Text(
                value.toString(), color = fg,
                fontSize = when { value < 100 -> 24.sp; value < 1000 -> 20.sp; else -> 16.sp },
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun tileColors(value: Int): Pair<Color, Color> = when (value) {
    0 -> Color(0xFF2B272E) to Color.Transparent
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
