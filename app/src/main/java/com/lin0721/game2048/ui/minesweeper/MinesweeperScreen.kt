package com.lin0721.game2048.ui.minesweeper

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.game2048.core.MineCell
import com.lin0721.game2048.core.MineDifficulty
import com.lin0721.game2048.core.Minesweeper
import com.lin0721.game2048.core.MinesweeperState
import com.lin0721.game2048.core.MinesweeperStatus
import com.lin0721.game2048.data.Prefs
import com.lin0721.game2048.ui.LocalUiMetrics
import com.lin0721.game2048.ui.ScreenScaffold
import kotlinx.coroutines.delay

/** 扫雷页：三档难度、长按/插旗模式、计时、难度确认与胜负弹窗。 */
@Composable
fun MinesweeperScreen(prefs: Prefs, onBack: () -> Unit) {
    var difficulty by remember { mutableStateOf(prefs.mineDifficulty) }
    var game by remember { mutableStateOf(Minesweeper.newGame(difficulty)) }
    var flagMode by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var newRecord by remember { mutableStateOf(false) }
    var pendingDifficulty by remember { mutableStateOf<MineDifficulty?>(null) }

    BackHandler(onBack = onBack)

    LaunchedEffect(game.status) {
        when (game.status) {
            MinesweeperStatus.WON -> {
                val seconds = game
                    .elapsedSeconds(game.finishedAt ?: System.currentTimeMillis())
                    .coerceAtLeast(1)
                val old = prefs.bestMinesweeperSeconds(difficulty)
                if (old == 0 || seconds < old) {
                    prefs.setBestMinesweeperSeconds(difficulty, seconds)
                    newRecord = true
                }
                showResult = true
            }
            MinesweeperStatus.LOST -> showResult = true
            MinesweeperStatus.PLAYING -> Unit
        }
    }

    /** 切难度/重开都走这里，清掉上一局的所有临时状态。 */
    fun startNew(target: MineDifficulty) {
        difficulty = target
        prefs.mineDifficulty = target
        game = Minesweeper.newGame(target)
        flagMode = false
        showResult = false
        newRecord = false
    }

    fun tap(index: Int) {
        if (game.status != MinesweeperStatus.PLAYING) return
        val row = index / game.size
        val col = index % game.size
        game = if (flagMode) {
            Minesweeper.toggleFlag(game, row, col)
        } else {
            Minesweeper.reveal(game, row, col, System.currentTimeMillis())
        }
    }

    fun longPress(index: Int) {
        if (game.status != MinesweeperStatus.PLAYING) return
        game = Minesweeper.toggleFlag(game, index / game.size, index % game.size)
    }

    val best = prefs.bestMinesweeperSeconds(difficulty)
    val ui = LocalUiMetrics.current

    ScreenScaffold {
        Column(
            Modifier.fillMaxSize()
                .padding(horizontal = ui.space(12.dp), vertical = ui.space(4.dp)),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回首页",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "扫雷", color = MaterialTheme.colorScheme.onSurface,
                        fontSize = ui.title(20.sp), fontWeight = FontWeight.Bold,
                    )
                    Text(
                        buildString {
                            append("${difficulty.size}×${difficulty.size} · ${difficulty.mines} 雷")
                            if (best > 0) append(" · 最快 ${formatTime(best)}")
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp,
                    )
                }
                TimerText(game)
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MineDifficulty.entries.forEach { level ->
                    SelectChip(
                        text = difficultyName(level),
                        selected = level == difficulty,
                        onClick = {
                            if (level != difficulty) pendingDifficulty = level
                        },
                    )
                    Spacer(Modifier.padding(horizontal = 2.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "⚑ ${game.remainingMines}",
                    color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.padding(horizontal = 3.dp))
                SelectChip(
                    text = if (flagMode) "插旗中" else "插旗",
                    selected = flagMode,
                    onClick = { flagMode = !flagMode },
                )
            }

            Spacer(Modifier.height(4.dp))

            // 网格在剩下的安全区里取 min(宽, 高) 居中，最外圈格子不会被圆角裁掉
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                BoxWithConstraints {
                    val side = minOf(maxWidth, maxHeight)
                    MineBoard(
                        game = game,
                        modifier = Modifier.size(side),
                        onTap = { tap(it) },
                        onLongPress = { longPress(it) },
                    )
                }
            }
        }
    }

    pendingDifficulty?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDifficulty = null },
            title = {
                Text(
                    "切换难度", color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    "将开始一局新的「${difficultyName(target)}」扫雷，当前进度会丢失。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    startNew(target)
                    pendingDifficulty = null
                }) { Text("开新局") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDifficulty = null }) { Text("取消") }
            },
        )
    }

    if (showResult && game.status != MinesweeperStatus.PLAYING) {
        val won = game.status == MinesweeperStatus.WON
        val used = game.elapsedSeconds(game.finishedAt ?: System.currentTimeMillis()).coerceAtLeast(1)
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    if (won) "通关！" else "踩雷了",
                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    if (won) {
                        "「${difficultyName(difficulty)}」用时 ${formatTime(used)}" +
                            if (newRecord) "，新纪录！" else ""
                    } else {
                        "这局有 ${game.mineCount} 颗雷，再来一次？"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = { startNew(difficulty) }) { Text("再来一局") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showResult = false
                    onBack()
                }) { Text("返回首页") }
            },
        )
    }
}

