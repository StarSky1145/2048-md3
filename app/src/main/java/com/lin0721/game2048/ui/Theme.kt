package com.lin0721.game2048.ui

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** 只做深色：2048 的方块配色在深底上层次最清楚。 */
private val DarkScheme = darkColorScheme(
    primary = Color(0xFFEFB8C8),
    onPrimary = Color(0xFF4A2530),
    background = Color(0xFF121014),
    surface = Color(0xFF1B181D),
    surfaceVariant = Color(0xFF262229),
    onBackground = Color(0xFFEDE0E3),
    onSurface = Color(0xFFEDE0E3),
    onSurfaceVariant = Color(0xFFC9BFC3),
)

@Composable
fun Game2048Theme(content: @Composable () -> Unit) {
    val scheme = if (Build.VERSION.SDK_INT >= 31) dynamicDarkColorScheme(LocalContext.current) else DarkScheme
    MaterialTheme(colorScheme = scheme, content = content)
}
