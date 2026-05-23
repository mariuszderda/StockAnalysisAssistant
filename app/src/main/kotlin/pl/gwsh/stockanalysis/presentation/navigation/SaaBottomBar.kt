package pl.gwsh.stockanalysis.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import pl.gwsh.stockanalysis.R

/**
 * Dolny pasek z dwiema zakladkami widocznymi tylko na ekranach Search i Favorites.
 * Na ekranie wykresu pasek znika — chart ma wlasny TopBar z przyciskiem wstecz.
 */
@Composable
fun SaaBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: SaaDestinations.SEARCH

    val items = listOf(
        BottomItem(SaaDestinations.SEARCH, Icons.Filled.Search, R.string.nav_search),
        BottomItem(SaaDestinations.FAVORITES, Icons.Filled.Star, R.string.nav_favorites),
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                label = { Text(stringResource(item.labelRes)) },
            )
        }
    }
}

/** Pokazuj bottom bar tylko na ekranach top-levelowych (nie na ekranie wykresu). */
fun shouldShowBottomBar(currentRoute: String?): Boolean =
    currentRoute == SaaDestinations.SEARCH || currentRoute == SaaDestinations.FAVORITES

private data class BottomItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int,
)
