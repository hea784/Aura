package com.heasafe.aura.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * iOS 27 Liquid Glass 材质（仅用于功能层：工具栏 / TabBar / 面板 / 悬浮控件）。
 * 半透明 + 顶部高光 + 发丝描边，模拟玻璃折射；内容层不使用玻璃（HIG 层纪律）。
 */
@Composable
fun Modifier.liquidGlass(shape: Shape = CircleShape): Modifier {
    val dark = isSystemInDarkTheme()
    val body = if (dark) {
        Brush.verticalGradient(
            listOf(Color(0xFF3A3A3C).copy(alpha = 0.72f), Color(0xFF1C1C1E).copy(alpha = 0.66f))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.82f), Color.White.copy(alpha = 0.62f))
        )
    }
    val edge = if (dark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.7f)
    return this
        .clip(shape)
        .background(body)
        .border(0.5.dp, edge, shape)
}