/** 计时单独成节点，秒数跳动时不会带着整个棋盘重组。 */
@Composable
private fun TimerText(game: MinesweeperState) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(game.status, game.startedAt) {
        while (game.status == MinesweeperStatus.PLAYING && game.startedAt != null) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    Text(
        formatTime(game.elapsedSeconds(game.finishedAt ?: now)),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 18.sp, fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier.height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp, fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun MineBoard(
    game: MinesweeperState,
    modifier: Modifier,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
) {
    BoxWithConstraints(modifier) {
        val gap = if (game.size >= 14) 1.dp else 2.dp
        val inner = 4.dp
        val cell = (maxWidth - gap * (game.size - 1).toFloat() - inner * 2) / game.size.toFloat()
        val numberSize = (cell.value * 0.56f).coerceIn(8f, 18f).sp
        val symbolSize = (cell.value * 0.68f).coerceIn(9f, 22f).sp
        val corner = (cell.value * 0.16f).coerceIn(2f, 5f).dp

        Column(
            Modifier.fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(inner),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            for (row in 0 until game.size) {
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    for (col in 0 until game.size) {
                        val index = row * game.size + col
                        MineCellView(
                            cell = game.cells[index],
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            numberSize = numberSize,
                            symbolSize = symbolSize,
                            corner = corner,
                            onClick = { onTap(index) },
                            onLongClick = { onLongPress(index) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MineCellView(
    cell: MineCell,
    modifier: Modifier,
    numberSize: TextUnit,
    symbolSize: TextUnit,
    corner: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val background = if (cell.revealed) Color(0xFF1E1A20) else Color(0xFF353039)
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            cell.revealed && cell.mine -> Text(
                "✹", color = Color(0xFFEF9A9A),
                fontSize = symbolSize, fontWeight = FontWeight.Bold,
            )
            cell.revealed && cell.adjacent > 0 -> Text(
                "${cell.adjacent}", color = numberColor(cell.adjacent),
                fontSize = numberSize, fontWeight = FontWeight.Bold,
            )
            !cell.revealed && cell.flagged -> Text(
                "⚑", color = MaterialTheme.colorScheme.primary,
                fontSize = symbolSize,
            )
            else -> Unit
        }
    }
}

private fun difficultyName(difficulty: MineDifficulty): String = when (difficulty) {
    MineDifficulty.EASY -> "简单"
    MineDifficulty.MEDIUM -> "中等"
    MineDifficulty.HARD -> "困难"
}

private fun numberColor(value: Int): Color = when (value) {
    1 -> Color(0xFF8AB4F8)
    2 -> Color(0xFF81C784)
    3 -> Color(0xFFEF9A9A)
    4 -> Color(0xFF9FA8DA)
    5 -> Color(0xFFFFCC80)
    6 -> Color(0xFF80DEEA)
    7 -> Color(0xFFF48FB1)
    else -> Color(0xFFE0E0E0)
}

/** 秒数格式化成 mm:ss，小屏上不占位置。 */
private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return (if (m < 10) "0$m" else "$m") + ":" + (if (s < 10) "0$s" else "$s")
}
