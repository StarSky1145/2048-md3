package com.lin0721.game2048.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.game2048.data.LayoutMode

/**
 * 界面布局的尺寸集合，由 [LocalUiMetrics] 下发到各个页面。
 *
 * 标准档就是当前的样子；紧凑档把边距减半、标题字号 -2sp、按钮高度再矮 4dp，
 * 给棋盘和网格多让出一点空间。
 */
class UiMetrics(val mode: LayoutMode) {

    val compact: Boolean get() = mode == LayoutMode.COMPACT

    /** 边距：紧凑档减半。 */
    fun space(value: Dp): Dp = if (compact) value / 2 else value

    /** 标题字号：紧凑档 -2sp。 */
    fun title(value: TextUnit): TextUnit = if (compact) (value.value - 2f).sp else value

    /** 按钮高度：紧凑档比标准矮 4dp（标准 44dp 的按钮就是 44 → 40）。 */
    fun buttonHeight(standard: Dp = 44.dp): Dp = if (compact) standard - 4.dp else standard
}

val LocalUiMetrics = staticCompositionLocalOf { UiMetrics(LayoutMode.STANDARD) }
