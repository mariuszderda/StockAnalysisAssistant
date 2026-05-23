package pl.gwsh.stockanalysis.domain.indicator

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import pl.gwsh.stockanalysis.domain.model.Candle
import java.time.LocalDate

class RsiStrategyTest {

    private val tol = 0.01

    /**
     * Wilder 1978, "New Concepts in Technical Trading Systems", str. 65-67.
     * 33 ceny zamkniecia (zreplikowane w referencji StockCharts.com
     * ChartSchool "Relative Strength Index"). RSI(14) z pierwsza wartoscia
     * w indeksie 14 ≈ 70.46.
     */
    private val wilderCloses = listOf(
        44.34, 44.09, 44.15, 43.61, 44.33, 44.83, 45.10, 45.42, 45.84, 46.08,
        45.89, 46.03, 45.61, 46.28, 46.28, 46.00, 46.03, 46.41, 46.22, 45.64,
        46.21, 46.25, 45.71, 46.45, 45.78, 45.35, 44.03, 44.18, 44.22, 44.57,
        43.42, 42.66, 43.13,
    )

    private fun closesToCandles(closes: List<Double>): List<Candle> =
        closes.mapIndexed { i, c ->
            // Pozostale pola OHLCV niewazne dla RSI; trzymamy spojne, zeby
            // testy mapperow nie krzywily nam danych przy reuzyciu fixture.
            Candle(
                date = LocalDate.of(2024, 1, 1).plusDays(i.toLong()),
                open = c, high = c, low = c, close = c, volume = 0L,
            )
        }

    @Test
    fun `RSI on Wilder reference dataset matches published values within tolerance`() {
        val rsi = RsiStrategy(period = 14).compute(closesToCandles(wilderCloses))
        val line = (rsi as IndicatorResult.SingleLine).values

        // Pierwsze 14 indeksow musi byc null — RSI nieosiagalny bez seeda.
        for (i in 0..13) assertThat(line[i]).isNull()

        // RSI[14] — opublikowana wartosc 70.46 (Wilder / StockCharts).
        assertThat(line[14]!!).isWithin(tol).of(70.46)

        // RSI[15] — wyliczone recznie z rekurencji Wildera: ≈ 66.25.
        assertThat(line[15]!!).isWithin(tol).of(66.25)

        // RSI[19] — RSI[19] ≈ 57.92 (po dluzszej serii nieznacznych spadkow).
        assertThat(line[19]!!).isWithin(tol).of(57.92)

        // RSI[32] — ostatnia wartosc na zbiorze; ≈ 37.79.
        assertThat(line[32]!!).isWithin(tol).of(37.79)
    }

    @Test
    fun `RSI output length equals candle count`() {
        val candles = closesToCandles(wilderCloses)
        val rsi = RsiStrategy().compute(candles) as IndicatorResult.SingleLine
        assertThat(rsi.values).hasSize(candles.size)
    }

    @Test
    fun `RSI with fewer candles than period plus one returns all nulls`() {
        val short = closesToCandles(List(10) { it.toDouble() + 100.0 })
        val rsi = RsiStrategy(period = 14).compute(short) as IndicatorResult.SingleLine
        assertThat(rsi.values).hasSize(10)
        assertThat(rsi.values.all { it == null }).isTrue()
    }

    @Test
    fun `RSI on monotonically rising series approaches 100`() {
        val rising = closesToCandles((1..30).map { 100.0 + it })
        val rsi = RsiStrategy(period = 14).compute(rising) as IndicatorResult.SingleLine
        // Wszystkie roznice dodatnie → avgLoss == 0 → RSI = 100 (konwencja).
        assertThat(rsi.values[14]!!).isWithin(tol).of(100.0)
        assertThat(rsi.values[29]!!).isWithin(tol).of(100.0)
    }

    @Test
    fun `RSI on monotonically falling series approaches 0`() {
        val falling = closesToCandles((1..30).map { 200.0 - it })
        val rsi = RsiStrategy(period = 14).compute(falling) as IndicatorResult.SingleLine
        // Wszystkie roznice ujemne → avgGain == 0 → RS = 0 → RSI = 0.
        assertThat(rsi.values[14]!!).isWithin(tol).of(0.0)
        assertThat(rsi.values[29]!!).isWithin(tol).of(0.0)
    }

    @Test
    fun `RSI type and displayName carry expected metadata`() {
        val s = RsiStrategy(period = 14)
        assertThat(s.type).isEqualTo(IndicatorType.OSCILLATOR)
        assertThat(s.displayName).isEqualTo("RSI(14)")
        val s7 = RsiStrategy(period = 7)
        assertThat(s7.displayName).isEqualTo("RSI(7)")
    }
}
