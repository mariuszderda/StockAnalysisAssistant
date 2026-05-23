package pl.gwsh.stockanalysis.domain.indicator

import pl.gwsh.stockanalysis.domain.indicator.util.ema
import pl.gwsh.stockanalysis.domain.model.Candle

/**
 * Exponential Moving Average — wykladnicza srednia krocząca z `k = 2/(period+1)`.
 * OVERLAY. Reaguje na nowsze ceny szybciej niz [SmaStrategy].
 *
 * Matematyka w [pl.gwsh.stockanalysis.domain.indicator.util.ema].
 */
class EmaStrategy(private val period: Int = 20) : IndicatorStrategy {

    override val type: IndicatorType = IndicatorType.OVERLAY
    override val displayName: String = "EMA($period)"

    override fun compute(candles: List<Candle>): IndicatorResult {
        val closes = candles.map { it.close }
        return IndicatorResult.SingleLine(
            name = displayName,
            type = type,
            values = ema(closes, period),
        )
    }
}
