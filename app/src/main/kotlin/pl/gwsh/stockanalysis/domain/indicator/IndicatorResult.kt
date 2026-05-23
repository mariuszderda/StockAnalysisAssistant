package pl.gwsh.stockanalysis.domain.indicator

/**
 * Wynik obliczenia wskaznika dla konkretnej serii swiec.
 *
 * **Inwariant alignmentu (kluczowy dla renderowania):** indeks `i` w
 * `values` / `lines[*]` odpowiada dokladnie indeksowi swiecy `candles[i]`,
 * dla ktorej wynik zostal policzony. Brak wartosci (np. okres jeszcze nie
 * osiagniety) reprezentujemy jako `null` — **nigdy nie pomijamy indeksu**,
 * bo wykres polegalby na zgodnosci osi X miedzy seria ceny a seria
 * wskaznika.
 *
 * Wybor sealed + dwa warianty:
 * - [SingleLine] — RSI(14), SMA(20), EMA(20) — jedna seria liczb.
 * - [MultiLine] — MACD (linie: macd / signal / histogram), Bollinger
 *   (linie: upper / mid / lower). `Map` z nazwami klucza, bo kolejnosc
 *   linii nie ma znaczenia dla matematyki, a UI moze chciec rysowac je
 *   roznymi kolorami / w roznej kolejnosci warstw.
 */
sealed class IndicatorResult {
    abstract val name: String
    abstract val type: IndicatorType

    data class SingleLine(
        override val name: String,
        override val type: IndicatorType,
        val values: List<Double?>,
    ) : IndicatorResult()

    data class MultiLine(
        override val name: String,
        override val type: IndicatorType,
        val lines: Map<String, List<Double?>>,
    ) : IndicatorResult()
}
