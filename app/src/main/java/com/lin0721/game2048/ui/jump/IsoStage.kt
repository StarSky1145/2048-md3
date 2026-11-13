package com.lin0721.game2048.ui.jump

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.lin0721.game2048.core.JumpPlatform
import kotlin.math.sin

/**
 * 等轴测舞台：纯 Canvas 手写 iso 投影，不引 OpenGL。
 *
 * 世界坐标 (f, s, h)：f 向前、s 横向、h 离地高度，单位 dp。
 * 相机在 -f 方向看向 +f，投影到屏幕：
 *
 *   sx = (df - ds) * ISO_X
 *   sy = -((df + ds) * ISO_Y + h)
 *
 * 再平移到屏幕锚点 (anchorX, anchorY)。当前方块固定落在锚点上（屏幕偏下），
 * 所以"相机跟随"只是把 cameraF 平滑到当前方块的中心。
 *
 * 一个方块就是 8 个顶点：顶面菱形 4 点 + 竖直落下的底面 4 点，
 * 只画顶面、左前侧、右前侧三个面，三面用不同明度表现体积。
 * 顶点在 draw 阶段只做平移缩放，Path 与数组全部复用，帧内零分配。
 */
internal class IsoStage {

    /** 三个面的复用 Path。 */
    private enum class JumpKind { NORMAL, BONUS, SPRING, NARROW, HEAVY }
    private enum class JumpShape { BOX, CYLINDER, HEX }

    private fun kindOf(i: Int): JumpKind = when {
        i < 5 -> JumpKind.NORMAL
        i % 7 == 0 -> JumpKind.BONUS
        i % 6 == 0 -> JumpKind.SPRING
        i % 5 == 0 -> JumpKind.NARROW
        i % 11 == 0 -> JumpKind.HEAVY
        else -> JumpKind.NORMAL
    }

    private fun shapeOf(i: Int): JumpShape = when (i % 3) {
        1 -> JumpShape.CYLINDER
        2 -> JumpShape.HEX
        else -> JumpShape.BOX
    }

    private fun topColor(k: JumpKind): androidx.compose.ui.graphics.Color = when (k) {
        JumpKind.NORMAL -> TOP_IDLE
        JumpKind.BONUS -> androidx.compose.ui.graphics.Color(0xFFE6C35C)
        JumpKind.SPRING -> androidx.compose.ui.graphics.Color(0xFF6FCF97)
        JumpKind.NARROW -> androidx.compose.ui.graphics.Color(0xFFEF9BB6)
        JumpKind.HEAVY -> androidx.compose.ui.graphics.Color(0xFFA88BD6)
    }

    private val guideColor = androidx.compose.ui.graphics.Color(0x99EFB8C8)

    private val faceTop = Path()
    private val faceLeft = Path()
    private val faceRight = Path()

    /** 复用顶点：0..3 顶面（下、右、上、左），4..7 对应的底面。 */
    private val vx = FloatArray(8)
    private val vy = FloatArray(8)

