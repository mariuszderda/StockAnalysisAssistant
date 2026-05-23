package pl.gwsh.stockanalysis.presentation.screens.chart

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.gwsh.stockanalysis.R

/**
 * Placeholder dla panelu wskaznikow technicznych. Wlasciwa implementacja
 * (RSI/MACD/SMA/EMA, Strategy + Factory) trafia tu w Fazie 4.
 */
@Composable
fun IndicatorPanel(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
    ) {
        Text(
            text = stringResource(R.string.chart_indicators_placeholder),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}
