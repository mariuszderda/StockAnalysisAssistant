package pl.gwsh.stockanalysis.data.mapper

import pl.gwsh.stockanalysis.data.remote.dto.TimeSeriesResponseDto
import pl.gwsh.stockanalysis.data.remote.dto.TimeSeriesValueDto
import pl.gwsh.stockanalysis.domain.model.Candle
import java.time.LocalDate

/**
 * Top-level mappery DTO Twelve Data → domena. Rzucaja `DateTimeParseException`
 * lub `NumberFormatException` przy niepoprawnym formacie; warstwa repository
 * przejmuje i konwertuje na [pl.gwsh.stockanalysis.domain.model.DataError.ParseError].
 */
fun TimeSeriesValueDto.toCandle(): Candle = Candle(
    date = LocalDate.parse(datetime),
    open = open.toDouble(),
    high = high.toDouble(),
    low = low.toDouble(),
    close = close.toDouble(),
    volume = volume.toLong(),
)

/**
 * Mapuje cale ciało odpowiedzi `/time_series`. Twelve Data zwraca swiece w
 * kolejnosci malejacej (najnowsza pierwsza); UI i wskazniki potrzebuja
 * rosnacej — sortujemy raz na granicy systemu.
 */
fun TimeSeriesResponseDto.toCandles(): List<Candle> =
    values.orEmpty()
        .map { it.toCandle() }
        .sortedBy { it.date }
