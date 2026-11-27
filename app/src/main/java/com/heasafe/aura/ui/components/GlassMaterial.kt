package com.heasafe.aura.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * iOS 27 Liquid Glass（Apple HIG Materials）：
 *  - regular 变体：真实背景模糊 + 亮度调整（Haze），用于内容上方的功能层；
 *  - clear 变体：高透 + 高光/折射边缘，用于媒体（视频/图片）上方；
 *  - 玻璃仅用于功能层（工具栏/TabBar/面板），内容层用标准材质。
 * 高光（specular）+ 发丝折射描边模拟玻璃边缘反光。
 */

private fun DrawScope.drawSpecular(dark: Boolean) {
    // 顶部高光：玻璃上缘的反光 sheen
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (dark) 0.10f else 0.45f),
                Color.White.copy(alpha = 0f)
            ),
            startY = 0f,
            endY = size.height * 0.5f
        ),
        size = Size(size.width, size.height * 0.5f)
    )
    // 上缘一条更亮的折射边
    drawRect(
        color = Color.White.copy(alpha = if (dark) 0.18f else 0.65f),
        size = Size(size.width, 1.dp.toPx())
    )
}

/** clear 变体：高透玻璃 + 高光/折射边（媒体上方）。 */
@Composable
fun Modifier.liquidGlass(shape: Shape = CircleShape): Modifier {
    val dark = isSystemInDarkTheme()
    val body = if (dark) {
        Brush.verticalGradient(
            listOf(Color(0xFF3A3A3C).copy(alpha = 0.55f), Color(0xFF1C1C1E).copy(alpha = 0.45f))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.62f), Color.White.copy(alpha = 0.42f))
        )
    }
    val edge = if (dark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.75f)
    return this
        .clip(shape)
        .background(body)
        .drawBehind { drawSpecular(dark) }
        .border(0.5.dp, edge, shape)
}

/** regular 变体：Haze 真实背景模糊 + 亮度 tint + 高光/折射边（内容上方）。 */
@Composable
fun Modifier.hazeGlass(state: HazeState, shape: Shape = CircleShape): Modifier {
    val dark = isSystemInDarkTheme()
    val base = if (dark) Color(0xFF1C1C1E) else Color.White
    val edge = if (dark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.75f)
    return this
        .hazeChild(
            state = state,
            style = HazeStyle(
                backgroundColor = base,
                tint = HazeTint(base.copy(alpha = if (dark) 0.55f else 0.62f)),
                blurRadius = 28.dp
            )
        )
        .clip(shape)
        .drawBehind { drawSpecular(dark) }
        .border(0.5.dp, edge, shape)
}
