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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heasafe.aura.ui.theme.AuraGreen

/** 防护页 — iOS 分组列表：大标题 + 状态摘要 + 防护项列表。 */
@Composable
fun ProtectionScreen(viewModel: AuraViewModel, onOpenLogs: () -> Unit, modifier: Modifier = Modifier) {
    val events = viewModel.securityLog.snapshot()

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 120.dp)
    ) {
        Text("防护", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Text("你的上网安全，时刻在线", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        // 状态摘要行
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(AuraGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, null, tint = AuraGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("全部防护已开启", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("本次会话拦截 ${events.size} 次风险", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onOpenLogs)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.List, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text("查看完整日志", fontWeight = FontWeight.Medium, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "已启用的防护", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.6.sp
        )
        Spacer(Modifier.height(8.dp))

        val protections = listOf(
            "仅允许 HTTP / HTTPS" to "阻止 file、javascript、data 等危险协议",
            "本地与内网地址拦截" to "拒绝 localhost、环回与私有地址",
            "跟踪器域名拦截" to "阻止广告与行为追踪脚本",
            "Cookie 与存储隔离" to "退出即清除，不保留登录态",
            "权限请求全部拒绝" to "摄像头、麦克风、定位与文件选择",
            "下载与弹窗拦截" to "阻止可疑文件与页面弹窗"
        )

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            protections.forEachIndexed { index, (title, desc) ->
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 13.dp, end = 16.dp, bottom = 13.dp)) {
                    Box(
                        Modifier.size(8.dp).clip(CircleShape).background(AuraGreen)
                            .align(Alignment.Top)
                            .padding(top = 6.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (index < protections.lastIndex) {
                    Box(
                        Modifier.fillMaxWidth().padding(start = 38.dp).height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )
                }
            }
        }

        if (events.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Text(
                "最近拦截", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.6.sp
            )
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                events.take(10).forEach { e ->
                    Text("• ${e.detail}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 3.dp))
                }
            }
        }
    }
}
