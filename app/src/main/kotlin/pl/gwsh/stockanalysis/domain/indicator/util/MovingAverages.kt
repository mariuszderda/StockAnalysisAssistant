package pl.gwsh.stockanalysis.domain.indicator.util

/**
 * Simple Moving Average — krocząca srednia arytmetyczna na oknie `period`.
 *
 * Definicja klasyczna (Murphy, "Technical Analysis of the Financial Markets",
 * rozdz. 9): `SMA[i] = (v[i-period+1] + ... + v[i]) / period` dla
 * `i >= period - 1`, `null` dla `i < period - 1`.
 *
 * @param values  ciag wartosci, np. ceny zamkniecia.
 * @param period  rozmiar okna; musi byc dodatni.
 * @return lista o tej samej dlugosci co `values`, z `period - 1` wiodacymi
 *         `null` oraz wartosciami SMA dla pozostalych indeksow.
 */
fun sma(values: List<Double>, period: Int): List<Double?> {
    require(period > 0) { "period must be positive, was $period" }
    val n = values.size
    val out = arrayOfNulls<Double>(n)
    if (n < period) return out.toList()

    // Rolling-sum window — O(n) zamiast O(n*period).
    var window = 0.0
    for (i in 0 until period) window += values[i]
    out[period - 1] = window / period
    for (i in period until n) {
        window += values[i] - values[i - period]
        out[i] = window / period
    }
    return out.toList()
}

/**
 * Exponential Moving Average — wykladnicza srednia krocząca z wagą
 * `k = 2 / (period + 1)`.
 *
 * Standard nauczany w literaturze (Murphy, rozdz. 9; Achelis, "Technical
 * Analysis from A to Z"): pierwsza wartosc to SMA z pierwszych `period`
 * obserwacji (seed), nastepnie rekurencja:
 *
 * ```
 * EMA[i] = values[i] * k + EMA[i-1] * (1 - k),    k = 2 / (period + 1)
 * ```
 *
 * Wagi sumuja sie do 1, najnowsza obserwacja ma najwieksza wage. To NIE
 * jest "Wilder smoothing" — Wilder uzywa k = 1/period, patrz [wilderSmooth].
 *
 * @param values  ciag wartosci.
 * @param period  rozmiar okna seeda (i jednoczesnie parametr wagi); > 0.
 * @return lista o tej samej dlugosci co `values`, z `period - 1` wiodacymi
 *         `null`, wartoscia SMA seed w indeksie `period - 1`, dalej EMA.
 */
fun ema(values: List<Double>, period: Int): List<Double?> {
    require(period > 0) { "period must be positive, was $period" }
    val n = values.size
    val out = arrayOfNulls<Double>(n)
    if (n < period) return out.toList()

    var seed = 0.0
    for (i in 0 until period) seed += values[i]
    seed /= period
    out[period - 1] = seed

    val k = 2.0 / (period + 1)
    var prev = seed
    for (i in period until n) {
        val next = values[i] * k + prev * (1 - k)
        out[i] = next
        prev = next
    }
    return out.toList()
}
