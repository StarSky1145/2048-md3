package com.lin0721.game2048.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.game2048.data.Prefs

/**
 * 游戏选择页：卡片列表，每张卡显示名称、玩法和该游戏的最高分。
 *
 * 最高分都从 [Prefs] 实时读取，所以从游戏里返回时卡片会立刻刷新。
 */
@Composable
fun HubScreen(
    prefs: Prefs,
    onOpen2048: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                "小游戏", color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp, fontWeight = FontWeight.Bold,
            )
            Text(
                "挑一个开始，随时回来", color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )

            Spacer(Modifier.height(16.dp))

            GameCard(
                name = "2048",
                subtitle = "合并方块，凑出 2048",
                best = "最高分 ${prefs.bestScore}",
                badge = "2048",
                badgeColor = Color(0xFFB4643C),
                onClick = onOpen2048,
            )
        }
    }
}

@Composable
private fun GameCard(
    name: String,
    subtitle: String,
    best: String,
    badge: String,
    badgeColor: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(badgeColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                badge, color = Color.White,
                fontSize = if (badge.length > 2) 12.sp else 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                name, color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp, fontWeight = FontWeight.Bold,
            )
            Text(
                subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                best, color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp, fontWeight = FontWeight.Medium,
            )
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 22.sp)
        }
    }
}
