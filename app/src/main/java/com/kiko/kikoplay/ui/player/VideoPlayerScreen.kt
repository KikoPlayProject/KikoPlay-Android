package com.kiko.kikoplay.ui.player

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.AudioManager
import android.graphics.Matrix
import android.content.pm.ActivityInfo
import android.view.View
import androidx.annotation.OptIn
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import com.kiko.kikoplay.ui.player.components.KikoSlider
import com.kiko.kikoplay.ui.player.danmaku.DanmakuSourceSummary
import com.kiko.kikoplay.ui.player.danmaku.DanmakuParser
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.roundToInt
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.ui.widget.DanmakuView

private enum class GestureOverlayMode {
    Seek,
    Brightness,
    Volume,
    Speed
}

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
    val audioManager = remember { context.getSystemService(Activity.AUDIO_SERVICE) as AudioManager }
    val constrainedPlaybackDevice = remember(context) { isConstrainedPlaybackDevice(context) }

    // ExoPlayer
    val exoPlayer = remember {
        createVideoPlayer(context, constrainedPlaybackDevice).apply {
            playWhenReady = true
        }
    }

    // Player state
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var videoSize by remember { mutableStateOf(VideoSize.UNKNOWN) }
    var autoAdvanceHandled by remember(uiState.mediaId) { mutableStateOf(false) }
    var playerTextureView by remember { mutableStateOf<android.view.TextureView?>(null) }
    val latestUiState by rememberUpdatedState(uiState)
    val latestCurrentPosition by rememberUpdatedState(currentPosition)
    val latestDuration by rememberUpdatedState(duration)
    val latestTextureView by rememberUpdatedState(playerTextureView)
    val latestVideoSize by rememberUpdatedState(videoSize)
    val latestOnPlayMedia by rememberUpdatedState(onPlayMedia)
    val finalHistorySaved = remember(uiState.mediaId) { AtomicBoolean(false) }
    val latestContext by rememberUpdatedState(context)

    fun captureCurrentFrameThumbnail(): ByteArray? {
        val textureView = latestTextureView ?: return null
        val viewWidth = textureView.width
        val viewHeight = textureView.height
        if (viewWidth <= 0 || viewHeight <= 0) return null
        if (!canSafelyCaptureThumbnail(latestContext)) return null

        val targetWidth = minOf(320, viewWidth)
        val targetHeight = ((viewHeight.toFloat() * targetWidth) / viewWidth)
            .roundToInt()
            .coerceAtLeast(1)
        val bitmap = runCatching { textureView.getBitmap(targetWidth, targetHeight) }.getOrNull() ?: return null
        return try {
            runCatching {
                bitmap.toHistoryThumbnailBytes(videoSize = latestVideoSize)
            }.getOrNull()
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    fun syncTerminalPlayState(positionMs: Long, durationMs: Long) {
        if (!finalHistorySaved.compareAndSet(false, true)) return

        val finalState = playbackState(positionMs, durationMs, latestUiState.initialPlayTimeState)
        viewModel.syncPlayTime(
            positionMs = positionMs,
            playTimeState = finalState,
            durationMs = durationMs,
            thumbnailData = captureCurrentFrameThumbnail()
        )
    }

    fun navigateBackWithSnapshot() {
        syncTerminalPlayState(currentPosition, duration)
        onBack()
    }

    BackHandler {
        if (uiState.isFullscreen || isLandscape) {
            viewModel.setFullscreen(false)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            navigateBackWithSnapshot()
        }
    }

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
            val mediaItem = builder.build()
            if (uiState.startPositionMs > 0L) {
                exoPlayer.setMediaItem(mediaItem, uiState.startPositionMs)
            } else {
                exoPlayer.setMediaItem(mediaItem)
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
                syncTerminalPlayState(finalPosition, latestDuration)

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

            override fun onVideoSizeChanged(newVideoSize: VideoSize) {
                videoSize = newVideoSize
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
                syncTerminalPlayState(currentPosition, duration)
            }
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(activity, isLandscape, uiState.isFullscreen) {
        val currentActivity = activity
        val window = currentActivity?.window
        val decorView = window?.decorView
        val fullscreen = isLandscape || uiState.isFullscreen
        val controller = if (window != null && decorView != null) {
            WindowCompat.getInsetsController(window, decorView)
        } else {
            null
        }

        if (window != null && decorView != null && controller != null) {
            WindowCompat.setDecorFitsSystemWindows(window, !fullscreen)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (fullscreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }

        onDispose {
            if (window != null && decorView != null && controller != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                controller.show(WindowInsetsCompat.Type.systemBars())
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
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

    // Controls visibility
    var controlsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Seek preview state
    var gestureSeekPreviewMs by remember { mutableLongStateOf(0L) }
    var gestureSeekOriginalMs by remember { mutableLongStateOf(0L) }
    var sliderSeekPreviewMs by remember { mutableLongStateOf(0L) }
    var isGestureSeeking by remember { mutableStateOf(false) }
    var isGestureSeekCancelled by remember { mutableStateOf(false) }
    var isSliderSeeking by remember { mutableStateOf(false) }
    var gestureOverlayMode by remember { mutableStateOf<GestureOverlayMode?>(null) }
    var gestureBrightnessFraction by remember { mutableFloatStateOf(0f) }
    var gestureVolumeFraction by remember { mutableFloatStateOf(0f) }
    var isSpeedBoosting by remember { mutableStateOf(false) }
    val centerSeekPreviewMs = if (isGestureSeeking) gestureSeekPreviewMs else sliderSeekPreviewMs

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
        danmakuContext.setR2LDanmakuVisibility(danmakuSettings.showScroll)
        danmakuContext.setL2RDanmakuVisibility(danmakuSettings.showScroll)
        danmakuContext.setFTDanmakuVisibility(danmakuSettings.showTop)
        danmakuContext.setFBDanmakuVisibility(danmakuSettings.showBottom)
        danmakuContext.setMaximumVisibleSizeInScreen(
            if (danmakuSettings.displayArea < 1f) (danmakuSettings.displayArea * 100).toInt() else -1
        )
    }

    // Apply playback speed
    LaunchedEffect(danmakuSettings.playbackSpeed) {
        exoPlayer.setPlaybackSpeed(if (isSpeedBoosting) 2f else danmakuSettings.playbackSpeed)
    }

    LaunchedEffect(isSpeedBoosting, danmakuSettings.playbackSpeed) {
        exoPlayer.setPlaybackSpeed(if (isSpeedBoosting) 2f else danmakuSettings.playbackSpeed)
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

    fun performSeek(targetPositionMs: Long) {
        val safeDuration = duration.coerceAtLeast(0L)
        val target = targetPositionMs.coerceIn(0L, safeDuration)
        exoPlayer.seekTo(target)
        if (danmakuPrepared) {
            try {
                danmakuView.seekTo(target)
            } catch (_: Exception) {
            }
        }
    }

    fun applyScreenBrightness(brightnessFraction: Float) {
        val window = activity?.window ?: return
        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightnessFraction.coerceIn(0.01f, 1f)
        window.attributes = layoutParams
    }

    fun applyStreamVolume(volumeFraction: Float) {
        val targetVolume = (volumeFraction.coerceIn(0f, 1f) * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
            .roundToInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
    }

    if (isLandscape || uiState.isFullscreen) {
        // Fullscreen player
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            PlayerSurface(
                exoPlayer = exoPlayer,
                danmakuView = danmakuView,
                isDanmakuVisible = uiState.isDanmakuVisible,
                videoSize = videoSize,
                onTextureViewReady = { playerTextureView = it },
                modifier = Modifier.fillMaxSize()
            )

            // Gesture layer
            GestureLayer(
                currentPosition = currentPosition,
                duration = duration,
                activity = activity,
                audioManager = audioManager,
                playbackSpeed = if (isSpeedBoosting) 2f else danmakuSettings.playbackSpeed,
                onToggleControls = { controlsVisible = !controlsVisible },
                onTogglePlayPause = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                onSeekPreviewStart = { startPosition ->
                    controlsVisible = true
                    isGestureSeeking = true
                    isGestureSeekCancelled = false
                    gestureOverlayMode = GestureOverlayMode.Seek
                    gestureSeekOriginalMs = startPosition
                    gestureSeekPreviewMs = startPosition
                },
                onSeekPreviewChange = { previewPosition, cancelled ->
                    gestureOverlayMode = GestureOverlayMode.Seek
                    gestureSeekPreviewMs = previewPosition
                    isGestureSeekCancelled = cancelled
                },
                onSeekPreviewFinish = { previewPosition, cancelled ->
                    isGestureSeeking = false
                    isGestureSeekCancelled = false
                    gestureSeekPreviewMs = previewPosition
                    gestureOverlayMode = null
                    if (!cancelled) {
                        performSeek(previewPosition)
                    }
                },
                onBrightnessChangeStart = { fraction ->
                    controlsVisible = true
                    gestureOverlayMode = GestureOverlayMode.Brightness
                    gestureBrightnessFraction = fraction
                    applyScreenBrightness(fraction)
                },
                onBrightnessChange = { fraction ->
                    gestureOverlayMode = GestureOverlayMode.Brightness
                    gestureBrightnessFraction = fraction
                    applyScreenBrightness(fraction)
                },
                onBrightnessChangeFinish = {
                    gestureOverlayMode = null
                },
                onVolumeChangeStart = { fraction ->
                    controlsVisible = true
                    gestureOverlayMode = GestureOverlayMode.Volume
                    gestureVolumeFraction = fraction
                    applyStreamVolume(fraction)
                },
                onVolumeChange = { fraction ->
                    gestureOverlayMode = GestureOverlayMode.Volume
                    gestureVolumeFraction = fraction
                    applyStreamVolume(fraction)
                },
                onVolumeChangeFinish = {
                    gestureOverlayMode = null
                },
                onSpeedBoostStart = {
                    controlsVisible = true
                    gestureOverlayMode = GestureOverlayMode.Speed
                    isSpeedBoosting = true
                },
                onSpeedBoostEnd = {
                    gestureOverlayMode = null
                    isSpeedBoosting = false
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
                            navigateBackWithSnapshot()
                        }
                    },
                    onPlayPause = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                    centerSeekPreviewMs = centerSeekPreviewMs,
                    onSeekPreviewChange = { previewPosition ->
                        controlsVisible = true
                        isSliderSeeking = true
                        sliderSeekPreviewMs = previewPosition
                    },
                    onSeekPreviewEnd = { previewPosition ->
                        isSliderSeeking = false
                        sliderSeekPreviewMs = 0L
                        performSeek(previewPosition)
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

            if (isGestureSeeking || isSliderSeeking || gestureOverlayMode == GestureOverlayMode.Brightness || gestureOverlayMode == GestureOverlayMode.Volume || gestureOverlayMode == GestureOverlayMode.Speed) {
                CenterSeekPreview(
                    mode = gestureOverlayMode ?: GestureOverlayMode.Seek,
                    previewPosition = centerSeekPreviewMs,
                    duration = duration,
                    deltaMs = if (isGestureSeeking) {
                        gestureSeekPreviewMs - gestureSeekOriginalMs
                    } else {
                        centerSeekPreviewMs - currentPosition
                    },
                    cancelled = isGestureSeeking && isGestureSeekCancelled,
                    brightnessFraction = gestureBrightnessFraction,
                    volumeFraction = gestureVolumeFraction,
                    speedBoostActive = isSpeedBoosting,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(48.dp),
                    color = Color.White
                )
            }

            if (showDanmakuSettings) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { showDanmakuSettings = false }
                        }
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
                    videoSize = videoSize,
                    onTextureViewReady = { playerTextureView = it },
                    modifier = Modifier.fillMaxSize()
                )

                GestureLayer(
                    currentPosition = currentPosition,
                    duration = duration,
                    activity = activity,
                    audioManager = audioManager,
                    playbackSpeed = if (isSpeedBoosting) 2f else danmakuSettings.playbackSpeed,
                    onToggleControls = { controlsVisible = !controlsVisible },
                    onTogglePlayPause = {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    onSeekPreviewStart = { startPosition ->
                        controlsVisible = true
                        isGestureSeeking = true
                        isGestureSeekCancelled = false
                        gestureOverlayMode = GestureOverlayMode.Seek
                        gestureSeekOriginalMs = startPosition
                        gestureSeekPreviewMs = startPosition
                    },
                    onSeekPreviewChange = { previewPosition, cancelled ->
                        gestureOverlayMode = GestureOverlayMode.Seek
                        gestureSeekPreviewMs = previewPosition
                        isGestureSeekCancelled = cancelled
                    },
                    onSeekPreviewFinish = { previewPosition, cancelled ->
                        isGestureSeeking = false
                        isGestureSeekCancelled = false
                        gestureSeekPreviewMs = previewPosition
                        gestureOverlayMode = null
                        if (!cancelled) {
                            performSeek(previewPosition)
                        }
                    },
                    onBrightnessChangeStart = { fraction ->
                        controlsVisible = true
                        gestureOverlayMode = GestureOverlayMode.Brightness
                        gestureBrightnessFraction = fraction
                        applyScreenBrightness(fraction)
                    },
                    onBrightnessChange = { fraction ->
                        gestureOverlayMode = GestureOverlayMode.Brightness
                        gestureBrightnessFraction = fraction
                        applyScreenBrightness(fraction)
                    },
                    onBrightnessChangeFinish = {
                        gestureOverlayMode = null
                    },
                    onVolumeChangeStart = { fraction ->
                        controlsVisible = true
                        gestureOverlayMode = GestureOverlayMode.Volume
                        gestureVolumeFraction = fraction
                        applyStreamVolume(fraction)
                    },
                    onVolumeChange = { fraction ->
                        gestureOverlayMode = GestureOverlayMode.Volume
                        gestureVolumeFraction = fraction
                        applyStreamVolume(fraction)
                    },
                    onVolumeChangeFinish = {
                        gestureOverlayMode = null
                    },
                    onSpeedBoostStart = {
                        controlsVisible = true
                        gestureOverlayMode = GestureOverlayMode.Speed
                        isSpeedBoosting = true
                    },
                    onSpeedBoostEnd = {
                        gestureOverlayMode = null
                        isSpeedBoosting = false
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
                        onBack = ::navigateBackWithSnapshot,
                        onPlayPause = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                        centerSeekPreviewMs = centerSeekPreviewMs,
                        onSeekPreviewChange = { previewPosition ->
                            controlsVisible = true
                            isSliderSeeking = true
                            sliderSeekPreviewMs = previewPosition
                        },
                        onSeekPreviewEnd = { previewPosition ->
                            isSliderSeeking = false
                            sliderSeekPreviewMs = 0L
                            performSeek(previewPosition)
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

                if (isGestureSeeking || isSliderSeeking || gestureOverlayMode == GestureOverlayMode.Brightness || gestureOverlayMode == GestureOverlayMode.Volume || gestureOverlayMode == GestureOverlayMode.Speed) {
                    CenterSeekPreview(
                        mode = gestureOverlayMode ?: GestureOverlayMode.Seek,
                        previewPosition = centerSeekPreviewMs,
                        duration = duration,
                        deltaMs = if (isGestureSeeking) {
                            gestureSeekPreviewMs - gestureSeekOriginalMs
                        } else {
                            centerSeekPreviewMs - currentPosition
                        },
                        cancelled = isGestureSeeking && isGestureSeekCancelled,
                        brightnessFraction = gestureBrightnessFraction,
                        volumeFraction = gestureVolumeFraction,
                        speedBoostActive = isSpeedBoosting,
                        modifier = Modifier.align(Alignment.Center)
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
            PlayerContentPager(
                currentMediaId = uiState.mediaId,
                currentPositionMs = currentPosition,
                title = uiState.title,
                animeTitle = uiState.animeTitle,
                episodes = uiState.episodes,
                danmakuSourceSummaries = uiState.danmakuSourceSummaries,
                isDanmakuLoading = uiState.isDanmakuLoading,
                isDanmakuRefreshing = uiState.isDanmakuRefreshing,
                onPlayEpisode = { episode ->
                    syncTerminalPlayState(currentPosition, duration)
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
                onCacheEpisodes = { episodes -> viewModel.cacheEpisodes(episodes) },
                onRefreshDanmaku = { viewModel.refreshDanmaku() },
                onUpdateDelay = { id, delay -> viewModel.updateSourceDelay(id, delay) },
                onUpdateTimeline = { id, timeline -> viewModel.updateSourceTimeline(id, timeline) },
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
    videoSize: VideoSize,
    onTextureViewReady: (android.view.TextureView) -> Unit,
    modifier: Modifier = Modifier
) {
    var playerSurfaceSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val surfaceRefreshKey = remember(videoSize, playerSurfaceSize) { videoSize to playerSurfaceSize }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                android.view.TextureView(ctx).also { textureView ->
                    exoPlayer.setVideoTextureView(textureView)
                    onTextureViewReady(textureView)
                }
            },
            update = { textureView ->
                surfaceRefreshKey
                onTextureViewReady(textureView)
                applyVideoTransform(textureView, videoSize)
            },
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { playerSurfaceSize = it }
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
    currentPosition: Long,
    duration: Long,
    activity: Activity?,
    audioManager: AudioManager,
    playbackSpeed: Float,
    onToggleControls: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekPreviewStart: (Long) -> Unit,
    onSeekPreviewChange: (Long, Boolean) -> Unit,
    onSeekPreviewFinish: (Long, Boolean) -> Unit,
    onBrightnessChangeStart: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onBrightnessChangeFinish: () -> Unit,
    onVolumeChangeStart: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeChangeFinish: () -> Unit,
    onSpeedBoostStart: () -> Unit,
    onSpeedBoostEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val horizontalSlopPx = with(density) { 12.dp.toPx() }
    val verticalSlopPx = with(density) { 12.dp.toPx() }
    val cancelThresholdPx = with(density) { 48.dp.toPx() }
    val maxSeekDeltaMs = minOf(180_000L, (duration * 0.2f).toLong().coerceAtLeast(30_000L))
    var layerWidthPx by remember { mutableIntStateOf(0) }
    var layerHeightPx by remember { mutableIntStateOf(0) }
    val latestCurrentPosition by rememberUpdatedState(currentPosition)
    val latestDuration by rememberUpdatedState(duration)
    val latestOnSeekPreviewStart by rememberUpdatedState(onSeekPreviewStart)
    val latestOnSeekPreviewChange by rememberUpdatedState(onSeekPreviewChange)
    val latestOnSeekPreviewFinish by rememberUpdatedState(onSeekPreviewFinish)
    val latestOnBrightnessChangeStart by rememberUpdatedState(onBrightnessChangeStart)
    val latestOnBrightnessChange by rememberUpdatedState(onBrightnessChange)
    val latestOnBrightnessChangeFinish by rememberUpdatedState(onBrightnessChangeFinish)
    val latestOnVolumeChangeStart by rememberUpdatedState(onVolumeChangeStart)
    val latestOnVolumeChange by rememberUpdatedState(onVolumeChange)
    val latestOnVolumeChangeFinish by rememberUpdatedState(onVolumeChangeFinish)
    val latestOnSpeedBoostStart by rememberUpdatedState(onSpeedBoostStart)
    val latestOnSpeedBoostEnd by rememberUpdatedState(onSpeedBoostEnd)
    val maxVolume = remember(audioManager) { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    val latestPlaybackSpeed by rememberUpdatedState(playbackSpeed)

    Box(
        modifier = modifier
            .onSizeChanged {
                layerWidthPx = it.width
                layerHeightPx = it.height
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { onTogglePlayPause() }
                )
            }
            .pointerInput(layerWidthPx, layerHeightPx) {
                coroutineScope {
                    awaitEachGesture {
                        val currentDuration = latestDuration
                        if (layerWidthPx <= 0 || layerHeightPx <= 0) return@awaitEachGesture

                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPosition = latestCurrentPosition
                        val startX = down.position.x
                        val startY = down.position.y
                        val startBrightness = activity?.window?.attributes?.screenBrightness
                            ?.takeIf { it >= 0f }
                            ?: 0.5f
                        val startVolume = if (maxVolume > 0) {
                            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume.toFloat()
                        } else {
                            0f
                        }
                        val isBottomRegion = startY >= layerHeightPx * 0.78f

                        var totalHorizontalDrag = 0f
                        var totalVerticalDrag = 0f
                        var seekActive = false
                        var brightnessActive = false
                        var volumeActive = false
                        var speedBoostActive = false
                        var speedBoostTriggered = false
                        var cancelled = false
                        var previewPosition = startPosition
                        var finishDispatched = false

                        val speedBoostJob = if (isBottomRegion) {
                            launch {
                                delay(2000)
                                speedBoostTriggered = true
                                speedBoostActive = true
                                latestOnSpeedBoostStart()
                            }
                        } else {
                            null
                        }

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                if (!change.pressed) {
                                    if (seekActive) {
                                        finishDispatched = true
                                        latestOnSeekPreviewFinish(previewPosition, cancelled)
                                    }
                                    break
                                }

                                val delta = change.position - change.previousPosition
                                totalHorizontalDrag += delta.x
                                totalVerticalDrag += delta.y
                                val horizontalDistance = abs(totalHorizontalDrag)
                                val verticalDistance = abs(totalVerticalDrag)

                                if (!seekActive && !brightnessActive && !volumeActive) {
                                    if (horizontalDistance >= horizontalSlopPx && horizontalDistance > verticalDistance) {
                                        speedBoostJob?.cancel()
                                        seekActive = true
                                        previewPosition = startPosition
                                        latestOnSeekPreviewStart(startPosition)
                                    } else if (verticalDistance >= verticalSlopPx && verticalDistance > horizontalDistance) {
                                        speedBoostJob?.cancel()
                                        if (startX < layerWidthPx / 2f) {
                                            brightnessActive = true
                                            latestOnBrightnessChangeStart(startBrightness)
                                        } else {
                                            volumeActive = true
                                            latestOnVolumeChangeStart(startVolume)
                                        }
                                    } else if (horizontalDistance >= horizontalSlopPx || verticalDistance >= verticalSlopPx) {
                                        speedBoostJob?.cancel()
                                    }
                                }

                                if (seekActive) {
                                    change.consume()
                                    val maxDelta = minOf(maxSeekDeltaMs, currentDuration)
                                    val clampedFraction = (totalHorizontalDrag / layerWidthPx.toFloat()).coerceIn(-1f, 1f)
                                    previewPosition = (startPosition + maxDelta * clampedFraction)
                                        .toLong()
                                        .coerceIn(0L, currentDuration)
                                    cancelled = -totalVerticalDrag >= cancelThresholdPx
                                    latestOnSeekPreviewChange(previewPosition, cancelled)
                                } else if (brightnessActive) {
                                    change.consume()
                                    val fraction = (startBrightness - totalVerticalDrag / layerHeightPx.toFloat())
                                        .coerceIn(0.01f, 1f)
                                    latestOnBrightnessChange(fraction)
                                } else if (volumeActive) {
                                    change.consume()
                                    val fraction = (startVolume - totalVerticalDrag / layerHeightPx.toFloat())
                                        .coerceIn(0f, 1f)
                                    latestOnVolumeChange(fraction)
                                } else if (speedBoostActive) {
                                    change.consume()
                                } else if (speedBoostTriggered) {
                                    change.consume()
                                }
                            }
                        } finally {
                            speedBoostJob?.cancel()
                            if (seekActive && !finishDispatched) {
                                latestOnSeekPreviewFinish(previewPosition, cancelled)
                            }
                            if (brightnessActive) {
                                latestOnBrightnessChangeFinish()
                            }
                            if (volumeActive) {
                                latestOnVolumeChangeFinish()
                            }
                            if (speedBoostTriggered || speedBoostActive || latestPlaybackSpeed == 2f) {
                                latestOnSpeedBoostEnd()
                            }
                        }
                    }
                }
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
    centerSeekPreviewMs: Long,
    onSeekPreviewChange: (Long) -> Unit,
    onSeekPreviewEnd: (Long) -> Unit,
    onToggleDanmaku: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onScreenshot: () -> Unit,
    onSendDanmaku: () -> Unit = {},
    onDanmakuSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPortraitControls = !isFullscreen
    val legacyOnSeek: (Long) -> Unit = onSeekPreviewEnd

    Box(modifier = modifier) {
        TopBottomGradientOverlay()

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
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
            IconButton(onClick = onScreenshot, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.CameraAlt, "截图", tint = Color.White)
                }
        }

        // Bottom bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactControlButton(onClick = onPlayPause) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White
                )
            }

            CompactControlButton(onClick = onToggleDanmaku) {
                Icon(
                    if (isDanmakuVisible) Icons.Default.Subtitles else Icons.Default.SubtitlesOff,
                    contentDescription = "弹幕开关",
                    tint = if (isDanmakuVisible) Color.White else Color.White.copy(alpha = 0.5f)
                )
            }

            SlimPlayerSeekBar(
                currentPosition = currentPosition,
                duration = duration,
                centerSeekPreviewMs = centerSeekPreviewMs,
                onSeekPreviewChange = onSeekPreviewChange,
                onSeekPreviewEnd = onSeekPreviewEnd,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.widthIn(min = if (isPortraitControls) 70.dp else 80.dp)
            )

            CompactControlButton(onClick = onSendDanmaku) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送弹幕",
                    tint = Color.White
                )
            }

            if (!isPortraitControls) {
                CompactControlButton(onClick = onDanmakuSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "弹幕设置", tint = Color.White)
                }
            }

            CompactControlButton(onClick = onToggleFullscreen) {
                Icon(
                    if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "全屏",
                    tint = Color.White
                )
            }
        }

        if (false) {
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
                onValueChangeFinished = { legacyOnSeek(sliderPosition.toLong()) },
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
}

@Composable
private fun CompactControlButton(
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun TopBottomGradientOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
        )
    }
}

@Composable
private fun CenterSeekPreview(
    mode: GestureOverlayMode,
    previewPosition: Long,
    duration: Long,
    deltaMs: Long,
    cancelled: Boolean,
    brightnessFraction: Float,
    volumeFraction: Float,
    speedBoostActive: Boolean,
    modifier: Modifier = Modifier
) {
    val deltaPrefix = when {
        deltaMs > 0L -> "+"
        deltaMs < 0L -> "-"
        else -> ""
    }
    val deltaText = "$deltaPrefix${formatTime(abs(deltaMs))}"
    val titleText = when (mode) {
        GestureOverlayMode.Seek -> formatTime(previewPosition)
        GestureOverlayMode.Brightness -> "${(brightnessFraction * 100).roundToInt()}%"
        GestureOverlayMode.Volume -> "${(volumeFraction * 100).roundToInt()}%"
        GestureOverlayMode.Speed -> "2.0x"
    }
    val detailText = when (mode) {
        GestureOverlayMode.Seek -> "$deltaText / ${formatTime(duration)}"
        GestureOverlayMode.Brightness -> "亮度"
        GestureOverlayMode.Volume -> "音量"
        GestureOverlayMode.Speed -> "长按加速播放中"
    }
    val hintText = when (mode) {
        GestureOverlayMode.Seek -> if (cancelled) "上滑后松开取消调整" else "松开后跳转"
        GestureOverlayMode.Brightness -> "左侧上下滑动调整"
        GestureOverlayMode.Volume -> "右侧上下滑动调整"
        GestureOverlayMode.Speed -> if (speedBoostActive) "松开后恢复原速" else "底部长按 2 秒触发"
    }

    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.64f), shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = titleText,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = detailText,
            color = Color.White.copy(alpha = 0.88f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = hintText,
            color = if (cancelled) Color(0xFFFFB74D) else Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun SlimPlayerSeekBar(
    currentPosition: Long,
    duration: Long,
    centerSeekPreviewMs: Long,
    onSeekPreviewChange: (Long) -> Unit,
    onSeekPreviewEnd: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxValue = duration.toFloat().coerceAtLeast(1f)
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isScrubbing by remember { mutableStateOf(false) }

    LaunchedEffect(currentPosition, maxValue, isScrubbing, centerSeekPreviewMs) {
        if (!isScrubbing) {
            sliderPosition = currentPosition.toFloat().coerceIn(0f, maxValue)
        }
    }

    KikoSlider(
        value = sliderPosition,
        onValueChange = {
            isScrubbing = true
            sliderPosition = it.coerceIn(0f, maxValue)
            onSeekPreviewChange(sliderPosition.toLong())
        },
        onValueChangeFinished = {
            isScrubbing = false
            onSeekPreviewEnd(sliderPosition.toLong())
        },
        valueRange = 0f..maxValue,
        modifier = modifier,
        activeColor = MaterialTheme.colorScheme.primary,
        inactiveColor = Color.White.copy(alpha = 0.28f),
        thumbDiameter = 10.dp,
        draggingThumbDiameter = 12.dp,
        sliderHeight = 18.dp
    )
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
    onCacheEpisodes: (List<EpisodeUiItem>) -> Unit,
    onRefreshDanmaku: () -> Unit,
    onUpdateDelay: (Int, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val episodeListState = rememberLazyListState()
    var isEpisodeSelectionMode by remember { mutableStateOf(false) }
    var selectedEpisodeIds by remember { mutableStateOf(setOf<String>()) }
    val tabs = listOf("剧集", "弹幕")

    LaunchedEffect(selectedTab, currentMediaId, episodes) {
        if (selectedTab != 0) return@LaunchedEffect
        val currentIndex = episodes.indexOfFirst { it.mediaId == currentMediaId }
        if (currentIndex >= 0) {
            episodeListState.scrollToItem(currentIndex)
        }
    }

    LaunchedEffect(episodes) {
        selectedEpisodeIds = selectedEpisodeIds.filterTo(linkedSetOf()) { selectedId ->
            episodes.any { it.mediaId == selectedId }
        }
        if (selectedEpisodeIds.isEmpty()) {
            isEpisodeSelectionMode = false
        }
    }

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
                    Column(modifier = Modifier.fillMaxSize()) {
                        androidx.compose.animation.AnimatedVisibility(visible = isEpisodeSelectionMode) {
                            EpisodeSelectionBar(
                                selectedCount = selectedEpisodeIds.size,
                                onSelectAll = { selectedEpisodeIds = episodes.map { it.mediaId }.toSet() },
                                onClearSelection = {
                                    isEpisodeSelectionMode = false
                                    selectedEpisodeIds = emptySet()
                                },
                                onCache = {
                                    onCacheEpisodes(episodes.filter { it.mediaId in selectedEpisodeIds })
                                    isEpisodeSelectionMode = false
                                    selectedEpisodeIds = emptySet()
                                }
                            )
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = episodeListState,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(episodes, key = { _, episode -> episode.mediaId }) { _, episode ->
                                val isCurrent = episode.mediaId == currentMediaId
                                val isSelected = selectedEpisodeIds.contains(episode.mediaId)
                                EpisodeListItem(
                                    episode = episode,
                                    isCurrent = isCurrent,
                                    isSelected = isSelected,
                                    isSelectionMode = isEpisodeSelectionMode,
                                    currentPositionMs = if (isCurrent) currentPositionMs else null,
                                    onClick = {
                                        if (isEpisodeSelectionMode) {
                                            val updatedSelection = selectedEpisodeIds.toMutableSet().apply {
                                                if (!add(episode.mediaId)) remove(episode.mediaId)
                                            }
                                            selectedEpisodeIds = updatedSelection
                                            if (updatedSelection.isEmpty()) {
                                                isEpisodeSelectionMode = false
                                            }
                                        } else {
                                            onPlayEpisode(episode)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isEpisodeSelectionMode) {
                                            isEpisodeSelectionMode = true
                                            selectedEpisodeIds = setOf(episode.mediaId)
                                        }
                                    }
                                )
                            }
                            item { Spacer(Modifier.height(8.dp)) }
                        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerContentPager(
    currentMediaId: String,
    currentPositionMs: Long,
    title: String,
    animeTitle: String?,
    episodes: List<EpisodeUiItem>,
    danmakuSourceSummaries: List<DanmakuSourceSummary>,
    isDanmakuLoading: Boolean,
    isDanmakuRefreshing: Boolean,
    onPlayEpisode: (EpisodeUiItem) -> Unit,
    onCacheEpisodes: (List<EpisodeUiItem>) -> Unit,
    onRefreshDanmaku: () -> Unit,
    onUpdateDelay: (Int, Long) -> Unit,
    onUpdateTimeline: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("剧集", "弹幕")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val episodeListState = rememberLazyListState()
    var isEpisodeSelectionMode by remember { mutableStateOf(false) }
    var selectedEpisodeIds by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(pagerState.currentPage, currentMediaId, episodes) {
        if (pagerState.currentPage != 0) return@LaunchedEffect
        val currentIndex = episodes.indexOfFirst { it.mediaId == currentMediaId }
        if (currentIndex >= 0) {
            episodeListState.scrollToItem(currentIndex)
        }
    }

    LaunchedEffect(episodes) {
        selectedEpisodeIds = selectedEpisodeIds.filterTo(linkedSetOf()) { selectedId ->
            episodes.any { it.mediaId == selectedId }
        }
        if (selectedEpisodeIds.isEmpty()) {
            isEpisodeSelectionMode = false
        }
    }

    Column(modifier = modifier) {
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

        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(tab) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> EpisodeTabPage(
                    currentMediaId = currentMediaId,
                    currentPositionMs = currentPositionMs,
                    episodes = episodes,
                    episodeListState = episodeListState,
                    isEpisodeSelectionMode = isEpisodeSelectionMode,
                    selectedEpisodeIds = selectedEpisodeIds,
                    onEpisodeSelectionModeChange = { isEpisodeSelectionMode = it },
                    onSelectedEpisodeIdsChange = { selectedEpisodeIds = it },
                    onPlayEpisode = onPlayEpisode,
                    onCacheEpisodes = onCacheEpisodes
                )

                1 -> DanmakuSourcesPage(
                    currentPositionMs = currentPositionMs,
                    danmakuSourceSummaries = danmakuSourceSummaries,
                    isDanmakuLoading = isDanmakuLoading,
                    isDanmakuRefreshing = isDanmakuRefreshing,
                    onRefreshDanmaku = onRefreshDanmaku,
                    onUpdateDelay = onUpdateDelay,
                    onUpdateTimeline = onUpdateTimeline
                )
            }
        }
    }
}

@Composable
private fun EpisodeTabPage(
    currentMediaId: String,
    currentPositionMs: Long,
    episodes: List<EpisodeUiItem>,
    episodeListState: androidx.compose.foundation.lazy.LazyListState,
    isEpisodeSelectionMode: Boolean,
    selectedEpisodeIds: Set<String>,
    onEpisodeSelectionModeChange: (Boolean) -> Unit,
    onSelectedEpisodeIdsChange: (Set<String>) -> Unit,
    onPlayEpisode: (EpisodeUiItem) -> Unit,
    onCacheEpisodes: (List<EpisodeUiItem>) -> Unit
) {
    if (episodes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("当前目录没有可播放的视频条目", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.animation.AnimatedVisibility(visible = isEpisodeSelectionMode) {
            EpisodeSelectionBar(
                selectedCount = selectedEpisodeIds.size,
                onSelectAll = { onSelectedEpisodeIdsChange(episodes.map { it.mediaId }.toSet()) },
                onClearSelection = {
                    onEpisodeSelectionModeChange(false)
                    onSelectedEpisodeIdsChange(emptySet())
                },
                onCache = {
                    onCacheEpisodes(episodes.filter { it.mediaId in selectedEpisodeIds })
                    onEpisodeSelectionModeChange(false)
                    onSelectedEpisodeIdsChange(emptySet())
                }
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = episodeListState,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(episodes, key = { _, episode -> episode.mediaId }) { _, episode ->
                val isCurrent = episode.mediaId == currentMediaId
                val isSelected = selectedEpisodeIds.contains(episode.mediaId)
                EpisodeListItem(
                    episode = episode,
                    isCurrent = isCurrent,
                    isSelected = isSelected,
                    isSelectionMode = isEpisodeSelectionMode,
                    currentPositionMs = if (isCurrent) currentPositionMs else null,
                    onClick = {
                        if (isEpisodeSelectionMode) {
                            val updatedSelection = selectedEpisodeIds.toMutableSet().apply {
                                if (!add(episode.mediaId)) remove(episode.mediaId)
                            }
                            onSelectedEpisodeIdsChange(updatedSelection)
                            if (updatedSelection.isEmpty()) {
                                onEpisodeSelectionModeChange(false)
                            }
                        } else {
                            onPlayEpisode(episode)
                        }
                    },
                    onLongClick = {
                        if (!isEpisodeSelectionMode) {
                            onEpisodeSelectionModeChange(true)
                            onSelectedEpisodeIdsChange(setOf(episode.mediaId))
                        }
                    }
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun DanmakuSourcesPage(
    currentPositionMs: Long,
    danmakuSourceSummaries: List<DanmakuSourceSummary>,
    isDanmakuLoading: Boolean,
    isDanmakuRefreshing: Boolean,
    onRefreshDanmaku: () -> Unit,
    onUpdateDelay: (Int, Long) -> Unit,
    onUpdateTimeline: (Int, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("弹幕来源", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (danmakuSourceSummaries.isEmpty()) {
                        "显示每个来源的数量、分布与同步信息"
                    } else {
                        "共 ${danmakuSourceSummaries.sumOf { it.commentCount }} 条弹幕"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = onRefreshDanmaku,
                enabled = !isDanmakuLoading && !isDanmakuRefreshing
            ) {
                if (isDanmakuRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text(if (isDanmakuRefreshing) "刷新中" else "刷新弹幕")
            }
        }

        if (isDanmakuLoading && danmakuSourceSummaries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (danmakuSourceSummaries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无弹幕源信息", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(danmakuSourceSummaries, key = { _, source -> source.id }) { _, source ->
                    DanmakuSourceCard(
                        source = source,
                        currentPositionMs = currentPositionMs,
                        onUpdateDelay = onUpdateDelay,
                        onUpdateTimeline = onUpdateTimeline
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun DanmakuSourceCard(
    source: DanmakuSourceSummary,
    currentPositionMs: Long,
    onUpdateDelay: (Int, Long) -> Unit,
    onUpdateTimeline: (Int, String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var initialTimePointMs by remember { mutableLongStateOf(0L) }

    if (showEditDialog) {
        DanmakuSourceEditDialog(
            source = source,
            initialTimePointMs = initialTimePointMs,
            onDismiss = { showEditDialog = false },
            onSave = { delayMs, timeline ->
                if (delayMs != source.delayMs) {
                    onUpdateDelay(source.id, delayMs)
                }
                if (timeline != (source.timeline ?: "")) {
                    onUpdateTimeline(source.id, timeline)
                }
                showEditDialog = false
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = buildList {
                        add("弹幕条数 ${source.commentCount}")
                        source.scriptName?.let { add(it) }
                        source.scriptId?.let { add(it) }
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = buildList {
                        add("延迟: ${formatDelaySeconds(source.delayMs)}")
                        add("时间轴段数: ${source.timelineSegmentCount}")
                        source.durationSeconds
                            ?.takeIf { it > 0.0 }
                            ?.let { add("源时长: ${formatSourceDuration(it)}") }
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!source.timeline.isNullOrBlank()) {
                    Text(
                        text = "时间轴: ${source.timeline}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = {
                    initialTimePointMs = currentPositionMs.coerceAtLeast(0L)
                    showEditDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑弹幕源"
                )
            }
        }
    }
}

private data class TimelineEditorEntry(
    val timePointMs: Long,
    val offsetMs: Long
)

private fun parseTimelineEditorEntries(timeline: String?): List<TimelineEditorEntry> {
    if (timeline.isNullOrBlank()) return emptyList()

    return timeline
        .split(';')
        .mapNotNull { segment ->
            val parts = segment
                .trim()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
            if (parts.size < 2) return@mapNotNull null

            val timePointMs = parts[0].toLongOrNull() ?: return@mapNotNull null
            val offsetMs = parts[1].toLongOrNull() ?: return@mapNotNull null
            TimelineEditorEntry(timePointMs = timePointMs, offsetMs = offsetMs)
        }
        .sortedBy { it.timePointMs }
        .fold(mutableListOf<TimelineEditorEntry>()) { acc, entry ->
            if (acc.isNotEmpty() && acc.last().timePointMs == entry.timePointMs) {
                acc[acc.lastIndex] = entry
            } else {
                acc += entry
            }
            acc
        }
}

private fun formatTimelineEditorEntries(entries: List<TimelineEditorEntry>): String {
    return entries
        .sortedBy { it.timePointMs }
        .joinToString(";") { entry -> "${entry.timePointMs} ${entry.offsetMs}" }
}

@Composable
private fun DanmakuSourceEditDialog(
    source: DanmakuSourceSummary,
    initialTimePointMs: Long,
    onDismiss: () -> Unit,
    onSave: (delayMs: Long, timeline: String) -> Unit
) {
    var delayText by remember(source.id, source.delayMs) {
        mutableStateOf(formatMillisecondsAsSeconds(source.delayMs))
    }
    var timelineEntries by remember(source.id, source.timeline) {
        mutableStateOf(parseTimelineEditorEntries(source.timeline))
    }
    var newTimePointText by remember(source.id, initialTimePointMs) {
        mutableStateOf(formatMillisecondsAsSeconds(initialTimePointMs.coerceAtLeast(0L)))
    }
    var newOffsetText by remember(source.id) { mutableStateOf("") }
    val parsedDelayMs = parseSecondsTextToMs(delayText, allowNegative = true)
    val isDelayValid = parsedDelayMs != null
    val parsedTimePointMs = parseSecondsTextToMs(newTimePointText, allowNegative = false)
    val parsedOffsetMs = parseSecondsTextToMs(newOffsetText, allowNegative = true)
    val isTimePointValid = parsedTimePointMs != null
    val isTimePointInputError = newTimePointText.isNotBlank() && !isTimePointValid
    val isOffsetInputError = newOffsetText.isNotBlank() && parsedOffsetMs == null
    val canAddTimelineEntry = parsedTimePointMs != null && parsedOffsetMs != null
    val hasExistingTimePoint = parsedTimePointMs != null && timelineEntries.any { it.timePointMs == parsedTimePointMs }
    val formattedTimeline = formatTimelineEditorEntries(timelineEntries)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑弹幕源") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = delayText,
                    onValueChange = { delayText = it },
                    label = { Text("延迟（秒）") },
                    placeholder = { Text("可输入小数或负数，例如 0.3") },
                    singleLine = true,
                    isError = !isDelayValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "时间轴（时间点 / 偏移单位：秒）",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = newTimePointText,
                        onValueChange = { newTimePointText = it },
                        label = { Text("时间点（秒）") },
                        placeholder = { Text("例如 362.317") },
                        singleLine = true,
                        isError = isTimePointInputError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = newOffsetText,
                        onValueChange = { newOffsetText = it },
                        label = { Text("偏移（秒）") },
                        placeholder = { Text("例如 -0.3") },
                        singleLine = true,
                        isError = isOffsetInputError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "输入单位都是秒，偏移支持负数。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilledTonalButton(
                    onClick = {
                        val timePointMs = parsedTimePointMs ?: return@FilledTonalButton
                        val offsetMs = parsedOffsetMs ?: return@FilledTonalButton
                        timelineEntries = (
                            timelineEntries.filterNot { it.timePointMs == timePointMs } +
                                TimelineEditorEntry(timePointMs = timePointMs, offsetMs = offsetMs)
                            )
                            .sortedBy { it.timePointMs }
                        newTimePointText = ""
                        newOffsetText = ""
                    },
                    enabled = canAddTimelineEntry,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (hasExistingTimePoint) "确认更新" else "确认添加")
                }
                if (timelineEntries.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(timelineEntries) { index, entry ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "时间点: ${formatMillisecondsAsSeconds(entry.timePointMs)} 秒",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "偏移: ${formatMillisecondsAsSeconds(entry.offsetMs)} 秒",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            timelineEntries = timelineEntries.filterIndexed { itemIndex, _ ->
                                                itemIndex != index
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "删除时间轴条目"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(parsedDelayMs ?: source.delayMs, formattedTimeline) },
                enabled = isDelayValid
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun EpisodeListItem(
    episode: EpisodeUiItem,
    isCurrent: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    currentPositionMs: Long?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val displayPlayTimeMs = currentPositionMs ?: episode.startPositionMs

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .combinedClickable(
                onClick = {
                    if (!isCurrent || isSelectionMode) {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else if (isCurrent) {
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
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onClick() })
                Spacer(Modifier.width(4.dp))
            }

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
private fun EpisodeSelectionBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onCache: () -> Unit
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
                Icon(Icons.Default.Close, contentDescription = "取消")
            }
            Text("已选 $selectedCount 项", style = MaterialTheme.typography.bodyMedium)
        }
        Row {
            IconButton(onClick = onCache, enabled = selectedCount > 0) {
                Icon(Icons.Default.Download, contentDescription = "缓存")
            }
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "全选")
            }
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

private fun formatSourceDuration(seconds: Double): String {
    return formatEpisodeDuration(seconds.toLong().coerceAtLeast(0L))
}

private fun formatDelaySeconds(delayMs: Long): String {
    val isNegative = delayMs < 0
    val absoluteMs = kotlin.math.abs(delayMs)
    val secondsPart = absoluteMs / 1000
    val millisPart = absoluteMs % 1000
    val fraction = when {
        millisPart == 0L -> ""
        millisPart % 100L == 0L -> ".${millisPart / 100}"
        millisPart % 10L == 0L -> ".${(millisPart / 10).toString().padStart(2, '0')}"
        else -> ".${millisPart.toString().padStart(3, '0')}"
    }
    val prefix = if (isNegative) "-" else ""
    return "${prefix}${secondsPart}${fraction}s"
}

private fun formatMillisecondsAsSeconds(valueMs: Long): String {
    val seconds = BigDecimal.valueOf(valueMs)
        .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP)
        .stripTrailingZeros()
    return seconds.toPlainString()
}

private fun parseSecondsTextToMs(text: String, allowNegative: Boolean): Long? {
    val normalized = text.trim()
    if (normalized.isEmpty()) return null

    val seconds = normalized.toBigDecimalOrNull() ?: return null
    if (!allowNegative && seconds < BigDecimal.ZERO) return null

    return seconds
        .multiply(BigDecimal.valueOf(1000))
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
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

@OptIn(UnstableApi::class)
private fun createVideoPlayer(
    context: android.content.Context,
    constrainedPlaybackDevice: Boolean
): ExoPlayer {
    val renderersFactory = DefaultRenderersFactory(context)
        .setEnableDecoderFallback(true)

    val loadControlBuilder = DefaultLoadControl.Builder()
    if (constrainedPlaybackDevice) {
        loadControlBuilder
            .setBufferDurationsMs(
                5_000,
                15_000,
                750,
                1_500
            )
            .setTargetBufferBytes(8 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(false)
    }

    return ExoPlayer.Builder(context, renderersFactory)
        .setLoadControl(loadControlBuilder.build())
        .build()
        .apply {
            if (constrainedPlaybackDevice) {
                setSeekParameters(SeekParameters.CLOSEST_SYNC)
            }
        }
}

private fun isConstrainedPlaybackDevice(context: android.content.Context): Boolean {
    val activityManager =
        context.getSystemService(Activity.ACTIVITY_SERVICE) as? android.app.ActivityManager
            ?: return false
    val memoryInfo = android.app.ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
    return activityManager.isLowRamDevice ||
        activityManager.memoryClass <= 192 ||
        activityManager.largeMemoryClass <= 256 ||
        memoryInfo.lowMemory
}

private fun Bitmap.toHistoryThumbnailBytes(
    maxWidth: Int = 320,
    videoSize: VideoSize = VideoSize.UNKNOWN
): ByteArray {
    val output = ByteArrayOutputStream()
    val thumbnailBitmap = renderHistoryThumbnail(maxWidth, videoSize)
    try {
        thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
        return output.toByteArray()
    } finally {
        if (thumbnailBitmap != this && !thumbnailBitmap.isRecycled) {
            thumbnailBitmap.recycle()
        }
    }
}

private fun canSafelyCaptureThumbnail(context: android.content.Context): Boolean {
    val activityManager = context.getSystemService(Activity.ACTIVITY_SERVICE) as? android.app.ActivityManager
        ?: return false
    val memoryInfo = android.app.ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    if (memoryInfo.lowMemory) return false

    val runtime = Runtime.getRuntime()
    val availableHeapBytes = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
    return availableHeapBytes >= 24L * 1024L * 1024L
}

private fun Bitmap.renderHistoryThumbnail(
    maxWidth: Int,
    videoSize: VideoSize
): Bitmap {
    if (width <= 0 || height <= 0 || maxWidth <= 0) return this

    val targetWidth = maxWidth
    val targetHeight = (targetWidth * 9f / 16f).roundToInt().coerceAtLeast(1)

    val pixelRatio = videoSize.pixelWidthHeightRatio.takeIf { it > 0f } ?: 1f
    val sourceAspect = when {
        videoSize.width > 0 && videoSize.height > 0 ->
            (videoSize.width.toFloat() * pixelRatio) / videoSize.height.toFloat()
        else -> width.toFloat() / height.toFloat()
    }
    val targetAspect = targetWidth.toFloat() / targetHeight.toFloat()
    val scaledWidth: Int
    val scaledHeight: Int

    if (sourceAspect > targetAspect) {
        scaledWidth = targetWidth
        scaledHeight = (targetWidth / sourceAspect).roundToInt().coerceAtLeast(1)
    } else {
        scaledHeight = targetHeight
        scaledWidth = (targetHeight * sourceAspect).roundToInt().coerceAtLeast(1)
    }

    val scaledBitmap = Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
    val outputBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(outputBitmap)
    canvas.drawColor(android.graphics.Color.BLACK)

    val left = ((targetWidth - scaledWidth) / 2f)
    val top = ((targetHeight - scaledHeight) / 2f)
    canvas.drawBitmap(scaledBitmap, left, top, null)

    if (scaledBitmap != this && !scaledBitmap.isRecycled) {
        scaledBitmap.recycle()
    }

    return outputBitmap
}

private fun applyVideoTransform(
    textureView: android.view.TextureView,
    videoSize: VideoSize
) {
    val viewWidth = textureView.width.toFloat()
    val viewHeight = textureView.height.toFloat()
    val videoWidth = videoSize.width.toFloat()
    val videoHeight = videoSize.height.toFloat()
    val pixelRatio = videoSize.pixelWidthHeightRatio.takeIf { it > 0f } ?: 1f

    if (viewWidth <= 0f || viewHeight <= 0f || videoWidth <= 0f || videoHeight <= 0f) {
        textureView.setTransform(null)
        return
    }

    val contentAspect = (videoWidth * pixelRatio) / videoHeight
    val viewAspect = viewWidth / viewHeight
    val scaleX: Float
    val scaleY: Float

    if (contentAspect > viewAspect) {
        scaleX = 1f
        scaleY = viewAspect / contentAspect
    } else {
        scaleX = contentAspect / viewAspect
        scaleY = 1f
    }

    val matrix = Matrix().apply {
        setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    }
    textureView.setTransform(matrix)
}

private fun List<EpisodeUiItem>.nextEpisodeAfter(currentMediaId: String): EpisodeUiItem? {
    val currentIndex = indexOfFirst { it.mediaId == currentMediaId }
    if (currentIndex < 0) return null
    return getOrNull(currentIndex + 1)
}
