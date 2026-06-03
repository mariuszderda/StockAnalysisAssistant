package pl.gwsh.stockanalysis.presentation.screens.chart

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
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
import pl.gwsh.stockanalysis.domain.indicator.IndicatorResult
import pl.gwsh.stockanalysis.domain.model.Candle

/**
 * Wykres Vico — swiece lub linia close, w zaleznosci od [type]. Opcjonalnie
 * nakladamy serie overlay (SMA, EMA — kazda jako linia o wlasnym kolorze).
 *
 * Re-uploaduje dane do model-producera kiedy zmieni sie [candles], [type] albo
 * zestaw [overlays] (porownywany po referencjach listy).
 *
 * Vico 2.1.2: `Double.NaN` w `lineSeries` powoduje przerwy w linii — uzywamy
 * tego, zeby nie pomijac indeksow X dla wskaznikow z leading nullami.
 */
@Composable
fun VicoChart(
    candles: List<Candle>,
    type: ChartType,
    overlays: List<IndicatorResult.SingleLine> = emptyList(),
    modifier: Modifier = Modifier,
) {
    // key na (type, overlays.isNotEmpty()) — przy zmianie liczby layers resetujemy
    // cały komponent (modelProducer + LaunchedEffect), eliminując race condition
    // między asynchroniczną aktualizacją modelu a synchroniczną zmianą chart layers.
    key(type, overlays.isNotEmpty()) {
        VicoChartInternal(candles = candles, type = type, overlays = overlays, modifier = modifier)
    }
}

@Composable
private fun VicoChartInternal(
    candles: List<Candle>,
    type: ChartType,
    overlays: List<IndicatorResult.SingleLine>,
    modifier: Modifier,
) {
    val hasOverlays = overlays.isNotEmpty()
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(candles, type, overlays) {
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
            if (hasOverlays) {
                lineSeries {
                    overlays.forEach { ov ->
                        val pairs = ov.values.withIndex()
                            .filter { (_, v) -> v != null }
                            .map { (i, v) -> i to v!! }
                        if (pairs.isNotEmpty()) {
                            series(x = pairs.map { it.first }, y = pairs.map { it.second })
                        }
                    }
                }
            }
        }
    }

    val candlestickLayer = rememberCandlestickCartesianLayer()
    val priceLineLayer = rememberLineCartesianLayer()
    val overlayLineLayer = rememberLineCartesianLayer()

    val chart = when (type) {
        ChartType.CANDLE -> if (hasOverlays) {
            rememberCartesianChart(
                candlestickLayer, overlayLineLayer,
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(),
            )
        } else {
            rememberCartesianChart(
                candlestickLayer,
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(),
            )
        }
        ChartType.LINE -> if (hasOverlays) {
            rememberCartesianChart(
                priceLineLayer, overlayLineLayer,
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(),
            )
        } else {
            rememberCartesianChart(
                priceLineLayer,
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(),
            )
        }
    }

    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        modifier = modifier.fillMaxSize(),
    )
}
