package pl.gwsh.stockanalysis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import pl.gwsh.stockanalysis.presentation.navigation.SaaBottomBar
import pl.gwsh.stockanalysis.presentation.navigation.SaaNavHost
import pl.gwsh.stockanalysis.presentation.navigation.shouldShowBottomBar
import pl.gwsh.stockanalysis.presentation.theme.SaaTheme

/**
 * Jedyna aktywnosc aplikacji — single-activity + Compose nawigacja.
 * Hostuje [SaaNavHost] w [Scaffold] z dolnym paskiem dla ekranow top-levelowych.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SaaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    SaaApp()
                }
            }
        }
    }
}

@Composable
private fun SaaApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar(currentRoute)) {
                SaaBottomBar(navController)
            }
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            SaaNavHost(navController = navController)
        }
    }
}
