package com.lin0721.game2048.data

/**
 * 界面布局档位：标准保留当前边距与字号，紧凑把边距减半、标题 -2sp，
 * 尽量把屏幕空间让给棋盘与网格。
 */
enum class LayoutMode(val label: String) {
    STANDARD("标准"),
    COMPACT("紧凑"),
    ;

    /** 存档损坏或旧版本没有这个键时退回标准。 */
    companion object {
        fun of(name: String?): LayoutMode = entries.firstOrNull { it.name == name } ?: STANDARD
    }
}
