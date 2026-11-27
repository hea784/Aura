package com.heasafe.aura.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.rememberHazeState
import com.heasafe.aura.ui.components.hazeGlass
import com.heasafe.aura.ui.components.liquidGlass
import com.heasafe.aura.ui.theme.AuraTheme
import com.heasafe.aura.ui.theme.AuraBlue
import com.heasafe.aura.ui.theme.AuraGreen

class MainActivity : ComponentActivity() {
    private val viewModel: AuraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AuraTheme {
                AuraApp(viewModel)
            }
        }
    }
}

@Composable
fun AuraApp(viewModel: AuraViewModel) {
    val sites by viewModel.sites.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeSite by viewModel.activeSite.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val update by viewModel.update.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var tab by remember { mutableStateOf(0) } // 0=空间 1=防护 2=设置
    var showLogs by remember { mutableStateOf(false) }
    val hazeState = rememberHazeState()

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    // 启动时静默检查更新
    LaunchedEffect(Unit) { viewModel.checkUpdate(manual = false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            // 内容层作为 Haze 模糊源：功能层玻璃实时模糊其下方内容
            Box(Modifier.fillMaxSize().haze(hazeState)) {
                when {
                    showLogs -> LogsScreen(viewModel = viewModel, onBack = { showLogs = false })
                    activeSite != null -> {
                        val site = activeSite!!
                        BrowseScreen(
                            url = site.url,
                            siteTitle = site.title,
                            initialMode = site.videoMode,
                            viewModel = viewModel,
                            onClose = { viewModel.closeSite() }
                        )
                    }
                    tab == 0 -> LibraryScreen(
                        sites = sites,
                        viewModel = viewModel,
                        onOpen = { viewModel.openSite(it) }
                    )
                    tab == 1 -> ProtectionScreen(viewModel = viewModel, onOpenLogs = { showLogs = true })
                    else -> SettingsScreen(viewModel = viewModel, onOpenLogs = { showLogs = true })
                }
            }

            // 底部 Tab Bar（regular 液态玻璃：真实背景模糊；仅在非浏览页/非日志页显示）
            if (activeSite == null && !showLogs) {
                GlassTabBar(
                    current = tab,
                    onTab = { tab = it },
                    hazeState = hazeState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 40.dp, end = 40.dp, bottom = 20.dp)
                )
            }
        }
    }

    // 更新提示弹窗
    update?.let { info ->
        AlertDialog(
            onDismissRequest = { viewModel.consumeUpdate() },
            title = { Text("发现新版本 ${info.version}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        info.notes.ifBlank { "包含改进与修复。" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl ?: info.pageUrl)))
                    } catch (_: Exception) { }
                    viewModel.consumeUpdate()
                }) { Text("下载更新") }
            },
            dismissButton = { TextButton(onClick = { viewModel.consumeUpdate() }) { Text("稍后") } }
        )
    }
}

@Composable
private fun GlassTabBar(
    current: Int,
    onTab: (Int) -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Icons.Filled.Apps to "空间",
        Icons.Filled.Security to "防护",
        Icons.Filled.Settings to "设置"
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .hazeGlass(hazeState, RoundedCornerShape(22.dp))
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEachIndexed { index, (icon, label) ->
                val selected = current == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTab(index) }
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Icon(
                        icon, contentDescription = label,
                        tint = if (selected) AuraBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        label, fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) AuraBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}
