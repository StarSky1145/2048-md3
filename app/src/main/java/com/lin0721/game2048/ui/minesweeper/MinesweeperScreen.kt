package com.lin0721.game2048.ui.minesweeper

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.game2048.core.MineCell
import com.lin0721.game2048.core.Minesweeper
import com.lin0721.game2048.core.MinesweeperState
import com.lin0721.game2048.core.MinesweeperStatus
import com.lin0721.game2048.data.Prefs
import kotlinx.coroutines.delay

/** 扫雷页：9×9 小屏棋盘、长按/插旗模式、计时与胜负弹窗。 */
@Composable
fun MinesweeperScreen(prefs: Prefs, onBack: () -> Unit) {
    var game by remember { mutableStateOf(Minesweeper.newGame()) }
    var flagMode by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showResult by remember { mutableStateOf(false) }
    var newRecord by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    // 第一次挖开后才开始走秒；胜负已分后停在 finishedAt
    LaunchedEffect(game.status, game.startedAt) {
        while (game.status == MinesweeperStatus.PLAYING && game.startedAt != null) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }

    LaunchedEffect(game.status) {
        when (game.status) {
            MinesweeperStatus.WON -> {
                val seconds = game
                    .elapsedSeconds(game.finishedAt ?: System.currentTimeMillis())
                    .coerceAtLeast(1)
                val old = prefs.bestMinesweeperSeconds
                if (old == 0 || seconds < old) {
                    prefs.bestMinesweeperSeconds = seconds
                    newRecord = true
                }
                showResult = true
            }
            MinesweeperStatus.LOST -> showResult = true
            MinesweeperStatus.PLAYING -> Unit
        }
    }

    fun restart() {
        game = Minesweeper.newGame()
        flagMode = false
        showResult = false
        newRecord = false
        now = System.currentTimeMillis()
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

    val seconds = game.elapsedSeconds(now)
    val best = prefs.bestMinesweeperSeconds

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回首页",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "扫雷", color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (best > 0) "9×9 · 10 雷 · 最快 ${formatTime(best)}" else "9×9 · 10 雷",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                    )
                }
                Text(
                    formatTime(seconds), color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold,
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "⚑ ${game.remainingMines}",
                    color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                FilterChip(
                    selected = flagMode,
                    onClick = { flagMode = !flagMode },
                    label = { Text(if (flagMode) "插旗中" else "挖开") },
                )
            }

            Spacer(Modifier.height(6.dp))

            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.fillMaxSize().aspectRatio(1f, matchHeightConstraintsFirst = true)) {
                    MineBoard(game, onTap = { tap(it) }, onLongPress = { longPress(it) })
                }
            }
        }
    }

    if (showResult && game.status != MinesweeperStatus.PLAYING) {
        val won = game.status == MinesweeperStatus.WON
        val used = game.elapsedSeconds(game.finishedAt ?: now).coerceAtLeast(1)
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
                        "用时 ${formatTime(used)}" + if (newRecord) "，新纪录！" else ""
                    } else {
                        "这局有 ${game.mineCount} 颗雷，再来一次？"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = { restart() }) { Text("再来一局") }
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

@Composable
private fun MineBoard(
    game: MinesweeperState,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
) {
    Column(
        Modifier.fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (row in 0 until game.size) {
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                for (col in 0 until game.size) {
                    val index = row * game.size + col
                    MineCellView(
                        cell = game.cells[index],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { onTap(index) },
                        onLongClick = { onLongPress(index) },
                    )
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
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val background = if (cell.revealed) Color(0xFF1E1A20) else Color(0xFF353039)
    Box(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            cell.revealed && cell.mine -> Text(
                "✹", color = Color(0xFFEF9A9A),
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
            )
            cell.revealed && cell.adjacent > 0 -> Text(
                "${cell.adjacent}", color = numberColor(cell.adjacent),
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
            )
            !cell.revealed && cell.flagged -> Text(
                "⚑", color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
            )
            else -> Unit
        }
    }
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
