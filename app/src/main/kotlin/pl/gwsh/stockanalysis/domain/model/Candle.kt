package pl.gwsh.stockanalysis.domain.model

import java.time.LocalDate

/**
 * Pojedyncza świeca OHLC z danej sesji. Wolumen jako `Long` — Twelve Data
 * potrafi zwrocic wartosci > Int.MAX_VALUE dla popularnych indeksow.
 *
 * Niezmiennik biznesowy: `low <= open, close <= high`. Nie wymuszamy go w
 * konstruktorze, bo mapper z DTO musi obsluzyc tez dane potencjalnie skorumpowane
 * (zwraca wtedy ParseError przez DataError, patrz [DataError.ParseError]).
 */
data class Candle(
    val date: LocalDate,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
)
