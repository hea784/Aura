package com.heasafe.aura.ui

import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoSettings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heasafe.aura.core.VisualEnhancement
import com.heasafe.aura.security.SecureWebView
import com.heasafe.aura.ui.components.SegmentedControl
import com.heasafe.aura.ui.components.liquidGlass
import com.heasafe.aura.ui.components.liquidChromeEnter
import com.heasafe.aura.ui.components.liquidChromeExit
import com.heasafe.aura.ui.components.liquidMenuEnter
import com.heasafe.aura.ui.components.liquidMenuExit
import com.heasafe.aura.ui.components.rememberFlightBlur
import com.heasafe.aura.ui.components.rememberReduceMotion
import com.heasafe.aura.ui.theme.AuraBlue
import com.heasafe.aura.ui.theme.AuraGreen

/** 浏览页：安全 WebView + 双视频模式 + 画质增强。 */
@Composable
fun BrowseScreen(
    url: String,
    siteTitle: String,
    initialMode: String,
    viewModel: AuraViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videoMode by remember { mutableStateOf(initialMode) } // auto | short | normal
    var enhancement by remember { mutableStateOf(VisualEnhancement.BALANCED) }
    var showEnhancePanel by remember { mutableStateOf(false) }
    var showSecurityPanel by remember { mutableStateOf(false) }
    var blockedReason by remember { mutableStateOf<String?>(null) }
    var videoScanResult by remember { mutableStateOf<String?>(null) } // 探测到的视频摘要
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var chromeVisible by remember { mutableStateOf(true) } // 顶部工具层可见性
    var blockedCount by remember { mutableStateOf(0) } // 本页已拦截广告/跟踪数
    val currentHost = remember(url) {
        runCatching { java.net.URI(url).host?.lowercase() }.getOrNull() ?: url
    }

    val secureWebView = remember(
        settings.strictHttps, settings.antiFingerprint, settings.autoHideChrome, settings.allowedHosts
    ) {
        SecureWebView(
            context = context,
            securityLog = viewModel.securityLog,
            onBlocked = { reason -> blockedReason = reason },
            onPageFinished = { },
            onScroll = { y, old ->
                // 向下滚动收起工具层，向上滚动恢复（可设置关闭）
                if (settings.autoHideChrome) {
                    if (y > old + 24) chromeVisible = false
                    else if (y < old - 24) chromeVisible = true
                }
            },
            strictHttps = settings.strictHttps,
            isHostAllowed = { host -> settings.allowedHosts.contains(host) },
            onTrackerBlocked = { blockedCount++ },
            onMessage = { raw ->
                // 解析 AuraBridge 回传的视频探测结果，仅读取元数据
                try {
                    val obj = org.json.JSONObject(raw)
                    if (obj.optString("kind") == "aura-video-scan") {
                        val count = obj.optInt("count", 0)
                        val mode = obj.optString("suggestedMode", "normal")
                        val streams = obj.optJSONArray("streams")?.length() ?: 0
                        videoScanResult = if (count > 0) {
                            "检测到 $count 个视频${if (streams > 0) " · $streams 条视频流" else ""} · 建议${if (mode == "short") "短视频" else "普通视频"}模式"
                        } else if (streams > 0) {
                            "检测到 $streams 条视频流（页面内嵌播放）"
                        } else null
                    }
                } catch (_: Exception) { /* 非探测消息，忽略 */ }
            }
        )
    }

    // 系统返回键：优先网页内回退，无可回退时关闭浏览页回到空间（符合人类习惯）
    BackHandler {
        if (secureWebView.webView.canGoBack()) secureWebView.webView.goBack() else onClose()
    }

    // 根据模式选择 effective mode；自动模式默认按普通视频呈现
    val effectiveShort = videoMode == "short"

    DisposableEffect(secureWebView) {
        secureWebView.load(url)
        if (effectiveShort) secureWebView.enableShortVideoMode()
        if (settings.antiFingerprint) secureWebView.injectAntiFingerprint()
        onDispose { secureWebView.purge() }
    }

    // 画质增强开关：仅应用/清除滤镜，不重载页面
    LaunchedEffect(settings.enhance, enhancement) {
        if (settings.enhance) {
            secureWebView.applyVisualEnhancement(enhancement.contrast, enhancement.saturate, enhancement.brightness)
        } else {
            secureWebView.clearVisualEnhancement()
        }
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // WebView 铺满
        AndroidView(
            factory = { secureWebView.webView.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            } },
            modifier = Modifier.fillMaxSize()
        )

        // 顶部玻璃工具栏（悬浮，可隐藏：滚动自动收起 + 手动开关，液态玻璃弹簧动效）
        val reduceMotion = rememberReduceMotion()
        val chromeBlur = rememberFlightBlur(chromeVisible, reduceMotion)
        AnimatedVisibility(
            visible = chromeVisible,
            enter = liquidChromeEnter(reduceMotion),
            exit = liquidChromeExit(reduceMotion),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
        Column(
            Modifier
                .fillMaxWidth()
                .blur(chromeBlur.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0f)
                        )
                    )
                )
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassCircleButton(icon = Icons.AutoMirrored.Rounded.ArrowBack) { onClose() }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(siteTitle, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                    Text(
                        url.replace("https://", "").replace("http://", "").take(42),
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
                    )
                }
                GlassCircleButton(icon = Icons.Rounded.Security, tint = AuraGreen) { showSecurityPanel = true }
                Spacer(Modifier.width(6.dp))
                GlassCircleButton(icon = Icons.Rounded.AutoAwesome, tint = if (enhancement.enabled) AuraBlue else MaterialTheme.colorScheme.onSurfaceVariant) {
                    showEnhancePanel = !showEnhancePanel
                }
                Spacer(Modifier.width(6.dp))
                GlassCircleButton(icon = Icons.Rounded.KeyboardArrowUp) { chromeVisible = false }
            }
            Spacer(Modifier.height(10.dp))
            // 视频模式切换
            SegmentedControl(
                items = listOf("auto" to "自动", "short" to "短视频", "normal" to "普通视频"),
                selected = videoMode,
                onSelect = { mode ->
                    videoMode = mode
                    viewModel.setVideoModeOnCurrent(mode)
                    if (mode == "short") {
                        secureWebView.enableShortVideoMode()
                    } else {
                        // 普通/自动模式下重新应用画质滤镜（短视频模式可能改动过样式）
                        secureWebView.clearVisualEnhancement()
                        secureWebView.applyVisualEnhancement(
                            enhancement.contrast, enhancement.saturate, enhancement.brightness
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            // 视频探测结果提示：让用户知道站点内的视频已被识别
            videoScanResult?.let { hint ->
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.VideoSettings, contentDescription = null,
                        tint = com.heasafe.aura.ui.theme.AuraBlue, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        }

        // 收起后的悬浮展开把手（玻璃水珠锚点，点按恢复工具层）
        AnimatedVisibility(
            visible = !chromeVisible,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(120)),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Box(Modifier.padding(end = 12.dp, top = 12.dp)) {
                GlassCircleButton(icon = Icons.Rounded.KeyboardArrowDown) { chromeVisible = true }
            }
        }

        // 拦截提示
        blockedReason?.let { reason ->
            BlockedToast(reason = reason, onDismiss = { blockedReason = null })
        }

        // 画质增强面板（液态玻璃：从工具栏水珠飞出形变，飞行模糊）
        val enhanceBlur = rememberFlightBlur(showEnhancePanel, reduceMotion)
        AnimatedVisibility(
            visible = showEnhancePanel,
            enter = liquidMenuEnter(reduceMotion),
            exit = liquidMenuExit(reduceMotion),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(Modifier.blur(enhanceBlur.dp)) {
                EnhancePanel(
                    enhancement = enhancement,
                    onChange = { new ->
                        enhancement = new
                        if (new.enabled) {
                            secureWebView.applyVisualEnhancement(new.contrast, new.saturate, new.brightness)
                        } else {
                            secureWebView.clearVisualEnhancement()
                        }
                    },
                    onClose = { showEnhancePanel = false }
                )
            }
        }

        // 安全面板（液态玻璃菜单动效）
        val securityBlur = rememberFlightBlur(showSecurityPanel, reduceMotion)
        AnimatedVisibility(
            visible = showSecurityPanel,
            enter = liquidMenuEnter(reduceMotion),
            exit = liquidMenuExit(reduceMotion),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(Modifier.blur(securityBlur.dp)) {
                SecurityPanel(
                    events = viewModel.securityLog.snapshot(),
                    blockedCount = blockedCount,
                    host = currentHost,
                    adsAllowed = settings.allowedHosts.contains(currentHost),
                    onToggleSiteAds = { allow -> viewModel.setSiteAdsAllowed(currentHost, allow) },
                    onClose = { showSecurityPanel = false }
                )
            }
        }
    }
}

@Composable
private fun GlassCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(40.dp)
            .liquidGlass(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun BoxScope.BlockedToast(reason: String, onDismiss: () -> Unit) {
    Row(
        Modifier
            .align(Alignment.TopCenter)
            .padding(top = 130.dp, start = 20.dp, end = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFF3B30).copy(alpha = 0.92f))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Security, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(reason, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EnhancePanel(
    enhancement: VisualEnhancement,
    onChange: (VisualEnhancement) -> Unit,
    onClose: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .liquidGlass(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = AuraBlue)
            Spacer(Modifier.width(10.dp))
            Text("画质增强", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, null) }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "参考 Anime4K 锐化思路：提升对比度、饱和度与清晰度",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        SegmentedControl(
            items = listOf(
                VisualEnhancement.OFF to "关闭",
                VisualEnhancement.BALANCED to "均衡",
                VisualEnhancement.VIVID to "鲜明"
            ),
            selected = enhancement,
            onSelect = onChange,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SecurityPanel(
    events: List<com.heasafe.aura.security.SecurityLog.Event>,
    blockedCount: Int,
    host: String,
    adsAllowed: Boolean,
    onToggleSiteAds: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onClose)
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Security, null, tint = AuraGreen)
                Spacer(Modifier.width(10.dp))
                Text("安全防护", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, null) }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "本页已拦截 $blockedCount 条广告/跟踪 · 会话事件 ${events.size} 条",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(10.dp))
            // 按站放行（修复站点破版）
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .clickable { onToggleSiteAds(!adsAllowed) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (adsAllowed) "本站已放行广告（点按恢复拦截）" else "为 $host 放行广告（修复破版）",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.verticalScroll(rememberScrollState()).height(260.dp)) {
                events.take(40).forEach { event ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(
                            when (event.type) {
                                com.heasafe.aura.security.SecurityLog.Event.Type.BLOCKED_URL -> "拦截"
                                com.heasafe.aura.security.SecurityLog.Event.Type.TRACKER -> "跟踪"
                                com.heasafe.aura.security.SecurityLog.Event.Type.PERMISSION -> "权限"
                                com.heasafe.aura.security.SecurityLog.Event.Type.DOWNLOAD -> "下载"
                                else -> "事件"
                            },
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(44.dp)
                        )
                        Text(event.detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
