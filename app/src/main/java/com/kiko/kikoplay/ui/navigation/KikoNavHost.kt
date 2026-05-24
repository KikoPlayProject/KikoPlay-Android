package com.kiko.kikoplay.ui.navigation

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.kiko.kikoplay.ui.cache.CacheScreen
import com.kiko.kikoplay.ui.connection.ConnectionScreen
import com.kiko.kikoplay.ui.home.HomeScreen
import com.kiko.kikoplay.ui.home.WatchHistoryScreen
import com.kiko.kikoplay.ui.local.LocalVideosScreen
import com.kiko.kikoplay.ui.player.PlayerPictureInPictureState
import com.kiko.kikoplay.ui.player.VideoPlayerScreen
import com.kiko.kikoplay.ui.playlist.PlaylistBrowserScreen
import com.kiko.kikoplay.ui.settings.SettingsScreen

@Composable
fun KikoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isInPictureInPictureMode: Boolean = false,
    backgroundPlaybackEnabled: Boolean = false,
    onPlayerPictureInPictureStateChange: (PlayerPictureInPictureState) -> Unit = {}
) {
    var lastBackNavigationAt by remember { mutableLongStateOf(0L) }

    fun navigateUpSafely() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBackNavigationAt < 350L) return
        val currentDestination = navController.currentDestination ?: return
        val isTopLevelDestination = TopLevelDestination.entries.any { destination ->
            currentDestination.hasRoute(destination.route::class)
        }
        if (isTopLevelDestination || currentDestination.id == navController.graph.findStartDestination().id) {
            return
        }
        lastBackNavigationAt = now
        navController.navigateUp()
    }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToConnection = { navController.navigate(LanConnectionRoute) },
                onNavigateToPlaylist = { navController.navigate(PlaylistBrowserRoute()) },
                onNavigateToPlayer = { target ->
                    navController.navigate(
                        VideoPlayerRoute(
                            mediaId = target.mediaId,
                            title = target.title,
                            sourceType = target.sourceType,
                            danmuPool = target.danmuPool,
                            animeTitle = target.animeTitle,
                            localPath = target.localPath,
                            serverAddress = target.serverAddress,
                            startPositionMs = target.startPositionMs,
                            initialPlayTimeState = target.initialPlayTimeState
                        )
                    )
                },
                onNavigateToHistory = { navController.navigate(WatchHistoryRoute) }
            )
        }

        composable<LocalVideosRoute> {
            LocalVideosScreen(
                onNavigateToPlayer = { mediaId, title, localPath ->
                    navController.navigate(
                        VideoPlayerRoute(
                            mediaId = mediaId,
                            title = title,
                            sourceType = 1,
                            localPath = localPath,
                            startPositionMs = 0L,
                            initialPlayTimeState = 0
                        )
                    )
                }
            )
        }

        composable<CacheManagementRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CacheManagementRoute>()
            CacheScreen(
                initialTab = route.initialTab,
                onNavigateToPlayer = { mediaId, title, danmuPool, localPath, serverAddress ->
                    navController.navigate(
                        VideoPlayerRoute(
                            mediaId = mediaId,
                            title = title,
                            sourceType = 2,
                            danmuPool = danmuPool,
                            localPath = localPath,
                            serverAddress = serverAddress,
                            startPositionMs = 0L,
                            initialPlayTimeState = 0
                        )
                    )
                }
            )
        }

        composable<SettingsRoute> {
            SettingsScreen()
        }

        composable<LanConnectionRoute> {
            ConnectionScreen(
                onBack = ::navigateUpSafely,
                onConnected = {
                    navController.navigate(PlaylistBrowserRoute()) {
                        popUpTo(LanConnectionRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<PlaylistBrowserRoute> {
            PlaylistBrowserScreen(
                onBack = ::navigateUpSafely,
                onPlayMedia = { targetRoute -> navController.navigate(targetRoute) }
            )
        }

        composable<VideoPlayerRoute> {
            VideoPlayerScreen(
                onBack = ::navigateUpSafely,
                onPlayMedia = { targetRoute ->
                    navController.popBackStack()
                    navController.navigate(targetRoute)
                },
                isInPictureInPictureMode = isInPictureInPictureMode,
                backgroundPlaybackEnabled = backgroundPlaybackEnabled,
                onPictureInPictureStateChange = onPlayerPictureInPictureStateChange
            )
        }

        composable<WatchHistoryRoute> {
            WatchHistoryScreen(
                onBack = ::navigateUpSafely,
                onNavigateToPlayer = { target ->
                    navController.navigate(
                        VideoPlayerRoute(
                            mediaId = target.mediaId,
                            title = target.title,
                            sourceType = target.sourceType,
                            danmuPool = target.danmuPool,
                            animeTitle = target.animeTitle,
                            localPath = target.localPath,
                            serverAddress = target.serverAddress,
                            startPositionMs = target.startPositionMs,
                            initialPlayTimeState = target.initialPlayTimeState
                        )
                    )
                }
            )
        }
    }
}
