package com.lin0721.game2048.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 全应用统一的页面骨架。
 *
 * 背景用 [Surface] 铺满整块屏幕（包括状态栏、手势条与圆角下面），
 * 内容层统一内缩 `safeDrawing`：上面避开状态栏与屏幕圆角，下面避开手势条。
 * 各页面只在这个 Box 里排内容，不再各自处理 inset。
 */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), content = content)
    }
}
