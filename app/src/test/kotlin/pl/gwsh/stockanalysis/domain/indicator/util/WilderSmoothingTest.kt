package pl.gwsh.stockanalysis.domain.indicator.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class WilderSmoothingTest {

    private val tol = 1e-6

    @Test
    fun `wilder constant series stays constant after seed`() {
        val input = List(20) { 42.0 }
        val out = wilderSmooth(input, period = 5)
        for (i in 0..3) assertThat(out[i]).isNull()
        for (i in 4..19) assertThat(out[i]!!).isWithin(tol).of(42.0)
    }

    @Test
    fun `wilder hand calculation on simple input matches recurrence`() {
        // ref na [1.0, 2.0, ..., 10.0] z period=4:
        //   seed at i=3: (1+2+3+4)/4 = 2.5
        //   i=4: (2.5*3 + 5)/4 = (7.5+5)/4 = 12.5/4 = 3.125
        //   i=5: (3.125*3 + 6)/4 = (9.375+6)/4 = 15.375/4 = 3.84375
        //   i=6: (3.84375*3 + 7)/4 = (11.53125+7)/4 = 18.53125/4 = 4.6328125
        //   i=7: (4.6328125*3 + 8)/4 = (13.8984375+8)/4 = 21.8984375/4 = 5.474609375
        //   i=8: (5.474609375*3 + 9)/4 = (16.423828125+9)/4 = 25.423828125/4 = 6.355957031
        //   i=9: (6.355957031*3 + 10)/4 = (19.067871094+10)/4 = 29.067871094/4 = 7.266967773
        val out = wilderSmooth((1..10).map(Int::toDouble), period = 4)
        assertThat(out).hasSize(10)
        for (i in 0..2) assertThat(out[i]).isNull()
        assertThat(out[3]!!).isWithin(tol).of(2.5)
        assertThat(out[4]!!).isWithin(tol).of(3.125)
        assertThat(out[5]!!).isWithin(tol).of(3.84375)
        assertThat(out[6]!!).isWithin(tol).of(4.6328125)
        assertThat(out[7]!!).isWithin(tol).of(5.474609375)
        assertThat(out[8]!!).isWithin(tol).of(6.355957031)
        assertThat(out[9]!!).isWithin(tol).of(7.266967773)
    }

    @Test
    fun `wilder length equals input length`() {
        assertThat(wilderSmooth((1..30).map(Int::toDouble), period = 14)).hasSize(30)
    }

    @Test
    fun `wilder with input shorter than period returns all nulls`() {
        val out = wilderSmooth(listOf(1.0, 2.0), period = 5)
        assertThat(out).containsExactly(null, null).inOrder()
    }

    @Test
    fun `wilder with non-positive period throws`() {
        assertThrows<IllegalArgumentException> { wilderSmooth(listOf(1.0), period = 0) }
        assertThrows<IllegalArgumentException> { wilderSmooth(listOf(1.0), period = -3) }
    }
}
