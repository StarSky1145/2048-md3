package com.lin0721.game2048.ui

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
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.game2048.BuildConfig
import com.lin0721.game2048.data.Prefs

/** 自动步进的候选间隔（毫秒），越短 AI 走得越快。 */
private val StepOptions = listOf(60, 90, 150)

/** 动画时长的三档：毫秒 to 档位名。 */
private val AnimationOptions = listOf(
    Prefs.FAST_ANIMATION_MS to "快",
    Prefs.DEFAULT_ANIMATION_MS to "标准",
    Prefs.SLOW_ANIMATION_MS to "慢",
)

/**
 * 独立设置页：自动游玩、动画、步进间隔、新游戏与关于。
 *
 * 所有开关都直接改 [GameScreen] 里提升上来的状态，改完立刻生效；
 * 返回键与左上角箭头都会回到牌局。
 */
@Composable
fun SettingsScreen(
    auto: Boolean,
    onAutoChange: (Boolean) -> Unit,
    animate: Boolean,
    onAnimateChange: (Boolean) -> Unit,
    animationMs: Int,
    onAnimationMsChange: (Int) -> Unit,
    stepMs: Int,
    onStepMsChange: (Int) -> Unit,
    onNewGame: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text(
                    "设置", color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                )
            }

            SectionTitle("游戏")
            SwitchRow(
                title = "自动游玩",
                subtitle = "由 expectimax 连续自动操作棋盘",
                checked = auto,
                onCheckedChange = onAutoChange,
            )
            SwitchRow(
                title = "动画",
                subtitle = "关闭后方块直接瞬移，适合性能较弱的设备",
                checked = animate,
                onCheckedChange = onAnimateChange,
            )
            AnimationRow(
                animationMs = animationMs,
                enabled = animate,
                onAnimationMsChange = onAnimationMsChange,
            )
            StepRow(stepMs, onStepMsChange)

            HorizontalDivider(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            SectionTitle("对局")
            Button(
                onClick = onNewGame,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            ) { Text("新游戏") }
            Hint("会清空当前得分与进度", Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp))

            HorizontalDivider(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            SectionTitle("关于")
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
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
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
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
    Row(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
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
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("动画时长", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnimationOptions.forEach { (ms, label) ->
                FilterChip(
                    selected = animationMs == ms,
                    onClick = { onAnimationMsChange(ms) },
                    enabled = enabled,
                    label = { Text("$label $ms ms") },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Hint("滑动与合并的时长，关闭动画时不生效", Modifier.padding(top = 4.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepRow(stepMs: Int, onStepMsChange: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("自动步进间隔", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepOptions.forEach { ms ->
                FilterChip(
                    selected = stepMs == ms,
                    onClick = { onStepMsChange(ms) },
                    label = { Text("$ms ms") },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Hint("仅影响自动游玩的速度", Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
    )
}
