package com.lin0721.game2048.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lin0721.game2048.data.Prefs
import com.lin0721.game2048.ui.game2048.GameScreen
import com.lin0721.game2048.ui.hub.HubScreen

/** 应用内导航：首页 ↔ 各个小游戏。 */
private enum class Destination { HUB, GAME_2048 }

@Composable
fun GameApp(prefs: Prefs) {
    var destination by remember { mutableStateOf(Destination.HUB) }
    when (destination) {
        Destination.HUB -> HubScreen(
            prefs = prefs,
            onOpen2048 = { destination = Destination.GAME_2048 },
        )
        Destination.GAME_2048 -> GameScreen(
            prefs = prefs,
            onBack = { destination = Destination.HUB },
        )
    }
}
