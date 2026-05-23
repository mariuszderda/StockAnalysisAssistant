package pl.gwsh.stockanalysis.presentation.screens.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pl.gwsh.stockanalysis.R
import pl.gwsh.stockanalysis.domain.indicator.IndicatorSpec

/** testTag prefiks dla pojedynczego chipa wyboru wskaznika — sufiks to `spec.key`. */
const val INDICATOR_CHIP_TAG_PREFIX = "indicator_chip_"

/** testTag samego bottomsheeta. */
const val INDICATOR_PANEL_SHEET_TAG = "indicator_panel_sheet"

/**
 * Panel wskaznikow technicznych — ModalBottomSheet z lista chipow.
 *
 * UI nie wie o konkretnych typach wskaznikow; otrzymuje `available` z
 * [IndicatorFactory.availableIndicators] (przez ViewModel) i `activeSpecs`
 * jako bezposrednia projekcje stanu. Toggle deleguje do callbacka
 * `onToggle(spec)` — caly stan zyje w ViewModelu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicatorPanel(
    available: List<IndicatorSpec>,
    activeSpecs: Set<IndicatorSpec>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onToggle: (IndicatorSpec) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val activeKeys = activeSpecs.mapTo(mutableSetOf()) { it.key }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag(INDICATOR_PANEL_SHEET_TAG),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.chart_indicators_sheet_title),
                style = MaterialTheme.typography.titleMedium,
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(available, key = { it.key }) { spec ->
                    val selected = spec.key in activeKeys
                    FilterChip(
                        selected = selected,
                        onClick = {
                            scope.launch { onToggle(spec) }
                        },
                        label = { Text(spec.displayLabel()) },
                        colors = FilterChipDefaults.filterChipColors(),
                        modifier = Modifier.testTag(INDICATOR_CHIP_TAG_PREFIX + spec.key),
                    )
                }
            }

            Text(
                text = stringResource(R.string.chart_indicators_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
        }
    }
}

/** Czytelna etykieta wskaznika do FilterChip — np. "SMA(20)", "MACD(12/26/9)". */
private fun IndicatorSpec.displayLabel(): String = when (this) {
    is IndicatorSpec.Sma  -> "SMA($period)"
    is IndicatorSpec.Ema  -> "EMA($period)"
    is IndicatorSpec.Rsi  -> "RSI($period)"
    is IndicatorSpec.Macd -> "MACD($fast/$slow/$signal)"
}
