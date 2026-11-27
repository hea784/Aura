package com.heasafe.aura.core

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.widget.ImageView

/**
 * 视觉增强参数。参考 Kazumi 的画质增强思路（Anime4K CNN 锐化链），
 * 在 Android 侧以轻量 ColorMatrix 卷积近似 + 对比度/饱和度调节实现，
 * 避免引入重型 native 着色器依赖，兼顾 WebView 与图片显示。
 */
data class VisualEnhancement(
    val enabled: Boolean = false,
    val contrast: Float = 1.06f,
    val saturate: Float = 1.12f,
    val brightness: Float = 1.0f,
    val sharpness: Float = 0.35f
) {
    companion object {
        val OFF = VisualEnhancement(enabled = false)
        val BALANCED = VisualEnhancement(enabled = true, contrast = 1.06f, saturate = 1.12f, sharpness = 0.35f)
        val VIVID = VisualEnhancement(enabled = true, contrast = 1.14f, saturate = 1.28f, brightness = 1.02f, sharpness = 0.55f)
    }

    /** 生成用于 WebView CSS filter 的字符串。 */
    fun toCssFilter(): String =
        "contrast($contrast) saturate($saturate) brightness($brightness)"

    /** 生成用于 ImageView 的锐化 + 色彩增强 ColorMatrix。 */
    fun toColorMatrix(): ColorMatrix {
        val s = saturate
        val c = contrast
        val b = brightness
        // 亮度保持的饱和度矩阵（BT.709 权重）
        val lumR = (1f - s) * 0.2126f
        val lumG = (1f - s) * 0.7152f
        val lumB = (1f - s) * 0.0722f
        // 锐化近似：在色彩矩阵上叠加轻微边缘增益（通过对比度放大实现）
        val sharp = 1f + sharpness * 0.25f
        return ColorMatrix(
            floatArrayOf(
                (lumR + s) * c * sharp, lumG * c * sharp, lumB * c * sharp, 0f, (b - 1f) * 255f,
                lumR * c * sharp, (lumG + s) * c * sharp, lumB * c * sharp, 0f, (b - 1f) * 255f,
                lumR * c * sharp, lumG * c * sharp, (lumB + s) * c * sharp, 0f, (b - 1f) * 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }
}

fun ImageView.applyEnhancement(enhancement: VisualEnhancement) {
    if (enhancement.enabled) colorFilter = ColorMatrixColorFilter(enhancement.toColorMatrix())
    else clearColorFilter()
}
