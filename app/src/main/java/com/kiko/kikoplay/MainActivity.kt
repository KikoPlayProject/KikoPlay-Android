package com.kiko.kikoplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import com.kiko.kikoplay.data.repository.CacheRepository
import com.kiko.kikoplay.data.repository.ConnectionRepository
import com.kiko.kikoplay.data.repository.SettingsRepository
import com.kiko.kikoplay.ui.navigation.CacheManagementRoute
import com.kiko.kikoplay.ui.navigation.KikoBottomBar
import com.kiko.kikoplay.ui.navigation.KikoNavHost
import com.kiko.kikoplay.ui.navigation.TopLevelDestination
import com.kiko.kikoplay.ui.navigation.VideoPlayerRoute
import com.kiko.kikoplay.ui.theme.KikoPlayTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var connectionRepository: ConnectionRepository

    @Inject
    lateinit var cacheRepository: CacheRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            connectionRepository.reconnectLastConnection()
        }
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = "system")
            val resolvedDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            DisposableEffect(resolvedDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (resolvedDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (resolvedDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
                onDispose {}
            }

            KikoPlayTheme(darkTheme = resolvedDarkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val activeCacheTasks by cacheRepository
                    .getActiveTasks()
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val completedCacheTasks by cacheRepository
                    .getCompletedTasks()
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val isPlayerRoute = currentDestination?.hasRoute(VideoPlayerRoute::class) == true

                // Hide bottom bar on secondary pages
                val showBottomBar = TopLevelDestination.entries.any { dest ->
                    currentDestination?.hasRoute(dest.route::class) == true
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = if (isPlayerRoute) WindowInsets(0, 0, 0, 0) else WindowInsets.systemBars,
                    bottomBar = {
                        if (showBottomBar) {
                            KikoBottomBar(
                                currentDestination = currentDestination,
                                activeCacheCount = activeCacheTasks.size,
                                onNavigate = { route ->
                                    val targetRoute = when (route) {
                                        is CacheManagementRoute -> {
                                            val initialTab = when {
                                                activeCacheTasks.isNotEmpty() -> CacheManagementRoute.TAB_ACTIVE
                                                completedCacheTasks.isNotEmpty() -> CacheManagementRoute.TAB_COMPLETED
                                                else -> CacheManagementRoute.TAB_ACTIVE
                                            }
                                            route.copy(initialTab = initialTab)
                                        }
                                        else -> route
                                    }

                                    navController.navigate(targetRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = targetRoute !is CacheManagementRoute
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    KikoNavHost(
                        navController = navController,
                        modifier = Modifier.padding(if (isPlayerRoute) PaddingValues(0.dp) else innerPadding)
                    )
                }
            }
        }
    }
}
