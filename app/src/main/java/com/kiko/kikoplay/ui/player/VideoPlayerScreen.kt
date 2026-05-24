package com.kiko.kikoplay.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.AudioManager
import android.graphics.Matrix
import android.net.Uri
import android.graphics.Rect
import android.content.pm.ActivityInfo
import android.util.Rational
import android.view.View
import android.view.WindowManager
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.kiko.kikoplay.data.model.PlayerPreferences
import com.kiko.kikoplay.data.model.SubtitleStylePreset
import com.kiko.kikoplay.data.model.SubtitleTextSizePreset
import com.kiko.kikoplay.ui.navigation.VideoPlayerRoute
import com.kiko.kikoplay.ui.player.components.KikoSlider
import com.kiko.kikoplay.ui.player.danmaku.DanmakuSourceSummary
import com.kiko.kikoplay.ui.player.danmaku.DanmakuParser
import com.kiko.kikoplay.ui.player.danmaku.toPlayerPreferences
import com.kiko.kikoplay.ui.player.danmaku.toDanmakuSettings
import com.kiko.kikoplay.ui.player.subtitle.REMOTE_SUBTITLE_TRACK_ID
import com.kiko.kikoplay.ui.player.subtitle.SUBTITLE_TRACK_NONE_ID
import com.kiko.kikoplay.ui.player.subtitle.SubtitleTrackSelector
import com.kiko.kikoplay.ui.player.subtitle.SubtitleTrackSource
import com.kiko.kikoplay.ui.player.subtitle.SubtitleTrackUiItem
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    onPlayMedia: (VideoPlayerRoute) -> Unit,
    isInPictureInPictureMode: Boolean = false,
    backgroundPlaybackEnabled: Boolean = false,
    onPictureInPictureStateChange: (PlayerPictureInPictureState) -> Unit = {},
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isWideSplitWindow = configuration.screenWidthDp >= 840 && configuration.screenHeightDp >= 480
    val shouldUseFullscreenPlayer = uiState.isFullscreen || (isLandscape && !isWideSplitWindow)
    val isTabletSplitLayout = isLandscape && isWideSplitWindow && !uiState.isFullscreen
    val restoreLandscapeOnStart = remember { uiState.isFullscreen }
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
    var playbackErrorMessage by remember(uiState.mediaId) { mutableStateOf<String?>(null) }
    var playerPlaybackState by remember { mutableIntStateOf(exoPlayer.playbackState) }
    var playerPlayWhenReady by remember { mutableStateOf(exoPlayer.playWhenReady) }
    var videoSize by remember { mutableStateOf(VideoSize.UNKNOWN) }
    var currentSubtitleCues by remember { mutableStateOf<List<Cue>>(emptyList()) }
    var subtitleTracks by remember(uiState.mediaId) { mutableStateOf(listOf(SubtitleTrackSelector.NoneTrack)) }
    var selectedSubtitleTrackId by remember(uiState.mediaId) { mutableStateOf(SUBTITLE_TRACK_NONE_ID) }
    var userSelectedSubtitle by remember(uiState.mediaId) { mutableStateOf(false) }
    var defaultSubtitleApplied by remember(uiState.mediaId) { mutableStateOf(false) }
    var playerSurfaceRect by remember { mutableStateOf<Rect?>(null) }
    var autoAdvanceHandled by remember(uiState.mediaId) { mutableStateOf(false) }
    var playerTextureView by remember { mutableStateOf<android.view.TextureView?>(null) }
    val latestUiState by rememberUpdatedState(uiState)
    val latestCurrentPosition by rememberUpdatedState(currentPosition)
    val latestDuration by rememberUpdatedState(duration)
    val latestTextureView by rememberUpdatedState(playerTextureView)
    val latestVideoSize by rememberUpdatedState(videoSize)
    val latestIsLandscape by rememberUpdatedState(isLandscape)
    val latestOnPlayMedia by rememberUpdatedState(onPlayMedia)
    val finalHistorySaved = remember(uiState.mediaId) { AtomicBoolean(false) }
    val latestContext by rememberUpdatedState(context)
    val latestBackgroundPlaybackEnabled by rememberUpdatedState(backgroundPlaybackEnabled)
    val latestIsInPictureInPictureMode by rememberUpdatedState(isInPictureInPictureMode)
    val navigationScope = rememberCoroutineScope()
    var handoffFullscreenOnDispose by remember { mutableStateOf(false) }
    var pausedByBackgroundPolicy by remember { mutableStateOf(false) }

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
        val bitmap = runCatching { textureView.getBitmap(targetWidth, targetHeight) }.getOrNull()
        if (bitmap == null) return null
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

    fun shouldKeepLandscapeOnHandoff(): Boolean {
        return latestUiState.isFullscreen ||
            (latestIsLandscape && !isWideSplitWindow) ||
            isLandscapeRequested(activity?.requestedOrientation)
    }

    BackHandler {
        if (shouldUseFullscreenPlayer) {
            viewModel.setFullscreen(false)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            navigateBackWithSnapshot()
        }
    }

    fun applySubtitleTrack(track: SubtitleTrackUiItem, markUserSelection: Boolean) {
        val builder = exoPlayer.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)

        if (track.source == SubtitleTrackSource.NONE || track.trackGroup == null || track.trackIndex == C.INDEX_UNSET) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            selectedSubtitleTrackId = SUBTITLE_TRACK_NONE_ID
            currentSubtitleCues = emptyList()
        } else {
            builder
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(track.trackGroup, track.trackIndex))
            selectedSubtitleTrackId = track.id
        }

        exoPlayer.trackSelectionParameters = builder.build()
        if (markUserSelection) {
            userSelectedSubtitle = true
        }
    }

    fun resetTextTrackSelection() {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
    }

    fun togglePlayback() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            return
        }

        playbackErrorMessage = null
        if (exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare()
        }
        exoPlayer.play()
    }

    // Set media source with subtitle
    LaunchedEffect(uiState.mediaUrl, uiState.subtitleUrl, uiState.subtitleFormat, uiState.startPositionMs) {
        if (uiState.mediaUrl.isNotBlank()) {
            val builder = MediaItem.Builder().setUri(uiState.mediaUrl)
            if (!uiState.subtitleUrl.isNullOrBlank()) {
                val subtitle = MediaItem.SubtitleConfiguration.Builder(
                    Uri.parse(uiState.subtitleUrl)
                )
                    .setMimeType(SubtitleTrackSelector.remoteSubtitleMimeType(uiState.subtitleFormat))
                    .setLanguage("und")
                    .setLabel(uiState.subtitleLabel ?: SubtitleTrackSelector.remoteSubtitleLabel(uiState.subtitleFormat))
                    .setId(uiState.subtitleId ?: REMOTE_SUBTITLE_TRACK_ID)
                    .build()
                builder.setSubtitleConfigurations(listOf(subtitle))
            }
            val mediaItem = builder.build()
            val hasPreparedCurrentMedia = exoPlayer.currentMediaItem != null &&
                (exoPlayer.playbackState != Player.STATE_IDLE || exoPlayer.currentPosition > 0L)
            val startPosition = if (hasPreparedCurrentMedia) {
                exoPlayer.currentPosition.coerceAtLeast(0L)
            } else {
                uiState.startPositionMs
            }
            val shouldPlayWhenReady = exoPlayer.playWhenReady
            defaultSubtitleApplied = false
            currentSubtitleCues = emptyList()
            playbackErrorMessage = null
            if (!userSelectedSubtitle) {
                resetTextTrackSelection()
            }
            exoPlayer.setMediaItem(mediaItem, startPosition)
            exoPlayer.playWhenReady = shouldPlayWhenReady
            exoPlayer.prepare()
        }
    }

    LaunchedEffect(subtitleTracks, playerPlaybackState, userSelectedSubtitle, selectedSubtitleTrackId) {
        if (subtitleTracks.size <= 1) return@LaunchedEffect

        if (userSelectedSubtitle) {
            val selectedTrack = subtitleTracks.firstOrNull { it.id == selectedSubtitleTrackId }
            if (selectedTrack != null) {
                applySubtitleTrack(selectedTrack, markUserSelection = false)
            }
            return@LaunchedEffect
        }

        if (!defaultSubtitleApplied && playerPlaybackState == Player.STATE_READY) {
            defaultSubtitleApplied = true
            val defaultTrackId = SubtitleTrackSelector.defaultTrackId(subtitleTracks)
            val defaultTrack = subtitleTracks.firstOrNull { it.id == defaultTrackId }
                ?: SubtitleTrackSelector.NoneTrack
            applySubtitleTrack(defaultTrack, markUserSelection = false)
        }
    }

    // Position polling
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0)
            isPlaying = exoPlayer.isPlaying
            playerPlaybackState = exoPlayer.playbackState
            playerPlayWhenReady = exoPlayer.playWhenReady
            isBuffering = playerPlaybackState == Player.STATE_BUFFERING
            delay(200)
        }
    }

    DisposableEffect(exoPlayer, uiState.mediaId) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                playerPlaybackState = playbackState
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState != Player.STATE_ENDED || autoAdvanceHandled) return

                autoAdvanceHandled = true
                val finalPosition = latestDuration.takeIf { it > 0L } ?: latestCurrentPosition
                syncTerminalPlayState(finalPosition, latestDuration)

                val nextEpisode = latestUiState.episodes.nextEpisodeAfter(latestUiState.mediaId) ?: return
                navigationScope.launch {
                    val keepLandscape = shouldKeepLandscapeOnHandoff()
                    handoffFullscreenOnDispose = keepLandscape
                    latestOnPlayMedia(
                        viewModel.resolvePlaybackRouteForEpisode(nextEpisode)
                            .copy(startFullscreen = keepLandscape)
                    )
                }
            }

            override fun onVideoSizeChanged(newVideoSize: VideoSize) {
                videoSize = newVideoSize
            }

            override fun onTracksChanged(tracks: Tracks) {
                subtitleTracks = SubtitleTrackSelector.fromPlayerTracks(tracks)
            }

            override fun onCues(cueGroup: CueGroup) {
                currentSubtitleCues = cueGroup.cues
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackErrorMessage = if (
                    error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
                ) {
                    "视频解码失败，当前设备可能不支持该视频编码"
                } else {
                    "播放失败：${error.errorCodeName}"
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                playerPlayWhenReady = playWhenReady
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
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

    // Keep the screen awake while playback is expected to run, including buffering.
    val shouldKeepScreenOn = playerPlayWhenReady &&
        playerPlaybackState != Player.STATE_IDLE &&
        playerPlaybackState != Player.STATE_ENDED
    KeepScreenOnEffect(activity = activity, keepScreenOn = shouldKeepScreenOn)

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (
                        !latestBackgroundPlaybackEnabled &&
                        !latestIsInPictureInPictureMode &&
                        exoPlayer.playWhenReady &&
                        exoPlayer.playbackState != Player.STATE_ENDED
                    ) {
                        pausedByBackgroundPolicy = true
                        exoPlayer.pause()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (pausedByBackgroundPolicy) {
                        pausedByBackgroundPolicy = false
                        exoPlayer.play()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val canEnterPictureInPicture = playerPlayWhenReady &&
        uiState.mediaUrl.isNotBlank() &&
        (playerPlaybackState == Player.STATE_READY || playerPlaybackState == Player.STATE_BUFFERING)
    val pictureInPictureAspectRatio = remember(videoSize) {
        videoSize.toPictureInPictureAspectRatio()
    }

    LaunchedEffect(canEnterPictureInPicture, playerSurfaceRect, pictureInPictureAspectRatio) {
        onPictureInPictureStateChange(
            PlayerPictureInPictureState(
                canEnter = canEnterPictureInPicture,
                sourceRectHint = playerSurfaceRect?.let { Rect(it) },
                aspectRatio = pictureInPictureAspectRatio
            )
        )
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            onPictureInPictureStateChange(PlayerPictureInPictureState())
            if (currentPosition > 0) {
                syncTerminalPlayState(currentPosition, duration)
            }
            exoPlayer.release()
            if (!handoffFullscreenOnDispose) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    LaunchedEffect(restoreLandscapeOnStart) {
        if (restoreLandscapeOnStart && activity?.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    val shouldHideSystemBars = shouldUseFullscreenPlayer

    DisposableEffect(activity, shouldHideSystemBars) {
        val currentActivity = activity
        val window = currentActivity?.window
        val decorView = window?.decorView
        val controller = if (window != null && decorView != null) {
            WindowCompat.getInsetsController(window, decorView)
        } else {
            null
        }

        if (window != null && decorView != null && controller != null) {
            WindowCompat.setDecorFitsSystemWindows(window, !shouldHideSystemBars)
            window.statusBarColor = android.graphics.Color.BLACK
            window.navigationBarColor = android.graphics.Color.BLACK
            controller.isAppearanceLightStatusBars = false
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (shouldHideSystemBars) {
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
                if (!handoffFullscreenOnDispose) {
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    controller.show(WindowInsetsCompat.Type.systemBars())
                    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                }
            }
        }
    }

    LaunchedEffect(activity, shouldHideSystemBars) {
        if (!shouldHideSystemBars) return@LaunchedEffect
        val window = activity?.window ?: return@LaunchedEffect
        val decorView = window.decorView ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
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
    val isPortraitVideo = remember(videoSize) { isPortraitVideo(videoSize) }
    val showPortraitFullscreenToggle = !isLandscape && !uiState.isFullscreen && isPortraitVideo
    val canSendDanmaku = !uiState.danmuPool.isNullOrBlank()
    val showCenterGesturePreview = isGestureSeeking ||
        isSliderSeeking ||
        gestureOverlayMode == GestureOverlayMode.Brightness ||
        gestureOverlayMode == GestureOverlayMode.Volume
    val showSpeedBoostHint = gestureOverlayMode == GestureOverlayMode.Speed

    // Dialog states
    var showSendDanmaku by remember { mutableStateOf(false) }
    var showScreenshotDialog by remember { mutableStateOf(false) }
    var showDanmakuSettings by remember { mutableStateOf(false) }

    val danmakuSettings = uiState.playerPreferences.toDanmakuSettings()

    LaunchedEffect(isInPictureInPictureMode) {
        if (isInPictureInPictureMode) {
            controlsVisible = false
            showSendDanmaku = false
            showScreenshotDialog = false
            showDanmakuSettings = false
            isGestureSeeking = false
            isGestureSeekCancelled = false
            isSliderSeeking = false
            gestureOverlayMode = null
            isSpeedBoosting = false
        }
    }

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
    if (!isInPictureInPictureMode && showSendDanmaku && canSendDanmaku) {
        com.kiko.kikoplay.ui.player.danmaku.SendDanmakuSheet(
            onDismiss = { showSendDanmaku = false },
            onSend = { text, color, type ->
                viewModel.sendDanmaku(text, currentPosition, color, type)
            }
        )
    }

    // Screenshot/clip dialog
    if (!isInPictureInPictureMode && showScreenshotDialog) {
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

    if (isInPictureInPictureMode) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            PlayerSurface(
                exoPlayer = exoPlayer,
                danmakuView = danmakuView,
                isDanmakuVisible = false,
                subtitleCues = emptyList(),
                playerPreferences = uiState.playerPreferences,
                renderDanmakuLayer = false,
                renderSubtitleLayer = false,
                videoSize = videoSize,
                onTextureViewReady = { playerTextureView = it },
                onSourceRectChanged = { playerSurfaceRect = it },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else if (shouldUseFullscreenPlayer) {
        // Fullscreen player
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            PlayerSurface(
                exoPlayer = exoPlayer,
                danmakuView = danmakuView,
                isDanmakuVisible = uiState.isDanmakuVisible,
                subtitleCues = currentSubtitleCues,
                playerPreferences = uiState.playerPreferences,
                videoSize = videoSize,
                onTextureViewReady = { playerTextureView = it },
                onSourceRectChanged = { playerSurfaceRect = it },
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
                    togglePlayback()
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
                excludeTopGestureArea = true,
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
                    usePortraitControls = !isLandscape,
                    showPortraitFullscreenToggle = false,
                    canSendDanmaku = canSendDanmaku,
                    onBack = {
                        if (uiState.isFullscreen || isLandscape) {
                            viewModel.setFullscreen(false)
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        } else {
                            navigateBackWithSnapshot()
                        }
                    },
                    onPlayPause = { togglePlayback() },
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
                    onTogglePortraitFullscreen = {},
                    onScreenshot = { showScreenshotDialog = true },
                    onSendDanmaku = { showSendDanmaku = true },
                    onDanmakuSettings = { showDanmakuSettings = !showDanmakuSettings },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (showCenterGesturePreview) {
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

            if (showSpeedBoostHint) {
                SpeedBoostHint(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 56.dp)
                )
            }

            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(48.dp),
                    color = Color.White
                )
            }

            playbackErrorMessage?.let { message ->
                PlaybackErrorOverlay(
                    message = message,
                    modifier = Modifier.align(Alignment.Center)
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
                    onSettingsChange = { viewModel.updatePlayerPreferences(it.toPlayerPreferences(uiState.playerPreferences)) },
                    subtitleTracks = subtitleTracks,
                    selectedSubtitleTrackId = selectedSubtitleTrackId,
                    onSubtitleTrackSelected = { track -> applySubtitleTrack(track, markUserSelection = true) }
                )
            }
        }
    } else if (isTabletSplitLayout) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight()
                        .clipToBounds()
                        .background(Color.Black)
                ) {
                    PlayerSurface(
                        exoPlayer = exoPlayer,
                        danmakuView = danmakuView,
                        isDanmakuVisible = uiState.isDanmakuVisible,
                        subtitleCues = currentSubtitleCues,
                        playerPreferences = uiState.playerPreferences,
                        videoSize = videoSize,
                        onTextureViewReady = { playerTextureView = it },
                        onSourceRectChanged = { playerSurfaceRect = it },
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
                            togglePlayback()
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
                        excludeTopGestureArea = true,
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
                            usePortraitControls = false,
                            showPortraitFullscreenToggle = false,
                            canSendDanmaku = canSendDanmaku,
                            onBack = ::navigateBackWithSnapshot,
                            onPlayPause = { togglePlayback() },
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
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            },
                            onTogglePortraitFullscreen = {},
                            onScreenshot = { showScreenshotDialog = true },
                            onSendDanmaku = { showSendDanmaku = true },
                            onDanmakuSettings = { showDanmakuSettings = !showDanmakuSettings },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (showCenterGesturePreview) {
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

                    if (showSpeedBoostHint) {
                        SpeedBoostHint(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(top = 56.dp)
                        )
                    }

                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center).size(48.dp),
                            color = Color.White
                        )
                    }

                    playbackErrorMessage?.let { message ->
                        PlaybackErrorOverlay(
                            message = message,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                )

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
                        navigationScope.launch {
                            val keepLandscape = shouldUseFullscreenPlayer ||
                                isLandscapeRequested(activity?.requestedOrientation)
                            handoffFullscreenOnDispose = keepLandscape
                            onPlayMedia(
                                viewModel.resolvePlaybackRouteForEpisode(episode)
                                    .copy(startFullscreen = keepLandscape)
                            )
                        }
                    },
                    onCacheEpisodes = { episodes -> viewModel.cacheEpisodes(episodes) },
                    onRefreshDanmaku = { viewModel.refreshDanmaku() },
                    onUpdateDelay = { id, delay -> viewModel.updateSourceDelay(id, delay) },
                    onUpdateTimeline = { id, timeline -> viewModel.updateSourceTimeline(id, timeline) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
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

            androidx.compose.animation.AnimatedVisibility(
                visible = showDanmakuSettings,
                enter = androidx.compose.animation.slideInHorizontally { it },
                exit = androidx.compose.animation.slideOutHorizontally { it },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                com.kiko.kikoplay.ui.player.danmaku.DanmakuSettingsPanel(
                    settings = danmakuSettings,
                    onSettingsChange = { viewModel.updatePlayerPreferences(it.toPlayerPreferences(uiState.playerPreferences)) },
                    subtitleTracks = subtitleTracks,
                    selectedSubtitleTrackId = selectedSubtitleTrackId,
                    onSubtitleTrackSelected = { track -> applySubtitleTrack(track, markUserSelection = true) }
                )
            }
        }
    } else {
        // Portrait: player on top + content below
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
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
                    subtitleCues = currentSubtitleCues,
                    playerPreferences = uiState.playerPreferences,
                    videoSize = videoSize,
                    onTextureViewReady = { playerTextureView = it },
                    onSourceRectChanged = { playerSurfaceRect = it },
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
                        togglePlayback()
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
                        usePortraitControls = true,
                        showPortraitFullscreenToggle = showPortraitFullscreenToggle,
                        canSendDanmaku = canSendDanmaku,
                        onBack = ::navigateBackWithSnapshot,
                        onPlayPause = { togglePlayback() },
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
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        },
                        onTogglePortraitFullscreen = {
                            controlsVisible = true
                            viewModel.setFullscreen(true)
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        },
                        onScreenshot = { showScreenshotDialog = true },
                        onSendDanmaku = { showSendDanmaku = true },
                        onDanmakuSettings = { showDanmakuSettings = !showDanmakuSettings },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (showCenterGesturePreview) {
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

                if (showSpeedBoostHint) {
                    SpeedBoostHint(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 56.dp)
                    )
                }

                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(36.dp),
                        color = Color.White
                    )
                }

                playbackErrorMessage?.let { message ->
                    PlaybackErrorOverlay(
                        message = message,
                        modifier = Modifier.align(Alignment.Center)
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
                    navigationScope.launch {
                        val keepLandscape = shouldUseFullscreenPlayer ||
                            isLandscapeRequested(activity?.requestedOrientation)
                        handoffFullscreenOnDispose = keepLandscape
                        onPlayMedia(
                            viewModel.resolvePlaybackRouteForEpisode(episode)
                                .copy(startFullscreen = keepLandscape)
                        )
                    }
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

@Composable
private fun PlaybackErrorOverlay(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(horizontal = 32.dp),
        color = Color.Black.copy(alpha = 0.62f),
        contentColor = Color.White,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun KeepScreenOnEffect(
    activity: Activity?,
    keepScreenOn: Boolean
) {
    val view = LocalView.current
    DisposableEffect(activity, view, keepScreenOn) {
        val window = activity?.window
        val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        if (keepScreenOn) {
            window?.addFlags(flag)
            view.keepScreenOn = true
        } else {
            window?.clearFlags(flag)
            view.keepScreenOn = false
        }

        onDispose {
            // Do not clear the flag on dispose — when navigating between player instances
            // (auto-advance to next episode), the old composable disposes before the new one
            // sets up its effect. Clearing here would create a gap where the system can sleep.
            // The flag is only cleared explicitly when keepScreenOn becomes false above.
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerSurface(
    exoPlayer: ExoPlayer,
    danmakuView: DanmakuView,
    isDanmakuVisible: Boolean,
    subtitleCues: List<Cue>,
    playerPreferences: PlayerPreferences,
    renderDanmakuLayer: Boolean = true,
    renderSubtitleLayer: Boolean = true,
    videoSize: VideoSize,
    onTextureViewReady: (android.view.TextureView) -> Unit,
    onSourceRectChanged: (Rect) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var playerSurfaceSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val surfaceRefreshKey = remember(videoSize, playerSurfaceSize) { videoSize to playerSurfaceSize }

    Box(
        modifier = modifier
            .clipToBounds()
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                if (bounds.width > 0f && bounds.height > 0f) {
                    onSourceRectChanged(
                        Rect(
                            bounds.left.roundToInt(),
                            bounds.top.roundToInt(),
                            bounds.right.roundToInt(),
                            bounds.bottom.roundToInt()
                        )
                    )
                }
            }
    ) {
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

        if (renderDanmakuLayer) {
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

        if (renderSubtitleLayer) {
            AndroidView(
                factory = { context ->
                    SubtitleView(context).apply {
                        setUserDefaultStyle()
                        setUserDefaultTextSize()
                    }
                },
                update = { subtitleView ->
                    subtitleView.applyPlayerSubtitlePreferences(playerPreferences)
                    subtitleView.setCues(subtitleCues)
                    subtitleView.visibility = if (subtitleCues.isEmpty()) {
                        android.view.View.GONE
                    } else {
                        android.view.View.VISIBLE
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun SubtitleView.applyPlayerSubtitlePreferences(preferences: PlayerPreferences) {
    when (preferences.subtitleStylePreset) {
        SubtitleStylePreset.SYSTEM -> setUserDefaultStyle()
        SubtitleStylePreset.BLACK_BACKGROUND -> {
            setStyle(
                CaptionStyleCompat(
                    android.graphics.Color.WHITE,
                    android.graphics.Color.BLACK,
                    android.graphics.Color.TRANSPARENT,
                    CaptionStyleCompat.EDGE_TYPE_NONE,
                    android.graphics.Color.TRANSPARENT,
                    null
                )
            )
        }

        SubtitleStylePreset.TRANSLUCENT_BLACK_BACKGROUND -> {
            setStyle(
                CaptionStyleCompat(
                    android.graphics.Color.WHITE,
                    android.graphics.Color.argb(180, 0, 0, 0),
                    android.graphics.Color.TRANSPARENT,
                    CaptionStyleCompat.EDGE_TYPE_NONE,
                    android.graphics.Color.TRANSPARENT,
                    null
                )
            )
        }

        SubtitleStylePreset.OUTLINE -> {
            setStyle(
                CaptionStyleCompat(
                    android.graphics.Color.WHITE,
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                    CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                    android.graphics.Color.BLACK,
                    null
                )
            )
        }

        SubtitleStylePreset.SHADOW -> {
            setStyle(
                CaptionStyleCompat(
                    android.graphics.Color.WHITE,
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                    CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                    android.graphics.Color.BLACK,
                    null
                )
            )
        }

        SubtitleStylePreset.YELLOW_OUTLINE -> {
            setStyle(
                CaptionStyleCompat(
                    android.graphics.Color.rgb(255, 235, 59),
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                    CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                    android.graphics.Color.BLACK,
                    null
                )
            )
        }

        SubtitleStylePreset.YELLOW_SHADOW -> {
            setStyle(
                CaptionStyleCompat(
                    android.graphics.Color.rgb(255, 235, 59),
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                    CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                    android.graphics.Color.BLACK,
                    null
                )
            )
        }
    }

    when (preferences.subtitleTextSizePreset) {
        SubtitleTextSizePreset.SYSTEM -> setUserDefaultTextSize()
        SubtitleTextSizePreset.SMALL -> setFractionalTextSize(0.04f, false)
        SubtitleTextSizePreset.MEDIUM -> setFractionalTextSize(0.0533f, false)
        SubtitleTextSizePreset.LARGE -> setFractionalTextSize(0.065f, false)
        SubtitleTextSizePreset.EXTRA_LARGE -> setFractionalTextSize(0.078f, false)
    }

    val useEmbeddedStyle = preferences.subtitleStylePreset == SubtitleStylePreset.SYSTEM
    val useEmbeddedFontSize = preferences.subtitleStylePreset == SubtitleStylePreset.SYSTEM &&
        preferences.subtitleTextSizePreset == SubtitleTextSizePreset.SYSTEM
    setApplyEmbeddedStyles(useEmbeddedStyle)
    setApplyEmbeddedFontSizes(useEmbeddedFontSize)
    setBottomPaddingFraction(0.08f)
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
    excludeTopGestureArea: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val horizontalSlopPx = with(density) { 12.dp.toPx() }
    val verticalSlopPx = with(density) { 12.dp.toPx() }
    val topGestureExclusionPx = if (excludeTopGestureArea) with(density) { 72.dp.toPx() } else 0f
    val cancelThresholdPx = with(density) { 48.dp.toPx() }
    val verticalAdjustmentScale = 0.7f
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
    val latestHapticFeedback by rememberUpdatedState(hapticFeedback)
    val maxVolume = remember(audioManager) { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    val latestPlaybackSpeed by rememberUpdatedState(playbackSpeed)

    Box(
        modifier = modifier
            .onSizeChanged {
                layerWidthPx = it.width
                layerHeightPx = it.height
            }
            .pointerInput(topGestureExclusionPx) {
                detectTapGestures(
                    onTap = { offset ->
                        if (offset.y >= topGestureExclusionPx) onToggleControls()
                    },
                    onDoubleTap = { offset ->
                        if (offset.y >= topGestureExclusionPx) onTogglePlayPause()
                    }
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
                        val verticalControlRegion = when {
                            startX < layerWidthPx / 3f -> GestureOverlayMode.Brightness
                            startX > layerWidthPx * 2f / 3f -> GestureOverlayMode.Volume
                            else -> null
                        }
                        val gesturesEnabled = startY >= topGestureExclusionPx
                        val startBrightness = activity?.window?.attributes?.screenBrightness
                            ?.takeIf { it >= 0f }
                            ?: 0.5f
                        val startVolume = if (maxVolume > 0) {
                            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume.toFloat()
                        } else {
                            0f
                        }
                        val isBottomRegion = startY >= layerHeightPx * 0.5f

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

                        val speedBoostJob = if (gesturesEnabled && isBottomRegion) {
                            launch {
                                delay(1000)
                                speedBoostTriggered = true
                                speedBoostActive = true
                                latestHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
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

                                if (!gesturesEnabled) {
                                    if (horizontalDistance >= horizontalSlopPx || verticalDistance >= verticalSlopPx) {
                                        speedBoostJob?.cancel()
                                    }
                                    continue
                                }

                                if (!seekActive && !brightnessActive && !volumeActive) {
                                    if (horizontalDistance >= horizontalSlopPx && horizontalDistance > verticalDistance) {
                                        speedBoostJob?.cancel()
                                        seekActive = true
                                        previewPosition = startPosition
                                        latestOnSeekPreviewStart(startPosition)
                                    } else if (
                                        verticalControlRegion != null &&
                                        verticalDistance >= verticalSlopPx &&
                                        verticalDistance > horizontalDistance
                                    ) {
                                        speedBoostJob?.cancel()
                                        if (verticalControlRegion == GestureOverlayMode.Brightness) {
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
                                    val fraction = (startBrightness - (totalVerticalDrag / layerHeightPx.toFloat()) * verticalAdjustmentScale)
                                        .coerceIn(0.01f, 1f)
                                    latestOnBrightnessChange(fraction)
                                } else if (volumeActive) {
                                    change.consume()
                                    val fraction = (startVolume - (totalVerticalDrag / layerHeightPx.toFloat()) * verticalAdjustmentScale)
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
    usePortraitControls: Boolean,
    showPortraitFullscreenToggle: Boolean,
    canSendDanmaku: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    centerSeekPreviewMs: Long,
    onSeekPreviewChange: (Long) -> Unit,
    onSeekPreviewEnd: (Long) -> Unit,
    onToggleDanmaku: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onTogglePortraitFullscreen: () -> Unit,
    onScreenshot: () -> Unit,
    onSendDanmaku: () -> Unit = {},
    onDanmakuSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPortraitControls = usePortraitControls
    val legacyOnSeek: (Long) -> Unit = onSeekPreviewEnd
    val currentClockText = rememberCurrentClockText()

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

        if (!isPortraitControls) {
            Text(
                text = currentClockText,
                color = Color.White,
                fontSize = 11.sp,
                lineHeight = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
            )
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

            if (canSendDanmaku) {
                CompactControlButton(onClick = onSendDanmaku) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送弹幕",
                        tint = Color.White
                    )
                }
            }

            if (!isPortraitControls) {
                CompactControlButton(onClick = onDanmakuSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "弹幕设置", tint = Color.White)
                }
            }

            if (showPortraitFullscreenToggle) {
                CompactControlButton(onClick = onTogglePortraitFullscreen) {
                    Text(
                        text = "竖",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
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

                if (canSendDanmaku) {
                    // Send danmaku
                    IconButton(onClick = onSendDanmaku) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送弹幕", tint = Color.White)
                    }
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
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .widthIn(min = 32.dp)
            .height(32.dp)
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
        GestureOverlayMode.Speed -> if (speedBoostActive) "松开后恢复原速" else "下半区域长按 1 秒触发"
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
private fun SpeedBoostHint(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.42f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "2.0x",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
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
        PlayerContentHeader(
            title = title,
            animeTitle = animeTitle
        )

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
    if (episodes.isEmpty()) {
        Column(modifier = modifier) {
            PlayerContentHeader(
                title = title,
                animeTitle = animeTitle
            )
            DanmakuSourcesPage(
                currentPositionMs = currentPositionMs,
                danmakuSourceSummaries = danmakuSourceSummaries,
                isDanmakuLoading = isDanmakuLoading,
                isDanmakuRefreshing = isDanmakuRefreshing,
                onRefreshDanmaku = onRefreshDanmaku,
                onUpdateDelay = onUpdateDelay,
                onUpdateTimeline = onUpdateTimeline
            )
        }
        return
    }

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
        PlayerContentHeader(
            title = title,
            animeTitle = animeTitle
        )

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
private fun PlayerContentHeader(
    title: String,
    animeTitle: String?
) {
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("弹幕来源", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (danmakuSourceSummaries.isEmpty()) {
                        "显示每个来源的数量、分布与同步信息"
                    } else {
                        val totalCount = danmakuSourceSummaries.sumOf { it.commentCount }
                        val totalSenders = danmakuSourceSummaries.sumOf { it.senderCount }
                        if (totalSenders > 0) "共 $totalCount 条弹幕 · $totalSenders 位发送者"
                        else "共 $totalCount 条弹幕"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onRefreshDanmaku,
                enabled = !isDanmakuLoading && !isDanmakuRefreshing
            ) {
                if (isDanmakuRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(danmakuSourceSummaries, key = { _, source -> source.id }) { _, source ->
                    DanmakuSourceCard(
                        source = source,
                        currentPositionMs = currentPositionMs,
                        onUpdateDelay = onUpdateDelay,
                        onUpdateTimeline = onUpdateTimeline
                    )
                }
                item { Spacer(Modifier.height(10.dp)) }
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    val scriptInfo = buildList {
                        source.scriptName?.let { add(it) }
                        source.scriptId?.let { add(it) }
                    }.joinToString(" · ")
                    if (scriptInfo.isNotEmpty()) {
                        Text(
                            text = scriptInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = {
                        initialTimePointMs = currentPositionMs.coerceAtLeast(0L)
                        showEditDialog = true
                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑弹幕源",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DanmakuStatChip(
                    count = source.commentCount,
                    label = "总计",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                DanmakuStatChip(
                    count = source.scrollCount,
                    label = "滚动",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                DanmakuStatChip(
                    count = source.topCount,
                    label = "顶部",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                DanmakuStatChip(
                    count = source.bottomCount,
                    label = "底部",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SyncInfoItem(
                    icon = Icons.Default.Timer,
                    text = "延迟 ${formatDelaySeconds(source.delayMs)}"
                )
                if (source.timelineSegmentCount > 0) {
                    SyncInfoItem(
                        icon = Icons.Default.Timeline,
                        text = "时间轴 ×${source.timelineSegmentCount}"
                    )
                }
                source.durationSeconds
                    ?.takeIf { it > 0.0 }
                    ?.let {
                        SyncInfoItem(
                            icon = Icons.Default.Schedule,
                            text = formatSourceDuration(it)
                        )
                    }
            }
        }
    }
}

@Composable
private fun DanmakuStatChip(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatCount(count),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SyncInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                if (displayPlayTimeMs > 0L || episode.isCached) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (displayPlayTimeMs > 0L) {
                            Text(
                                text = formatEpisodeDuration(displayPlayTimeMs / 1000),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (episode.isCached) {
                            Text(
                                text = "\u5df2\u7f13\u5b58",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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

private fun formatCount(count: Int): String {
    return when {
        count >= 10_000 -> String.format("%.1fw", count / 10_000.0)
        count >= 1_000 -> String.format("%.1fk", count / 1000.0)
        else -> count.toString()
    }
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

@Composable
private fun rememberCurrentClockText(): String {
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var currentClockText by remember { mutableStateOf(formatter.format(Date())) }

    LaunchedEffect(formatter) {
        while (true) {
            currentClockText = formatter.format(Date())
            val now = System.currentTimeMillis()
            val delayToNextMinute = 60_000L - (now % 60_000L)
            delay(delayToNextMinute.coerceAtLeast(1L))
        }
    }

    return currentClockText
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
                15_000,
                60_000,
                1_500,
                5_000
            )
            .setTargetBufferBytes(64 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(true)
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

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun Bitmap.toHistoryThumbnailBytes(
    maxWidth: Int = 320,
    videoSize: VideoSize = VideoSize.UNKNOWN
): ByteArray {
    val output = ByteArrayOutputStream()
    val thumbnailBitmap = renderHistoryThumbnail(maxWidth, videoSize)
    try {
        thumbnailBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
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

private fun isPortraitVideo(videoSize: VideoSize): Boolean {
    val videoWidth = videoSize.width
    val videoHeight = videoSize.height
    if (videoWidth <= 0 || videoHeight <= 0) return false

    val pixelRatio = videoSize.pixelWidthHeightRatio.takeIf { it > 0f } ?: 1f
    val displayAspectRatio = (videoWidth.toFloat() * pixelRatio) / videoHeight.toFloat()
    return displayAspectRatio < 1f
}

private fun VideoSize.toPictureInPictureAspectRatio(): Rational {
    if (width <= 0 || height <= 0) return Rational(16, 9)

    val pixelRatio = pixelWidthHeightRatio.takeIf { it > 0f } ?: 1f
    val rawAspectRatio = (width.toFloat() * pixelRatio) / height.toFloat()
    val clampedAspectRatio = rawAspectRatio.coerceIn(0.42f, 2.39f)
    return Rational((clampedAspectRatio * 1000).roundToInt().coerceAtLeast(1), 1000)
}

private fun isLandscapeRequested(requestedOrientation: Int?): Boolean {
    return requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
        requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE ||
        requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE ||
        requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
}

private fun List<EpisodeUiItem>.nextEpisodeAfter(currentMediaId: String): EpisodeUiItem? {
    val currentIndex = indexOfFirst { it.mediaId == currentMediaId }
    if (currentIndex < 0) return null
    return getOrNull(currentIndex + 1)
}
