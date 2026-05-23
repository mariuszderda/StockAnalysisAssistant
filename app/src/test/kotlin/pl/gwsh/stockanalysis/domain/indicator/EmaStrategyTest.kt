package pl.gwsh.stockanalysis.domain.indicator

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import pl.gwsh.stockanalysis.domain.model.Candle
import java.time.LocalDate

class EmaStrategyTest {

    private val tol = 1e-4

    private fun closesToCandles(closes: List<Double>): List<Candle> =
        closes.mapIndexed { i, c ->
            Candle(
                date = LocalDate.of(2024, 1, 1).plusDays(i.toLong()),
                open = c, high = c, low = c, close = c, volume = 0L,
            )
        }

    @Test
    fun `EMA on closes 1 to 20 with period 5 matches hand calculation`() {
        // Dla rampy [1..N] z EMA(5): seed at i=4 = 3.0, k=1/3;
        // EMA[5]=4.0, EMA[6]=5.0, EMA[7]=6.0, ..., EMA[i] = i-1.
        val result = EmaStrategy(period = 5).compute(closesToCandles((1..20).map(Int::toDouble)))
            as IndicatorResult.SingleLine
        for (i in 0..3) assertThat(result.values[i]).isNull()
        assertThat(result.values[4]!!).isWithin(tol).of(3.0)
        assertThat(result.values[5]!!).isWithin(tol).of(4.0)
        assertThat(result.values[6]!!).isWithin(tol).of(5.0)
        assertThat(result.values[7]!!).isWithin(tol).of(6.0)
        assertThat(result.values[19]!!).isWithin(tol).of(18.0)
    }

    @Test
    fun `EMA output length equals candle count`() {
        val candles = closesToCandles(List(40) { 100.0 + it })
        val result = EmaStrategy(period = 12).compute(candles) as IndicatorResult.SingleLine
        assertThat(result.values).hasSize(40)
    }

    @Test
    fun `EMA on constant series stays constant after seed`() {
        val candles = closesToCandles(List(30) { 50.0 })
        val result = EmaStrategy(period = 10).compute(candles) as IndicatorResult.SingleLine
        for (i in 0..8) assertThat(result.values[i]).isNull()
        for (i in 9..29) assertThat(result.values[i]!!).isWithin(tol).of(50.0)
    }

    @Test
    fun `EMA type and displayName carry expected metadata`() {
        val s = EmaStrategy(period = 20)
        assertThat(s.type).isEqualTo(IndicatorType.OVERLAY)
        assertThat(s.displayName).isEqualTo("EMA(20)")
    }
}
