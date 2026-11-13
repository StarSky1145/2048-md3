package com.lin0721.game2048.ui.jump

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.game2048.core.Jump
import com.lin0721.game2048.core.JumpResult
import com.lin0721.game2048.core.JumpStatus
import com.lin0721.game2048.data.Prefs
import com.lin0721.game2048.ui.LocalUiMetrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.roundToInt

/** 蓄满力的时长（毫秒）。 */
private const val CHARGE_MS = 900f

/** 一次跳跃的动画时长（毫秒）。 */
private const val FLIGHT_MS = 380

/** 摔下去到弹失败框的时长（毫秒）。 */
private const val FAIL_MS = 800

/** 一次跳跃的动画数据：起飞点、落点、抛物线顶点与结算结果。 */
private data class Flight(
    val fromF: Float,
    val toF: Float,
    val apex: Float,
    val result: JumpResult,
)

/**
 * 跳一跳页：等轴测 3D 渲染，按住蓄力、松手起跳。
 *
 * 手感参数沿用老版本（0.9 秒满蓄力、110–240 的平台间距、落中心加分），
 * 只把表现换成 3D：落地后相机平移到新方块偏下的锚点，角色在 3D 空间走抛物线，
 * 失败时向前翻滚摔出画面，0.8 秒后弹结算框。
 */
