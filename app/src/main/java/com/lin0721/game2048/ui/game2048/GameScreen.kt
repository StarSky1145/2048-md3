package com.lin0721.game2048.ui.game2048

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.game2048.data.Prefs
import com.lin0721.game2048.ui.ScreenScaffold
import com.lin0721.game2048.core.Dir
import com.lin0721.game2048.core.Expectimax
import com.lin0721.game2048.core.MoveResult
import com.lin0721.game2048.core.TileBoard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 一局 2048 的全部界面状态：棋盘、分数、自动游玩、动画开关与设置页导航。
 *
 * 棋盘状态放在这里而不是设置页里，所以打开设置再返回时牌局不会丢；
 * 启动时从 [prefs] 读回最高分和未结束的牌局，每步再写回去。
 */
@Composable
fun GameScreen(prefs: Prefs, onBack: () -> Unit) {
    // 只恢复"还能继续走"的牌局；已结束的存档直接开新局
    val saved = remember { prefs.savedGame?.let { TileBoard.decode(it) }?.takeIf { it.canMove() } }
    val savedScore = if (saved != null) prefs.savedScore else 0

    var board by remember { mutableStateOf(saved ?: TileBoard.newGame()) }
    var lastMove by remember { mutableStateOf<MoveResult?>(null) }
    var moveSeq by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(savedScore) }
    var best by remember { mutableIntStateOf(maxOf(prefs.bestScore, savedScore)) }
    var auto by remember { mutableStateOf(false) }
    var over by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var animate by remember { mutableStateOf(prefs.animate) }
    var animationMs by remember { mutableIntStateOf(prefs.animationMs) }
    var stepMs by remember { mutableIntStateOf(prefs.stepMs) }

    // 设置页自己处理返回键；这里只在牌局界面把返回交给上层
    BackHandler(enabled = !settingsOpen, onBack = onBack)

    fun saveGame(b: TileBoard, s: Int) {
        prefs.savedGame = b.encode()
        prefs.savedScore = s
    }

    fun startNewGame() {
        val fresh = TileBoard.newGame()
        board = fresh
        lastMove = null
        moveSeq++
        score = 0
        over = false
        auto = false
        saveGame(fresh, 0)
    }

    /** 走一步：先按规则移动，再补一个新方块，最后把整段变化交给 UI 做动画。 */
    fun play(dir: Dir) {
        if (over) return
        val result = board.move(dir)
        if (!result.moved) return
        val (next, spawned) = TileBoard(result.tiles).spawn()
        board = next
        lastMove = result.copy(tiles = next.tiles, spawned = spawned)
        moveSeq++
        score += result.gained
        if (score > best) {
            best = score
            prefs.bestScore = best
        }
        if (!next.canMove()) {
            over = true
            auto = false
            prefs.clearSavedGame()
        } else {
            saveGame(next, score)
        }
    }

    // 首次进入时如果没有可恢复的牌局，把新棋盘也存下来，
    // 这样去首页再回来不会换一副牌
    LaunchedEffect(Unit) {
        if (saved == null) saveGame(board, score)
    }

    // 自动游玩：按设置里的间隔循环，搜索放到后台线程，避免拖慢滑动动画
    LaunchedEffect(auto, stepMs, settingsOpen) {
        while (auto && !over && !settingsOpen) {
            delay(stepMs.toLong())
            if (!auto || over || settingsOpen) break
            val snapshot = board.toBoard()
            val dir = withContext(Dispatchers.Default) { Expectimax.bestMove(snapshot) } ?: break
            play(dir)
        }
    }

    if (settingsOpen) {
        SettingsScreen(
            auto = auto,
            onAutoChange = { auto = it },
            animate = animate,
            onAnimateChange = {
                animate = it
                prefs.animate = it
            },
            animationMs = animationMs,
            onAnimationMsChange = {
                animationMs = it
                prefs.animationMs = it
            },
            stepMs = stepMs,
            onStepMsChange = {
                stepMs = it
                prefs.stepMs = it
            },
            onNewGame = {
                startNewGame()
                settingsOpen = false
            },
            onBack = { settingsOpen = false },
        )
        return
    }

    ScreenScaffold {
        // 头两行固定高度，棋盘在剩下的安全区里取 min(宽, 高) 居中，小屏也不会被圆角切掉
        Column(
            Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TopIconButton(onClick = onBack, description = "返回首页") {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    "2048", color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                CompactScore("得分", score)
                Spacer(Modifier.width(6.dp))
                CompactScore("最高", best)
                Spacer(Modifier.width(2.dp))
                TopIconButton(onClick = { settingsOpen = true }, description = "设置") {
                    Icon(
                        Icons.Filled.Settings, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactButton(
                    text = "新游戏",
                    container = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { startNewGame() },
                )
                CompactButton(
                    text = if (auto) "停止自动" else "自动游玩",
                    container = if (auto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (auto) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    onClick = { auto = !auto },
                )
            }

            Spacer(Modifier.height(4.dp))

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                BoxWithConstraints {
                    val side = minOf(maxWidth, maxHeight)
                    Box(
                        Modifier.size(side)
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
                                ) { change, drag ->
                                    change.consume()
                                    dx += drag.x
                                    dy += drag.y
                                }
                            },
                    ) {
                        BoardView(
                            board = board,
                            move = lastMove,
                            seq = moveSeq,
                            animate = animate,
                            animationMs = animationMs,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (over) {
                            Box(
                                Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "结束", color = Color.White,
                                        fontSize = 24.sp, fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "得分 $score", color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 顶栏图标：自己做 36dp，避免 Material 默认 48dp 把标题行撑高。 */
@Composable
private fun TopIconButton(
    onClick: () -> Unit,
    description: String,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** 得分/最高压成一行小胶囊，给棋盘让出高度。 */
@Composable
private fun CompactScore(label: String, value: Int) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            value.toString(), color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp, fontWeight = FontWeight.Bold,
        )
    }
}

/** 34dp 紧凑按钮，保证 368 宽下按钮行不会把棋盘挤小。 */
@Composable
private fun CompactButton(
    text: String,
    container: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier.height(34.dp).clip(RoundedCornerShape(10.dp))
            .background(container).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
