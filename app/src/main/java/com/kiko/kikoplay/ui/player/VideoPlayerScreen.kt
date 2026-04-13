package com.kiko.kikoplay.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.kiko.kikoplay.ui.player.danmaku.DanmakuParser
import kotlinx.coroutines.delay
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.ui.widget.DanmakuView

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    onBack: () -> Unit,
    onPlayMedia: (mediaId: String, title: String, danmuPool: String?, animeTitle: String?, parentPath: List<Int>, startPositionMs: Long, initialPlayTimeState: Int) -> Unit,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Player state
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var autoAdvanceHandled by remember(uiState.mediaId) { mutableStateOf(false) }
    val latestUiState by rememberUpdatedState(uiState)
    val latestCurrentPosition by rememberUpdatedState(currentPosition)
    val latestDuration by rememberUpdatedState(duration)
    val latestOnPlayMedia by rememberUpdatedState(onPlayMedia)

    // Set media source with subtitle
    LaunchedEffect(uiState.mediaUrl, uiState.subtitleUrl, uiState.startPositionMs) {
        if (uiState.mediaUrl.isNotBlank()) {
            val builder = MediaItem.Builder().setUri(uiState.mediaUrl)
            if (!uiState.subtitleUrl.isNullOrBlank()) {
                val mimeType = when (uiState.subtitleFormat) {
                    "ass", "ssa" -> "text/x-ssa"
                    "srt" -> "application/x-subrip"
                    else -> "text/x-ssa"
                }
                val subtitle = MediaItem.SubtitleConfiguration.Builder(
                    android.net.Uri.parse(uiState.subtitleUrl)
                ).setMimeType(mimeType).setLanguage("und").build()
                builder.setSubtitleConfigurations(listOf(subtitle))
            }
            exoPlayer.setMediaItem(builder.build())
            if (uiState.startPositionMs > 0L) {
                exoPlayer.seekTo(uiState.startPositionMs)
            }
            exoPlayer.prepare()
        }
    }

    // Position polling
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0)
            isPlaying = exoPlayer.isPlaying
            isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING
            delay(200)
        }
    }

    DisposableEffect(exoPlayer, uiState.mediaId) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState != Player.STATE_ENDED || autoAdvanceHandled) return

                autoAdvanceHandled = true
                val finalPosition = latestDuration.takeIf { it > 0L } ?: latestCurrentPosition
                val finalState = playbackState(finalPosition, latestDuration, latestUiState.initialPlayTimeState)
                viewModel.syncPlayTime(finalPosition, finalState, latestDuration)

                val nextEpisode = latestUiState.episodes.nextEpisodeAfter(latestUiState.mediaId) ?: return
                latestOnPlayMedia(
                    nextEpisode.mediaId,
                    nextEpisode.title,
                    nextEpisode.danmuPool,
                    nextEpisode.animeTitle,
                    viewModel.parentPath,
                    nextEpisode.startPositionMs,
                    nextEpisode.playTimeState ?: 0
                )
            }
        }

        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Sync play time on pause/stop
    LaunchedEffect(isPlaying) {
        if (!isPlaying && currentPosition > 0) {
            val state = playbackState(currentPosition, duration, uiState.initialPlayTimeState)
            viewModel.syncPlayTime(currentPosition, state, duration)
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            if (currentPosition > 0) {
                val state = playbackState(currentPosition, duration, uiState.initialPlayTimeState)
                viewModel.syncPlayTime(currentPosition, state, duration)
            }
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // DanmakuView — 只创建一次，在竖屏/横屏间复用
    val danmakuContext = remember {
        DanmakuContext.create()
    }
    val danmakuView = remember {
        DanmakuView(context).apply {
            enableDanmakuDrawingCache(true)
            show()
        }
    }
    var danmakuPrepared by remember { mutableStateOf(false) }

    // Load danmaku into view
    LaunchedEffect(uiState.danmakuItems) {
        android.util.Log.d("DanmakuDebug", "LaunchedEffect: items=${uiState.danmakuItems.size}")
        danmakuPrepared = false
        if (uiState.danmakuItems.isNotEmpty()) {
            val parser = DanmakuParser.createParser(uiState.danmakuItems)
            danmakuView.setCallback(object : master.flame.danmaku.controller.DrawHandler.Callback {
                override fun prepared() {
                    // prepared() 在后台线程回调，必须 post 到主线程操作 DanmakuView
                    danmakuView.post {
                        val exoTime = exoPlayer.currentPosition
                        danmakuView.show()
                        danmakuView.start(exoTime)
                        if (exoPlayer.isPlaying) {
                            danmakuView.resume()
                        } else {
                            danmakuView.pause()
                        }
                        danmakuPrepared = true
                    }
                }
                override fun updateTimer(timer: master.flame.danmaku.danmaku.model.DanmakuTimer) {}
                override fun danmakuShown(danmaku: BaseDanmaku?) {}
                override fun drawingFinished() {}
            })
            danmakuView.enableDanmakuDrawingCache(true)
            danmakuView.show()
            danmakuView.prepare(parser, danmakuContext)
        }
    }

    // Release on dispose
    DisposableEffect(danmakuView) {
        onDispose {
            danmakuView.release()
        }
    }

    // Danmaku play/pause sync
    LaunchedEffect(isPlaying, danmakuPrepared) {
        if (!danmakuPrepared) return@LaunchedEffect
        delay(300)
        try {
            if (isPlaying) danmakuView.resume() else danmakuView.pause()
        } catch (_: Exception) {}
    }

    // 持续同步弹幕时间和 ExoPlayer 时间
    LaunchedEffect(danmakuPrepared) {
        if (!danmakuPrepared) return@LaunchedEffect
        while (true) {
            delay(500)
            val dvTime = danmakuView.currentTime
            val exoTime = exoPlayer.currentPosition
            val diff = exoTime - dvTime
            // 当弹幕时间与播放器时间偏差超过 1 秒时，强制同步
            if (kotlin.math.abs(diff) > 1000) {
                try {
                    danmakuView.seekTo(exoTime)
                } catch (e: Exception) {
                    android.util.Log.w("DanmakuDebug", "seekTo failed", e)
                }
            }
        }
    }

    // Controls visibility
    var controlsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Gesture state
    var seekDelta by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }

    // Dialog states
    var showSendDanmaku by remember { mutableStateOf(false) }
    var showScreenshotDialog by remember { mutableStateOf(false) }
    var showDanmakuSettings by remember { mutableStateOf(false) }

    // Danmaku settings state
    var danmakuSettings by remember { mutableStateOf(com.kiko.kikoplay.ui.player.danmaku.DanmakuSettings()) }

    // Apply danmaku settings to DanmakuContext
    LaunchedEffect(danmakuSettings, danmakuContext) {
        danmakuContext.setDanmakuTransparency(danmakuSettings.alpha)
        danmakuContext.setScaleTextSize(danmakuSettings.fontSize)
        danmakuContext.setScrollSpeedFactor(1f / danmakuSettings.speed)
        danmakuContext.setMaximumVisibleSizeInScreen(
            if (danmakuSettings.displayArea < 1f) (danmakuSettings.displayArea * 100).toInt() else -1
        )
    }

    // Apply playback speed
    LaunchedEffect(danmakuSettings.playbackSpeed) {
        exoPlayer.setPlaybackSpeed(danmakuSettings.playbackSpeed)
    }

    // Send danmaku sheet
    if (showSendDanmaku) {
        com.kiko.kikoplay.ui.player.danmaku.SendDanmakuSheet(
            onDismiss = { showSendDanmaku = false },
            onSend = { text, color, type ->
                viewModel.sendDanmaku(text, currentPosition, color, type)
            }
        )
    }

    // Screenshot/clip dialog
    if (showScreenshotDialog) {
        com.kiko.kikoplay.ui.player.components.ScreenshotClipDialog(
            currentPositionMs = currentPosition,
            onDismiss = { showScreenshotDialog = false },
            onScreenshot = { viewModel.screenshot(currentPosition / 1000.0) },
            onClip = { dur -> viewModel.screenshot(currentPosition / 1000.0, dur) }
        )
    }

    if (isLandscape || uiState.isFullscreen) {
        // Fullscreen player
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            PlayerSurface(
                exoPlayer = exoPlayer,
                danmakuView = danmakuView,
                isDanmakuVisible = uiState.isDanmakuVisible,
                modifier = Modifier.fillMaxSize()
            )

            // Gesture layer
            GestureLayer(
                context = context,
                exoPlayer = exoPlayer,
                onToggleControls = { controlsVisible = !controlsVisible },
                onTogglePlayPause = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                modifier = Modifier.fillMaxSize()
            )

            // Controls overlay
            if (controlsVisible) {
                PlayerControlsOverlay(
                    title = uiState.title,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    isDanmakuVisible = uiState.isDanmakuVisible,
                    isFullscreen = true,
                    onBack = {
                        if (uiState.isFullscreen) {
                            viewModel.setFullscreen(false)
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        } else {
                            onBack()
                        }
                    },
                    onPlayPause = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                    onSeek = { pos ->
                        exoPlayer.seekTo(pos)
                        if (danmakuPrepared) try { danmakuView.seekTo(pos) } catch (_: Exception) {}
                    },
                    onToggleDanmaku = { viewModel.toggleDanmaku() },
                    onToggleFullscreen = {
                        viewModel.setFullscreen(false)
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    },
                    onScreenshot = { showScreenshotDialog = true },
                    onSendDanmaku = { showSendDanmaku = true },
                    onDanmakuSettings = { showDanmakuSettings = !showDanmakuSettings },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(48.dp),
                    color = Color.White
                )
            }

            // Danmaku settings panel (slide from right)
            androidx.compose.animation.AnimatedVisibility(
                visible = showDanmakuSettings,
                enter = androidx.compose.animation.slideInHorizontally { it },
                exit = androidx.compose.animation.slideOutHorizontally { it },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                com.kiko.kikoplay.ui.player.danmaku.DanmakuSettingsPanel(
                    settings = danmakuSettings,
                    onSettingsChange = { danmakuSettings = it }
                )
            }
        }
    } else {
        // Portrait: player on top + content below
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Player area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                PlayerSurface(
                    exoPlayer = exoPlayer,
                    danmakuView = danmakuView,
                    isDanmakuVisible = uiState.isDanmakuVisible,
                    modifier = Modifier.fillMaxSize()
                )

                GestureLayer(
                    context = context,
                    exoPlayer = exoPlayer,
                    onToggleControls = { controlsVisible = !controlsVisible },
                    onTogglePlayPause = {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (controlsVisible) {
                    PlayerControlsOverlay(
                        title = uiState.title,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        isDanmakuVisible = uiState.isDanmakuVisible,
                        isFullscreen = false,
                        onBack = onBack,
                        onPlayPause = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                        onSeek = { pos ->
                            exoPlayer.seekTo(pos)
                            if (danmakuPrepared) try { danmakuView.seekTo(pos) } catch (_: Exception) {}
                        },
                        onToggleDanmaku = { viewModel.toggleDanmaku() },
                        onToggleFullscreen = {
                            viewModel.setFullscreen(true)
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        },
                        onScreenshot = { showScreenshotDialog = true },
                        onSendDanmaku = { showSendDanmaku = true },
                        onDanmakuSettings = { showDanmakuSettings = !showDanmakuSettings },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(36.dp),
                        color = Color.White
                    )
                }
            }

            // Content info below player
            PlayerContentInfo(
                currentMediaId = uiState.mediaId,
                currentPositionMs = currentPosition,
                title = uiState.title,
                animeTitle = uiState.animeTitle,
                episodes = uiState.episodes,
                danmakuSources = uiState.danmakuSources,
                onPlayEpisode = { episode ->
                    val state = playbackState(currentPosition, duration, uiState.initialPlayTimeState)
                    viewModel.syncPlayTime(currentPosition, state, duration)
                    onPlayMedia(
                        episode.mediaId,
                        episode.title,
                        episode.danmuPool,
                        episode.animeTitle,
                        viewModel.parentPath,
                        episode.startPositionMs,
                        episode.playTimeState ?: 0
                    )
                },
                onRefreshDanmaku = { viewModel.refreshDanmaku() },
                onUpdateDelay = { id, delay -> viewModel.updateSourceDelay(id, delay) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerSurface(
    exoPlayer: ExoPlayer,
    danmakuView: DanmakuView,
    isDanmakuVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // 用 TextureView 替代 PlayerView（SurfaceView 会覆盖弹幕层）
        AndroidView(
            factory = { ctx ->
                android.view.TextureView(ctx).also { textureView ->
                    exoPlayer.setVideoTextureView(textureView)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Danmaku overlay — 复用外部创建的 DanmakuView 实例
        AndroidView(
            factory = {
                // 如果 danmakuView 已有 parent，先 detach
                (danmakuView.parent as? android.view.ViewGroup)?.removeView(danmakuView)
                danmakuView
            },
            update = { view ->
                view.visibility = if (isDanmakuVisible) android.view.View.VISIBLE else android.view.View.GONE
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun GestureLayer(
    context: Context,
    exoPlayer: ExoPlayer,
    onToggleControls: () -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { onTogglePlayPause() }
                )
            }
    )
}

@Composable
private fun PlayerControlsOverlay(
    title: String,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    isDanmakuVisible: Boolean,
    isFullscreen: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleDanmaku: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onScreenshot: () -> Unit,
    onSendDanmaku: () -> Unit = {},
    onDanmakuSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(Color.Black.copy(alpha = 0.3f))) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
            }
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isFullscreen) {
                IconButton(onClick = onScreenshot) {
                    Icon(Icons.Default.CameraAlt, "截图", tint = Color.White)
                }
            }
        }

        // Bottom bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            // Seek bar
            var sliderPosition by remember(currentPosition) { mutableFloatStateOf(currentPosition.toFloat()) }
            Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                onValueChangeFinished = { onSeek(sliderPosition.toLong()) },
                valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play/Pause
                IconButton(onClick = onPlayPause) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White
                    )
                }

                // Danmaku toggle
                IconButton(onClick = onToggleDanmaku) {
                    Icon(
                        if (isDanmakuVisible) Icons.Default.Subtitles else Icons.Default.SubtitlesOff,
                        contentDescription = "弹幕",
                        tint = if (isDanmakuVisible) Color.White else Color.White.copy(alpha = 0.5f)
                    )
                }

                // Send danmaku
                IconButton(onClick = onSendDanmaku) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送弹幕", tint = Color.White)
                }

                // Time
                Text(
                    text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )

                // Danmaku settings
                IconButton(onClick = onDanmakuSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "弹幕设置", tint = Color.White)
                }

                // Screenshot
                IconButton(onClick = onScreenshot) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "截图", tint = Color.White)
                }

                // Fullscreen toggle
                IconButton(onClick = onToggleFullscreen) {
                    Icon(
                        if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "全屏",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerContentInfo(
    currentMediaId: String,
    currentPositionMs: Long,
    title: String,
    animeTitle: String?,
    episodes: List<EpisodeUiItem>,
    danmakuSources: List<com.kiko.kikoplay.data.remote.model.DanmakuSource>,
    onPlayEpisode: (EpisodeUiItem) -> Unit,
    onRefreshDanmaku: () -> Unit,
    onUpdateDelay: (Int, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("剧集", "弹幕")

    Column(modifier = modifier) {
        // Title area
        Column(modifier = Modifier.padding(16.dp)) {
            if (animeTitle != null) {
                Text(animeTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tabs
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(tab) }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                if (episodes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("当前目录没有可播放的视频条目", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(episodes, key = { _, episode -> episode.mediaId }) { _, episode ->
                            val isCurrent = episode.mediaId == currentMediaId
                            EpisodeListItem(
                                episode = episode,
                                isCurrent = isCurrent,
                                currentPositionMs = if (isCurrent) currentPositionMs else null,
                                onClick = { onPlayEpisode(episode) }
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
            1 -> {
                // Danmaku sources
                if (danmakuSources.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无弹幕源信息", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(danmakuSources) { _, source ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(source.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "延迟: ${source.delay}ms",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeListItem(
    episode: EpisodeUiItem,
    isCurrent: Boolean,
    currentPositionMs: Long?,
    onClick: () -> Unit
) {
    val displayPlayTimeMs = currentPositionMs ?: episode.startPositionMs

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable(enabled = !isCurrent, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.VideoFile,
                contentDescription = null,
                tint = episodePlayStateColor(episode.playTimeState),
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (displayPlayTimeMs > 0L) {
                    Text(
                        text = formatEpisodeDuration(displayPlayTimeMs / 1000),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            EpisodePlayStateIcon(episode.playTimeState)
        }
    }
}

@Composable
private fun EpisodePlayStateIcon(state: Int?) {
    val (icon, tint) = when (state) {
        2 -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        1 -> Icons.Default.PlayCircle to MaterialTheme.colorScheme.tertiary
        else -> return
    }
    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
}

private fun episodePlayStateColor(state: Int?): Color {
    return when (state) {
        2 -> Color(0xFF4CAF50)
        1 -> Color(0xFFFFC107)
        else -> Color(0xFF9E9E9E)
    }
}

private fun formatEpisodeDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%d:%02d", m, s)
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%d:%02d", m, s)
}

private fun playbackState(currentPosition: Long, duration: Long, initialPlayTimeState: Int): Int {
    if (initialPlayTimeState == 2 && currentPosition < 15_000L) return 2
    if (currentPosition < 15_000L) return 0
    if (duration > 0 && duration - currentPosition < 15_000L) return 2
    return 1
}

private fun List<EpisodeUiItem>.nextEpisodeAfter(currentMediaId: String): EpisodeUiItem? {
    val currentIndex = indexOfFirst { it.mediaId == currentMediaId }
    if (currentIndex < 0) return null
    return getOrNull(currentIndex + 1)
}
