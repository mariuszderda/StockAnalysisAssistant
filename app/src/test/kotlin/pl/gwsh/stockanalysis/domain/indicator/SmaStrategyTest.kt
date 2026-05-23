package pl.gwsh.stockanalysis.domain.indicator

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import pl.gwsh.stockanalysis.domain.model.Candle
import java.time.LocalDate

class SmaStrategyTest {

    private val tol = 1e-4

    private fun closesToCandles(closes: List<Double>): List<Candle> =
        closes.mapIndexed { i, c ->
            Candle(
                date = LocalDate.of(2024, 1, 1).plusDays(i.toLong()),
                open = c, high = c, low = c, close = c, volume = 0L,
            )
        }

    @Test
    fun `SMA on closes 1 to 10 with period 3 matches hand calculation`() {
        val candles = closesToCandles((1..10).map(Int::toDouble))
        val result = SmaStrategy(period = 3).compute(candles) as IndicatorResult.SingleLine
        assertThat(result.values).hasSize(10)
        assertThat(result.values[0]).isNull()
        assertThat(result.values[1]).isNull()
        val expected = doubleArrayOf(2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0)
        for ((i, exp) in expected.withIndex()) {
            assertThat(result.values[i + 2]!!).isWithin(tol).of(exp)
        }
    }

    @Test
    fun `SMA output length equals candle count`() {
        val candles = closesToCandles(List(50) { 100.0 + it })
        val result = SmaStrategy(period = 14).compute(candles) as IndicatorResult.SingleLine
        assertThat(result.values).hasSize(50)
    }

    @Test
    fun `SMA reads close prices not other OHLC fields`() {
        // Roznicujemy OHLC, zeby przylapac ewentualny bug "uzywam wrong field".
        val candles = listOf(
            Candle(LocalDate.of(2024, 1, 1), open = 1.0, high = 9.0, low = 0.0, close = 10.0, volume = 0),
            Candle(LocalDate.of(2024, 1, 2), open = 2.0, high = 9.0, low = 0.0, close = 20.0, volume = 0),
            Candle(LocalDate.of(2024, 1, 3), open = 3.0, high = 9.0, low = 0.0, close = 30.0, volume = 0),
        )
        val result = SmaStrategy(period = 3).compute(candles) as IndicatorResult.SingleLine
        // SMA closes [10,20,30] / 3 = 20.0
        assertThat(result.values[2]!!).isWithin(tol).of(20.0)
    }

    @Test
    fun `SMA type and displayName carry expected metadata`() {
        val s = SmaStrategy(period = 20)
        assertThat(s.type).isEqualTo(IndicatorType.OVERLAY)
        assertThat(s.displayName).isEqualTo("SMA(20)")
    }
}
