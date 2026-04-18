package com.kiko.kikoplay.ui.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiko.kikoplay.data.local.entity.WatchHistoryEntity
import com.kiko.kikoplay.data.repository.HistoryPlaybackTarget
import com.kiko.kikoplay.ui.common.EmptyState
import kotlinx.coroutines.flow.collectLatest

private const val HOME_RECENT_HISTORY_LIMIT = 4

@Composable
fun HomeScreen(
    onNavigateToConnection: () -> Unit = {},
    onNavigateToPlaylist: () -> Unit = {},
    onNavigateToPlayer: (HistoryPlaybackTarget) -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val connectionInfo by viewModel.connectionInfo.collectAsStateWithLifecycle()
    val recentHistory by viewModel.recentHistory.collectAsStateWithLifecycle()
    val recentDirs by viewModel.recentDirs.collectAsStateWithLifecycle()
    val pendingSwitchRequest by viewModel.pendingSwitchRequest.collectAsStateWithLifecycle()
    val isResolvingHistory by viewModel.isResolvingHistory.collectAsStateWithLifecycle()
    val isConnected = connectionInfo != null

    LaunchedEffect(isConnected) {
        if (isConnected) viewModel.loadRecentDirs()
    }

    LaunchedEffect(viewModel) {
        viewModel.historyPlaybackEvents.collectLatest(onNavigateToPlayer)
    }

    if (pendingSwitchRequest != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSwitchConnection,
            title = { Text("切换连接") },
            text = {
                Text(
                    "当前已连接 ${pendingSwitchRequest?.currentAddress}，是否切换到 ${pendingSwitchRequest?.targetAddress} 并播放该条目？"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmSwitchConnection,
                    enabled = !isResolvingHistory
                ) {
                    Text("切换")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::dismissSwitchConnection,
                    enabled = !isResolvingHistory
                ) {
                    Text("取消")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            onClick = { if (isConnected) onNavigateToPlaylist() else onNavigateToConnection() },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Computer else Icons.Default.WifiOff,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (isConnected) {
                        Text(
                            text = connectionInfo?.deviceName ?: "KikoPlay",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${connectionInfo?.host}:${connectionInfo?.port}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "点击浏览播放列表",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    } else {
                        Text(
                            text = "连接 PC",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "连接 KikoPlay 浏览播放列表",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                if (isConnected) {
                    FilledTonalIconButton(
                        onClick = onNavigateToConnection,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "本地服务发现"
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                    )
                }
            }
        }

        if (isConnected && recentDirs.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentDirs.forEach { dir ->
                    Card(
                        onClick = onNavigateToPlaylist,
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                dir.text,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("最近观看", style = MaterialTheme.typography.titleMedium)
            if (recentHistory.isNotEmpty()) {
                TextButton(onClick = onNavigateToHistory) { Text("更多") }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (recentHistory.isEmpty()) {
            EmptyState(
                icon = Icons.Default.History,
                message = "还没有观看记录"
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                recentHistory.take(HOME_RECENT_HISTORY_LIMIT).forEach { item ->
                    RecentWatchCard(
                        item = item,
                        onClick = {
                            viewModel.onHistoryItemClick(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentWatchCard(
    item: WatchHistoryEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryThumbnail(
                item = item,
                modifier = Modifier
                    .width(118.dp)
                    .aspectRatio(16f / 9f)
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (item.sourceType) {
                            SOURCE_TYPE_PC -> Icons.Default.Computer
                            else -> Icons.Default.PhoneAndroid
                        },
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = when (item.sourceType) {
                            SOURCE_TYPE_PC -> "PC"
                            SOURCE_TYPE_LOCAL -> "本地"
                            else -> "缓存"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.isCached && item.sourceType != SOURCE_TYPE_LOCAL) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "已缓存",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(5.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.animeTitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.animeTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (item.duration > 0) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (item.playTime.toFloat() / item.duration).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryThumbnail(
    item: WatchHistoryEntity,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(item.thumbnailData) {
        item.thumbnailData?.let { data ->
            BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
        }
    }
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(androidx.compose.ui.graphics.Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "${item.title} 缩略图",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(
                imageVector = Icons.Default.VideoFile,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
        }
    }
}

private const val SOURCE_TYPE_PC = 0
private const val SOURCE_TYPE_LOCAL = 1
