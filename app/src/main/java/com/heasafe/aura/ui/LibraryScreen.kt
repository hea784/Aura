package com.heasafe.aura.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heasafe.aura.data.SavedSite
import com.heasafe.aura.ui.components.SegmentedControl
import com.heasafe.aura.ui.theme.AuraBlue
import com.heasafe.aura.ui.theme.AuraGreen
import com.heasafe.aura.ui.theme.AuraIndigo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 空间页 — iOS 分组列表风格。
 * 设计要点：大标题 + 精致状态徽章（呼吸点）+ 细分隔线分组列表 + 轻量空态 + 悬浮添加按钮。
 */
@Composable
fun LibraryScreen(
    sites: List<SavedSite>,
    viewModel: AuraViewModel,
    onOpen: (SavedSite) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<SavedSite?>(null) }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                // 顶部：大标题 + 副标题，克制排版
                Text(
                    text = "Aura",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "安全上网空间",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                StatusBadge(siteCount = sites.size)
                Spacer(Modifier.height(30.dp))
            }

            item {
                SectionHeader(title = "我的网站", count = sites.size)
                Spacer(Modifier.height(8.dp))
            }

            if (sites.isEmpty()) {
                item {
                    EmptyState(onAdd = { showAddDialog = true })
                }
            } else {
                items(items = sites, key = { it.id }, contentType = { "site" }) { site ->
                    SiteRow(
                        site = site,
                        onOpen = { onOpen(site) },
                        onFavorite = { viewModel.setFavorite(site, !site.favorite) },
                        onDelete = { pendingDelete = site },
                        onVideoMode = { mode -> viewModel.setVideoMode(site, mode) }
                    )
                }
            }
        }

        // 悬浮添加按钮：唯一的醒目操作入口
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 108.dp),
            containerColor = AuraBlue,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "添加网站", modifier = Modifier.size(26.dp))
        }

        if (showAddDialog) {
            AddSiteDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { url -> viewModel.addSite(url); showAddDialog = false }
            )
        }

        pendingDelete?.let { site ->
            DeleteConfirmDialog(
                site = site,
                onDismiss = { pendingDelete = null },
                onConfirm = { viewModel.deleteSite(site); pendingDelete = null }
            )
        }
    }
}

/** 状态徽章：白底 + 呼吸绿点，唯一的动效记忆点。 */
@Composable
private fun StatusBadge(siteCount: Int) {
    val transition = rememberInfiniteTransition(label = "shield-pulse")
    val scale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulse"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AuraGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(9.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(AuraGreen)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("防护运行中", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                "$siteCount 个网站 · 跟踪与隐私全面隔离",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.6.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

/** 站点行：iOS 列表行 — 图标 + 标题/副标题 + 收藏/删除 + 展开箭头。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SiteRow(
    site: SavedSite,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onVideoMode: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val date = remember(site.lastOpenedAt) {
        if (site.lastOpenedAt == 0L) "尚未打开"
        else "最近打开 " + SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(site.lastOpenedAt))
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(onClick = onOpen, onLongClick = { expanded = true })
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 12.dp, end = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 字母图标：柔和渐变
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(AuraBlue, AuraIndigo)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(site.title.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(site.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
                Text(site.host, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(38.dp)) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = "收藏",
                    tint = if (site.favorite) Color(0xFFFF3B30)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(Modifier.padding(start = 68.dp, end = 16.dp, bottom = 14.dp)) {
                Text("视频观看模式", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                SegmentedControl(
                    items = listOf("auto" to "自动", "short" to "短视频", "normal" to "普通视频"),
                    selected = site.videoMode,
                    onSelect = { onVideoMode(it) }
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}

/** 轻量空态：不占据半屏，留出呼吸感。 */
@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 36.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(AuraGreen.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Link, null, tint = AuraGreen, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("还没有网站", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "轻点右下角 + 添加常去的网站，\n它会一直留在这里。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun AddSiteDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加网站", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("输入网址，它会持久保存在你的空间里。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("example.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    leadingIcon = { Icon(Icons.Rounded.Link, null) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onAdd(text.trim()) }) { Text("添加") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun DeleteConfirmDialog(site: SavedSite, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除「${site.title}」？", fontWeight = FontWeight.Bold) },
        text = { Text("删除后可以随时重新添加。") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("删除", color = Color(0xFFFF3B30)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
