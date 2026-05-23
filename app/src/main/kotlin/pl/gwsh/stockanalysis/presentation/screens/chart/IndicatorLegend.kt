package pl.gwsh.stockanalysis.presentation.screens.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pl.gwsh.stockanalysis.domain.indicator.IndicatorResult
import pl.gwsh.stockanalysis.presentation.common.formatPrice

/** testTag dla testow UI. */
const val INDICATOR_LEGEND_TAG = "indicator_legend"

/**
 * Mini-legenda OVERLAY wskaznikow pod glownym wykresem. Pokazuje:
 *  - kropke z kolorem matchujacym kolejnosc serii w VicoChart,
 *  - nazwe wskaznika,
 *  - biezaca (ostatnia non-null) wartosc.
 *
 * Kolory celowo proste — Vico ma wlasna palete defaultowa; w MVP nie
 * kontrolujemy kolorow serii rycznie, ale w legendzie wybieramy spojny
 * zestaw, zeby komisja widziala "ktora seria to ktory wskaznik".
 */
@Composable
fun IndicatorLegend(
    overlays: List<IndicatorResult.SingleLine>,
    modifier: Modifier = Modifier,
) {
    if (overlays.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag(INDICATOR_LEGEND_TAG),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        overlays.forEachIndexed { index, overlay ->
            LegendEntry(
                color = legendColors[index % legendColors.size],
                label = overlay.name,
                value = overlay.values.lastOrNull { it != null }?.let(::formatPrice),
            )
        }
    }
}

@Composable
private fun LegendEntry(color: Color, label: String, value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = color, shape = CircleShape),
        )
        val text = if (value != null) "  $label: $value" else "  $label"
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val legendColors = listOf(
    Color(0xFF1976D2), // blue
    Color(0xFFD81B60), // pink
    Color(0xFF388E3C), // green
    Color(0xFFF57C00), // orange
)
