package pl.gwsh.stockanalysis.presentation.screens.chart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import pl.gwsh.stockanalysis.domain.indicator.IndicatorResult

/** testTag prefiks dla oscylatora — sufiks to nazwa wskaznika (np. "RSI(14)"). */
const val OSCILLATOR_PANEL_TAG_PREFIX = "oscillator_panel_"

/**
 * Komponent renderujacy pojedynczy wskaznik typu OSCILLATOR pod glownym wykresem.
 * Wysokosc stala 120 dp; wlasny CartesianChartHost z wlasnymi osiami (oscylatory
 * maja inna skale niz cena — RSI 0-100, MACD blizej 0).
 */
@Composable
fun OscillatorPanel(
    result: IndicatorResult,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag(OSCILLATOR_PANEL_TAG_PREFIX + result.name),
    ) {
        Text(
            text = result.name,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        val series: List<List<Double?>> = when (result) {
            is IndicatorResult.SingleLine -> listOf(result.values)
            is IndicatorResult.MultiLine -> result.lines.values.toList()
        }

        val modelProducer = remember(result.name) { CartesianChartModelProducer() }

        LaunchedEffect(series) {
            if (series.all { it.all { v -> v == null } }) return@LaunchedEffect
            modelProducer.runTransaction {
                lineSeries {
                    series.forEach { line ->
                        val pairs = line.withIndex()
                            .filter { (_, v) -> v != null }
                            .map { (i, v) -> i to v!! }
                        if (pairs.isNotEmpty()) {
                            series(x = pairs.map { it.first }, y = pairs.map { it.second })
                        }
                    }
                }
            }
        }

        val chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(),
        )

        CartesianChartHost(
            chart = chart,
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        )
    }
}
