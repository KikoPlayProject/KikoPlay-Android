package com.kiko.kikoplay.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute

@Composable
fun KikoBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (Any) -> Unit,
    activeCacheCount: Int,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = currentDestination?.hasRoute(destination.route::class) == true
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination.route) },
                icon = {
                    if (destination == TopLevelDestination.CACHE && activeCacheCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(if (activeCacheCount > 99) "99+" else activeCacheCount.toString())
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.label
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = destination.label
                        )
                    }
                },
                label = { Text(destination.label) }
            )
        }
    }
}
