package pl.gwsh.stockanalysis.domain.indicator.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MovingAveragesTest {

    /** Tolerancja matematyczna — SMA i EMA dla calkowitych wejsc daja
     *  wyniki dokladne; ±0.0001 zostawia margines na obliczenia FP. */
    private val tol = 1e-4

    // --- SMA ----------------------------------------------------------------

    @Test
    fun `sma on ramp 1 to 10 with period 3 matches hand calculation`() {
        // ref: SMA(3) na [1,2,...,10]
        // i=0,1 -> null (period-1 = 2 leading nulls)
        // i=2 -> (1+2+3)/3 = 2.0
        // i=3 -> (2+3+4)/3 = 3.0
        // ...
        // i=9 -> (8+9+10)/3 = 9.0
        val result = sma((1..10).map(Int::toDouble), period = 3)
        assertThat(result).hasSize(10)
        assertThat(result[0]).isNull()
        assertThat(result[1]).isNull()
        val expected = doubleArrayOf(2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0)
        for ((idx, exp) in expected.withIndex()) {
            assertThat(result[idx + 2]!!).isWithin(tol).of(exp)
        }
    }

    @Test
    fun `sma length equals input length`() {
        val input = (1..50).map(Int::toDouble)
        assertThat(sma(input, period = 14)).hasSize(50)
    }

    @Test
    fun `sma with input shorter than period returns all nulls of input length`() {
        val out = sma(listOf(1.0, 2.0), period = 5)
        assertThat(out).hasSize(2)
        assertThat(out).containsExactly(null, null).inOrder()
    }

    @Test
    fun `sma with non-positive period throws`() {
        assertThrows<IllegalArgumentException> { sma(listOf(1.0, 2.0, 3.0), period = 0) }
        assertThrows<IllegalArgumentException> { sma(listOf(1.0, 2.0, 3.0), period = -1) }
    }

    @Test
    fun `sma rolling window matches naive implementation`() {
        // Cross-validate optymalizacji rolling-sum z naiwnym O(n*period).
        val input = listOf(2.0, 4.0, 6.0, 1.0, 3.0, 5.0, 7.0, 9.0, 0.0, 11.0)
        val period = 4
        val ours = sma(input, period)
        val naive = input.indices.map { i ->
            if (i < period - 1) null
            else input.subList(i - period + 1, i + 1).average()
        }
        for (i in input.indices) {
            if (naive[i] == null) assertThat(ours[i]).isNull()
            else assertThat(ours[i]!!).isWithin(tol).of(naive[i]!!)
        }
    }

    // --- EMA ----------------------------------------------------------------

    @Test
    fun `ema on ramp 1 to 20 with period 5 matches hand calculation`() {
        // ref: EMA(5) na [1,2,...,20]
        // seed at i=4 = (1+2+3+4+5)/5 = 3.0
        // k = 2/(5+1) = 1/3
        // EMA[5] = 6*(1/3) + 3*(2/3)   = 2 + 2     = 4.0
        // EMA[6] = 7*(1/3) + 4*(2/3)   = 7/3 + 8/3 = 5.0
        // EMA[7] = 8*(1/3) + 5*(2/3)   = 8/3 + 10/3 = 6.0
        // EMA[8] = 9*(1/3) + 6*(2/3)   = 7.0
        // ...
        // czyli: dla rampy [1..N] z EMA(period) wartosci stabilizuja sie do
        //   EMA[i] = i+1 - 2  (= input[i] - 2.0) zaczynajac od ~i = period.
        // Czyli kazda kolejna wartosc EMA[i] = i - 1 (1-based: i-1 dla 0-based).
        val result = ema((1..20).map(Int::toDouble), period = 5)
        assertThat(result).hasSize(20)
        for (i in 0..3) assertThat(result[i]).isNull()

        assertThat(result[4]!!).isWithin(tol).of(3.0)   // SMA seed
        assertThat(result[5]!!).isWithin(tol).of(4.0)
        assertThat(result[6]!!).isWithin(tol).of(5.0)
        assertThat(result[7]!!).isWithin(tol).of(6.0)
        assertThat(result[8]!!).isWithin(tol).of(7.0)
        assertThat(result[19]!!).isWithin(tol).of(18.0)
    }

    @Test
    fun `ema constant series returns constant after seed`() {
        // const = 50.0 powinno dac EMA = 50.0 dla wszystkich indeksow >= period-1.
        val input = List(30) { 50.0 }
        val out = ema(input, period = 10)
        for (i in 0..8) assertThat(out[i]).isNull()
        for (i in 9..29) assertThat(out[i]!!).isWithin(tol).of(50.0)
    }

    @Test
    fun `ema length equals input length`() {
        assertThat(ema((1..40).map(Int::toDouble), period = 12)).hasSize(40)
    }

    @Test
    fun `ema with input shorter than period returns all nulls`() {
        val out = ema(listOf(1.0, 2.0, 3.0), period = 5)
        assertThat(out).containsExactly(null, null, null).inOrder()
    }

    @Test
    fun `ema with non-positive period throws`() {
        assertThrows<IllegalArgumentException> { ema(listOf(1.0, 2.0, 3.0), period = 0) }
        assertThrows<IllegalArgumentException> { ema(listOf(1.0, 2.0, 3.0), period = -2) }
    }
}
