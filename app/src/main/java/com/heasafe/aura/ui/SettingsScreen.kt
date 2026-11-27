package com.heasafe.aura.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heasafe.aura.ui.components.ToggleRow

/** 设置页 — iOS 分组列表风格。 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var blockTrackers by remember { mutableStateOf(true) }
    var isolateCookies by remember { mutableStateOf(true) }
    var blockDownloads by remember { mutableStateOf(true) }
    var enhance by remember { mutableStateOf(true) }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 120.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Text("按你的习惯调整 Aura", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))

        Text("隐私与安全", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            ToggleRow("拦截跟踪器", "阻止广告与行为追踪域名", blockTrackers, { blockTrackers = it }, Icons.Filled.Security)
            Divider()
            ToggleRow("隔离 Cookie", "每个网站独立、退出即清除", isolateCookies, { isolateCookies = it })
            Divider()
            ToggleRow("禁止下载", "拦截可能携带风险的文件", blockDownloads, { blockDownloads = it }, Icons.Filled.DeleteSweep)
        }

        Spacer(Modifier.height(28.dp))
        Text("视觉体验", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            ToggleRow("画质增强", "提升对比度、饱和度与清晰度", enhance, { enhance = it }, Icons.Filled.AutoAwesome)
        }

        Spacer(Modifier.height(28.dp))
        Text("关于", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text("Aura 1.0.0", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "以隐私为先的上网空间。网站添加后会一直保留，只有你手动删除才会消失。所有安全策略在本地执行。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier.fillMaxWidth().padding(start = 16.dp).height(0.5.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    )
}
