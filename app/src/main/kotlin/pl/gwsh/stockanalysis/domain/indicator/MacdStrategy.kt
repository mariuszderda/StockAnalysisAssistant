package pl.gwsh.stockanalysis.domain.indicator

import pl.gwsh.stockanalysis.domain.indicator.util.ema
import pl.gwsh.stockanalysis.domain.model.Candle

/** Klucze linii w [IndicatorResult.MultiLine] zwracanym przez [MacdStrategy]. */
const val MACD_LINE_MACD = "macd"
const val MACD_LINE_SIGNAL = "signal"
const val MACD_LINE_HISTOGRAM = "histogram"

/**
 * Moving Average Convergence/Divergence — Gerald Appel, lata 70.
 *
 * Trzy linie:
 *  - **MACD line**: `EMA(closes, fast) - EMA(closes, slow)`. Domyslnie fast=12, slow=26.
 *  - **Signal line**: `EMA(macd_line, signal)`. Domyslnie signal=9.
 *  - **Histogram**: `MACD - Signal`. Wizualizuje przyspieszenie/zwolnienie ruchu MACD.
 *
 * Pochodne EMA, wiec algorytmika siedzi w [pl.gwsh.stockanalysis.domain.indicator.util.ema].
 * Tutaj tylko skladamy.
 *
 * **Alignment:** zwracamy [IndicatorResult.MultiLine] z trzema listami,
 * kazda dlugosci `candles.size`. Wiodace `null` zgodnie z dostepnoscia
 * komponentow:
 *  - MACD: `slow - 1` wiodacych null.
 *  - Signal: `slow - 1 + signal - 1` wiodacych null.
 *  - Histogram: jak Signal.
 */
class MacdStrategy(
    private val fast: Int = 12,
    private val slow: Int = 26,
    private val signal: Int = 9,
) : IndicatorStrategy {

    override val type: IndicatorType = IndicatorType.OSCILLATOR
    override val displayName: String = "MACD($fast/$slow/$signal)"

    override fun compute(candles: List<Candle>): IndicatorResult {
        require(fast > 0 && slow > 0 && signal > 0) {
            "MACD parameters must be positive: fast=$fast, slow=$slow, signal=$signal"
        }
        require(fast < slow) { "MACD fast period must be < slow period (got $fast vs $slow)" }

        val n = candles.size
        val closes = candles.map { it.close }
        val emaFast = ema(closes, fast)
        val emaSlow = ema(closes, slow)

        // 1. MACD line — element-wise difference.
        val macd: List<Double?> = (0 until n).map { i ->
            val f = emaFast[i]
            val s = emaSlow[i]
            if (f == null || s == null) null else f - s
        }

        // 2. Signal — EMA(period=signal) na MACD bez wiodacych nulli, potem repad.
        val firstMacdIdx = macd.indexOfFirst { it != null }
        val signalLine = arrayOfNulls<Double>(n)
        if (firstMacdIdx >= 0) {
            val macdNonNull = macd.drop(firstMacdIdx).map { it!! }
            val signalNonNull = ema(macdNonNull, signal)
            for (k in signalNonNull.indices) {
                signalLine[firstMacdIdx + k] = signalNonNull[k]
            }
        }

        // 3. Histogram — MACD - Signal element-wise.
        val histogram: List<Double?> = (0 until n).map { i ->
            val m = macd[i]
            val s = signalLine[i]
            if (m == null || s == null) null else m - s
        }

        return IndicatorResult.MultiLine(
            name = displayName,
            type = type,
            lines = linkedMapOf(
                MACD_LINE_MACD to macd,
                MACD_LINE_SIGNAL to signalLine.toList(),
                MACD_LINE_HISTOGRAM to histogram,
            ),
        )
    }
}