    fun draw(
        scope: DrawScope,
        platforms: List<JumpPlatform>,
        currentIndex: Int,
        cameraF: Float,
        playerF: Float,
        playerH: Float,
        squash: Float,
        sink: Float,
        fallT: Float,
        tilt: Float,
        anchorX: Float,
        anchorY: Float,
        chargeDistance: Float = 0f,
    ) {
        val scale = scope.density
        // 远的先画：平台按 f 递增排列，从最后一块往前画
        for (i in platforms.size - 1 downTo 0) {
            val df = platforms[i].center - cameraF
            val cx = anchorX + df * ISO_X * scale
            if (cx < -160f * scale || cx > scope.size.width + 160f * scale) continue
            val current = i == currentIndex
            val drop = if (current) sink else 0f
            val cy = anchorY - (df * ISO_Y + CUBE_HEIGHT - drop) * scale
            drawCube(scope, cx, cy, scale, current, kindOf(i), shapeOf(i))
        }
        // 蓄力辅助线：从当前方块前沿沿 +f 铺到预计落点，长度就是这一跳能到的位置
        if (chargeDistance > 0f) {
            val base = platforms[currentIndex]
            val from = base.center
            val to = from + chargeDistance
            val y0 = anchorY - ((from - cameraF) * ISO_Y + CUBE_HEIGHT) * scale
            val x0 = anchorX + (from - cameraF) * ISO_X * scale
            val x1 = anchorX + (to - cameraF) * ISO_X * scale
            val y1 = anchorY - ((to - cameraF) * ISO_Y + CUBE_HEIGHT) * scale
            var k = 0f
            while (k < 1f) {
                val a = k
                val b = (k + 0.055f).coerceAtMost(1f)
                drawLine(
                    color = guideColor,
                    start = androidx.compose.ui.geometry.Offset(x0 + (x1 - x0) * a, y0 + (y1 - y0) * a),
                    end = androidx.compose.ui.geometry.Offset(x0 + (x1 - x0) * b, y0 + (y1 - y0) * b),
                    strokeWidth = 2f * scale,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                k += 0.11f
            }
        }
        drawPlayer(
            scope = scope,
            cameraF = cameraF,
            playerF = playerF,
            playerH = playerH,
            squash = squash,
            sink = sink,
            fallT = fallT,
            tilt = tilt,
            anchorX = anchorX,
            anchorY = anchorY,
        )
    }

    private fun drawCube(
        scope: DrawScope,
        cx: Float,
        cy: Float,
        scale: Float,
        current: Boolean,
        kind: JumpKind,
        shape: JumpShape,
    ) {
        val hx = CUBE_HALF * ISO_X * scale
        val hy = CUBE_HALF * ISO_Y * scale
        val down = CUBE_HEIGHT * scale

        // 顶面菱形：下 A、右 B、上 C、左 D
        vx[0] = cx
        vy[0] = cy + 2f * hy
        vx[1] = cx + 2f * hx
        vy[1] = cy
        vx[2] = cx
        vy[2] = cy - 2f * hy
        vx[3] = cx - 2f * hx
        vy[3] = cy
        for (i in 0 until 4) {
            vx[4 + i] = vx[i]
            vy[4 + i] = vy[i] + down
        }

        // 左前侧：A-D-D'-A'
        faceLeft.rewind()
        faceLeft.moveTo(vx[0], vy[0])
        faceLeft.lineTo(vx[3], vy[3])
        faceLeft.lineTo(vx[7], vy[7])
        faceLeft.lineTo(vx[4], vy[4])
        faceLeft.close()
        scope.drawPath(faceLeft, SIDE_LEFT)

        // 右前侧：B-A-A'-B'
        faceRight.rewind()
        faceRight.moveTo(vx[1], vy[1])
        faceRight.lineTo(vx[0], vy[0])
        faceRight.lineTo(vx[4], vy[4])
        faceRight.lineTo(vx[5], vy[5])
        faceRight.close()
        scope.drawPath(faceRight, SIDE_RIGHT)

        // 顶面最后盖上去，边缘自然收口
        faceTop.rewind()
        faceTop.moveTo(vx[0], vy[0])
        faceTop.lineTo(vx[1], vy[1])
        faceTop.lineTo(vx[2], vy[2])
        faceTop.lineTo(vx[3], vy[3])
        faceTop.close()
        scope.drawPath(faceTop, if (current) TOP_CURRENT else topColor(kind))
        scope.drawPath(faceTop, EDGE, style = EDGE_STROKE)

        // 形状装饰：圆柱用椭圆收顶，六棱柱加两条棱线
        when (shape) {
            JumpShape.CYLINDER -> {
                val top = if (current) TOP_CURRENT else topColor(kind)
                scope.drawOval(color = top, topLeft = Offset(cx - hx, cy - hy), size = Size(2f * hx, 2f * hy))
                scope.drawOval(color = EDGE, topLeft = Offset(cx - hx, cy - hy), size = Size(2f * hx, 2f * hy), style = EDGE_STROKE)
            }
            JumpShape.HEX -> {
                scope.drawLine(EDGE, Offset(cx - hx, cy), Offset(cx, cy + 2f * hy), 1.5f * scale)
                scope.drawLine(EDGE, Offset(cx + hx, cy), Offset(cx, cy + 2f * hy), 1.5f * scale)
            }
            JumpShape.BOX -> Unit
        }

        // 中心的落点标记（加分判定半径）
        scope.drawOval(
            color = MARKER,
            topLeft = Offset(cx - 3f * scale, cy - 1.6f * scale),
            size = Size(6f * scale, 3.2f * scale),
        )
    }

    private fun drawPlayer(
        scope: DrawScope,
        cameraF: Float,
        playerF: Float,
        playerH: Float,
        squash: Float,
        sink: Float,
        fallT: Float,
        tilt: Float,
        anchorX: Float,
        anchorY: Float,
    ) {
        val scale = scope.density
        val df = playerF - cameraF
        val surface = CUBE_HEIGHT - sink
        val baseX = anchorX + df * ISO_X * scale
        val baseY = anchorY - (df * ISO_Y + surface + playerH) * scale
        val w = PLAYER_WIDTH * (1f + 0.16f * squash) * scale
        val h = PLAYER_HEIGHT * (1f - 0.34f * squash) * scale
        val dome = w * 0.82f

        // 脚下阴影：跳得越高越小越淡
        if (fallT <= 0f) {
            val shrink = (1f - playerH / 260f).coerceIn(0.4f, 1f)
            val shadowY = anchorY - (df * ISO_Y + surface) * scale
            scope.drawOval(
                color = SHADOW,
                topLeft = Offset(baseX - w * 0.72f * shrink, shadowY - 2.6f * scale),
                size = Size(w * 1.44f * shrink, 5.2f * scale),
            )
        }

        if (tilt != 0f) {
            scope.rotate(tilt, Offset(baseX, baseY - h * 0.5f)) {
                capsule(scope, baseX, baseY, w, h, dome)
            }
        } else {
            capsule(scope, baseX, baseY, w, h, dome)
        }
    }

    /** 胶囊体 = 圆柱筒身 + 半球顶 + 一点高光。 */
    private fun capsule(scope: DrawScope, baseX: Float, baseY: Float, w: Float, h: Float, dome: Float) {
        val top = baseY - h
        scope.drawRoundRect(
            color = PLAYER_BODY,
            topLeft = Offset(baseX - w / 2f, top + dome / 2f),
            size = Size(w, h - dome / 2f),
            cornerRadius = CornerRadius(w / 2f, w / 2f),
        )
        scope.drawOval(
            color = PLAYER_TOP,
            topLeft = Offset(baseX - w / 2f, top),
            size = Size(w, dome),
        )
        scope.drawOval(
            color = PLAYER_GLOW,
            topLeft = Offset(baseX - w * 0.32f, top + dome * 0.3f),
            size = Size(w * 0.26f, dome * 0.42f),
        )
        // 底部收口，让圆柱看起来落在方块上
        scope.drawOval(
            color = PLAYER_BODY_DIM,
            topLeft = Offset(baseX - w / 2f, baseY - w * 0.16f),
            size = Size(w, w * 0.32f),
        )
    }

    private companion object {
        /** 投影系数（每 dp 世界坐标对应的屏幕 dp）。 */
        const val ISO_X = 0.86f
        const val ISO_Y = 0.46f

        /** 方块半宽与高度（世界 dp）。 */
        const val CUBE_HALF = 28f
        const val CUBE_HEIGHT = 22f

        /** 角色宽高（世界 dp）。 */
        const val PLAYER_WIDTH = 17f
        const val PLAYER_HEIGHT = 27f

        val TOP_CURRENT = Color(0xFF544C58)
        val TOP_IDLE = Color(0xFF3E3742)
        val SIDE_RIGHT = Color(0xFF2C2731)
        val SIDE_LEFT = Color(0xFF221E27)
        val EDGE = Color(0xFF8E8494).copy(alpha = 0.25f)
        val EDGE_STROKE = Stroke(width = 1f)
        val MARKER = Color(0xFFEFB8C8).copy(alpha = 0.7f)
        val PLAYER_BODY = Color(0xFFE8A9BC)
        val PLAYER_BODY_DIM = Color(0xFFC98A9C)
        val PLAYER_TOP = Color(0xFFFFD3DE)
        val PLAYER_GLOW = Color.White.copy(alpha = 0.3f)
        val SHADOW = Color.Black.copy(alpha = 0.34f)
    }
}

/** 抛物线抬升：t 从 0 到 1，两端为 0。 */
internal fun parabola(t: Float, apex: Float): Float = sin(PI.toFloat() * t) * apex

private const val PI = 3.1415927f
