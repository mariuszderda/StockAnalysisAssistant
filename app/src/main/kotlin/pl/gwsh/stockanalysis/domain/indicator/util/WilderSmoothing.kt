package pl.gwsh.stockanalysis.domain.indicator.util

/**
 * Wilder smoothing (RMA — Running / Wilder's Moving Average).
 *
 * Wprowadzone przez J. Welles'a Wildera w "New Concepts in Technical
 * Trading Systems" (1978). Wykorzystywane w jego oryginalnych
 * wskaznikach: RSI, ATR, ADX.
 *
 * **Roznica wzgledem EMA:** Wilder uzywa wagi `alpha = 1 / period`, a EMA
 * (Murphy / Achelis) `alpha = 2 / (period + 1)`. Ta sama struktura
 * rekurencji, inna stala — Wilder gladzi mocniej (dluzsza pamiec).
 *
 * Algorytm:
 *  1. Pierwsza wartosc (seed) w indeksie `period - 1`: arytmetyczna srednia
 *     pierwszych `period` obserwacji.
 *  2. Kolejne: `avg[i] = (avg[i-1] * (period - 1) + values[i]) / period`.
 *
 * Wzor 2 wyglada inaczej niz EMA, ale jest matematycznie rownowazny
 * `avg[i] = alpha * values[i] + (1 - alpha) * avg[i-1]` przy
 * `alpha = 1 / period` — wymnozenie przez `period / period` daje
 * `(values[i] + (period - 1) * avg[i-1]) / period`.
 *
 * Uzywany w SAA wylacznie przez RSI; gdyby doszedl ATR/ADX — wystarczy
 * ta sama funkcja.
 *
 * @param values  ciag obserwacji.
 * @param period  okres; > 0.
 * @return lista o tej samej dlugosci co `values`, z `period - 1` wiodacymi
 *         `null`, seed w `period - 1`, dalej rekurencja Wildera.
 */
fun wilderSmooth(values: List<Double>, period: Int): List<Double?> {
    require(period > 0) { "period must be positive, was $period" }
    val n = values.size
    val out = arrayOfNulls<Double>(n)
    if (n < period) return out.toList()

    var seed = 0.0
    for (i in 0 until period) seed += values[i]
    seed /= period
    out[period - 1] = seed

    var prev = seed
    for (i in period until n) {
        val next = (prev * (period - 1) + values[i]) / period
        out[i] = next
        prev = next
    }
    return out.toList()
}
