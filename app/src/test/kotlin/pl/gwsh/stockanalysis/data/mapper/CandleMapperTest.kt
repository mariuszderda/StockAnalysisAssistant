package pl.gwsh.stockanalysis.data.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.gwsh.stockanalysis.data.remote.dto.TimeSeriesResponseDto
import pl.gwsh.stockanalysis.data.remote.dto.TimeSeriesValueDto
import java.time.LocalDate
import java.time.format.DateTimeParseException

class CandleMapperTest {

    @Test
    fun `happy path - DTO mapuje sie 1 do 1`() {
        val dto = TimeSeriesValueDto(
            datetime = "2026-05-20",
            open = "172.45",
            high = "173.10",
            low = "171.90",
            close = "172.88",
            volume = "55432100",
        )

        val candle = dto.toCandle()

        assertThat(candle.date).isEqualTo(LocalDate.of(2026, 5, 20))
        assertThat(candle.open).isEqualTo(172.45)
        assertThat(candle.high).isEqualTo(173.10)
        assertThat(candle.low).isEqualTo(171.90)
        assertThat(candle.close).isEqualTo(172.88)
        assertThat(candle.volume).isEqualTo(55_432_100L)
    }

    @Test
    fun `zla data rzuca DateTimeParseException`() {
        val dto = TimeSeriesValueDto(
            datetime = "20-05-2026",
            open = "1", high = "1", low = "1", close = "1", volume = "1",
        )
        assertThrows<DateTimeParseException> { dto.toCandle() }
    }

    @Test
    fun `nieprawidlowa liczba rzuca NumberFormatException`() {
        val dto = TimeSeriesValueDto(
            datetime = "2026-05-20",
            open = "abc", high = "1", low = "1", close = "1", volume = "1",
        )
        assertThrows<NumberFormatException> { dto.toCandle() }
    }

    @Test
    fun `toCandles sortuje rosnaco po dacie`() {
        // Twelve Data zwraca malejaco — celowo zaburzona kolejnosc na wejsciu.
        val resp = TimeSeriesResponseDto(
            values = listOf(
                value("2026-05-22"),
                value("2026-05-20"),
                value("2026-05-21"),
            ),
        )

        val candles = resp.toCandles()

        assertThat(candles.map { it.date }).containsExactly(
            LocalDate.of(2026, 5, 20),
            LocalDate.of(2026, 5, 21),
            LocalDate.of(2026, 5, 22),
        ).inOrder()
    }

    @Test
    fun `toCandles na pustym values zwraca pusta liste`() {
        val resp = TimeSeriesResponseDto(values = null)
        assertThat(resp.toCandles()).isEmpty()
    }

    private fun value(date: String) = TimeSeriesValueDto(
        datetime = date,
        open = "1.0", high = "1.0", low = "1.0", close = "1.0", volume = "1",
    )
}
