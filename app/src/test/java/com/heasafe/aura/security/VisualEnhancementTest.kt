package com.heasafe.aura.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualEnhancementTest {

    @Test
    fun offHasNeutralFilter() {
        // OFF 仅关闭增强开关，参数本身保持默认；WebView 侧在 enabled=false 时不注入滤镜
        assertEquals(false, VisualEnhancement.OFF.enabled)
        assertEquals("contrast(1.06) saturate(1.12) brightness(1.0)", VisualEnhancement.OFF.toCssFilter())
    }

    @Test
    fun balancedEnhancesContrastAndSaturation() {
        val css = VisualEnhancement.BALANCED.toCssFilter()
        assertTrue(css.contains("contrast(1.06)"))
        assertTrue(css.contains("saturate(1.12)"))
    }

    @Test
    fun vividIsStrongerThanBalanced() {
        assertTrue(VisualEnhancement.VIVID.contrast > VisualEnhancement.BALANCED.contrast)
        assertTrue(VisualEnhancement.VIVID.saturate > VisualEnhancement.BALANCED.saturate)
        assertTrue(VisualEnhancement.VIVID.sharpness > VisualEnhancement.BALANCED.sharpness)
    }
}
