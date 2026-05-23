package pl.gwsh.stockanalysis.domain.indicator

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.gwsh.stockanalysis.domain.indicator.util.ema
import pl.gwsh.stockanalysis.domain.model.Candle
import java.time.LocalDate

class MacdStrategyTest {

    private val tol = 0.01

    private fun closesToCandles(closes: List<Double>): List<Candle> =
        closes.mapIndexed { i, c ->
            Candle(
                date = LocalDate.of(2024, 1, 1).plusDays(i.toLong()),
                open = c, high = c, low = c, close = c, volume = 0L,
            )
        }

    // --- Reference test (b): linear ramp [1..60] -----------------------------
    //
    // Wlasciwosc rampy liniowej dla EMA: poniewaz seed SMA(period) na ramp[1..N]
    // wyjsciowo daje wartosc (period+1)/2 = input[period-1] - (period-1)/2,
    // a rekurencja EMA z waga k = 2/(period+1) zachowuje tę samą stała stratę,
    // EMA jest dokladnie liniowa od pierwszego seeda:
    //   EMA(period)[i] = input[i] - (period-1)/2 dla wszystkich i >= period-1.
    //
    // Stad dla [1..60]:
    //   EMA(12)[i] = (i+1) - 5.5  dla i >= 11
    //   EMA(26)[i] = (i+1) - 12.5 dla i >= 25
    //   MACD[i]    = 7.0          dla i >= 25  (stała: 12.5 - 5.5)
    //   signal     = EMA(macd, 9) na ciagu stalym 7.0 → 7.0 od indeksu seeda
    //   histogram  = 0.0          dla i >= 33  (signal seed at macd_nonNull[8] = candles[33])
    //
    // Te wartosci pozwalaja zweryfikowac caly skomplikowany lancuch EMA→diff→EMA
    // bez polegania na zewnetrznej implementacji.
    @Test
    fun `MACD on linear ramp 1 to 60 produces stable terminal values`() {
        val candles = closesToCandles((1..60).map(Int::toDouble))
        val result = MacdStrategy(fast = 12, slow = 26, signal = 9).compute(candles)
            as IndicatorResult.MultiLine

        val macd = result.lines.getValue(MACD_LINE_MACD)
        val signal = result.lines.getValue(MACD_LINE_SIGNAL)
        val histogram = result.lines.getValue(MACD_LINE_HISTOGRAM)

        // MACD: null dla i < 25 (slow EMA niedostepna), 7.0 dalej.
        for (i in 0..24) assertThat(macd[i]).isNull()
        for (i in 25..59) assertThat(macd[i]!!).isWithin(tol).of(7.0)

        // Signal: null dla i < 33 (= 25 + signal-1 = 25+8), 7.0 dalej.
        for (i in 0..32) assertThat(signal[i]).isNull()
        for (i in 33..59) assertThat(signal[i]!!).isWithin(tol).of(7.0)

        // Histogram: tam, gdzie signal istnieje = 0.0.
        for (i in 0..32) assertThat(histogram[i]).isNull()
        for (i in 33..59) assertThat(histogram[i]!!).isWithin(tol).of(0.0)
    }

    // --- Reference test (a): structural invariant ---------------------------
    @Test
    fun `MACD line equals fast EMA minus slow EMA at every index`() {
        // Niezalezne wyliczenie tych samych komponentow przez util/ema
        // i sprawdzenie, ze MACD line = EMA(fast) - EMA(slow) wszedzie tam,
        // gdzie obie strony sa niezerowe.
        val closes = listOf(
            44.34, 44.09, 44.15, 43.61, 44.33, 44.83, 45.10, 45.42, 45.84, 46.08,
            45.89, 46.03, 45.61, 46.28, 46.28, 46.00, 46.03, 46.41, 46.22, 45.64,
            46.21, 46.25, 45.71, 46.45, 45.78, 45.35, 44.03, 44.18, 44.22, 44.57,
            43.42, 42.66, 43.13, 43.50, 44.00, 44.50, 45.10, 45.30, 45.50, 45.80,
        )
        val candles = closesToCandles(closes)
        val result = MacdStrategy(fast = 12, slow = 26, signal = 9).compute(candles)
            as IndicatorResult.MultiLine
        val macd = result.lines.getValue(MACD_LINE_MACD)

        val expectedFast = ema(closes, 12)
        val expectedSlow = ema(closes, 26)
        for (i in candles.indices) {
            val f = expectedFast[i]
            val s = expectedSlow[i]
            if (f == null || s == null) {
                assertThat(macd[i]).isNull()
            } else {
                assertThat(macd[i]!!).isWithin(1e-9).of(f - s)
            }
        }
    }

    @Test
    fun `histogram equals macd minus signal where both non-null`() {
        val closes = (1..50).map { 100.0 + 0.5 * it + Math.sin(it.toDouble()) }
        val result = MacdStrategy().compute(closesToCandles(closes)) as IndicatorResult.MultiLine
        val macd = result.lines.getValue(MACD_LINE_MACD)
        val signal = result.lines.getValue(MACD_LINE_SIGNAL)
        val histogram = result.lines.getValue(MACD_LINE_HISTOGRAM)

        for (i in closes.indices) {
            val m = macd[i]
            val s = signal[i]
            val h = histogram[i]
            if (m == null || s == null) {
                assertThat(h).isNull()
            } else {
                assertThat(h!!).isWithin(1e-9).of(m - s)
            }
        }
    }

    @Test
    fun `MACD all three lines have length equal to candle count`() {
        val candles = closesToCandles(List(40) { 100.0 + it * 0.1 })
        val result = MacdStrategy().compute(candles) as IndicatorResult.MultiLine
        assertThat(result.lines).hasSize(3)
        for ((_, line) in result.lines) {
            assertThat(line).hasSize(40)
        }
    }

    @Test
    fun `MACD with too few candles returns all-null lines`() {
        val short = closesToCandles(List(10) { 100.0 + it })
        val result = MacdStrategy().compute(short) as IndicatorResult.MultiLine
        for ((_, line) in result.lines) {
            assertThat(line).hasSize(10)
            assertThat(line.all { it == null }).isTrue()
        }
    }

    @Test
    fun `MACD invalid params throw`() {
        val candles = closesToCandles(List(30) { it.toDouble() })
        assertThrows<IllegalArgumentException> { MacdStrategy(fast = 0).compute(candles) }
        assertThrows<IllegalArgumentException> { MacdStrategy(fast = 26, slow = 12).compute(candles) }
        assertThrows<IllegalArgumentException> { MacdStrategy(signal = -1).compute(candles) }
    }

    @Test
    fun `MACD type and displayName carry expected metadata`() {
        val s = MacdStrategy()
        assertThat(s.type).isEqualTo(IndicatorType.OSCILLATOR)
        assertThat(s.displayName).isEqualTo("MACD(12/26/9)")
        assertThat(MacdStrategy(fast = 5, slow = 15, signal = 3).displayName)
            .isEqualTo("MACD(5/15/3)")
    }
}
