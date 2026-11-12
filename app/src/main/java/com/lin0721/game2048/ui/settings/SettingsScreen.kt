package com.lin0721.game2048.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.game2048.BuildConfig
import com.lin0721.game2048.data.LayoutMode
import com.lin0721.game2048.data.Prefs
import com.lin0721.game2048.ui.LocalUiMetrics
import com.lin0721.game2048.ui.ScreenScaffold

/** 自动步进的候选间隔（毫秒），越短 AI 走得越快。 */
private val StepOptions = listOf(60, 90, 150)

/** 动画时长的三档：毫秒 to 档位名。 */
private val AnimationOptions = listOf(
    Prefs.FAST_ANIMATION_MS to "快",
    Prefs.DEFAULT_ANIMATION_MS to "标准",
    Prefs.SLOW_ANIMATION_MS to "慢",
)

/**
 * 设置页：首页齿轮和 2048 里的设置按钮都进这一页。
 *
 * 从首页进来时只有应用级设置；从牌局进来时额外带自动游玩开关与"新游戏"。
 * 所有开关直接写 [Prefs]，界面布局走 [onLayoutChange] 让整个应用立刻换挡。
 */
@Composable
fun SettingsScreen(
    prefs: Prefs,
    layout: LayoutMode,
    onLayoutChange: (LayoutMode) -> Unit,
    onBack: () -> Unit,
    auto: Boolean? = null,
    onAutoChange: ((Boolean) -> Unit)? = null,
    onNewGame: (() -> Unit)? = null,
) {
    BackHandler(onBack = onBack)
    val ui = LocalUiMetrics.current
    var animate by remember { mutableStateOf(prefs.animate) }
    var animationMs by remember { mutableIntStateOf(prefs.animationMs) }
    var stepMs by remember { mutableIntStateOf(prefs.stepMs) }

    ScreenScaffold {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth()
                    .padding(start = ui.space(4.dp), end = ui.space(16.dp), top = ui.space(4.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text(
                    "设置", color = MaterialTheme.colorScheme.onBackground,
                    fontSize = ui.title(22.sp), fontWeight = FontWeight.Bold,
                )
            }

            SectionTitle("游戏")
            if (onAutoChange != null) {
                SwitchRow(
                    title = "自动游玩",
                    subtitle = "由 expectimax 连续自动操作棋盘",
                    checked = auto == true,
                    onCheckedChange = onAutoChange,
                )
            }
            SwitchRow(
                title = "动画",
                subtitle = "关闭后方块直接瞬移，适合性能较弱的设备",
                checked = animate,
                onCheckedChange = {
                    animate = it
                    prefs.animate = it
                },
            )
            AnimationRow(
                animationMs = animationMs,
                enabled = animate,
                onAnimationMsChange = {
                    animationMs = it
                    prefs.animationMs = it
                },
            )
            StepRow(
                stepMs = stepMs,
                onStepMsChange = {
                    stepMs = it
                    prefs.stepMs = it
                },
            )
            LayoutRow(mode = layout, onLayoutChange = onLayoutChange)
            Hint(
                "自动游玩用 expectimax 逐层加深；界面布局对所有页面立刻生效",
                Modifier.padding(start = ui.space(20.dp), end = ui.space(20.dp), top = ui.space(8.dp)),
            )

            HorizontalDivider(
                Modifier.padding(horizontal = ui.space(20.dp), vertical = ui.space(12.dp)),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            if (onNewGame != null) {
                SectionTitle("对局")
                Button(
                    onClick = onNewGame,
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = ui.space(20.dp), vertical = ui.space(8.dp))
                        .height(ui.buttonHeight()),
                ) { Text("新游戏") }
                Hint(
                    "会清空当前得分与进度",
                    Modifier.padding(start = ui.space(20.dp), end = ui.space(20.dp), top = ui.space(2.dp)),
                )

                HorizontalDivider(
                    Modifier.padding(horizontal = ui.space(20.dp), vertical = ui.space(12.dp)),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }

            SectionTitle("关于")
            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = ui.space(20.dp), vertical = ui.space(6.dp)),
            ) {
                Text("2048", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                Text(
                    "版本 ${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Kotlin + Jetpack Compose · Material 3 深色",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(ui.space(24.dp)))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val ui = LocalUiMetrics.current
    Text(
        text,
        Modifier.fillMaxWidth()
            .padding(start = ui.space(20.dp), end = ui.space(20.dp), top = ui.space(16.dp), bottom = ui.space(6.dp)),
        color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val ui = LocalUiMetrics.current
    Row(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }
            .padding(horizontal = ui.space(20.dp), vertical = ui.space(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = ui.space(12.dp))) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnimationRow(
    animationMs: Int,
    enabled: Boolean,
    onAnimationMsChange: (Int) -> Unit,
) {
    val ui = LocalUiMetrics.current
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = ui.space(20.dp), vertical = ui.space(8.dp)),
    ) {
        Text("动画时长", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
        Spacer(Modifier.height(ui.space(8.dp)))
        Row(horizontalArrangement = Arrangement.spacedBy(ui.space(8.dp))) {
            AnimationOptions.forEach { (ms, label) ->
                FilterChip(
                    selected = animationMs == ms,
                    onClick = { onAnimationMsChange(ms) },
                    enabled = enabled,
                    label = { Text("$label $ms ms") },
                )
            }
        }
        Spacer(Modifier.height(ui.space(4.dp)))
        Hint("滑动与合并的时长，关闭动画时不生效", Modifier.padding(top = ui.space(4.dp)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepRow(stepMs: Int, onStepMsChange: (Int) -> Unit) {
    val ui = LocalUiMetrics.current
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = ui.space(20.dp), vertical = ui.space(8.dp)),
    ) {
        Text("自动步进间隔", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
        Spacer(Modifier.height(ui.space(8.dp)))
        Row(horizontalArrangement = Arrangement.spacedBy(ui.space(8.dp))) {
            StepOptions.forEach { ms ->
                FilterChip(
                    selected = stepMs == ms,
                    onClick = { onStepMsChange(ms) },
                    label = { Text("$ms ms") },
                )
            }
        }
        Spacer(Modifier.height(ui.space(4.dp)))
        Hint("仅影响自动游玩的速度", Modifier.padding(top = ui.space(4.dp)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayoutRow(mode: LayoutMode, onLayoutChange: (LayoutMode) -> Unit) {
    val ui = LocalUiMetrics.current
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = ui.space(20.dp), vertical = ui.space(8.dp)),
    ) {
        Text("界面布局", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
        Spacer(Modifier.height(ui.space(8.dp)))
        Row(horizontalArrangement = Arrangement.spacedBy(ui.space(8.dp))) {
            LayoutMode.entries.forEach { level ->
                FilterChip(
                    selected = mode == level,
                    onClick = { onLayoutChange(level) },
                    label = { Text(level.label) },
                )
            }
        }
        Spacer(Modifier.height(ui.space(4.dp)))
        Hint("紧凑：边距减半、标题 -2sp、按钮更矮，给小屏让出棋盘", Modifier.padding(top = ui.space(4.dp)))
    }
}

@Composable
private fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
    )
}
