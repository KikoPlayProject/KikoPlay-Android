package com.kiko.kikoplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            connectionRepository.reconnectLastConnection()
        }
        enableEdgeToEdge()
        setContent {
            KikoPlayTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val activeCacheTasks by cacheRepository
                    .getActiveTasks()
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
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
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
