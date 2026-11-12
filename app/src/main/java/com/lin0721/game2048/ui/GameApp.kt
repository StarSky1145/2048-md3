package com.lin0721.game2048.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lin0721.game2048.data.LayoutMode
import com.lin0721.game2048.data.Prefs
import com.lin0721.game2048.ui.game2048.GameScreen
import com.lin0721.game2048.ui.hub.HubScreen
import com.lin0721.game2048.ui.jump.JumpScreen
import com.lin0721.game2048.ui.minesweeper.MinesweeperScreen
import com.lin0721.game2048.ui.settings.SettingsScreen

/** 应用内导航：首页 ↔ 各个小游戏 / 设置页。 */
private enum class Destination { HUB, GAME_2048, MINESWEEPER, JUMP, SETTINGS }

@Composable
fun GameApp(prefs: Prefs) {
    var destination by remember { mutableStateOf(Destination.HUB) }
    // 布局档位在这里存一份，切档时整个应用立刻按新尺寸重组
    var layout by remember { mutableStateOf(prefs.layoutMode) }
    val metrics = remember(layout) { UiMetrics(layout) }

    fun changeLayout(mode: LayoutMode) {
        layout = mode
        prefs.layoutMode = mode
    }

    CompositionLocalProvider(LocalUiMetrics provides metrics) {
        when (destination) {
            Destination.HUB -> HubScreen(
                prefs = prefs,
                onOpen2048 = { destination = Destination.GAME_2048 },
                onOpenMinesweeper = { destination = Destination.MINESWEEPER },
                onOpenJump = { destination = Destination.JUMP },
                onOpenSettings = { destination = Destination.SETTINGS },
            )
            Destination.GAME_2048 -> GameScreen(
                prefs = prefs,
                layout = layout,
                onLayoutChange = { changeLayout(it) },
                onBack = { destination = Destination.HUB },
            )
            Destination.MINESWEEPER -> MinesweeperScreen(
                prefs = prefs,
                onBack = { destination = Destination.HUB },
            )
            Destination.JUMP -> JumpScreen(
                prefs = prefs,
                onBack = { destination = Destination.HUB },
            )
            Destination.SETTINGS -> SettingsScreen(
                prefs = prefs,
                layout = layout,
                onLayoutChange = { changeLayout(it) },
                onBack = { destination = Destination.HUB },
            )
        }
    }
}
