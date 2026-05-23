package pl.gwsh.stockanalysis.domain.indicator

import pl.gwsh.stockanalysis.domain.indicator.util.sma
import pl.gwsh.stockanalysis.domain.model.Candle

/**
 * Simple Moving Average — krocząca srednia arytmetyczna z `period` ostatnich
 * cen zamkniecia. Klasyczny wskaznik trendu, OVERLAY (rysowany na wykresie
 * ceny).
 *
 * Cala matematyka jest w [pl.gwsh.stockanalysis.domain.indicator.util.sma];
 * ta klasa jest cienkim wrapperem implementujacym [IndicatorStrategy] —
 * istnieje, zeby [IndicatorFactory] mial co zwrocic z `when (IndicatorSpec.Sma)`.
 */
class SmaStrategy(private val period: Int = 20) : IndicatorStrategy {

    override val type: IndicatorType = IndicatorType.OVERLAY
    override val displayName: String = "SMA($period)"

    override fun compute(candles: List<Candle>): IndicatorResult {
        val closes = candles.map { it.close }
        return IndicatorResult.SingleLine(
            name = displayName,
            type = type,
            values = sma(closes, period),
        )
    }
}
