package pl.gwsh.stockanalysis.presentation.screens.chart

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberCandlestickCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.candlestickSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import pl.gwsh.stockanalysis.domain.model.Candle

/**
 * Wykres Vico — swiece lub linia close, w zaleznosci od [type].
 * Re-uploaduje dane do model-producera kiedy zmieni sie [candles] lub [type].
 *
 * Vico 2.1.2 — patrz `docs/ADR/004-strategy-factory.md` nie ma na ten temat;
 * to wybor warstwy prezentacji.
 */
@Composable
fun VicoChart(
    candles: List<Candle>,
    type: ChartType,
    modifier: Modifier = Modifier,
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(candles, type) {
        if (candles.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            when (type) {
                ChartType.CANDLE -> candlestickSeries(
                    x = candles.indices.toList(),
                    opening = candles.map { it.open },
                    closing = candles.map { it.close },
                    low = candles.map { it.low },
                    high = candles.map { it.high },
                )
                ChartType.LINE -> lineSeries {
                    series(y = candles.map { it.close })
                }
            }
        }
    }

    val chart = when (type) {
        ChartType.CANDLE -> rememberCartesianChart(
            rememberCandlestickCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(),
        )
        ChartType.LINE -> rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(),
        )
    }

    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        modifier = modifier.fillMaxSize(),
    )
}
