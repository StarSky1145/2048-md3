package com.lin0721.game2048.ui.jump

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.game2048.core.Jump
import com.lin0721.game2048.core.JumpGame
import com.lin0721.game2048.core.JumpResult
import com.lin0721.game2048.core.JumpStatus
import com.lin0721.game2048.data.Prefs
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private const val CHARGE_MS = 900f
private const val JUMP_ANIM_MS = 340
private const val PLAYER_RADIUS = 13f
private const val PLATFORM_HEIGHT = 12f
private const val ARC_HEIGHT = 110f

/** 一次跳跃的动画数据：从旧位置飞到落点，动画结束后才落地。 */
private data class JumpAnim(
    val fromX: Float,
    val toX: Float,
    val result: JumpResult,
)

/** 跳一跳页：按住蓄力、松手起跳，落地结算并更新最高分。 */
@Composable
fun JumpScreen(prefs: Prefs, onBack: () -> Unit) {
    var game by remember { mutableStateOf(Jump.newGame()) }
    var charging by remember { mutableStateOf(false) }
    var power by remember { mutableFloatStateOf(0f) }
    var anim by remember { mutableStateOf<JumpAnim?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var newRecord by remember { mutableStateOf(false) }

    val jumpProgress = remember { Animatable(1f) }

    BackHandler(onBack = onBack)

    // 蓄力进度：按住后 0.9 秒到满
    LaunchedEffect(charging) {
        if (!charging) return@LaunchedEffect
        val started = System.currentTimeMillis()
        while (charging) {
            val progress = ((System.currentTimeMillis() - started) / CHARGE_MS).coerceIn(0f, 1f)
            power = progress
            if (progress >= 1f) break
            delay(16)
        }
    }

    // 跳跃动画放完再应用落点结果，角色不会中途瞬移
    LaunchedEffect(anim) {
        val current = anim ?: return@LaunchedEffect
        jumpProgress.snapTo(0f)
        jumpProgress.animateTo(1f, tween(JUMP_ANIM_MS, easing = LinearEasing))
        game = current.result.next
        if (game.score > prefs.bestJump) {
            prefs.bestJump = game.score
            newRecord = true
        }
        if (game.status == JumpStatus.OVER) showResult = true
        anim = null
    }

    fun restart() {
        game = Jump.newGame()
        charging = false
        power = 0f
        anim = null
        showResult = false
        newRecord = false
    }

    fun release() {
        if (game.status != JumpStatus.PLAYING || anim != null) return
        val distance = Jump.MIN_JUMP + power * (Jump.MAX_JUMP - Jump.MIN_JUMP)
        val result = Jump.jump(game, distance)
        power = 0f
        anim = JumpAnim(game.playerX, result.landingX, result)
    }

    val currentAnim = anim
    val progress = jumpProgress.value
    val displayX = if (currentAnim != null) {
        currentAnim.fromX + (currentAnim.toX - currentAnim.fromX) * progress
    } else {
        game.playerX
    }
    val lift = if (currentAnim != null) {
        (sin(PI * progress) * ARC_HEIGHT).toFloat()
    } else {
        0f
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            JumpStage(
                game = game,
                playerX = displayX,
                playerLift = lift,
                modifier = Modifier.fillMaxSize().pointerInput(game.status, anim) {
                    if (game.status != JumpStatus.PLAYING || anim != null) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            charging = true
                            power = 0f
                            tryAwaitRelease()
                            charging = false
                            release()
                        },
                    )
                },
            )

            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回首页",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "跳一跳", color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "按住蓄力，松手起跳", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "得分 ${game.score}", color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "最高 ${prefs.bestJump}", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        if (charging) "蓄力 ${(power * 100).roundToInt()}%" else "按住蓄力，松手起跳",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.fillMaxWidth().height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        if (power > 0f) {
                            Box(
                                Modifier.fillMaxWidth(power.coerceIn(0f, 1f)).fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showResult) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    "游戏结束", color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    "得分 ${game.score}" + if (newRecord) "，新纪录！" else "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = { restart() }) { Text("再来一局") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showResult = false
                    onBack()
                }) { Text("返回首页") }
            },
        )
    }
}

/** 画布：平台、中心奖励点、角色与落地影子，坐标以 dp 世界坐标换算。 */
@Composable
private fun JumpStage(
    game: JumpGame,
    playerX: Float,
    playerLift: Float,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val scale = density
        val groundY = size.height * 0.72f
        val camera = playerX - (size.width / scale) * 0.28f
        val platformTop = groundY - PLATFORM_HEIGHT * scale
        val radius = PLAYER_RADIUS * scale

        for (platform in game.platforms) {
            val left = (platform.left - camera) * scale
            val width = platform.width * scale
            if (left + width < 0f || left > size.width) continue
            drawRoundRect(
                color = Color(0xFF3A3540),
                topLeft = Offset(left, platformTop),
                size = Size(width, PLATFORM_HEIGHT * scale),
                cornerRadius = CornerRadius(6f * scale),
            )
            drawCircle(
                color = Color(0xFFEFB8C8).copy(alpha = 0.7f),
                radius = 2.5f * scale,
                center = Offset((platform.center - camera) * scale, platformTop - 6f * scale),
            )
        }

        val playerCenter = Offset(
            x = (playerX - camera) * scale,
            y = groundY - playerLift * scale - radius,
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.25f),
            radius = radius * 0.75f,
            center = Offset(playerCenter.x, groundY - 2f * scale),
        )
        drawCircle(color = Color(0xFFEFB8C8), radius = radius, center = playerCenter)
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = radius * 0.28f,
            center = Offset(playerCenter.x - radius * 0.3f, playerCenter.y - radius * 0.3f),
        )
    }
}
