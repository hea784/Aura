package com.heasafe.aura.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heasafe.aura.data.LogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 日志页：持久化安全/应用事件，支持类型过滤与清空。 */
@Composable
fun LogsScreen(viewModel: AuraViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val entries by viewModel.logs().collectAsStateWithLifecycle(initialValue = emptyList())
    var filter by remember { mutableStateOf("all") }

    val filtered = when (filter) {
        "block" -> entries.filter { it.type == "BLOCKED_URL" || it.type == "DOWNLOAD" }
        "tracker" -> entries.filter { it.type == "TRACKER" }
        "update" -> entries.filter { it.type == "UPDATE" }
        "other" -> entries.filter { it.type != "BLOCKED_URL" && it.type != "DOWNLOAD" && it.type != "TRACKER" && it.type != "UPDATE" }
        else -> entries
    }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, top = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
            Text("日志", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { viewModel.clearLogs() }) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = "清空日志", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // 过滤 chips
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("all" to "全部", "block" to "拦截", "tracker" to "跟踪", "update" to "更新", "other" to "其他")
                .forEach { (key, label) ->
                    val selected = filter == key
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .clickable { filter = key }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
        }

        if (filtered.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("暂无日志", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("浏览网站时的拦截与防护事件会记录在这里。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { e -> LogRow(e) }
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val time = remember(entry.time) { SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(entry.time)) }
    val (tag, color) = when (entry.type) {
        "BLOCKED_URL" -> "拦截" to MaterialTheme.colorScheme.error
        "DOWNLOAD" -> "下载" to MaterialTheme.colorScheme.error
        "TRACKER" -> "跟踪" to MaterialTheme.colorScheme.tertiary
        "PERMISSION" -> "权限" to MaterialTheme.colorScheme.secondary
        "UPDATE" -> "更新" to MaterialTheme.colorScheme.primary
        else -> "事件" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(tag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.width(36.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(3.dp))
            Text(time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}
