package pl.gwsh.stockanalysis.presentation.common

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class NumberFormattersTest {

    @Test
    fun `formatPrice uses Polish locale with comma decimal separator`() {
        assertThat(formatPrice(123.4)).isEqualTo("123,40")
        assertThat(formatPrice(0.05)).isEqualTo("0,05")
    }

    @Test
    fun `formatPrice keeps two decimals on large numbers`() {
        // Polski locale: separator tysiecy = spacja (U+00A0 non-breaking).
        val formatted = formatPrice(1234567.89)
        assertThat(formatted).endsWith("89")
        assertThat(formatted).contains("1")
        assertThat(formatted).contains(",")
    }

    @Test
    fun `formatVolume small numbers raw`() {
        assertThat(formatVolume(0L)).isEqualTo("0")
        assertThat(formatVolume(999L)).isEqualTo("999")
    }

    @Test
    fun `formatVolume thousands as K`() {
        assertThat(formatVolume(1_000L)).isEqualTo("1,0K")
        assertThat(formatVolume(12_500L)).isEqualTo("12,5K")
    }

    @Test
    fun `formatVolume millions as M`() {
        assertThat(formatVolume(1_500_000L)).isEqualTo("1,50M")
        assertThat(formatVolume(250_000_000L)).isEqualTo("250,00M")
    }

    @Test
    fun `formatVolume billions as B`() {
        assertThat(formatVolume(2_500_000_000L)).isEqualTo("2,50B")
    }
}
