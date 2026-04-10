package com.kiko.kikoplay.ui.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiko.kikoplay.data.remote.model.PlaylistNode
import com.kiko.kikoplay.ui.common.EmptyState
import com.kiko.kikoplay.ui.theme.MarkerBlue
import com.kiko.kikoplay.ui.theme.MarkerGreen
import com.kiko.kikoplay.ui.theme.MarkerOrange
import com.kiko.kikoplay.ui.theme.MarkerPink
import com.kiko.kikoplay.ui.theme.MarkerRed
import com.kiko.kikoplay.ui.theme.MarkerYellow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistBrowserScreen(
    onBack: () -> Unit,
    onPlayMedia: (mediaId: String, title: String, danmuPool: String?, animeTitle: String?) -> Unit,
    viewModel: PlaylistBrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Breadcrumb
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.navigateToPathIndex(-1) }) {
                            Text("播放列表", maxLines = 1)
                        }
                        uiState.pathStack.forEachIndexed { index, item ->
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                            TextButton(onClick = { viewModel.navigateToPathIndex(index) }) {
                                Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.pathStack.isNotEmpty()) viewModel.navigateUp()
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPlaylist() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "筛选")
                        }
                        DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                            PlayStateFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(filter.label)
                                            if (uiState.filter == filter) {
                                                Spacer(Modifier.width(8.dp))
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.setFilter(filter)
                                        showFilterMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = uiState.isSelectionMode) {
                SelectionBar(
                    selectedCount = uiState.selectedIndices.size,
                    onSelectAll = { viewModel.selectAll() },
                    onClearSelection = { viewModel.clearSelection() },
                    onCache = { viewModel.cacheSelected() },
                    onMarkWatched = { viewModel.markSelectedWatched() }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    EmptyState(
                        icon = Icons.Default.Refresh,
                        message = uiState.error ?: "加载失败",
                        actionLabel = "重试",
                        onAction = { viewModel.loadPlaylist() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.currentItems.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Folder,
                        message = "该目录下没有内容",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(uiState.currentItems) { index, node ->
                            PlaylistItemCard(
                                node = node,
                                isSelected = uiState.selectedIndices.contains(index),
                                isSelectionMode = uiState.isSelectionMode,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleSelection(index)
                                    } else if (node.isFolder) {
                                        viewModel.navigateToFolder(index, node)
                                    } else {
                                        onPlayMedia(
                                            node.mediaId ?: return@PlaylistItemCard,
                                            node.text,
                                            node.danmuPool,
                                            node.animeName
                                        )
                                    }
                                },
                                onLongClick = {
                                    if (!uiState.isSelectionMode) {
                                        viewModel.toggleSelectionMode()
                                        viewModel.toggleSelection(index)
                                    }
                                }
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistItemCard(
    node: PlaylistNode,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val markerColor = node.marker?.let { markerToColor(it) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
                Spacer(Modifier.width(4.dp))
            }

            // Icon
            Icon(
                imageVector = if (node.isFolder) Icons.Default.Folder else Icons.Default.VideoFile,
                contentDescription = null,
                tint = if (node.isFolder) MaterialTheme.colorScheme.primary
                else playStateColor(node.playTimeState),
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.text,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = markerColor ?: MaterialTheme.colorScheme.onSurface
                )
                if (!node.isFolder && node.animeName != null) {
                    Text(
                        text = node.animeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!node.isFolder && node.playTime != null && node.playTime > 0) {
                    Text(
                        text = formatDuration(node.playTime.toLong()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Trailing
            if (node.isFolder) {
                val childCount = node.nodes?.size ?: 0
                Text(
                    "$childCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                PlayStateIcon(node.playTimeState)
            }
        }
    }
}

@Composable
private fun PlayStateIcon(state: Int?) {
    val (icon, tint) = when (state) {
        2 -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        1 -> Icons.Default.PlayCircle to MaterialTheme.colorScheme.tertiary
        else -> return
    }
    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
}

@Composable
private fun SelectionBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onCache: () -> Unit = {},
    onMarkWatched: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "取消")
            }
            Text("已选 $selectedCount 项", style = MaterialTheme.typography.bodyMedium)
        }
        Row {
            IconButton(onClick = onCache, enabled = selectedCount > 0) {
                Icon(Icons.Default.Download, contentDescription = "缓存")
            }
            IconButton(onClick = onMarkWatched, enabled = selectedCount > 0) {
                Icon(Icons.Default.CheckCircle, contentDescription = "标记已看")
            }
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "全选")
            }
        }
    }
}

private fun playStateColor(state: Int?): Color {
    return when (state) {
        2 -> Color(0xFF4CAF50)
        1 -> Color(0xFFFFC107)
        else -> Color(0xFF9E9E9E)
    }
}

private fun markerToColor(marker: Int): Color? {
    return when (marker) {
        0 -> MarkerRed
        1 -> MarkerBlue
        2 -> MarkerGreen
        3 -> MarkerOrange
        4 -> MarkerPink
        5 -> MarkerYellow
        else -> null
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%d:%02d", m, s)
}
