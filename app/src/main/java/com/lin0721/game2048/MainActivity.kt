package com.lin0721.game2048

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lin0721.game2048.data.Prefs
import com.lin0721.game2048.ui.MiniGamesTheme
import com.lin0721.game2048.ui.GameApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 玩游戏时不自动息屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val prefs = Prefs(applicationContext)
        setContent { MiniGamesTheme { GameApp(prefs) } }
    }
}
