package pl.gwsh.stockanalysis.domain.indicator

import pl.gwsh.stockanalysis.domain.indicator.util.wilderSmooth
import pl.gwsh.stockanalysis.domain.model.Candle
import kotlin.math.max

/**
 * Relative Strength Index — J. Welles Wilder (1978), "New Concepts in
 * Technical Trading Systems", rozdz. 7.
 *
 * Algorytm:
 *  1. Dla kazdej kolejnej pary swiec liczymy `up = max(0, close[i] - close[i-1])`
 *     oraz `down = max(0, close[i-1] - close[i])`.
 *  2. Sredniujemy `up` i `down` rekurencja Wildera (patrz [wilderSmooth]) z
 *     domyslnym okresem `period = 14`.
 *  3. `RS = avgGain / avgLoss`; `RSI = 100 - 100 / (1 + RS)`.
 *  4. Konwencja brzegowa: `avgLoss == 0` → `RSI = 100` (StockCharts ChartSchool).
 *
 * Wynik jest oscylatorem 0-100. Konwencjonalne progi: > 70 wykupienie,
 * < 30 wyprzedanie. Sama strategia nie interpretuje progow — to jest praca
 * UI / AI w pozniejszych fazach.
 *
 * **Alignment:** dla N swiec zwracamy liste dlugosci N z `period` wiodacych
 * `null` (RSI nie istnieje przed `candles[period]`, bo seed Wilder
 * potrzebuje `period` roznic).
 */
class RsiStrategy(private val period: Int = 14) : IndicatorStrategy {

    override val type: IndicatorType = IndicatorType.OSCILLATOR
    override val displayName: String = "RSI($period)"

    override fun compute(candles: List<Candle>): IndicatorResult {
        require(period > 0) { "RSI period must be positive, was $period" }
        val n = candles.size
        val out = arrayOfNulls<Double>(n)

        if (n <= period) return IndicatorResult.SingleLine(displayName, type, out.toList())

        val ups = DoubleArray(n - 1)
        val downs = DoubleArray(n - 1)
        for (i in 1 until n) {
            val diff = candles[i].close - candles[i - 1].close
            ups[i - 1] = max(0.0, diff)
            downs[i - 1] = max(0.0, -diff)
        }

        val avgGain = wilderSmooth(ups.toList(), period)
        val avgLoss = wilderSmooth(downs.toList(), period)

        // up-index k odpowiada candle-index k+1. Pierwszy non-null seed
        // jest w up-index period-1, czyli candle-index period.
        for (upIdx in (period - 1) until (n - 1)) {
            val g = avgGain[upIdx] ?: continue
            val l = avgLoss[upIdx] ?: continue
            val rsi = if (l == 0.0) 100.0 else {
                val rs = g / l
                100.0 - 100.0 / (1.0 + rs)
            }
            out[upIdx + 1] = rsi
        }
        return IndicatorResult.SingleLine(displayName, type, out.toList())
    }
}
