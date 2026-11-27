package com.heasafe.aura.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heasafe.aura.BuildConfig
import com.heasafe.aura.data.SettingsKeys
import com.heasafe.aura.ui.components.ToggleRow

/** 设置页 — iOS 分组列表：持久化偏好 + 更新 + 日志入口。 */
@Composable
fun SettingsScreen(
    viewModel: AuraViewModel,
    onOpenLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

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

        SectionLabel("隐私与安全")
        Group {
            ToggleRow("仅 HTTPS", "拦截明文 http 请求，强制加密连接", settings.strictHttps, { viewModel.setSetting(SettingsKeys.STRICT_HTTPS, it) }, Icons.Filled.Security)
            Divider()
            ToggleRow("防指纹跟踪", "收敛硬件/Canvas/WebGL 指纹，降低可识别性", settings.antiFingerprint, { viewModel.setSetting(SettingsKeys.ANTI_FP, it) })
        }

        Spacer(Modifier.height(28.dp))
        SectionLabel("视觉与浏览")
        Group {
            ToggleRow("画质增强", "提升对比度、饱和度与清晰度", settings.enhance, { viewModel.setSetting(SettingsKeys.ENHANCE, it) }, Icons.Filled.AutoAwesome)
            Divider()
            ToggleRow("自动隐藏工具栏", "向下滚动时收起顶部工具层，向上滚动恢复", settings.autoHideChrome, { viewModel.setSetting(SettingsKeys.AUTO_HIDE, it) })
        }

        Spacer(Modifier.height(28.dp))
        SectionLabel("通用")
        Group {
            ActionRow("检查更新", "当前版本 ${BuildConfig.VERSION_NAME}", Icons.Filled.Refresh) { viewModel.checkUpdate(manual = true) }
            Divider()
            ActionRow("查看日志", "安全拦截与防护事件记录", Icons.Filled.List, onOpenLogs)
        }

        Spacer(Modifier.height(28.dp))
        SectionLabel("关于")
        Group {
            Column(Modifier.padding(16.dp)) {
                Text("Aura ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "以隐私为先的上网空间。网站添加后会一直保留，只有你手动删除才会消失。所有安全策略在本地执行。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.6.sp)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) { content() }
}

@Composable
private fun ActionRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier.fillMaxWidth().padding(start = 16.dp).height(0.5.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    )
}
