package com.heasafe.aura.ui.components

import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset

/**
 * iOS 26 液态玻璃「客体连续性」动效：
 * 菜单/面板从工具栏锚点以玻璃水珠形态飞出（scale+slide 弹簧形变），
 * 飞行阶段内容模糊、落定后清晰；关闭时收缩飞回锚点融合。
 * 遵循 Apple HIG motion：purposeful、brief、可取消、尊重 Reduce Motion。
 */

@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember {
        try {
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (_: Exception) {
            false
        }
    }
}

/** 飞行模糊半径：打开时 14→0（落定清晰），关闭时 0→14（飞回模糊）。 */
@Composable
fun rememberFlightBlur(visible: Boolean, reduceMotion: Boolean): Float {
    val target = if (visible) 0f else 14f
    val value by animateFloatAsState(
        targetValue = target,
        animationSpec = if (reduceMotion) tween(0) else tween(200),
        label = "flight-blur"
    )
    return if (reduceMotion) 0f else value
}

/** 菜单进入：从右上锚点（工具栏）弹簧飞出 + 形变放大 + 内容延迟淡入。 */
fun liquidMenuEnter(reduceMotion: Boolean): EnterTransition =
    if (reduceMotion) {
        fadeIn(tween(120))
    } else {
        slideIn(
            initialOffset = { IntOffset(it.width / 3, -it.height / 2) },
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f)
        ) + scaleIn(
            initialScale = 0.2f,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f)
        ) + fadeIn(tween(180, delayMillis = 60))
    }

/** 菜单退出：收缩 + 飞回右上锚点融合 + 内容淡出。 */
fun liquidMenuExit(reduceMotion: Boolean): ExitTransition =
    if (reduceMotion) {
        fadeOut(tween(100))
    } else {
        slideOut(
            targetOffset = { IntOffset(it.width / 3, -it.height / 2) },
            animationSpec = spring(dampingRatio = 0.9f, stiffness = 420f)
        ) + scaleOut(
            targetScale = 0.2f,
            animationSpec = spring(dampingRatio = 0.9f, stiffness = 420f)
        ) + fadeOut(tween(140))
    }

/** 顶部工具层收起/展开：垂直弹簧滑动（向下滚收起、向上滚恢复，符合手势预期）。 */
fun liquidChromeEnter(reduceMotion: Boolean): EnterTransition =
    if (reduceMotion) fadeIn(tween(120))
    else slideIn(
        initialOffset = { IntOffset(0, -it.height) },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 420f)
    ) + fadeIn(tween(160))

fun liquidChromeExit(reduceMotion: Boolean): ExitTransition =
    if (reduceMotion) fadeOut(tween(100))
    else slideOut(
        targetOffset = { IntOffset(0, -it.height) },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 420f)
    ) + fadeOut(tween(140))