@Composable
fun JumpScreen(prefs: Prefs, onBack: () -> Unit) {
    var game by remember { mutableStateOf(Jump.newGame()) }
    var gameSeq by remember { mutableIntStateOf(0) }
    var charging by remember { mutableStateOf(false) }
    var power by remember { mutableFloatStateOf(0f) }
    // 用自增序号而不是布尔量触发蓄力：布尔量在「上次按压被取消、charging 卡在 true」时
    // 再按下去值不变，LaunchedEffect 不会重启，表现就是偶发蓄不上力
    var chargeSeq by remember { mutableIntStateOf(0) }
    var flight by remember { mutableStateOf<Flight?>(null) }
    val scope = rememberCoroutineScope()
    var deadF by remember { mutableFloatStateOf(0f) }
    var showResult by remember { mutableStateOf(false) }
    var newRecord by remember { mutableStateOf(false) }

    val jumpT = remember { Animatable(0f) }
    val fallT = remember { Animatable(0f) }
    val rebound = remember { Animatable(1f) }
    val camera = remember { Animatable(game.platforms[0].center) }
    val stage = remember { IsoStage() }
    val ui = LocalUiMetrics.current

    BackHandler(onBack = onBack)

    // 蓄力进度：按住后 0.9 秒到满
    LaunchedEffect(chargeSeq) {
        if (!charging) return@LaunchedEffect
        val started = System.nanoTime()
        while (true) {
            val elapsedMs = (System.nanoTime() - started) / 1_000_000f
            power = (elapsedMs / CHARGE_MS).coerceIn(0f, 1f)
            if (power >= 1f) break
            delay(16)
        }
    }

    // 重开一局时把动画状态一起归零，避免上一局的摔落进度残留
    LaunchedEffect(gameSeq) {
        jumpT.snapTo(0f)
        fallT.snapTo(0f)
        rebound.snapTo(1f)
    }

    // 落地后整体平移，把新方块送回屏幕偏下的锚点
    LaunchedEffect(gameSeq, game.current) {
        val target = game.platforms[game.current].center
        if (game.current == 0) {
            camera.snapTo(target)
        } else {
            camera.animateTo(target, tween(300, easing = FastOutSlowInEasing))
        }
    }

    // 飞行放完再结算；失败再补一段翻滚下落
    LaunchedEffect(flight) {
        val current = flight ?: return@LaunchedEffect
        jumpT.snapTo(0f)
        fallT.snapTo(0f)
        jumpT.animateTo(1f, tween(FLIGHT_MS, easing = LinearEasing))
        game = current.result.next
        if (game.score > prefs.bestJump) {
            prefs.bestJump = game.score
            newRecord = true
        }
        if (game.status == JumpStatus.OVER) {
            deadF = current.result.landingX
            fallT.animateTo(1f, tween(FAIL_MS, easing = FastOutSlowInEasing))
            showResult = true
        } else {
            // 落地压扁再回弹
            rebound.snapTo(0.68f)
            rebound.animateTo(
                1f,
                spring(dampingRatio = 0.36f, stiffness = Spring.StiffnessMediumLow),
            )
        }
        flight = null
    }

    fun restart() {
        game = Jump.newGame()
        gameSeq++
        charging = false
        power = 0f
        flight = null
        deadF = 0f
        showResult = false
        newRecord = false
    }

    fun release() {
        if (game.status != JumpStatus.PLAYING || flight != null) return
        val distance = Jump.MIN_JUMP + power * (Jump.MAX_JUMP - Jump.MIN_JUMP)
        val result = Jump.jump(game, distance)
        power = 0f
        // 先把飞行进度归零，再挂上 flight：否则这一帧用的还是上一跳的 jumpT(=1)，
        // 角色会在终点闪一下才回到起点
        val pending = Flight(
            fromF = game.playerX,
            toF = result.landingX,
            apex = 90f + distance * 0.45f,
            result = result,
        )
        scope.launch {
            jumpT.snapTo(0f)
            flight = pending
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(game.status, flight) {
                    if (game.status != JumpStatus.PLAYING || flight != null) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            charging = true
                            power = 0f
                            chargeSeq++
                            tryAwaitRelease()
                            charging = false
                            release()
                        },
                    )
                },
            ) {
                val anchorX = size.width * 0.30f
                val anchorY = size.height * 0.68f

                val charge = if (charging) power else 0f
                val squash = if (charging) charge else (1f - rebound.value).coerceIn(0f, 1f)
                val t = jumpT.value
                val fall = fallT.value
                val flying = flight

                val playerF: Float
                val playerH: Float
                var tilt = 0f
                if (fall > 0f) {
                    // 失败：继续往前、加速下落、同时翻滚
                    playerF = deadF + fall * 130f
                    playerH = -fall * fall * 360f
                    tilt = fall * 330f
                } else if (flying != null) {
                    playerF = flying.fromF + (flying.toF - flying.fromF) * t
                    playerH = parabola(t, flying.apex)
                } else {
                    playerF = game.playerX
                    playerH = 0f
                }

                stage.draw(
                    scope = this,
                    platforms = game.platforms,
                    currentIndex = game.current,
                    cameraF = camera.value,
                    playerF = playerF,
                    playerH = playerH,
                    squash = squash,
                    sink = charge * 5f,
                    fallT = if (fall > 0f) 1f else 0f,
                    tilt = tilt,
                    anchorX = anchorX,
                    chargeDistance = if (charging) Jump.MIN_JUMP + power * (Jump.MAX_JUMP - Jump.MIN_JUMP) else 0f,
                    anchorY = anchorY,
                )
            }

            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = ui.space(8.dp), vertical = ui.space(4.dp)),
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
                            fontSize = ui.title(22.sp), fontWeight = FontWeight.Bold,
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

                // 蓄力条单独成一个 composable，读 power 时只有它重组，画布不受影响
                ChargeBar(charging = charging, power = { power })
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

/** 蓄力指示：单独重组，避免按住时整页跟着重组。 */
@Composable
private fun ChargeBar(charging: Boolean, power: () -> Float) {
    val ui = LocalUiMetrics.current
    val p = if (charging) power().coerceIn(0f, 1f) else 0f
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = ui.space(20.dp), vertical = ui.space(12.dp)),
    ) {
        Text(
            if (charging) "蓄力 ${(p * 100).roundToInt()}%" else "按住蓄力，松手起跳",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth().height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (p > 0f) {
                Box(
                    Modifier.fillMaxWidth(p).fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}
