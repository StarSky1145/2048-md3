package com.lin0721.game2048.data

import android.content.Context

/**
 * 本地持久化：设置、最高分与未结束的牌局。
 *
 * 只存几个标量和一段棋盘字符串，用 SharedPreferences 就够了；
 * 写入统一走 apply()，不阻塞主线程，也不额外引入依赖。
 */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var animate: Boolean
        get() = sp.getBoolean(KEY_ANIMATE, true)
        set(value) {
            sp.edit().putBoolean(KEY_ANIMATE, value).apply()
        }

    var animationMs: Int
        get() = sp.getInt(KEY_ANIMATION_MS, DEFAULT_ANIMATION_MS)
        set(value) {
            sp.edit().putInt(KEY_ANIMATION_MS, value).apply()
        }

    var stepMs: Int
        get() = sp.getInt(KEY_STEP_MS, DEFAULT_STEP_MS)
        set(value) {
            sp.edit().putInt(KEY_STEP_MS, value).apply()
        }

    var bestScore: Int
        get() = sp.getInt(KEY_BEST_SCORE, 0)
        set(value) {
            sp.edit().putInt(KEY_BEST_SCORE, value).apply()
        }

    /** 当前牌局的棋盘编码，格式见 [com.lin0721.game2048.game.TileBoard.encode]。 */
    var savedGame: String?
        get() = sp.getString(KEY_SAVED_GAME, null)
        set(value) {
            sp.edit().putString(KEY_SAVED_GAME, value).apply()
        }

    var savedScore: Int
        get() = sp.getInt(KEY_SAVED_SCORE, 0)
        set(value) {
            sp.edit().putInt(KEY_SAVED_SCORE, value).apply()
        }

    /** 清掉当前局，只保留最高分；死局或开新局时用。 */
    fun clearSavedGame() {
        sp.edit().remove(KEY_SAVED_GAME).remove(KEY_SAVED_SCORE).apply()
    }

    companion object {
        const val FAST_ANIMATION_MS = 80
        const val DEFAULT_ANIMATION_MS = 140
        const val SLOW_ANIMATION_MS = 240
        const val DEFAULT_STEP_MS = 150

        private const val FILE = "game2048"
        private const val KEY_ANIMATE = "animate"
        private const val KEY_ANIMATION_MS = "animation_ms"
        private const val KEY_STEP_MS = "step_ms"
        private const val KEY_BEST_SCORE = "best_score"
        private const val KEY_SAVED_GAME = "saved_game"
        private const val KEY_SAVED_SCORE = "saved_score"
    }
}
