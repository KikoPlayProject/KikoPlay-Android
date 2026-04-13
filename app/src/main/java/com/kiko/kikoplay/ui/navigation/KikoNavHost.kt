package com.kiko.kikoplay.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kiko.kikoplay.ui.cache.CacheScreen
import com.kiko.kikoplay.ui.connection.ConnectionScreen
import com.kiko.kikoplay.ui.home.HomeScreen
import com.kiko.kikoplay.ui.home.WatchHistoryScreen
import com.kiko.kikoplay.ui.local.LocalVideosScreen
import com.kiko.kikoplay.ui.player.VideoPlayerScreen
import com.kiko.kikoplay.ui.playlist.PlaylistBrowserScreen
import com.kiko.kikoplay.ui.settings.SettingsScreen

@Composable
fun KikoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
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

        composable<CacheManagementRoute> {
            CacheScreen(
                onNavigateToPlayer = { mediaId, title, danmuPool, localPath ->
                    navController.navigate(
                        VideoPlayerRoute(
                            mediaId = mediaId,
                            title = title,
                            sourceType = 2,
                            danmuPool = danmuPool,
                            localPath = localPath,
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
                onBack = { navController.popBackStack() },
                onConnected = {
                    navController.navigate(PlaylistBrowserRoute()) {
                        popUpTo(LanConnectionRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<PlaylistBrowserRoute> {
            PlaylistBrowserScreen(
                onBack = { navController.popBackStack() },
                onPlayMedia = { mediaId, title, danmuPool, animeTitle, parentPath, startPositionMs, initialPlayTimeState ->
                    navController.navigate(
                        VideoPlayerRoute(
                            mediaId = mediaId,
                            title = title,
                            danmuPool = danmuPool,
                            animeTitle = animeTitle,
                            parentPath = parentPath,
                            startPositionMs = startPositionMs,
                            initialPlayTimeState = initialPlayTimeState
                        )
                    )
                }
            )
        }

        composable<VideoPlayerRoute> {
            VideoPlayerScreen(
                onBack = { navController.popBackStack() },
                onPlayMedia = { mediaId, title, danmuPool, animeTitle, parentPath, startPositionMs, initialPlayTimeState ->
                    navController.popBackStack()
                    navController.navigate(
                        VideoPlayerRoute(
                            mediaId = mediaId,
                            title = title,
                            danmuPool = danmuPool,
                            animeTitle = animeTitle,
                            parentPath = parentPath,
                            startPositionMs = startPositionMs,
                            initialPlayTimeState = initialPlayTimeState
                        )
                    )
                }
            )
        }

        composable<WatchHistoryRoute> {
            WatchHistoryScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPlayer = { target ->
                    navController.navigate(
                        VideoPlayerRoute(
                            mediaId = target.mediaId,
                            title = target.title,
                            sourceType = target.sourceType,
                            danmuPool = target.danmuPool,
                            animeTitle = target.animeTitle,
                            localPath = target.localPath,
                            startPositionMs = target.startPositionMs,
                            initialPlayTimeState = target.initialPlayTimeState
                        )
                    )
                }
            )
        }
    }
}
