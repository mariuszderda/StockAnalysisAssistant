package pl.gwsh.stockanalysis.domain.indicator

import pl.gwsh.stockanalysis.domain.model.Candle

/**
 * **Wzorzec Strategy.** Wspolny interfejs dla wymiennych algorytmow
 * obliczania wskaznikow technicznych. Kazda konkretna implementacja
 * (`RsiStrategy`, `MacdStrategy`, `SmaStrategy`, `EmaStrategy`) hermetyzuje
 * jeden algorytm i moze byc niezalezne testowana / wymieniana.
 *
 * Klient ([pl.gwsh.stockanalysis.presentation.screens.chart.ChartViewModel])
 * widzi tylko ten interfejs — nie wie, ze pod spodem siedzi konkretna
 * matematyka Wildera czy EMA. Dodanie nowego wskaznika = nowa implementacja
 * + branch w fabryce. ViewModel i UI bez zmian.
 *
 * Dlaczego Strategy a nie `when (type)` w jednej duzej funkcji?
 *  - test-isolation: kazda strategia ma swoj plik testowy z odpowiednia
 *    referencja matematyczna (RSI: Wilder 1978, SMA: hand-calc, ...).
 *  - OCP: dodanie wskaznika nie wymaga edycji istniejacych algorytmow.
 *  - czytelnosc: 30-liniowa klasa per indykator > 200-liniowa funkcja.
 */
interface IndicatorStrategy {
    /** Klasyfikacja UI — patrz [IndicatorType]. */
    val type: IndicatorType

    /** Nazwa wyswietlana w panelu wskaznikow i w legendzie wykresu. */
    val displayName: String

    /**
     * Zwraca [IndicatorResult] zsynchronizowany 1:1 z `candles` po indeksie.
     * Brak wartosci (np. dla pierwszych N-1 swiec) reprezentowany jako
     * `null` w odpowiednim miejscu listy — nie wolno pomijac indeksow.
     */
    fun compute(candles: List<Candle>): IndicatorResult
}
