package pl.gwsh.stockanalysis.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class RangeTest {

    @Test
    fun `kazdy zakres ma dodatni outputSize i minExpectedCandles`() {
        Range.entries.forEach { range ->
            assertThat(range.outputSize).isGreaterThan(0)
            assertThat(range.minExpectedCandles).isGreaterThan(0)
        }
    }

    @Test
    fun `minExpectedCandles nie przekracza outputSize`() {
        Range.entries.forEach { range ->
            assertThat(range.minExpectedCandles).isAtMost(range.outputSize)
        }
    }

    @Test
    fun `outputSize rosnie wraz z dlugoscia zakresu`() {
        assertThat(Range.ONE_MONTH.outputSize).isLessThan(Range.THREE_MONTHS.outputSize)
        assertThat(Range.THREE_MONTHS.outputSize).isLessThan(Range.ONE_YEAR.outputSize)
    }
}
