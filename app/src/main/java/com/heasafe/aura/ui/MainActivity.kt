package com.heasafe.aura.ui

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val snackbarHostState = remember { SnackbarHostState() }

    var tab by remember { mutableStateOf(0) } // 0=空间 1=防护 2=设置

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
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
                tab == 1 -> ProtectionScreen(viewModel = viewModel)
                else -> SettingsScreen()
            }

            // 底部 Tab Bar（仅在非浏览页显示）
            if (activeSite == null) {
                GlassTabBar(
                    current = tab,
                    onTab = { tab = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 40.dp, end = 40.dp, bottom = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun GlassTabBar(
    current: Int,
    onTab: (Int) -> Unit,
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
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f))
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
