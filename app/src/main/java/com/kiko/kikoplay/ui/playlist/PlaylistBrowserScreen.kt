package com.kiko.kikoplay.ui.playlist

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.kiko.kikoplay.ui.navigation.VideoPlayerRoute
import com.kiko.kikoplay.ui.theme.MarkerBlue
import com.kiko.kikoplay.ui.theme.MarkerGreen
import com.kiko.kikoplay.ui.theme.MarkerOrange
import com.kiko.kikoplay.ui.theme.MarkerPink
import com.kiko.kikoplay.ui.theme.MarkerRed
import com.kiko.kikoplay.ui.theme.MarkerYellow
import kotlinx.coroutines.launch

private const val LABEL_PLAYLIST = "\u64ad\u653e\u5217\u8868"
private const val LABEL_BACK = "\u8fd4\u56de"
private const val LABEL_REFRESH = "\u5237\u65b0"
private const val LABEL_FILTER = "\u7b5b\u9009"
private const val LABEL_RETRY = "\u91cd\u8bd5"
private const val LABEL_EMPTY_DIR = "\u8be5\u76ee\u5f55\u4e0b\u6ca1\u6709\u5185\u5bb9"
private const val LABEL_LOAD_FAILED = "\u52a0\u8f7d\u5931\u8d25"
private const val LABEL_CANCEL = "\u53d6\u6d88"
private const val LABEL_CACHE = "\u7f13\u5b58"
private const val LABEL_CACHED = "\u5df2\u7f13\u5b58"
private const val LABEL_MARK_WATCHED = "\u6807\u8bb0\u5df2\u770b"
private const val LABEL_SELECT_ALL = "\u5168\u9009"
private const val LABEL_SELECTED_PREFIX = "\u5df2\u9009 "
private const val LABEL_SELECTED_SUFFIX = " \u9879"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistBrowserScreen(
    onBack: () -> Unit,
    onPlayMedia: (VideoPlayerRoute) -> Unit,
    viewModel: PlaylistBrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterMenu by remember { mutableStateOf(false) }
    val breadcrumbScrollState = rememberScrollState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    fun saveCurrentScrollPosition() {
        viewModel.rememberCurrentScrollPosition(
            index = listState.firstVisibleItemIndex,
            offset = listState.firstVisibleItemScrollOffset
        )
    }

    LaunchedEffect(uiState.pathStack) {
        val target = if (uiState.pathStack.isEmpty()) 0 else breadcrumbScrollState.maxValue
        breadcrumbScrollState.animateScrollTo(target)
    }

    LaunchedEffect(uiState.pathStack, uiState.currentItems.size, uiState.scrollPosition) {
        if (uiState.currentItems.isEmpty()) return@LaunchedEffect
        val targetIndex = uiState.scrollPosition.index.coerceIn(0, uiState.currentItems.lastIndex)
        listState.scrollToItem(targetIndex, uiState.scrollPosition.offset)
    }

    BackHandler(enabled = uiState.pathStack.isNotEmpty()) {
        saveCurrentScrollPosition()
        viewModel.navigateUp()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.horizontalScroll(breadcrumbScrollState),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                saveCurrentScrollPosition()
                                viewModel.navigateToPathIndex(-1)
                            }
                        ) {
                            Text(LABEL_PLAYLIST, maxLines = 1)
                        }
                        uiState.pathStack.forEachIndexed { index, item ->
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            TextButton(
                                onClick = {
                                    saveCurrentScrollPosition()
                                    viewModel.navigateToPathIndex(index)
                                }
                            ) {
                                Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = LABEL_BACK)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPlaylist() }) {
                        Icon(Icons.Default.Refresh, contentDescription = LABEL_REFRESH)
                    }
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = LABEL_FILTER)
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            PlayStateFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(filter.label)
                                            if (uiState.filter == filter) {
                                                Spacer(Modifier.width(8.dp))
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.error != null -> {
                    EmptyState(
                        icon = Icons.Default.Refresh,
                        message = uiState.error ?: LABEL_LOAD_FAILED,
                        actionLabel = LABEL_RETRY,
                        onAction = { viewModel.loadPlaylist() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.currentItems.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Folder,
                        message = LABEL_EMPTY_DIR,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(uiState.currentItems) { index, node ->
                            PlaylistItemCard(
                                node = node,
                                isCached = node.mediaId != null && node.mediaId in uiState.cachedMediaIds,
                                isSelected = uiState.selectedIndices.contains(index),
                                isSelectionMode = uiState.isSelectionMode,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleSelection(index)
                                    } else if (node.isFolder) {
                                        saveCurrentScrollPosition()
                                        viewModel.navigateToFolder(index, node)
                                    } else {
                                        saveCurrentScrollPosition()
                                        coroutineScope.launch {
                                            onPlayMedia(
                                                viewModel.resolvePlaybackRouteForNode(
                                                    node = node,
                                                    parentPath = uiState.pathStack.map { it.index },
                                                    startPositionMs = normalizeResumePositionMs(
                                                        playTimeSeconds = node.playTime,
                                                        playTimeState = node.playTimeState
                                                    ),
                                                    initialPlayTimeState = node.playTimeState ?: 0
                                                )
                                            )
                                        }
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
    isCached: Boolean,
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
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
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

            Icon(
                imageVector = if (node.isFolder) Icons.Default.Folder else Icons.Default.VideoFile,
                contentDescription = null,
                tint = if (node.isFolder) {
                    MaterialTheme.colorScheme.primary
                } else {
                    playStateColor(node.playTimeState)
                },
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(12.dp))

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
                if (!node.isFolder && (node.playTime != null && node.playTime > 0 || isCached)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (node.playTime != null && node.playTime > 0) {
                            Text(
                                text = formatDuration(node.playTime.toLong()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isCached) {
                            Text(
                                text = LABEL_CACHED,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (node.isFolder) {
                val childCount = node.nodes?.size ?: 0
                Text(
                    text = "$childCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = LABEL_CANCEL)
            }
            Text("$LABEL_SELECTED_PREFIX$selectedCount$LABEL_SELECTED_SUFFIX", style = MaterialTheme.typography.bodyMedium)
        }
        Row {
            IconButton(onClick = onCache, enabled = selectedCount > 0) {
                Icon(Icons.Default.Download, contentDescription = LABEL_CACHE)
            }
            IconButton(onClick = onMarkWatched, enabled = selectedCount > 0) {
                Icon(Icons.Default.CheckCircle, contentDescription = LABEL_MARK_WATCHED)
            }
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = LABEL_SELECT_ALL)
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
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}

private fun normalizeResumePositionMs(
    playTimeSeconds: Double?,
    playTimeState: Int?
): Long {
    if (playTimeState == 2) return 0L
    return ((playTimeSeconds ?: 0.0) * 1000).toLong()
}
