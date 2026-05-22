package pl.gwsh.stockanalysis.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = SeedLight,
    onPrimary = OnSeedLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
)

private val DarkColors = darkColorScheme(
    primary = SeedDark,
    onPrimary = OnSeedDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
)

/**
 * Główny motyw aplikacji. Na Androidzie 12+ używa kolorów dynamicznych z systemu;
 * niżej — statyczna paleta wokół koloru seed (zob. Color.kt).
 *
 * MVP nie eksponuje przełącznika dark/light — bierzemy ustawienie systemowe.
 */
@Composable
fun SaaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = SaaTypography,
        content = content,
    )
}
