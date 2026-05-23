package pl.gwsh.stockanalysis.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import pl.gwsh.stockanalysis.presentation.screens.chart.ChartScreen
import pl.gwsh.stockanalysis.presentation.screens.favorites.FavoritesScreen
import pl.gwsh.stockanalysis.presentation.screens.search.SearchScreen

/**
 * Korzen drzewa nawigacji. Trzy ekrany: wyszukiwarka, ulubione, wykres.
 * Wykres dostaje symbol tickera jako argument trasy.
 */
@Composable
fun SaaNavHost(
    navController: NavHostController,
    startDestination: String = SaaDestinations.SEARCH,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(SaaDestinations.SEARCH) {
            SearchScreen(
                onStockClick = { symbol ->
                    navController.navigate(SaaDestinations.chartRoute(symbol))
                },
            )
        }
        composable(SaaDestinations.FAVORITES) {
            FavoritesScreen(
                onStockClick = { symbol ->
                    navController.navigate(SaaDestinations.chartRoute(symbol))
                },
            )
        }
        composable(
            route = SaaDestinations.CHART_PATTERN,
            arguments = listOf(navArgument(SaaDestinations.CHART_ARG_SYMBOL) { type = NavType.StringType }),
        ) {
            ChartScreen(onBack = { navController.popBackStack() })
        }
    }
}
