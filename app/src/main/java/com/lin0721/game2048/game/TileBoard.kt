package com.lin0721.game2048.game

import kotlin.random.Random

/**
 * 一个带 id 的方块。
 *
 * id 在整局游戏里唯一，UI 靠它认出"同一个方块"跨移动的身份，
 * 从而知道某个方块是从哪一格滑到哪一格；合并后产生的新方块会拿到一个新的 id。
 */
data class Tile(val id: Int, val value: Int, val row: Int, val col: Int)

/** 一次滑动中某个方块的位移，起点 → 终点。 */
data class Slide(
    val id: Int,
    val value: Int,
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int,
)

/** 两个方块合并成一个：[a]、[b] 各自滑向同一格，合并后的方块用新 id [intoId]。 */
data class Merge(val intoId: Int, val a: Slide, val b: Slide)

/**
 * 一次移动的结果，UI 只靠它就能重放整段滑动动画。
 *
 * @property tiles 移动后的全部方块（不含被吞掉的来源方块）
 * @property slides 存活方块里真正移动过的那些方块的起点
 * @property merges 每组合并的两个来源，用来画"滑进去再合并"的过渡
 * @property spawned 本步随机生成的新方块；AI 推演时为 null
 * @property gained 本步得分
 * @property moved 这一步是否改变了棋盘
 */
data class MoveResult(
    val tiles: List<Tile>,
    val slides: List<Slide>,
    val merges: List<Merge>,
    val gained: Int,
    val moved: Boolean,
    val spawned: Tile? = null,
)

/**
 * 带 id 的棋盘，规则与 [Board] 完全一致，只是额外记录每个方块的 id 与位移。
 *
 * 面值仍然由 [Board] 的 [Board.slideLine] 计算，这里只是把"值"换成"id"再摆一遍，
 * 所以 AI 规则和手动规则永远同步。给 AI / 评估函数用时通过 [toBoard] 派生普通棋盘。
 */
class TileBoard(val tiles: List<Tile>) {

    /** 已分配过的最大 id + 1，合并出来的新方块从这里继续编号。 */
    private val nextId: Int = (tiles.maxOfOrNull { it.id } ?: 0) + 1

    /** 16 格面值，按 row * 4 + col 排列，0 表示空。 */
    fun toCells(): IntArray {
        val cells = IntArray(16)
        for (tile in tiles) cells[tile.row * 4 + tile.col] = tile.value
        return cells
    }

    /** 派生普通棋盘，交给评估函数或 AI 使用。 */
    fun toBoard(): Board = Board(toCells())

    fun canMove(): Boolean = toBoard().canMove()

    /**
     * 执行一次移动，不生成新方块（AI 推演用）。
     * 返回的结果里 [MoveResult.spawned] 为 null。
     */
    fun move(dir: Dir): MoveResult {
        val cells = IntArray(16)
        val ids = IntArray(16)
        for (tile in tiles) {
            val at = tile.row * 4 + tile.col
            cells[at] = tile.value
            ids[at] = tile.id
        }

        val outTiles = ArrayList<Tile>(16)
        val slides = ArrayList<Slide>(8)
        val merges = ArrayList<Merge>(4)
        var next = nextId
        var gained = 0
        var moved = false

        for (i in 0 until 4) {
            val at = IntArray(4) { j -> Board.index(dir, i, j) }
            val values = IntArray(4) { j -> cells[at[j]] }
            val sourceIds = IntArray(4) { j -> ids[at[j]] }
            val result = Board.slideLine(values)
            gained += result.score

            for (j in 0 until 4) {
                val a = result.fromA[j]
                if (a < 0) continue
                val dest = at[j]
                val toRow = dest / 4
                val toCol = dest % 4
                if (result.merged[j]) {
                    val b = result.fromB[j]
                    val id = next++
                    outTiles += Tile(id, result.values[j], toRow, toCol)
                    merges += Merge(
                        intoId = id,
                        a = Slide(sourceIds[a], values[a], at[a] / 4, at[a] % 4, toRow, toCol),
                        b = Slide(sourceIds[b], values[b], at[b] / 4, at[b] % 4, toRow, toCol),
                    )
                    moved = true
                } else {
                    outTiles += Tile(sourceIds[a], result.values[j], toRow, toCol)
                    if (a != j) {
                        slides += Slide(sourceIds[a], values[a], at[a] / 4, at[a] % 4, toRow, toCol)
                        moved = true
                    }
                }
            }
        }
        return MoveResult(outTiles, slides, merges, gained, moved)
    }

    /**
     * 在随机空格放一个 2 或 4，返回新棋盘与这个新方块；没有空位时返回自身与 null。
     */
    fun spawn(rand: Random = Random): Pair<TileBoard, Tile?> {
        val cells = toCells()
        val empty = cells.indices.filter { cells[it] == 0 }
        if (empty.isEmpty()) return this to null
        val at = empty.random(rand)
        val tile = Tile(nextId, if (rand.nextInt(10) == 0) 4 else 2, at / 4, at % 4)
        return TileBoard(tiles + tile) to tile
    }

    /** 存档编码：`id,value,row,col;...`，可以直接放进 SharedPreferences。 */
    fun encode(): String =
        tiles.joinToString(";") { "${it.id},${it.value},${it.row},${it.col}" }

    companion object {
        /** 新开一局：空棋盘上放两个方块。 */
        fun newGame(rand: Random = Random): TileBoard {
            var board = TileBoard(emptyList())
            repeat(2) { board = board.spawn(rand).first }
            return board
        }

        /**
         * 解析 [encode] 的结果；格式不合法返回 null，坏存档不会把游戏卡住。
         * 要求每个方块的面值是 2 的幂、行列不重复、id 不重复。
         */
        fun decode(text: String): TileBoard? {
            if (text.isBlank()) return null
            val tiles = ArrayList<Tile>()
            val ids = HashSet<Int>()
            val cells = HashSet<Int>()
            for (part in text.split(';')) {
                val fields = part.split(',')
                if (fields.size != 4) return null
                val id = fields[0].toIntOrNull() ?: return null
                val value = fields[1].toIntOrNull() ?: return null
                val row = fields[2].toIntOrNull() ?: return null
                val col = fields[3].toIntOrNull() ?: return null
                if (row !in 0 until 4 || col !in 0 until 4) return null
                if (value < 2 || (value and (value - 1)) != 0) return null
                if (!ids.add(id) || !cells.add(row * 4 + col)) return null
                tiles += Tile(id, value, row, col)
            }
            return if (tiles.isEmpty()) null else TileBoard(tiles)
        }
    }
}
